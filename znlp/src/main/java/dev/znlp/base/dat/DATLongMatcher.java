package dev.znlp.base.dat;

/**
 * 双数组Trie的长匹配器，支持最长匹配模式，优先匹配最长的词。
 * <p>
 * 注意：当调用next()返回false后不应该继续调用next()，除非重置状态。
 */
public class DATLongMatcher extends AbstractDATMatcher {

    private Object value = null;
    private final Object obj = new Object();

    /**
     * 构造一个双数组搜索工具
     *
     * @param dat     双数组Trie对象
     * @param text    搜索的目标字符串
     * @param offset  搜索的起始位置
     */
    public DATLongMatcher(DoubleArrayTrie dat, String text, int offset) {
        super(dat, text, offset);
    }

    /**
     * 构造一个双数组搜索工具
     *
     * @param dat       双数组Trie对象
     * @param charArray 搜索的目标字符数组
     * @param offset    搜索的起始位置
     */
    public DATLongMatcher(DoubleArrayTrie dat, char[] charArray, int offset) {
        super(dat, charArray, offset);
    }

    public DATLongMatcher(DoubleArrayTrie dat, char[] charArray, int offset, int end) {
        super(dat, charArray, offset, end);
    }

    /**
     * 取出下一个命中输出
     *
     * @return 是否命中，当返回false表示搜索结束，否则使用公开的成员读取命中的详细信息
     */
    @Override
    public boolean next() {
        value = null;
        begin = i;
        int b = base[0];
        int n;
        int p;

        for (; ; ++i) {
            if (i >= arrayLength)               // 指针到头了，将起点往前挪一个，重新开始，状态归零
            {
                return value != null;
            }
            p = b + (int) (charArray[i]) + 1;   // 状态转移 p = base[char[i-1]] + char[i] + 1
            if (p >= 0 && p < check.length && b == check[p]) {
                b = base[p];                    // 转移成功
            } else {
                if (begin == arrayLength) {
                    break;
                }
                if (value != null) {
                    i = begin + length;         // 输出最长词后，从该词语的下一个位置恢复扫描
                    return true;
                }

                i = begin;                      // 转移失败，也将起点往前挪一个，重新开始，状态归零
                ++begin;
                b = base[0];
            }
            p = b;
            if (p < 0 || p >= base.length || p >= check.length) {
                continue;
            }
            n = base[p];
            if (b == check[p] && n < 0)         // base[p] == check[p] && base[p] < 0 查到一个词
            {
                length = i - begin + 1;
                index = -n - 1;
                value = obj;
            }
        }

        return false;
    }

    /**
     * 获取匹配的起始位置
     *
     * @return 起始位置
     */
    @Override
    public int getBegin() {
        return begin;
    }

    /**
     * 获取匹配的长度
     *
     * @return 匹配长度
     */
    @Override
    public int getLength() {
        return length;
    }

    /**
     * 获取匹配的索引
     *
     * @return 字典序索引
     */
    @Override
    public int getIndex() {
        return index;
    }
}