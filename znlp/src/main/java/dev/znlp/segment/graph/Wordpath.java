package dev.znlp.segment.graph;

import dev.znlp.base.common.AbstractIterator;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.segment.Nature;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * 词路径（Wordpath）表示对文本进行词划分的描述。
 * 
 * <p>该类使用{@link BitSet}数据结构来表示文本的切分方式，具有以下特点：
 * <ul>
 *   <li>BitSet下标[i]为true，表示在第i个字符后插入分割符</li>
 *   <li>BitSet下标[i]为false，表示第i个字符后不插入分割符</li>
 * </ul>
 * 
 * <h3>设计优势：</h3>
 * <ul>
 *   <li>性能优越：基于BitSet的位运算，操作高效</li>
 *   <li>空间紧凑：使用位存储，内存占用少</li>
 *   <li>灵活方便：支持随时合并、分裂、拆分操作</li>
 *   <li>易于扩展：可以方便地进行路径的修改和优化</li>
 * </ul>
 *
 * @author jimichan jimichan@gmail.com
 */
public class Wordpath {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Wordpath.class);

    /**
     * 用于表示文本切分方式的BitSet。
     * bitset[i]为true表示在第i个字符后插入分割符。
     */
    private final BitSet bitSet;

    /**
     * 文本的长度（字符数量）。
     */
    private final int length;

    /**
     * 当前词路径所属的词图。
     */
    private final Wordnet wordnet;

    /**
     * 每个字符上已强制成词的优先级。0 表示还没有约束声明过这段。
     */
    private int[] forcedPriority;


    /**
     * 创建一个新的词路径，基于指定的词图。
     * 
     * <p>默认情况下，每个字符后都插入分割符（即默认将文本切分为单个字符）。
     * 
     * @param wordnet 词路径所属的词图
     */
    public Wordpath(Wordnet wordnet) {
        this.wordnet = wordnet;
        this.length = wordnet.length();
        this.bitSet = new BitSet(length);

        // 每个字符后面都有切分符号，默认切成单字
        bitSet.set(0, length);
    }

    /**
     * 获取当前词路径所属的词图。
     * 
     * @return 词图对象
     */
    public Wordnet getWordnet() {
        return wordnet;
    }

    /**
     * 获取包含起始节点和结束节点的最优路径。
     * <p>
     * 按「起始节点 → 路径中的词 → 结束节点」惰性产出，不预先构造列表。
     *
     * @return 包含起始节点、中间词节点和结束节点的完整路径
     */
    public Iterable<Vertex> getBestPathWithBE() {
        Iterable<Vertex> b = Collections.singletonList(wordnet.getBeginRow().getFirst());
        Iterable<Vertex> m = this::iteratorVertex;
        Iterable<Vertex> e = Collections.singletonList(wordnet.getEndRow().getFirst());

        return new Iterable<Vertex>() {
            @Override
            public Iterator<Vertex> iterator() {
                return new Iterator<Vertex>() {
                    private Iterator<Vertex> current = b.iterator();
                    private int stage = 0;

                    @Override
                    public boolean hasNext() {
                        while (!current.hasNext() && stage < 2) {
                            stage++;
                            if (stage == 1) {
                                current = m.iterator();
                            } else if (stage == 2) {
                                current = e.iterator();
                            }
                        }
                        return current.hasNext();
                    }

                    @Override
                    public Vertex next() {
                        if (!hasNext()) {
                            throw new java.util.NoSuchElementException();
                        }
                        return current.next();
                    }
                };
            }
        };
    }

    /**
     * 返回最优路径的迭代器，不包含起始节点和结束节点。
     * 
     * <p>该迭代器允许按需获取路径中的词节点，而无需预先构建完整的路径列表，
     * 从而节省内存并提高性能。
     * 
     * @return 最优路径的迭代器（不包含开始和结束节点）
     */
    public Iterator<Vertex> iteratorVertex() {
        return new AbstractIterator<Vertex>() {

            private final WordPointer pointer = wordPointer();


            @Override
            protected void computeNext() {

                boolean hasNext = pointer.next();

                if (!hasNext) {
                    done();
                    return;
//                    return endOfData();
                }

                final int from = pointer.getFrom();
                final int len = pointer.getLen();

                Vertex theVertex = wordnet.getVertex(from, len);

                if (theVertex == null) {
                    // System.out.println(wordnet.toMoreString());
                    //@ RepairWordnetProcessor 这里去修复了这个错误，到时要在之前去调用
//                    logger.error("row: " + from + " len " + len + " select is null"
//                            +"\nInput ");
                    Vertex tmp = wordnet.put(from, len);
                    tmp.nature = Nature.x;

                    setNext(tmp);
                    return;
//                    return tmp;
//                    throw new IllegalStateException("row: " + from + " len " + len + " select is null");
                }

                setNext(theVertex);
            }
        };
    }


    /**
     * 划定并连接指定范围内的词汇。
     * 
     * <p>该方法会：
     * <ol>
     *   <li>更新词路径的切分方式，将从from位置开始的length个字符合并为一个词</li>
     *   <li>如果词图中不存在该词，则自动创建并添加到词图中</li>
     *   <li>返回合并后的词节点</li>
     * </ol>
     * 
     * @param from   词的起始位置
     * @param length 词的长度
     * @return 合并后的词节点
     */
    public Vertex combine(int from, int length) {
        this.connect(from, length);
        if (wordnet.isNotContains(from, length)) {
            return wordnet.put(from, length);
        } else {
            return wordnet.getVertex(from, length);
        }
    }

    /**
     * 划定并连接指定的词节点。
     * 
     * <p>最优路径算法一般会调用这个方法来划定词语。
     * 
     * @param vertex 要连接的词节点
     */
    public void combine(Vertex vertex) {
        this.connect(vertex.getRowNum(), vertex.length());
    }

    /**
     * 寻找词路径中存在但词图中不存在的词，并自动创建对应的词节点。
     * 
     * <p>对于每个自动创建的词节点，会调用consumer进行属性设置。
     * 
     * @param consumer 用于设置新创建词节点属性的消费者函数
     */
    public void findunloadVertext(Consumer<Vertex> consumer) {
        final int lastIndex = length - 1;
        // 0 1 2 3 4 5 6
        // 0 1 1 0 1 1 1
        //bitSet.nextSetBit()
//        printlnBitSet(bitSet);
        for (int i = 0; i < length; ) {
            int nextSplitIndex = bitSet.nextSetBit(i);

            if (i == lastIndex) {
                if (wordnet.isNotContains(i, 1)) {
                    // 一个点
                    consumer.accept(wordnet.put(i, 1));
                }
                i++;
            } else {
                int len = nextSplitIndex - i + 1;
                if (wordnet.isNotContains(i, len)) {
                    // 一个点
                    consumer.accept(wordnet.put(i, len));

                }
                i += len;
            }
        }
    }

    /**
     * 计算词路径中包含的词的数量。
     * 
     * @return 词的数量
     */
    public int wordCount() {
        // BitSet的cardinality()方法返回true位的数量，即词的数量
        return bitSet.cardinality();
    }

    /**
     * 获取词路径中的词的数量（与wordCount()方法相同）。
     * 
     * @return 词的数量
     */
    public int wordCountInPath() {
        return this.wordCount();
    }

    /**
     * 词指针类，用于遍历词路径中的词。
     * 
     * <p>该类提供了一种高效的方式来遍历词路径中的所有词，无需预先构建完整的词列表。
     */
    public class WordPointer {
        /**
         * 当前词的起始位置。
         */
        private int from;
        
        /**
         * 当前词的长度。
         */
        private int len;

        private final int lastIndex = length - 1;
        private int i = 0;

        /**
         * 创建一个新的词指针。
         */
        public WordPointer() {
        }

        /**
         * 获取当前词的起始位置。
         * 
         * @return 当前词的起始位置
         */
        public int getFrom() {
            return from;
        }

        /**
         * 获取当前词的长度。
         * 
         * @return 当前词的长度
         */
        public int getLen() {
            return len;
        }

        /**
         * 移动到下一个词。
         * 
         * @return 如果成功移动到下一个词则返回true，否则返回false
         */
        public boolean next() {
            int nextSplitIndex = Wordpath.this.bitSet.nextSetBit(i);

            if (i >= length) {
                return false;
            }

            if (i == lastIndex) {
                this.from = i;
                this.len = 1;
                i++;

            } else {
                int le = nextSplitIndex - i + 1;
                this.from = i;
                this.len = le;
                i += le;
            }

            return true;
        }
    }

    /**
     * 创建一个新的词指针，用于遍历词路径中的词。
     * 
     * @return 新创建的词指针
     */
    public WordPointer wordPointer() {
        return new WordPointer();
    }

    /**
     * 划定并连接指定范围内的词语。
     * 
     * <p>该方法会更新词路径的切分方式：
     * <ol>
     *   <li>设置词语起始位置前的分割符（如果起始位置不是0）</li>
     *   <li>设置词语结束位置后的分割符</li>
     *   <li>清除词语内部的所有分割符</li>
     * </ol>
     * 
     * @param from   词语的起始位置
     * @param length 词语的长度
     */
    private void connect(int from, int length) {
        if (length <= 0 || from < 0) {
            return;
        }
        
        // 设置词语起始位置前的分割符（如果起始位置不是0）
        if (from > 0) {
            bitSet.set(from - 1);
        }
        
        // 设置词语结束位置后的分割符
        bitSet.set(from + length - 1);

        // 清除词语内部的所有分割符
        bitSet.set(from, from + length - 1, false);
    }

    /**
     * 检查合并指定范围的词语是否会打断现有的分词结果。
     * 
     * <p>例如：
     * <ul>
     *   <li>对于 "AA B CC D E"，可以联合 "BCC" 而不打断其他分词</li>
     *   <li>但不能联合 "ABCC"，因为会打断 "AA"</li>
     * </ul>
     * 
     * @param from 词语的起始位置
     * @param len 词语的长度
     * @return true表示会破坏现有分词，false表示不会破坏
     */
    /**
     * 这段目前是不是还由单字组成，没有被选路或约束合并过。
     */
    public boolean isUnmergedSpan(int from, int len) {
        if (len <= 1 || from < 0 || from + len > length) {
            return false;
        }
        if (from > 0 && !bitSet.get(from - 1)) {
            return false;
        }
        for (int i = from; i < from + len - 1; i++) {
            if (!bitSet.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 约束能否把 {@code [from, from+len)} 收成词。
     * 不能从已有词的中间切开，也不能覆盖更高优先级已经强制过的字符。
     */
    public boolean canForce(int from, int len, int priority) {
        if (len <= 0 || from < 0 || from + len > length) {
            return false;
        }
        if (willCutOtherWords(from, len)) {
            return false;
        }
        if (forcedPriority == null) {
            return true;
        }
        for (int i = from; i < from + len; i++) {
            if (forcedPriority[i] > priority) {
                return false;
            }
        }
        return true;
    }

    /**
     * 合并跨度，并记下优先级，供后来的约束避让。
     */
    public Vertex force(int from, int len, int priority) {
        Vertex vertex = combine(from, len);
        markForced(from, len, priority);
        return vertex;
    }

    public void markForced(int from, int len, int priority) {
        if (len <= 0 || from < 0 || from + len > length) {
            return;
        }
        if (forcedPriority == null) {
            forcedPriority = new int[length];
        }
        for (int i = from; i < from + len; i++) {
            if (forcedPriority[i] < priority) {
                forcedPriority[i] = priority;
            }
        }
    }

    /**
     * 判断把 {@code [from, from+len)} 收成一个词，是否会切开已经选定的邻词。
     * <p>
     * 只看跨度的左右两端有没有切分点，不看跨度内部：因此允许把 {@code 南京/市}
     * 合成 {@code 南京市}，但不允许在 {@code 南京市} 中间插一刀。
     * 调用方需先自行校验 {@code len > 0} 与下标范围（见 {@link #canForce}）。
     *
     * @param from 词的起始位置
     * @param len  词的长度
     * @return true 表示会打断左侧或右侧已有的词
     */
    public boolean willCutOtherWords(int from, int len) {
        // 起始位置前没有分割符，会打断前面的词
        if (from != 0 && !bitSet.get(from - 1)) {
            return true;
        }

        // 结束位置后没有分割符，会打断后面的词
        int to = from + len - 1;
        return !bitSet.get(to);
    }

    @Override
    public String toString() {
        int last = bitSet.length() - 1;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < wordnet.length(); i++) {
            sb.append(wordnet.charAt(i));
            if (bitSet.get(i) && i != last) {
                sb.append(" | ");
            }
        }
        return sb.toString();
    }

    /**
     * 获取表示词路径切分方式的BitSet。
     * 
     * @return 词路径的BitSet表示
     */
    public BitSet getBitSet() {
        return bitSet;
    }

    /**
     * 重置词路径的切分方式，恢复默认状态（每个字符后都有分割符）。
     */
    public void reset() {
        this.bitSet.set(0, length);
    }
}
