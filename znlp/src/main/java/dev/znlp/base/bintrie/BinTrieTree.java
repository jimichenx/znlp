package dev.znlp.base.bintrie;

import dev.znlp.base.common.AbstractIterator;
import dev.znlp.base.common.CharObjectHashMap;
import dev.znlp.base.common.CharObjectMap;

import java.util.*;
import java.util.Map.Entry;

/**
 * 二分Trie树实现，提供高效的字符串存储和查询功能。
 * <p>
 * 该类实现了{@link Trie}接口和{@link BinTrieNode}接口，可以作为Trie树使用，
 * 也可以作为Trie树的根节点使用。Trie树（字典树）是一种树形数据结构，用于高效存储和检索字符串集合。
 * <p>
 * <b>存储模式</b>：
 * <p>
 * 支持两种根节点子节点存储模式，可通过{@link BinTrieTreeBuilder}选择：
 * <ul>
 *   <li><b>数组模式</b>：使用65536大小的数组直接索引，查找时间复杂度O(1)
 *       <ul>
 *         <li>优点：查找速度快，适合首字符分布广泛的情况</li>
 *         <li>缺点：内存占用大（约512KB），即使只使用少量字符</li>
 *       </ul>
 *   </li>
 *   <li><b>映射模式</b>：使用哈希映射存储首字符，查找时间复杂度O(1)平均
 *       <ul>
 *         <li>优点：内存占用小，只存储实际使用的字符</li>
 *         <li>缺点：查找稍慢（但差异很小），适合首字符较少的情况</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * <b>节点类型</b>：
 * <p>
 * 子节点可以使用两种实现，通过{@link BinTrieTreeBuilder}选择：
 * <ul>
 *   <li><b>ArrayTrieNode</b>：数组存储，二分查找，适合子节点少的情况</li>
 *   <li><b>HashTrieNode</b>：哈希存储，O(1)查找，适合子节点多的情况</li>
 * </ul>
 * <p>
 * <b>主要功能</b>：
 * <ul>
 *   <li><b>插入和查询</b>：支持字符串、字符数组、字符序列等多种形式的插入和查询</li>
 *   <li><b>前缀搜索</b>：查找所有以指定前缀开头的词语</li>
 *   <li><b>匹配器</b>：提供全匹配和前向最大匹配两种匹配策略</li>
 *   <li><b>遍历</b>：支持遍历所有词语及其关联的值</li>
 * </ul>
 * <p>
 * <b>性能特点</b>：
 * <ul>
 *   <li>查询时间复杂度：O(m)，其中m是词语长度</li>
 *   <li>插入时间复杂度：O(m)，其中m是词语长度</li>
 *   <li>空间复杂度：O(n*m)，其中n是词语数量，m是平均词语长度</li>
 *   <li>支持大小写不敏感的查询（自动转换为小写）</li>
 * </ul>
 * <p>
 * <b>线程安全性</b>：
 * <p>
 * 该类不是线程安全的。如果需要在多线程环境中使用，需要外部同步。
 * 建议在多线程环境中使用只读操作，或在构建完成后不再修改。
 * <p>
 * <b>使用示例</b>：
 * <pre>{@code
 * // 创建Trie树
 * BinTrieTree<String> tree = BinTrieTreeBuilder._default.build();
 *
 * // 插入词语
 * tree.put("中国", "CN");
 * tree.put("中国人", "CNP");
 *
 * // 查询
 * String value = tree.get("中国"); // 返回 "CN"
 * boolean exists = tree.containsKey("中国人"); // 返回 true
 *
 * // 前缀搜索
 * Set<Entry<String, String>> results = tree.prefixSearch("中");
 * // 返回: [("中国", "CN"), ("中国人", "CNP")]
 *
 * // 创建匹配器进行文本匹配
 * TrieTreeMatcher<String> matcher = tree.newForwardMatcher("我是中国人");
 * String word;
 * while ((word = matcher.next()) != null) {
 *     System.out.println("匹配: " + word);
 * }
 * }</pre>
 *
 * @param <T> 存储的值的类型
 * @author jimichan
 */
