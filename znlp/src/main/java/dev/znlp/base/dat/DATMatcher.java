package dev.znlp.base.dat;

/**
 * 双数组Trie的匹配器，支持多模式匹配，查找所有可能的匹配项。
 * <p>
 * 注意：当调用next()返回false后不应该继续调用next()，除非重置状态。
 */
public class DATMatcher extends AbstractDATMatcher {

    private final int[][] failureHits;
    private int failureCursor;

    /**
     * 构造一个双数组搜索工具
     *
     * @param dat     双数组Trie对象
     * @param text    搜索的目标字符串
     * @param offset  搜索的起始位置
     */
    public DATMatcher(DoubleArrayTrie dat, String text, int offset) {
        super(dat, text, offset);
        this.failureHits = dat.hasFailure() ? dat.collectFailureHits(this.charArray, offset) : null;
        this.failureCursor = 0;
    }

    /**
     * 构造一个双数组搜索工具
     *
     * @param dat       双数组Trie对象
     * @param charArray 搜索的目标字符数组
     * @param offset    搜索的起始位置
     */
    public DATMatcher(DoubleArrayTrie dat, char[] charArray, int offset) {
        this(dat, charArray, offset, charArray.length);
    }

    public DATMatcher(DoubleArrayTrie dat, char[] charArray, int offset, int end) {
        super(dat, charArray, offset, end);
        this.failureHits = dat.hasFailure() ? dat.collectFailureHits(charArray, offset, end) : null;
        this.failureCursor = 0;
    }

    /**
     * 取出下一个命中输出
     *
     * @return 是否命中，当返回false表示搜索结束，否则使用公开的成员读取命中的详细信息
     */
    @Override
    public boolean next() {
        if (failureHits != null) {
            if (failureCursor >= failureHits.length) {
                return false;
            }
            int[] hit = failureHits[failureCursor++];
            begin = hit[0];
            length = hit[1];
            index = hit[2];
            return true;
        }
        int b = last;
        int n;
        int p;

        for (; ; ++i) {
            if (i == arrayLength) // 指针到头了,将起点往前挪一个,重新开始,状态归零
            {
                ++begin;
                if (begin == arrayLength) {
                    break;
                }
                i = begin;
                b = base[0];
            }

            // 再次检查i是否越界(因为上面可能重新赋值了i)
            if (i >= arrayLength) {
                break;
            }
            
            p = b + (int) (charArray[i]) + 1; // 状态转移 p = base[char[i-1]] +
            // char[i] + 1

            // 检查数组越界,如果p超出范围,则当前匹配失败,重新开始
            if (p < 0 || p >= check.length) {
                i = begin; // 转移失败,也将起点往前挪一个,重新开始,状态归零
                ++begin;
                if (begin == arrayLength) {
                    break;
                }
                b = base[0];
                continue;
            }
                    
            if (b == check[p]) // base[char[i-1]] == check[base[char[i-1]] +
            // char[i] + 1]
            {
                b = base[p]; // 转移成功
            } else {
                i = begin; // 转移失败,也将起点往前挪一个,重新开始,状态归零
                ++begin;
                if (begin == arrayLength) {
                    break;
                }
                b = base[0];
                continue;
            }
            p = b;

            // 检查访问base和check数组前的越界情况
            if (p < 0 || p >= base.length) {
                i = begin;
                ++begin;
                if (begin == arrayLength) {
                    break;
                }
                b = base[0];
                continue;
            }
                    
            n = base[p];
            if (b == check[p] && n < 0) // base[p] == check[p] && base[p] <
            // 0
            // 查到一个词
            {
                length = i - begin + 1;
                index = -n - 1;
                last = b;
                ++i;
                return true;
            }
        }

        return false;
    }

    public int getBegin() {
        return begin;
    }

    public int getLength() {
        return length;
    }

    public int getIndex() {
        return index;
    }
}