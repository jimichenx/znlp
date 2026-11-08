package dev.znlp.base.bintrie;

import java.util.Map;
import java.util.function.Function;

/**
 * BinTrieTree构建器枚举，提供多种预配置的构建方式。
 * <p>
 * 该枚举定义了多种Trie树构建策略，每种策略针对不同的使用场景进行了优化：
 * <ul>
 *   <li><b>_default</b>：默认配置，使用哈希节点，数组模式根节点</li>
 *   <li><b>bigArray</b>：大数组模式，使用数组节点，数组模式根节点</li>
 *   <li><b>bigHash</b>：大哈希模式，使用数组节点，数组模式根节点</li>
 *   <li><b>miniArray</b>：小数组模式，使用数组节点，映射模式根节点（节省内存）</li>
 *   <li><b>miniHash</b>：小哈希模式，使用哈希节点，映射模式根节点（节省内存）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 使用默认配置构建
 * BinTrieTree<String> tree = BinTrieTreeBuilder._default.build();
 *
 * // 从Map构建
 * Map<String, String> map = new HashMap<>();
 * map.put("中国", "CN");
 * BinTrieTree<String> tree2 = BinTrieTreeBuilder._default.build(map);
 *
 * // 从文本行构建
 * List<String> lines = Arrays.asList("中国 CN", "美国 US");
 * BinTrieTree<String[]> tree3 = BinTrieTreeBuilder._default.build(
 *     " ", lines,
 *     arr -> arr  // 将分割后的数组作为值
 * );
 * }</pre>
 *
 * @author jimichan
 */
public enum BinTrieTreeBuilder {

    /**
     * 默认配置：使用哈希节点，数组模式根节点。
     * <p>
     * 适合大多数场景，在性能和内存之间取得平衡。
     */
    _default(false, HashTrieNode::new),

    /**
     * 大数组模式：使用数组节点，数组模式根节点。
     * <p>
     * 适合子节点数量较多的情况，查找性能较好。
     */
    bigArray(false, ArrayTrieNode::new),

    /**
     * 大哈希模式：使用数组节点，数组模式根节点。
     * <p>
     * 与bigArray相同，使用数组节点。
     */
    bigHash(false, ArrayTrieNode::new),

    /**
     * 小数组模式：使用数组节点，映射模式根节点。
     * <p>
     * 节省内存，适合词语较少或临时词典的场景。
     */
    miniArray(true, ArrayTrieNode::new),

    /**
     * 小哈希模式：使用哈希节点，映射模式根节点。
     * <p>
     * 节省内存，适合词语较少或临时词典的场景。
     */
    miniHash(true, HashTrieNode::new);

    /**
     * 根节点是否使用映射模式
     */
    private final boolean rootUseMap;

    /**
     * 节点工厂
     */
    private final BinTrieTree.TrieNodeFactory<?> nodeFactory;

    /**
     * 构造一个构建器枚举值。
     *
     * @param rootUseMap  根节点是否使用映射模式
     * @param nodeFactory 节点工厂
     */
    BinTrieTreeBuilder(boolean rootUseMap, BinTrieTree.TrieNodeFactory<?> nodeFactory) {
        this.rootUseMap = rootUseMap;
        this.nodeFactory = nodeFactory;
    }

    /**
     * 构建一个空的BinTrieTree。
     *
     * @param <T> 值的类型
     * @return 构建好的BinTrieTree对象
     */
    @SuppressWarnings("unchecked")
    public <T> BinTrieTree<T> build() {
        return new BinTrieTree<>(rootUseMap, (BinTrieTree.TrieNodeFactory<T>) nodeFactory);
    }

    /**
     * 从Map构建BinTrieTree。
     * <p>
     * Map中的每个键值对会被插入到Trie树中，键作为词语，值作为关联的值。
     *
     * @param map 包含键值对的Map
     * @param <T> 值的类型
     * @return 构建好的BinTrieTree对象
     */
    @SuppressWarnings("unchecked")
    public <T> BinTrieTree<T> build(Map<String, T> map) {

        BinTrieTree<T> tree = new BinTrieTree<>(rootUseMap, (BinTrieTree.TrieNodeFactory<T>) nodeFactory);

        for (Map.Entry<String, T> entry : map.entrySet()) {
            tree.put(entry.getKey(), entry.getValue());
        }
        return tree;
    }

    /**
     * 从文本行构建BinTrieTree。
     * <p>
     * 每行的格式为：word[分隔符]args...
     * <p>
     * 例如，如果分隔符为空格，行内容为"中国 CN Beijing"，
     * 则词语为"中国"，参数为["CN", "Beijing"]。
     * <p>
     * 转换函数会将分割后的参数数组转换为值对象。
     *
     * @param split    分隔符
     * @param lines    文本行集合
     * @param transFun 转换函数，将分割后的字符串数组转换为值对象
     * @param <T>      值的类型
     * @return 构建好的BinTrieTree对象
     */
    @SuppressWarnings("unchecked")
    public <T> BinTrieTree<T> build(String split, Iterable<String> lines, Function<String[], T> transFun) {

        BinTrieTree<T> tree = new BinTrieTree<>(rootUseMap, (BinTrieTree.TrieNodeFactory<T>) nodeFactory);

        final String[] empty = new String[0];
        for (String line : lines) {
            String[] list = line.split(split);
            if (list.length == 1) {
                tree.put(list[0], transFun.apply(empty));
            } else {
                String[] sublist = new String[list.length - 1];
                System.arraycopy(list, 1, sublist, 0, sublist.length);
                tree.put(list[0], transFun.apply(sublist));
            }
        }
        return tree;
    }
}
