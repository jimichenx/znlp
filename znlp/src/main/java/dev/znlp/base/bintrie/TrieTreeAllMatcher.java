package dev.znlp.base.bintrie;

import java.util.List;

/**
 * Trie树全匹配器实现，可以扫描一次文本，获得词典中所有匹配的词语。
 * <p>
 * 该匹配器会找出文本中所有在Trie树中存在的词语，包括重叠的匹配。
 * 例如，如果词典中包含"中"、"中国"、"中国人"，那么对于文本"中国人"，
 * 该匹配器会返回所有三个匹配："中"、"中国"、"中国人"。
 * <p>
 * <b>匹配策略</b>：
 * <ul>
 *   <li>从文本的每个位置开始，尝试匹配所有可能的词语</li>
 *   <li>返回所有找到的匹配，包括重叠的匹配</li>
 *   <li>匹配结果按照在文本中出现的顺序返回</li>
 * </ul>
 * <p>
 * <b>性能特点</b>：
 * <ul>
 *   <li>时间复杂度：O(n*m)，其中n是文本长度，m是平均词语长度</li>
 *   <li>空间复杂度：O(1)，仅使用固定大小的临时变量</li>
 *   <li>适合需要找出所有匹配的场景，如关键词提取、实体识别等</li>
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
 * TrieTreeMatcher<String> matcher = tree.newAllMatcher("中国人");
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
class TrieTreeAllMatcher<T> implements TrieTreeMatcher<T> {

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

    private T param;

    private final int len;

    /**
     * 构造一个全匹配器。
     *
     * @param tree    Trie树对象
     * @param content 要匹配的文本内容
     */
    TrieTreeAllMatcher(BinTrieTree<T> tree, String content) {
        this.text = content;
        this.tree = tree;
        this.branch = tree;

        this.len = content.length();
    }

    /**
     * 获取下一个匹配的词语。
     * <p>
     * 该方法会跳过空字符串匹配，返回下一个非空的匹配结果。
     *
     * @return 下一个匹配的词语，如果没有更多匹配则返回null
     */
    @Override
    public String next() {
        String temp = this.allWordNext();
        while (EMPTY_STRING.equals(temp)) {
            temp = this.allWordNext();
        }
        return temp;
    }

    /**
     * 内部方法：获取下一个匹配的词语（可能为空字符串）。
     *
     * @return 下一个匹配的词语，如果没有更多匹配则返回null
     */
    private String allWordNext() {
        if ((!this.isBack) || (this.i == len - 1)) {
            this.i = (this.root - 1);
        }
        for (this.i += 1; this.i < len; this.i = (this.i + 1)) {
            this.branch = this.branch.findChild(this.text.charAt(this.i));
            if (this.branch == null) {
                this.root += 1;
                this.branch = this.tree;
                this.i = (this.root - 1);
                this.isBack = false;
            } else {
                switch (this.branch.getStatus()) {
                    case AbstractTrieNode.Status_Continue:
                        this.isBack = true;
                        this.offset = (this.tempOffset + this.root);
                        this.param = this.branch.getValue();
                        //return new String(this.chars, this.root, this.i - this.root + 1);
                        return this.text.substring(this.root, this.i + 1);
                    case AbstractTrieNode.Status_End:
                        this.offset = (this.tempOffset + this.root);
                        //this.str = new String(this.chars, this.root, this.i - this.root + 1);
                        this.str = this.text.substring(this.root, this.i + 1);
                        this.param = this.branch.getValue();
                        this.branch = this.tree;
                        this.isBack = false;
                        this.root += 1;
                        return this.str;
                }
            }
        }
        this.tempOffset += this.text.length();
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
