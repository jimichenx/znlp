package dev.znlp.segment.graph;

import dev.znlp.base.common.ZeroCopyString;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 词图（Wordnet）核心数据结构，用于中文分词的图结构表示。
 * 
 * <p>词图是指句子中所有可能的词构成的图结构，是分词算法的基础数据结构。
 * 每个字符位置对应一个{@link VertexRow}，包含该位置所有可能的词（{@link Vertex}）。
 * 
 * <h3>数据结构示例：</h3>
 * <pre>
 * #S# [0] #Start 品 [1] 品质 质 [2] 质 和 [3] {和、和服} 服 [4] 服务 务 [5] 务 #E# [6] #End
 * </pre>
 * 
 * <h3>智能下标（sindex）设计：</h3>
 * <ul>
 *   <li>-1 表示词图的起始位置（Start节点）</li>
 *   <li>0 到 charSize-1 表示原始文本的字符位置</li>
 *   <li>charSize 表示词图的结束位置（End节点）</li>
 * </ul>
 * 
 * <h3>设计亮点：</h3>
 * <ul>
 *   <li>实现{@link CharSequence}接口，可以作为字符串处理</li>
 *   <li>零拷贝优化：直接使用原始char[]数组，避免字符串复制</li>
 *   <li>内存优化：去除中间节点，使用链表直接连接，减少对象创建</li>
 *   <li>智能下标：统一处理普通字符位置和特殊的Start/End节点</li>
 * </ul>
 * 
 * @author jimichan
 */
public final class Wordnet implements CharSequence {

    /**
     * 词图的槽位数组，每个字符位置对应一个{@link VertexRow}。
     * slotList[i]对应原始文本中第i个字符位置的所有可能词。
     */
    private VertexRow[] slotList;

    /**
     * 词图的起始行（Start节点），智能下标为 -1。
     */
    private final VertexRow begin;

    /**
     * 词图的结束行（End节点），智能下标为 charSize。
     */
    private final VertexRow end;

    /**
     * 原始句子对应的字符数组，直接引用避免字符串复制（零拷贝优化）。
     */
    char[] charArray;

    /**
     * 本窗口在 {@link #charArray} 中的起点。顶点下标相对窗口，不相对整篇原文。
     */
    int origin;

    /**
     * 窗口长度。
     */
    private int charSize;

    private Vertex[] recycled = new Vertex[64];
    private int recycledCount;
    private int activeRows;

    /**
     * 词图上下文存储，用于传递额外的信息（3.1.0新增）。
     */
    private Map<String, Object> context = null;

    /**
     * 创建子词图，从父词图的指定范围提取子图。
     * 
     * @param parent 父词图
     * @param from   起始位置（包含）
     * @param length 子词图的长度
     */
    private Wordnet(Wordnet parent, int from, int length) {
        this.charArray = Arrays.copyOfRange(parent.charArray, parent.origin + from, parent.origin + from + length);
        this.origin = 0;
        this.charSize = length;

        // 初始化起始和结束行
        this.begin = new VertexRow(-1, this);
        this.end = new VertexRow(charSize, this);

        // 初始化槽位数组并复制父词图的节点信息
        slotList = new VertexRow[charSize];
        for (int i = 0; i < length; i++) {
            VertexRow row = new VertexRow(i, this);
            for (Vertex v : parent.slotList[from + i]) {
                row.getOrCrete(v.length);
            }
            slotList[i] = row;
        }

        // 添加起始和结束节点
        getBeginRow().put(VertexHelper.newBegin());
        getEndRow().put(VertexHelper.newEnd());
    }

    /**
     * 构建一个空的词图，用于后续添加词节点。
     * 
     * @param charArray 原始文本的字符数组
     */
    public Wordnet(char[] charArray) {
        this(charArray, 0, charArray.length);
    }

    /**
     * 在原文的 {@code [origin, origin + length)} 上建词图，不复制字符。
     */
    public Wordnet(char[] charArray, int origin, int length) {
        this.charArray = charArray;
        this.origin = origin;
        this.charSize = length;

        // 初始化起始和结束行
        this.begin = new VertexRow(-1, this);
        this.end = new VertexRow(charSize, this);

        // 创建并初始化槽位数组
        slotList = new VertexRow[charSize];
        for (int i = 0; i < charSize; i++) {
            slotList[i] = new VertexRow(i, this);
        }

        // 添加起始和结束节点
        getBeginRow().put(VertexHelper.newBegin());
        getEndRow().put(VertexHelper.newEnd());
    }

