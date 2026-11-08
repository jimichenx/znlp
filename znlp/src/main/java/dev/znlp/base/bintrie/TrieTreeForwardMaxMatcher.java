package dev.znlp.base.bintrie;

import java.util.List;

/**
 * Trie树前向最大匹配器实现，可以扫描一次文本，获得词典中的最大前向匹配。
 * <p>
 * 该匹配器采用前向最大匹配算法（Forward Maximum Matching，FMM），
 * 对于每个起始位置，会找到最长的匹配词语。这是中文分词中最常用的算法之一。
 * <p>
 * <b>匹配策略</b>：
 * <ul>
 *   <li>从文本的当前位置开始，尝试匹配尽可能长的词语</li>
 *   <li>当找到匹配时，返回最长的匹配结果</li>
 *   <li>匹配完成后，从匹配结束的位置继续下一次匹配</li>
 *   <li>不会返回重叠的匹配，每个字符最多被匹配一次</li>
 * </ul>
 * <p>
 * <b>示例</b>：
 * <p>
 * 如果词典中包含"中"、"中国"、"中国人"，对于文本"中国人"：
 * <ul>
 *   <li>全匹配器会返回："中"、"中国"、"中国人"（3个匹配）</li>
 *   <li>前向最大匹配器会返回："中国人"（1个匹配，最长）</li>
 * </ul>
 * <p>
 * <b>性能特点</b>：
 * <ul>
 *   <li>时间复杂度：O(n*m)，其中n是文本长度，m是平均词语长度</li>
 *   <li>空间复杂度：O(1)，仅使用固定大小的临时变量</li>
 *   <li>适合中文分词、关键词提取等需要最长匹配的场景</li>
 * </ul>
 * <p>
 * <b>算法来源</b>：改造自Ansj开源的分词项目。
 * <p>
 * <b>使用示例</b>：
 * <pre>{@code
 * BinTrieTree<String> tree = BinTrieTreeBuilder._default.build();
 * tree.put("中国", "CN");
 * tree.put("中国人", "CNP");
 *
 * TrieTreeMatcher<String> matcher = tree.newForwardMatcher("中国人");
 * String word;
 * while ((word = matcher.next()) != null) {
 *     System.out.println("匹配: " + word + ", 参数: " + matcher.getParams());
 * }
 * }</pre>
 *
 * @param <T> 匹配词语关联的参数类型
 * @author jimichan
 * @author ansj
 */
class TrieTreeForwardMaxMatcher<T> implements TrieTreeMatcher<T> {

    private static final String EMPTY_STRING = "";

    private final BinTrieTree<T> tree;

    private int offset;
    private int root = 0;
    private int i = this.root;
    private boolean isBack = false;
    private BinTrieNode<T> branch;
    private final String text;
    private String str;
    private int tempOffset;

    private final int len;

    private T param;

    /**
     * 构造一个前向最大匹配器。
     *
     * @param tree    Trie树对象
     * @param content 要匹配的文本内容
     */
    TrieTreeForwardMaxMatcher(BinTrieTree<T> tree, String content) {
        this.text = content;
        this.tree = tree;
        this.branch = tree;
        this.len = content.length();
    }

    /**
     * 获取下一个匹配的词语。
     * <p>
     * 该方法会跳过空字符串匹配，返回下一个非空的最大匹配结果。
     *
     * @return 下一个匹配的词语，如果没有更多匹配则返回null
     */
    @Override
    public String next() {
        String temp = this.frontWordNext();
        while (EMPTY_STRING.equals(temp)) {
            temp = this.frontWordNext();
        }
        return temp;
    }

    /**
     * 内部方法：获取下一个前向最大匹配的词语（可能为空字符串）。
     *
     * @return 下一个匹配的词语，如果没有更多匹配则返回null
     */
    private String frontWordNext() {

        for (int up = len + 1; this.i < up; this.i++) {
            if (i == len) {
                this.branch = null;
            } else {
                this.branch = this.branch.findChild(this.text.charAt(this.i));
            }
            if (this.branch == null) {
                this.branch = this.tree;
                if (this.isBack) {
                    this.offset = this.root;

                    this.str = this.text.substring(this.root, this.root + this.tempOffset);
                    //this.str = new String(this.chars, this.root, this.tempOffset);

                    if ((this.root > 0) && (isE(this.text.charAt(this.root - 1))) && (isE(this.str.charAt(0)))) {
                        this.str = EMPTY_STRING;
                    }

                    if ((this.str.length() != 0) && (this.root + this.tempOffset < this.text.length()) && (isE(this.str.charAt(this.str.length() - 1)))
                            && (isE(this.text.charAt(this.root + this.tempOffset)))) {
                        this.str = EMPTY_STRING;
                    }
                    if (this.str.length() == 0) {
                        this.root += 1;
                        this.i = this.root;
                    } else {
                        this.i = (this.root + this.tempOffset);
                        this.root = this.i;
                    }
                    this.isBack = false;

                    if (EMPTY_STRING.equals(this.str)) {
                        return EMPTY_STRING;
                    }
                    return this.str;
                }
                this.i = this.root;
                this.root += 1;
            } else {
                switch (this.branch.getStatus()) {
                    case AbstractTrieNode.Status_Continue:
                        this.isBack = true;
                        this.tempOffset = (this.i - this.root + 1);
                        this.param = this.branch.getValue();
                        break;
                    case AbstractTrieNode.Status_End:
                        this.offset = this.root;
                        //this.str = new String(this.chars, this.root, this.i - this.root + 1);
                        this.str = this.text.substring(this.root, this.i + 1);
                        String temp = this.str;

                        if ((this.root > 0) && (isE(this.text.charAt(this.root - 1))) && (isE(this.str.charAt(0)))) {
                            this.str = EMPTY_STRING;
                        }

                        if ((this.str.length() != 0) && (this.i + 1 < this.text.length()) && (isE(this.str.charAt(this.str.length() - 1)))
                                && (isE(this.text.charAt(this.i + 1)))) {
                            this.str = EMPTY_STRING;
                        }
                        this.param = this.branch.getValue();
                        this.branch = this.tree;
                        this.isBack = false;
                        if (temp.length() > 0) {
                            this.i += 1;
                            this.root = this.i;
                        } else {
                            this.i = (this.root + 1);
                        }
                        if (EMPTY_STRING.equals(this.str)) {
                            return EMPTY_STRING;
                        }
                        return this.str;
                }
            }
        }
        this.tempOffset += len;
        return null;
    }

    private boolean isE(char c) {
        return c == '.' || ((c >= 'a') && (c <= 'z'));
    }

    /**
     * 获取当前匹配词语中指定索引的参数。
     * <p>
     * 如果参数对象是数组或列表类型，则返回指定索引的元素（转换为字符串）；
     * 否则返回null。
     *
     * @param i 参数索引
     * @return 指定索引的参数值（字符串形式），如果不存在则返回null
     */
    @Override
    public String getParam(int i) {
        if (param != null) {
            if (param instanceof String[] _p) {
                if (_p.length > i) {
                    return _p[i];
                }
            } else if (param instanceof List<?> list) {
                return list.get(i).toString();
            }
        }
        return null;
    }

    /**
     * 获取当前匹配词语的全部参数。
     *
     * @return 当前匹配词语的参数对象，如果没有则返回null
     */
    @Override
    public T getParams() {
        return this.param;
    }

    /**
     * 获取当前匹配词语在文本中的偏移量。
     *
     * @return 匹配词语在文本中的偏移量
     */
    @Override
    public int getOffset() {
        return offset;
    }
}
