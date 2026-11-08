package dev.znlp.base.dat;

import dev.znlp.base.common.DataInputWrapper;
import dev.znlp.base.common.DataOutputWrapper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * DoubleArrayTrie 实现类，提供高效的字符串匹配和前缀搜索功能。
 * <p>
 * 主要功能：
 * - 精确匹配：通过 {@link #indexOf(CharSequence)} 或 {@link #containsKey(String)} 查找字符串
 * - 前缀搜索：通过 {@link #commonPrefixSearch(CharSequence)} 查找所有匹配的前缀
     * - 多匹配：通过 {@link #matcher(String)} 获取匹配器进行多模式匹配
     */
    public class DoubleArrayTrie {

    /**
     * check 数组
     */
    private final int[] check;

    /**
     * base 数组
     */
    private final int[] base;

    /**
     * key 数量（叶子节点数）
     */
    private final int size;

    /**
     * Aho-Corasick 失败指针，按下标为 DAT 节点。未启用时为 null。
     */
    private int[] fail;

    /**
     * 每个 DAT 节点上的输出（词下标，含 fail 链合并）。未启用时为 null。
     */
    private int[][] output;

    /**
     * 各词长度，与构建时的 keys 下标一致。
     */
    private int[] keyLength;

    public DoubleArrayTrie(int size, int[] check, int[] base) {
        this.size = size;
        this.check = check;
        this.base = base;
    }

    public int checkSize() {
        return check.length;
    }

    public int baseSize() {
        return base.length;
    }

    public int[] getCheck() {
        return check;
    }

    public int[] getBase() {
        return base;
    }

    public int size() {
        return size;
    }

    public boolean hasFailure() {
        return fail != null;
    }

    /**
     * 根据构建用的有序词表生成失败指针，使 {@link DATMatcher} 不必每个字符都从根重启。
     * {@code keys} 必须与构建 DAT 时的列表一致（已排序）。
     */
    public void enableFailure(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        int n = check.length;
        Map<Integer, Map<Character, Integer>> children = new HashMap<>();
        int[] keyLen = new int[keys.size()];
        List<Integer>[] direct = newListArray(n);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            keyLen[i] = key.length();
            int node = 0;
            for (int k = 0; k < key.length(); k++) {
                char c = key.charAt(k);
                int next = transitionNode(node, c);
                if (next < 0) {
                    return;
                }
                children.computeIfAbsent(node, x -> new HashMap<>()).put(c, next);
                node = next;
            }
            if (direct[node] == null) {
                direct[node] = new ArrayList<>(1);
            }
            direct[node].add(i);
        }

        int[] failArr = new int[n];
        int[][] out = new int[n][];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        Map<Character, Integer> rootChildren = children.get(0);
        if (rootChildren != null) {
            for (int child : rootChildren.values()) {
                failArr[child] = 0;
                queue.add(child);
            }
        }
        out[0] = toIntArray(direct[0]);

        while (!queue.isEmpty()) {
            int u = queue.removeFirst();
            Map<Character, Integer> trans = children.get(u);
            if (trans != null) {
                for (Map.Entry<Character, Integer> e : trans.entrySet()) {
                    char c = e.getKey();
                    int v = e.getValue();
                    queue.add(v);
                    int f = failArr[u];
                    int t;
                    while (f != 0 && (t = childOf(children, f, c)) < 0) {
                        f = failArr[f];
                    }
                    t = childOf(children, f, c);
                    failArr[v] = t >= 0 ? t : 0;
                }
            }
            out[u] = mergeOutput(direct[u], out[failArr[u]]);
        }

        this.fail = failArr;
        this.output = out;
        this.keyLength = keyLen;
    }

    private int transitionNode(int node, char c) {
        if (node < 0 || node >= base.length) {
            return -1;
        }
        int b = base[node];
        int p = b + c + 1;
        if (p < 0 || p >= check.length || b != check[p]) {
            return -1;
        }
        return p;
    }

    private static int childOf(Map<Integer, Map<Character, Integer>> children, int node, char c) {
        Map<Character, Integer> trans = children.get(node);
        if (trans == null) {
            return -1;
        }
        Integer next = trans.get(c);
        return next == null ? -1 : next;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer>[] newListArray(int n) {
        return (List<Integer>[]) new List<?>[n];
    }

    private static int[] toIntArray(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    private static int[] mergeOutput(List<Integer> direct, int[] failOut) {
        if (direct == null || direct.isEmpty()) {
            return failOut;
        }
        if (failOut == null || failOut.length == 0) {
            return toIntArray(direct);
        }
        int[] merged = new int[direct.size() + failOut.length];
        for (int i = 0; i < direct.size(); i++) {
            merged[i] = direct.get(i);
        }
        System.arraycopy(failOut, 0, merged, direct.size(), failOut.length);
        return merged;
    }

    /**
     * 用失败指针扫一遍，收集 [begin, length, index]。
     */
    int[][] collectFailureHits(char[] text, int offset) {
        return collectFailureHits(text, offset, text == null ? 0 : text.length);
    }

    int[][] collectFailureHits(char[] text, int offset, int end) {
        List<int[]> hits = new ArrayList<>();
        if (fail == null || text == null || offset >= end) {
            return new int[0][];
        }
        int start = Math.max(offset, 0);
        int state = 0;
        for (int i = start; i < end; i++) {
            char c = text[i];
            int next;
            while ((next = transitionNode(state, c)) < 0) {
                if (state == 0) {
                    break;
                }
                state = fail[state];
            }
            if (next >= 0) {
                state = next;
            } else {
                state = 0;
            }
            int[] outs = state < output.length ? output[state] : null;
            if (outs == null) {
                continue;
            }
            for (int wordIdx : outs) {
                int len = keyLength[wordIdx];
                int begin = i - len + 1;
                if (begin >= start && begin + len <= end) {
                    hits.add(new int[]{begin, len, wordIdx});
                }
            }
        }
        hits.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        return hits.toArray(new int[0][]);
    }

    // ======================= 精确匹配接口 =======================

    /**
     * 查找指定字符串在字典中的索引。
     *
     * @param key 要查找的字符串
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOf(CharSequence key) {
        return indexOf(key, 0, 0, 0);
    }

    /**
     * 从指定位置开始查找字符串的索引。
     *
     * @param key     要查找的字符串
     * @param pos     开始查找的位置
     * @param len     查找的长度，&lt;=0 表示从 pos 到结尾
     * @param nodePos 开始查找的节点位置，&lt;=0 表示根节点
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOf(CharSequence key, int pos, int len, int nodePos) {
        if (key == null) {
            return -1;
        }
        len = resolveLength(pos, len, key.length());
        if (len < 0) {
            return -1;
        }
        nodePos = resolveNode(nodePos);
        if (nodePos < 0) {
            return -1;
        }

        int b = base[nodePos];
        int p;

        for (int i = pos; i < pos + len; i++) {
            p = b + key.charAt(i) + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return -1;
            }
            b = base[p];
        }

        return checkEndNode(b);
    }

    /**
     * 查找单个字符在字典中的索引。
     *
     * @param ch 要查找的字符
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOf(char ch) {
        int b = base[0];
        int p = b + ch + 1;
        if (p < 0 || p >= check.length || b != check[p]) {
            return -1;
        }
        b = base[p];
        return checkEndNode(b);
    }

    /**
     * 查找字符数组在字典中的索引。
     *
     * @param chars 要查找的字符数组
     * @param pos   开始查找的位置
     * @param len   查找的长度
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOf(char[] chars, int pos, int len) {
        return indexOf(chars, pos, len, 0);
    }

    /**
     * 从指定位置开始查找字符数组的索引。
     *
     * @param keyChars 要查找的字符数组
     * @param pos      开始查找的位置
     * @param len      查找的长度，&lt;=0 表示从 pos 到数组末尾
     * @param nodePos  开始查找的节点位置，&lt;=0 表示根节点
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOf(char[] keyChars, int pos, int len, int nodePos) {
        if (keyChars == null) {
            return -1;
        }
        len = resolveLength(pos, len, keyChars.length);
        if (len < 0) {
            return -1;
        }
        nodePos = resolveNode(nodePos);
        if (nodePos < 0) {
            return -1;
        }

        int b = base[nodePos];
        int p;

        for (int i = pos; i < pos + len; i++) {
            p = b + keyChars[i] + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return -1;
            }
            b = base[p];
        }

        return checkEndNode(b);
    }

    /**
     * @return 实际比较长度；起点或长度越界时返回 -1
     */
    private static int resolveLength(int pos, int len, int total) {
        if (pos < 0 || pos > total) {
            return -1;
        }
        if (len <= 0) {
            return total - pos;
        }
        if (len > total - pos) {
            return -1;
        }
        return len;
    }

    /**
     * @return 根或合法结点；越界返回 -1
     */
    private int resolveNode(int nodePos) {
        if (nodePos <= 0) {
            return 0;
        }
        if (nodePos >= base.length) {
            return -1;
        }
        return nodePos;
    }

    /**
     * 检查节点是否为结束节点，如果是返回对应的索引，否则返回 -1。
     *
     * @param b 节点位置
     * @return 索引值，-1 表示不是结束节点
     */
    private int checkEndNode(int b) {
        int p = b;
        if (p < 0 || p >= base.length) {
            return -1;
        }
        int n = base[p];
        if (b == check[p] && n < 0) {
            return -n - 1;
        }
        return -1;
    }

    // ======================= 虚拟拼接查询（零拷贝优化） =======================

    /**
     * 虚拟拼接查询：查找 part1 + separator + part2 组成的字符串在字典中的索引。
     * <p>
     * 该方法避免创建新的 String 对象，通过连续状态转移实现虚拟拼接查询，
     * 适用于 BiGram 等需要频繁查询组合键的场景。
     * </p>
     *
     * @param part1     第一部分字符串
     * @param separator 分隔符字符
     * @param part2     第二部分字符串
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOfConcat(CharSequence part1, char separator, CharSequence part2) {
        if (part1 == null || part2 == null) {
            return -1;
        }

        int b = base[0];
        int p;

        // 处理 part1
        int len1 = part1.length();
        for (int i = 0; i < len1; i++) {
            p = b + part1.charAt(i) + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return -1;
            }
            b = base[p];
        }

        // 处理 separator
        p = b + separator + 1;
        if (p < 0 || p >= check.length || b != check[p]) {
            return -1;
        }
        b = base[p];

        // 处理 part2
        int len2 = part2.length();
        for (int i = 0; i < len2; i++) {
            p = b + part2.charAt(i) + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return -1;
            }
            b = base[p];
        }

        return checkEndNode(b);
    }

    /**
     * 虚拟拼接查询：查找 chars1[pos1, pos1+len1) + separator + chars2[pos2, pos2+len2) 组成的字符串在字典中的索引。
     * <p>
     * 该方法避免创建新的 String 对象，通过连续状态转移实现虚拟拼接查询。
     * 支持从 char 数组的指定位置和长度进行查询，适用于分词等零拷贝场景。
     * </p>
     *
     * @param chars1    第一部分字符数组
     * @param pos1      第一部分起始位置
     * @param len1      第一部分长度
     * @param separator 分隔符字符
     * @param chars2    第二部分字符数组
     * @param pos2      第二部分起始位置
     * @param len2      第二部分长度
     * @return 找到返回索引，否则返回 -1
     */
    public int indexOfConcat(char[] chars1, int pos1, int len1, char separator, char[] chars2, int pos2, int len2) {
        if (chars1 == null || chars2 == null) {
            return -1;
        }

        int b = base[0];
        int p;

        // 处理 chars1
        int end1 = pos1 + len1;
        for (int i = pos1; i < end1; i++) {
            p = b + chars1[i] + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return -1;
            }
            b = base[p];
        }

        // 处理 separator
        p = b + separator + 1;
        if (p < 0 || p >= check.length || b != check[p]) {
            return -1;
        }
        b = base[p];

        // 处理 chars2
        int end2 = pos2 + len2;
        for (int i = pos2; i < end2; i++) {
            p = b + chars2[i] + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return -1;
            }
            b = base[p];
        }

        return checkEndNode(b);
    }

    public boolean containsKey(String key) {
        return indexOf(key) >= 0;
    }


    public int getNonzeroSize() {
        int result = 0;
        for (int value : check) {
            if (value != 0) {
                result++;
            }
        }
        return result;
    }


    // ======================= matcher 接口 =======================

    public DATMatcher matcher(String text, int offset) {
        return new DATMatcher(this, text, offset);
    }

    public DATMatcher matcher(char[] text, int offset) {
        return new DATMatcher(this, text, offset);
    }

    public DATMatcher matcher(char[] text, int offset, int end) {
        return new DATMatcher(this, text, offset, end);
    }

    public DATMatcher matcher(String text) {
        return new DATMatcher(this, text, 0);
    }

    public DATMatcher matcher(char[] text) {
        return new DATMatcher(this, text, 0);
    }

    public DATLongMatcher matcherLong(String text, int offset) {
        return new DATLongMatcher(this, text, offset);
    }

    public DATLongMatcher matcherLong(char[] text, int offset) {
        return new DATLongMatcher(this, text, offset);
    }

    public DATLongMatcher matcherLong(char[] text, int offset, int end) {
        return new DATLongMatcher(this, text, offset, end);
    }

    public DATLongMatcher matcherLong(String text) {
        return new DATLongMatcher(this, text, 0);
    }

    public DATLongMatcher matcherLong(char[] text) {
        return new DATLongMatcher(this, text, 0);
    }


    // ======================= 前缀搜索 =======================

    /**
     * 查找指定字符串的所有匹配前缀。
     *
     * @param key 要查找的字符串
     * @return 匹配到的前缀索引列表
     */
    public List<Integer> commonPrefixSearch(CharSequence key) {
        return commonPrefixSearch(key, 0, 0, 0);
    }

    /**
     * 从指定位置开始查找字符串的所有匹配前缀。
     *
     * @param key     要查找的字符串
     * @param pos     开始查找的位置
     * @param len     查找的长度，&lt;=0 表示从pos到字符串末尾
     * @param nodePos 开始查找的节点位置，&lt;=0 表示根节点
     * @return 匹配到的前缀索引列表
     */
    public List<Integer> commonPrefixSearch(CharSequence key, int pos, int len, int nodePos) {
        if (key == null) {
            return new ArrayList<>();
        }
        // 确保 pos 不越界
        if (pos < 0 || pos >= key.length()) {
            return new ArrayList<>();
        }
        // 计算有效的查找长度
        if (len <= 0) {
            len = key.length() - pos;
        } else {
            // 确保 len 不超过从 pos 到字符串末尾的长度
            len = Math.min(len, key.length() - pos);
        }
        if (nodePos <= 0) {
            nodePos = 0;
        }

        List<Integer> result = new ArrayList<>();
        int b = base[nodePos];
        int p;

        for (int i = pos; i < pos + len; i++) {
            p = b + key.charAt(i) + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return result;
            }
            b = base[p];
            p = b;
            int n = base[p];
            if (b == check[p] && n < 0) {
                result.add(-n - 1);
            }
        }
        return result;
    }

    /**
     * 查找字符数组的所有匹配前缀。
     *
     * @param key 要查找的字符数组
     * @return 匹配到的前缀索引列表
     */
    public List<Integer> commonPrefixSearch(char[] key) {
        return commonPrefixSearch(key, 0, 0, 0);
    }

    /**
     * 从指定偏移量开始查找字符数组的所有匹配前缀。
     *
     * @param key    要查找的字符数组
     * @param offset 开始查找的偏移量
     * @return 匹配到的前缀索引列表
     */
    public List<Integer> commonPrefixSearch(char[] key, int offset) {
        return commonPrefixSearch(key, offset, 0, 0);
    }

    /**
     * 从指定位置开始查找字符数组的所有匹配前缀。
     *
     * @param key     要查找的字符数组
     * @param pos     开始查找的位置
     * @param len     查找的长度，&lt;=0 表示从pos到数组末尾
     * @param nodePos 开始查找的节点位置，&lt;=0 表示根节点
     * @return 匹配到的前缀索引列表
     */
    public List<Integer> commonPrefixSearch(char[] key, int pos, int len, int nodePos) {

        if (key == null) {
            return new ArrayList<>();
        }
        // 确保 pos 不越界
        if (pos < 0 || pos >= key.length) {
            return new ArrayList<>();
        }
        // 计算有效的查找长度
        if (len <= 0) {
            len = key.length - pos;
        } else {
            // 确保 len 不超过从 pos 到数组末尾的长度
            len = Math.min(len, key.length - pos);
        }
        if (nodePos <= 0) {
            nodePos = 0;
        }

        List<Integer> result = new ArrayList<>();
        int b = base[nodePos];
        int p;

        for (int i = pos; i < pos + len; i++) {
            p = b + key[i] + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return result;
            }
            b = base[p];
            p = b;
            int n = base[p];
            if (b == check[p] && n < 0) {
                result.add(-n - 1);
            }
        }
        return result;
    }

    /**
     * 返回匹配前缀及其索引值。
     *
     * @param keyChars 要查找的字符数组
     * @param begin    开始查找的位置
     * @return 匹配到的前缀及其索引值列表
     */
    public LinkedList<Map.Entry<String, Integer>> commonPrefixSearchWithValue(char[] keyChars, int begin) {
        LinkedList<Map.Entry<String, Integer>> result = new LinkedList<>();
        int len = keyChars.length;
        int b = base[0];
        int p;

        for (int i = begin; i < len; i++) {
            p = b;
            int n = base[p];
            if (b == check[p] && n < 0) {
                result.add(new AbstractMap.SimpleEntry<>(new String(keyChars, begin, i - begin), -n - 1));
            }

            p = b + keyChars[i] + 1;
            if (p < 0 || p >= check.length || b != check[p]) {
                return result;
            }
            b = base[p];
        }

        p = b;
        int n = base[p];
        if (b == check[p] && n < 0) {
            result.add(new AbstractMap.SimpleEntry<>(new String(keyChars, begin, len - begin), -n - 1));
        }

        return result;
    }


    // ======================= 其它工具方法 =======================

    /**
     * 检查指定状态是否为输出（叶子节点），是则返回索引，否则返回 -1。
     *
     * @param state 要检查的状态
     * @return 索引值，-1 表示不是输出节点
     */
    public int output(int state) {
        if (state < 0 || state >= base.length) {
            return -1;
        }
        int n = base[state];
        if (state == check[state] && n < 0) {
            return -n - 1;
        }
        return -1;
    }

    /**
     * 从当前状态通过字符转移到下一个状态，失败返回 -1。
     *
     * @param current 当前状态
     * @param c       转移字符
     * @return 下一个状态，-1 表示转移失败
     */
    private int transition(int current, char c) {
        int b = base[current];
        int p = b + c + 1;
        if (p < 0 || p >= check.length || b != check[p]) {
            return -1;
        }
        return base[p];
    }

    public void write(DataOutput out) throws IOException {
        DataOutputWrapper wrapper = new DataOutputWrapper(out);
        wrapper.writeInt(size);
        wrapper.writeIntArray(base);
        wrapper.writeIntArray(check);
    }

    public static DoubleArrayTrie read(DataInput in) throws IOException {
        DataInputWrapper wrapper = new DataInputWrapper(in);
        int size = wrapper.readInt();
        int[] base = wrapper.readIntArray();
        int[] check = wrapper.readIntArray();
        return new DoubleArrayTrie(size, check, base);
    }

    /**
     * 创建默认的 DoubleArrayTrie 构建器。
     *
     * @return DoubleArrayTrieBuilder 实例
     */
    public static DoubleArrayTrieBuilder builder() {
        return new DoubleArrayTrieBuilder2();
    }

    /**
     * 创建指定初始容量的 DoubleArrayTrie 构建器。
     *
     * @param capacity 初始容量
     * @return DoubleArrayTrieBuilder 实例
     */
    public static DoubleArrayTrieBuilder builder(int capacity) {
        return new DoubleArrayTrieBuilder2(capacity);
    }
}