    /**
     * 创建子词图，从当前词图的指定范围提取子图。
     * 
     * @param from   起始位置（包含）
     * @param length 子词图的长度
     * @return 新创建的子词图
     */
    public Wordnet subWordnet(int from, int length) {
        return new Wordnet(this, from, length);
    }

    /**
     * 补齐词图中的空洞，确保Viterbi等路径算法能够从起点走到终点。
     * 对于没有任何词的字符位置，自动添加长度为1的词节点。
     */
    public void fillNill() {
        for (int i = 0; i < charSize; i++) {
            VertexRow row = slotList[i];
            if (row.isEmpty()) {
                Vertex vertex = row.getOrCrete(1);
                vertex.freq = 1;
            }
        }
    }

    /**
     * 把这块词图绑到新的句窗口，并回收上一句的顶点。只给当前线程复用。
     */
    public void prepare(char[] text, int origin, int length) {
        if (slotList.length < length) {
            int capacity = Math.max(length, slotList.length * 2);
            VertexRow[] grown = new VertexRow[capacity];
            System.arraycopy(slotList, 0, grown, 0, slotList.length);
            for (int i = slotList.length; i < capacity; i++) {
                grown[i] = new VertexRow(i, this);
            }
            slotList = grown;
        }
        for (int i = 0; i < activeRows; i++) {
            slotList[i].releaseInto(this);
        }
        this.charArray = text;
        this.origin = origin;
        this.charSize = length;
        this.activeRows = length;
        clearSentinel(begin);
        clearSentinel(end);
        if (context != null) {
            context.clear();
        }
    }

    public Vertex acquire(int length, int wordId, int freq) {
        Vertex vertex;
        if (recycledCount > 0) {
            vertex = recycled[--recycledCount];
            recycled[recycledCount] = null;
            vertex.reuse(length, wordId, freq);
        } else {
            vertex = new Vertex(length, wordId, freq);
        }
        return vertex;
    }

    void recycle(Vertex vertex) {
        vertex.detach();
        if (recycledCount == recycled.length) {
            recycled = Arrays.copyOf(recycled, recycled.length * 2);
        }
        recycled[recycledCount++] = vertex;
    }

    private static void clearSentinel(VertexRow row) {
        int n = row.size();
        for (int i = 0; i < n; i++) {
            row.vertexAt(i).clearRoute();
        }
    }

    /**
     * 寻找词图中没有被任何词覆盖的字符位置。
     * 
     * @return {@link BitSet}，其中为true的位表示被词覆盖的位置
     */
    public BitSet findNoOverWords() {
        BitSet noOverWords = new BitSet();

        // 遍历所有词，标记它们覆盖的字符位置
        for (int i = 0; i < charSize; i++) {
            VertexRow row = slotList[i];
            if (row != null) {
                int rowSize = row.size();
                for (int vIndex = 0; vIndex < rowSize; vIndex++) {
                    Vertex p = row.vertexAt(vIndex);
                    noOverWords.set(row.rowNum, row.rowNum + p.length);
                }
            }
        }

        return noOverWords;
    }

    /**
     * 寻找词图中的悬空行（孤悬节点），即可能导致路径中断的节点。
     * 
     * <p>悬空行包括两种情况：
     * <ul>
     *   <li>存在跳转到当前行的路径，但当前行没有跳出节点</li>
     *   <li>根本不存在跳转到当前行的路径（且该位置没有被任何词覆盖）</li>
     * </ul>
     * 
     * @return {@link BitSet}，其中为false的位表示悬空行位置
     */
    public BitSet findDangling() {
        BitSet bitSet = new BitSet(charSize);
        BitSet noOverWords = findNoOverWords();

        // 第一行肯定是有路径跳入的
        bitSet.set(0);

        // 标记所有被词覆盖的位置
        for (int i = 0; i < charSize; i++) {
            VertexRow row = slotList[i];
            if (row != null) {
                int rowSize = row.size();
                for (int vIndex = 0; vIndex < rowSize; vIndex++) {
                    Vertex p = row.vertexAt(vIndex);
                    noOverWords.set(row.rowNum + p.length);
                }
            }
        }

        // 标记有路径跳入的位置
        for (int i = 0; i < charSize; i++) {
            // 如果没人跳入，而且也没被覆盖，则标记为悬空行
            if (!(bitSet.get(i) || noOverWords.get(i))) {
                bitSet.set(i);
            }
        }

        // bitSet中为false的位置是悬空行
        return null;
    }