public class BinTrieTree<T> implements Trie<T>, BinTrieNode<T> {

    /**
     * 根节点子节点是否使用映射存储。
     * <p>
     * 如果为true，则使用哈希映射存储首字符，更节省内存但查找稍慢；
     * 如果为false，则使用65536大小的数组直接索引，查找更快但内存占用较大。
     * <p>
     * 映射模式一般应用于词语较少或临时词典的场景。
     *
     * @see #max_width
     */
    final boolean rootChildUseMap;

    /**
     * 数组模式的最大宽度（Unicode字符的最大值）
     */
    static final int max_width = 65536;

    /**
     * 数组模式下的子节点数组
     */
    private AbstractTrieNode<T>[] children;

    /**
     * 映射模式下的子节点映射
     */
    private CharObjectMap<AbstractTrieNode<T>> childrenMap;

    /**
     * 节点工厂，用于创建子节点。
     * 不同的工厂实现会创建不同类型的节点（ArrayTrieNode或HashTrieNode）。
     */
    final TrieNodeFactory<T> nodeFactory;

    /**
     * 构造一个二分Trie树。
     *
     * @param rootChildUseMap 根节点子节点是否使用映射存储
     * @param nodeFactory     节点工厂
     */
    BinTrieTree(boolean rootChildUseMap, TrieNodeFactory<T> nodeFactory) {
        this.rootChildUseMap = rootChildUseMap;
        this.nodeFactory = nodeFactory;
        reset();
    }

    /**
     * 清空树并释放内存。
     * <p>
     * 该方法会重新初始化子节点存储结构，释放之前的所有数据。
     */
    @SuppressWarnings("unchecked")
    public void reset() {
        if (rootChildUseMap) {
            childrenMap = new CharObjectHashMap<>(500);
        } else {
            children = (AbstractTrieNode<T>[]) new AbstractTrieNode<?>[max_width];
        }
    }

