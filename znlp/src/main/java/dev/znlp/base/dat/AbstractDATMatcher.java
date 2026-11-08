package dev.znlp.base.dat;

/**
 * DATMatcher 的抽象基类，提取公共字段和方法，减少重复代码。
 */
public abstract class AbstractDATMatcher {

    /**
     * key的起点(在原始文本中)
     */
    protected int begin;
    /**
     * key的长度
     */
    protected int length;
    /**
     * key的字典序坐标
     */
    protected int index;

    /**
     * 传入的字符数组
     */
    protected char[] charArray;
    /**
     * 上一个node位置
     */
    protected int last;
    /**
     * 上一个字符的下标
     */
    protected int i;
    /**
     * charArray的长度，效率起见，开个变量
     */
    protected int arrayLength;

    protected DoubleArrayTrie dat;

    protected int[] base;
    protected int[] check;

    /**
     * 构造一个双数组搜索工具
     *
     * @param dat    双数组Trie对象
     * @param text   搜索的目标字符串
     * @param offset 搜索的起始位置
     */
    public AbstractDATMatcher(DoubleArrayTrie dat, String text, int offset) {
        this(dat, text.toCharArray(), offset);
    }

    /**
     * 构造一个双数组搜索工具
     *
     * @param dat       双数组Trie对象
     * @param charArray 搜索的目标字符数组
     * @param offset    搜索的起始位置
     */
    public AbstractDATMatcher(DoubleArrayTrie dat, char[] charArray, int offset) {
        this(dat, charArray, offset, charArray.length);
    }

    /**
     * @param end 搜索结束位置，不含。可以小于数组长度，用来限制在一个句窗口内。
     */
    public AbstractDATMatcher(DoubleArrayTrie dat, char[] charArray, int offset, int end) {
        this.charArray = charArray;
        this.dat = dat;
        this.i = offset;
        this.last = dat.getBase()[0];
        this.arrayLength = end;

        // 初始化begin位置，处理空字符串情况
        if (arrayLength == 0) {
            this.begin = -1;
        } else {
            this.begin = offset;
        }

        this.base = dat.getBase();
        this.check = dat.getCheck();
    }

    /**
     * 取出下一个命中输出
     *
     * @return 是否命中，当返回false表示搜索结束，否则使用公开的成员读取命中的详细信息
     */
    public abstract boolean next();

    /**
     * 获取匹配的起始位置
     *
     * @return 起始位置
     */
    public int getBegin() {
        return begin;
    }

    /**
     * 获取匹配的长度
     *
     * @return 匹配长度
     */
    public int getLength() {
        return length;
    }

    /**
     * 获取匹配的索引
     *
     * @return 字典序索引
     */
    public int getIndex() {
        return index;
    }
}