    /**
     * 获取指定智能下标对应的{@link VertexRow}。
     * 
     * @param sindex 智能下标，-1表示Start节点，charSize表示End节点
     * @return 对应的VertexRow
     */
    public VertexRow getRow(int sindex) {
        return indexAt(sindex);
    }

    /**
     * {@link #getRow(int)}方法的别名。
     * 
     * @param sindex 智能下标，-1表示Start节点，charSize表示End节点
     * @return 对应的VertexRow
     */
    public VertexRow row(int sindex) {
        return indexAt(sindex);
    }

    /**
     * 统计词图中所有节点的数量，包括Start和End节点。
     * 
     * <p>注意：此方法为动态统计，调用时会遍历所有节点，时间复杂度为O(n)。
     * 
     * @return 词图中所有节点的总数（包含Start和End节点）
     */
    public int size() {
        int count = 0;

        // 统计普通字符位置的节点数量
        for (int i = charSize - 1; i >= 0; i--) {
            VertexRow r = slotList[i];
            if (r != null) {
                count += r.size();
            }
        }
        // 统计Start和End节点的数量
        count += this.begin.size();
        count += this.end.size();

        return count;
    }

    /**
     * 根据智能下标获取对应的{@link VertexRow}。
     * 
     * <h3>智能下标映射关系：</h3>
     * <pre>
     * length = 5 (字符数量)
     * 智能下标: -1 0 1 2 3 4 5
     * 字符位置:    0 1 2 3 4
     * 槽位索引:    0 1 2 3 4
     * </pre>
     * 
     * @param sindex 智能下标，-1表示Start节点，charSize表示End节点
     * @return 对应的VertexRow
     */
    private VertexRow indexAt(int sindex) {
        if (sindex == -1) {
            return begin;
        }
        if (sindex == charSize) {
            return end;
        }
        return slotList[sindex];
    }

    /**
     * 获取词图中非空的行数（包含Start和End节点）。
     * 非空行是指包含至少一个词节点的行。
     * 
     * @return 非空行的数量
     */
    public int notNullRowNums() {
        int count = 0;
        
        // 统计普通字符位置的非空行
        for (int i = charSize - 1; i >= 0; i--) {
            VertexRow r = slotList[i];
            if (r != null && !r.isEmpty()) {
                count++;
            }
        }

        // 统计Start和End节点是否非空
        if (!this.begin.isEmpty()) {
            count++;
        }
        if (!this.end.isEmpty()) {
            count++;
        }

        return count;
    }

    ////////////// 顶点操作////////////////

    /**
     * 在指定智能下标位置添加词节点。
     * 
     * @param charOffset 智能下标位置
     * @param vertex     要添加的词节点
     * @return 如果存在相同长度的词节点，则返回被替换的节点；否则返回null
     */
    public Vertex put(int charOffset, Vertex vertex) {
        Vertex replaced = getRow(charOffset).put(vertex);
        if (replaced != null && replaced != vertex) {
            recycle(replaced);
        }
        return replaced;
    }

    /**
     * 在指定偏移量位置创建并添加新的词节点。
     * 
     * @param offset 智能下标位置
     * @param length 词的长度
     * @return 新创建的词节点，可以继续设置其属性
     */
    public Vertex put(int offset, int length) {
        Vertex vertex = acquire(length, -1, 0);
        getRow(offset).put(vertex);
        return vertex;
    }