    /**
     * 计算根节点的子节点数量。
     * <p>
     * 该方法用于统计根节点直接子节点的数量，不包括子节点的子节点。
     *
     * @return 根节点的子节点数量
     */
    public int rootChildCount() {
        if (childrenMap != null) {
            return childrenMap.size();
        } else {
            int count = 0;
            for (AbstractTrieNode<T> child : children) {
                if (child != null) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * 创建一个前向最大匹配器。
     * <p>
     * 该匹配器会找出文本中所有匹配的词语，采用前向最大匹配策略。
     *
     * @param text 要匹配的文本
     * @return 前向最大匹配器对象
     */
    public TrieTreeMatcher<T> newForwardMatcher(String text) {
        return new TrieTreeForwardMaxMatcher<>(this, text);
    }

    /**
     * 创建一个全匹配器。
     * <p>
     * 该匹配器会找出文本中所有匹配的词语，包括重叠的匹配。
     *
     * @param text 要匹配的文本
     * @return 全匹配器对象
     */
    public TrieTreeMatcher<T> newAllMatcher(String text) {
        return new TrieTreeAllMatcher<>(this, text);
    }

    @Override
    public boolean containsKey(String key) {
        key = key.toLowerCase();
        BinTrieNode<T> branch = this;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            char _char = key.charAt(i);
            if (branch == null) {
                return false;
            }
            branch = branch.findChild(_char);
        }

        if (branch == null) {
            return false;
        }
        // 下面这句可以保证只有成词的节点被返回
        byte status = branch.getStatus();
        return status == AbstractTrieNode.Status_End || status == AbstractTrieNode.Status_Continue;
    }

    @Override
    public T get(char[] key) {
        BinTrieNode<T> branch = this;
        int len = key.length;
        for (int i = 0; i < len; i++) {
            char _char = key[i];
            if (branch == null) {
                return null;
            }
            branch = branch.findChild(_char);
        }

        if (branch == null) {
            return null;
        }
        // 下面这句可以保证只有成词的节点被返回
        byte status = branch.getStatus();
        if (status != AbstractTrieNode.Status_End && status != AbstractTrieNode.Status_Continue) {
            return null;
        }

        return branch.getValue();
    }

    @Override
    public T get(char[] key, int offset, int len) {
        BinTrieNode<T> branch = this;
        for (int i = offset; i < len; i++) {
            char _char = key[i];
            if (branch == null) {
                return null;
            }
            branch = branch.findChild(_char);
        }

        if (branch == null) {
            return null;
        }
        // 下面这句可以保证只有成词的节点被返回
        byte status = branch.getStatus();
        if (status != AbstractTrieNode.Status_End && status != AbstractTrieNode.Status_Continue) {
            return null;
        }

        return branch.getValue();
    }

    @Override
    public T get(CharSequence key) {
        String lowerKey = key.toString().toLowerCase();
        BinTrieNode<T> branch = findNode(lowerKey);

        if (branch == null) {
            return null;
        }
        // 下面这句可以保证只有成词的节点被返回
        byte status = branch.getStatus();
        if (status != AbstractTrieNode.Status_End && status != AbstractTrieNode.Status_Continue) {
            return null;
        }

        return branch.getValue();
    }

    /**
     * 插入一个词项到Trie树中。
     * <p>
     * 插入的词语会被转换为小写。如果词语已存在，则更新其关联的值。
     *
     * @param word  要插入的词语
     * @param value 词语关联的值对象
     */
    public void put(String word, T value) {
        word = word.toLowerCase();
        BinTrieNode<T> point = this;
        int len = word.length(); // 不用toCharArray的原因是不用再复制一份
        int lenIndex = len - 1;
        for (int i = 0; i < len; i++) {
            char theChar = word.charAt(i);
            if (lenIndex == i) {
                point.addChildNode(nodeFactory.create(theChar, AbstractTrieNode.Status_End, value));
            } else {
                point.addChildNode(nodeFactory.create(theChar, AbstractTrieNode.Status_Begin, null));
            }
            point = point.findChild(theChar);
        }
    }

    /**
     * 前缀查询，返回所有以指定前缀开头的词语及其值。
     * <p>
     * 该方法会查找所有以指定前缀开头的词语，并返回它们的键值对集合。
     * 例如，如果Trie树中包含"中国"、"中国人"、"中国话"，查询前缀"中"会返回这三个词语。
     * <p>
     * <b>性能</b>：时间复杂度O(m + k)，其中m是前缀长度，k是匹配的词语数量。
     *
     * @param key 查询的前缀字符串（会自动转换为小写）
     * @return 匹配的键值对集合，如果没有匹配则返回空集合（不可修改）
     */
    public Set<Entry<String, T>> prefixSearch(String key) {
        key = key.toLowerCase();
        BinTrieNode<T> node = findNode(key);
        if (node == null) {
            return Collections.emptySet();
        }
        NodeHolder<T> holder = new NodeHolder<>();
        IteratorKeys ite = new IteratorKeys(holder, (AbstractTrieNode<T>) node, key);
        Set<Entry<String, T>> set = new HashSet<>();
        while (ite.hasNext()) {
            String k = ite.next();
            AbstractTrieNode<T> v = holder.node;
            set.add(new AbstractMap.SimpleEntry<>(k, v.value));
        }
        return set;
    }

    /**
     * 从Trie树中删除一个词语。
     * <p>
     * 删除操作会将词语的结尾节点标记为无效（Status_Null），但不会删除路径上的其他节点。
     * 这意味着删除后，该词语不再被识别为有效词语，但路径仍然保留。
     * <p>
     * <b>注意</b>：该方法不会真正释放内存，只是标记节点为无效。如果需要真正释放内存，
     * 建议重新构建Trie树。
     *
     * @param key 要删除的词语（会自动转换为小写）
     */
    public void remove(String key) {
        key = key.toLowerCase();
        BinTrieNode<T> branch = this;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            if (branch == null) {
                return;
            }
            char c = key.charAt(i);
            if (i == len - 1) {
                branch.addChildNode(nodeFactory.create(c, AbstractTrieNode.Status_Null, null));
            }
            branch = branch.findChild(c);
        }
    }

    /**
     * Trie节点访问器接口，用于遍历访问所有节点。
     *
     * @param <T> 节点值的类型
     */
    public interface TireNodeAccess<T> {
        /**
         * 访问一个节点。
         *
         * @param node 要访问的节点
         */
        void access(AbstractTrieNode<T> node);
    }

    /**
     * 访问Trie树中的所有节点。
     * <p>
     * 该方法使用深度优先搜索遍历所有节点，并对每个节点调用访问器。
     *
     * @param nodeAccess 节点访问器
     */
    public void accessFullTireNode(TireNodeAccess<T> nodeAccess) {
        LinkedList<AbstractTrieNode<T>> stack = new LinkedList<>();

        // 初始化堆栈
        if (childrenMap != null) {
            for (AbstractTrieNode<T> node : childrenMap.values()) {
                stack.push(node);
            }
        } else {
            for (AbstractTrieNode<T> x : children) {
                if (x != null) {
                    stack.push(x);
                }
            }
        }

        // 堆栈循环访问
        while (!stack.isEmpty()) {
            AbstractTrieNode<T> node = stack.pop();
            nodeAccess.access(node);

            List<AbstractTrieNode<T>> chl = node.getChildren();
            if (chl != null) {
                chl.forEach(x -> stack.push(x));// 改成放到栈顶
            }
        }
    }

    /**
     * 获取所有词语的迭代器，同时提供节点持有者以获取节点信息。
     *
     * @param holder 节点持有者，用于存储当前词语对应的节点
     * @return 词语迭代器
     */
    public Iterator<String> keys(NodeHolder<T> holder) {
        return new IteratorKeys(holder);
    }

    /**
     * 获取所有词语的迭代器。
     *
     * @return 词语迭代器
     */
    public Iterator<String> keys() {
        return new IteratorKeys(null);
    }

    /**
     * 节点持有者，用于在迭代过程中保存当前节点。
     *
     * @param <T> 节点值的类型
     */
    public static class NodeHolder<T> {
        /**
         * 当前节点
         */
        AbstractTrieNode<T> node;

        /**
         * 获取当前节点。
         *
         * @return 当前节点
         */
        public AbstractTrieNode<T> getNode() {
            return node;
        }
    }

    /**
     * 获取所有键值对的迭代器。
     *
     * @return 键值对的可迭代对象
     */
    public Iterable<Entry<String, T>> entry() {
        return () -> new AbstractIterator<Entry<String, T>>() {
            final NodeHolder<T> holder;
            final Iterator<String> ite;

            {
                holder = new NodeHolder<>();
                ite = keys(holder);
            }

            @Override
            protected void computeNext() {
                if (!ite.hasNext()) {
                    done();
                    return;
                }
                String key = ite.next();
                if (key != null) {
                    setNext(new AbstractMap.SimpleEntry<>(key, holder.node.value));
                    return;
                }

                done();
            }
        };
    }

    /**
     * 词语迭代器，用于遍历Trie树中的所有词语。
     */
    class IteratorKeys extends AbstractIterator<String> {

        LinkedList<AbstractTrieNode<T>> stack = new LinkedList<>();

        char[] buffer = new char[Short.MAX_VALUE];

        NodeHolder<T> holder;

        /**
         * 构造一个词语迭代器。
         *
         * @param holder 节点持有者
         */
        IteratorKeys(NodeHolder<T> holder) {
            this.holder = holder;
            // 初始化堆栈
            if (childrenMap != null) {
                for (AbstractTrieNode<T> node : childrenMap.values()) {
                    stack.push(node);
                }

            } else {
                for (AbstractTrieNode<T> x : children) {
                    if (x != null) {
                        stack.push(x);
                    }
                }
            }

            stack.forEach(x -> x.level = 1);
        }

        /**
         * 构造一个从指定节点开始的词语迭代器。
         *
         * @param holder   节点持有者
         * @param initNode 初始节点
         * @param prefix   前缀字符串
         */
        IteratorKeys(NodeHolder<T> holder, AbstractTrieNode<T> initNode, String prefix) { //指定了初始化节点
            this.holder = holder;
            stack.push(initNode);
            stack.forEach(x -> x.level = (short) prefix.length()); //FIXME 此处需要多测试

            if (prefix.length() >= 2) {
                for (int i = 0; i < prefix.length() - 1; i++) {
                    this.buffer[1 + i] = prefix.charAt(i);
                }
            }
        }

        @Override
        protected void computeNext() {

            if (stack.isEmpty()) {
                done();
                return;
            }

            String n = _next();

            while (n == null) {

                n = _next();

                if (n != null) {
                    setNext(n);
                    return;
                }

                if (stack.isEmpty()) {
                    done();
                    return;
                }
            }

            setNext(n);
        }

        /**
         * 内部方法：获取下一个词语。
         *
         * @return 下一个词语，如果没有则返回null
         */
        private String _next() {
            AbstractTrieNode<T> node = stack.pop();

            buffer[node.level] = node._char;

            List<AbstractTrieNode<T>> chl = node.getChildren();
            if (chl != null) {
                short level = (short) (node.level + 1);
                chl.forEach(x -> {
                    x.level = level;
                    stack.push(x);
                });
            }

            if (node.status == AbstractTrieNode.Status_Continue || node.status == AbstractTrieNode.Status_End) {
                if (holder != null) {
                    holder.node = node;
                }
                return new String(buffer, 1, node.level);
            }

            return null;
        }
    }

    // //////////////////////////////以下是作为NODE的行为////////////////////////////////////////////

    @Override
    public boolean contains(char c) {
        if (rootChildUseMap) {
            return childrenMap.containsKey(c);
        } else {
            return this.children[c] != null;
        }
    }

    @Override
    public BinTrieNode<T> addChildNode(BinTrieNode<T> n) {

        AbstractTrieNode<T> node = ((AbstractTrieNode<T>) n);

        AbstractTrieNode<T> oldNode = null;
        if (rootChildUseMap) {
            oldNode = this.childrenMap.get(node._char);
            if (oldNode == null) {
                this.childrenMap.put(node._char, node);
                oldNode = node;
            }
        } else {
            oldNode = this.children[node._char];
            if (oldNode == null) {
                this.children[node._char] = node;
                oldNode = node;
            }
        }

        switch (node.status) {
            case AbstractTrieNode.Status_Begin:
                if (oldNode.status == AbstractTrieNode.Status_End) {
                    oldNode.status = AbstractTrieNode.Status_Continue;
                }
                break;
            case AbstractTrieNode.Status_End:
                if (oldNode.status == AbstractTrieNode.Status_Begin) {
                    oldNode.status = AbstractTrieNode.Status_Continue;
                }
                oldNode.value = node.value;
        }

        return oldNode;
    }

    @Override
    public AbstractTrieNode<T> findChild(char c) {
        if (rootChildUseMap) {
            return childrenMap.get(c);
        } else {
            if (c > max_width) {
                return null;
            }
            return this.children[c];
        }
    }

    @Override
    public byte getStatus() {
        return 0;
    }

    @Override
    public T getValue() {
        return null;
    }

    @Override
    public int compareTo(char c) {
        return 0;
    }

    /**
     * 获取子节点映射（仅在映射模式下有效）。
     *
     * @return 子节点映射，如果使用数组模式则返回null
     */
    public CharObjectMap<AbstractTrieNode<T>> getChildrenMap() {
        return childrenMap;
    }

    /**
     * Trie节点工厂接口，用于创建Trie树节点。
     *
     * @param <T> 节点值的类型
     */
    public interface TrieNodeFactory<T> {
        /**
         * 创建一个Trie树节点。
         *
         * @param _char  节点代表的字符
         * @param status 节点的状态
         * @param param  节点关联的值
         * @return 创建的节点
         */
        BinTrieNode<T> create(char _char, byte status, T param);
    }
}