    /**
     * 获取指定智能下标位置的行首节点。
     * 
     * @param sindex 智能下标位置
     * @return 行首节点，如果行为空则返回null
     */
    public Vertex getRowFirst(int sindex) {
        VertexRow row = row(sindex);
        return row.getFirst();
    }

    /**
     * 获取指定智能下标位置、指定长度的词节点。
     * 
     * @param sindex 智能下标位置
     * @param length 词的长度
     * @return 对应的词节点，如果不存在则返回null
     */
    public Vertex getVertex(int sindex, int length) {
        VertexRow row = row(sindex);
        return row.get(length);
    }

    /**
     * 检查指定智能下标位置是否不包含指定长度的词节点。
     * 
     * @param sindex 智能下标位置
     * @param length 词的长度
     * @return 如果不包含则返回true，否则返回false
     */
    public boolean isNotContains(int sindex, int length) {
        VertexRow row = row(sindex);
        return !row.contains(length);
    }

    /**
     * 获取指定智能下标位置的节点数量。
     * 
     * @param sindex 智能下标位置
     * @return 该行的节点数量，如果行不存在则返回0
     */
    public int sizeInRow(int sindex) {
        VertexRow row = row(sindex);
        if (row == null) {
            return 0;
        }
        return row.size();
    }

    /**
     * 从后向前遍历词图中所有的词节点。
     * 
     * @param consumer 消费者函数，用于处理每个词节点
     */
    public void accessAllVertex(Consumer<Vertex> consumer) {
        for (int i = slotList.length - 1; i >= 0; i--) {
            VertexRow row = slotList[i];
            if (row != null) {
                int rowSize = row.size();
                for (int vIndex = 0; vIndex < rowSize; vIndex++) {
                    Vertex v = row.vertexAt(vIndex);
                    consumer.accept(v);
                }
            }
        }
    }


    /**
     * 返回词图的字符串表示形式。
     * 
     * @return 词图的字符串表示
     */
    @Override
    public String toString() {
        return new WordnetToStringBuilder(this, false).toString();
    }

    /**
     * 返回词图的详细字符串表示形式，包含更多调试信息。
     * 
     * @return 词图的详细字符串表示
     */
    public String toMoreString() {
        return new WordnetToStringBuilder(this, true).toString();
    }

    /**
     * 获取词图的起始行（Start节点）。
     * 
     * @return 起始行
     */
    public VertexRow getBeginRow() {
        return begin;
    }

    /**
     * 获取词图的结束行（End节点）。
     * 
     * @return 结束行
     */
    public VertexRow getEndRow() {
        return end;
    }

    /**
     * 获取原始文本的字符数量。
     * 
     * @return 字符数量
     */
    public int getCharSizeLength() {
        return charSize;
    }

    /**
     * 获取原始文本的字符数组。
     * 
     * @return 字符数组
     */
    public char[] getCharArray() {
        return charArray;
    }

    /**
     * 获取原始文本的长度（字符数量）。
     * 
     * @return 文本长度
     */
    @Override
    public int length() {
        return charSize;
    }

    public int origin() {
        return origin;
    }

    public int limit() {
        return origin + charSize;
    }

    /**
     * 获取指定索引位置的字符。
     * 
     * @param index 字符索引
     * @return 指定位置的字符，如果索引越界则返回空格
     */
    @Override
    public char charAt(int index) {
        if (index < 0 || index >= charSize) {
            return ' ';
        }
        return charArray[origin + index];
    }

    /**
     * 获取指定范围内的子序列。
     * 
     * @param start 起始索引（包含）
     * @param end   结束索引（不包含）
     * @return 子序列
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return new ZeroCopyString(charArray, origin + start, end - start);
    }

    /**
     * 获取词图的槽位数组。
     * 
     * @return 槽位数组
     */
    public VertexRow[] getSlotList() {
        return slotList;
    }

    /**
     * 获取上下文信息。
     * 
     * @param key 上下文键
     * @param <T> 值的类型
     * @return 上下文值，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (context == null) {
            return null;
        } else {
            return (T) context.get(key);
        }
    }

    /**
     * 设置上下文信息。
     * 
     * @param key   上下文键
     * @param value 上下文值
     */
    public void set(String key, Object value) {
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(key, value);
    }

}