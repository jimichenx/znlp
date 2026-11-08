package dev.znlp.base.bintrie;

/**
 * 二分Trie树节点接口，定义了Trie树节点的基本操作。
 * <p>
 * 该接口提供了节点的增删查改等基本操作，支持通过字符进行子节点的查找和添加。
 * 节点可以存储关联的值，并维护节点的状态信息。
 *
 * @param <T> 节点存储的值的类型
 * @author jimichan
 */
public interface BinTrieNode<T> {

    /**
     * 添加子节点。
     *
     * @param nodeToInsert 要插入的子节点
     * @return 插入后的节点（可能是新节点或已存在的节点）
     */
    BinTrieNode<T> addChildNode(BinTrieNode<T> nodeToInsert);

    /**
     * 查找指定字符对应的子节点。
     *
     * @param c 要查找的字符
     * @return 找到的子节点，如果不存在则返回null
     */
    BinTrieNode<T> findChild(char c);

    /**
     * 获取节点的状态。
     * <p>
     * 节点状态用于标识节点在Trie树中的位置：
     * <ul>
     *   <li>{@link AbstractTrieNode#Status_Begin} (1): 中间节点，不是词的结尾</li>
     *   <li>{@link AbstractTrieNode#Status_Continue} (2): 既是中间节点也是词的结尾</li>
     *   <li>{@link AbstractTrieNode#Status_End} (3): 词的结尾节点</li>
     *   <li>{@link AbstractTrieNode#Status_Null} (-1): 无效节点（用于删除）</li>
     * </ul>
     *
     * @return 节点的状态字节
     */
    byte getStatus();

    /**
     * 获取节点关联的值。
     *
     * @return 节点存储的值，如果未设置则返回null
     */
    T getValue();

    /**
     * 将节点的字符与指定字符进行比较。
     *
     * @param c 要比较的字符
     * @return 如果节点字符大于c返回正数，小于c返回负数，相等返回0
     */
    int compareTo(char c);

    /**
     * 检查是否包含指定字符的子节点。
     *
     * @param c 要检查的字符
     * @return 如果包含该字符的子节点则返回true，否则返回false
     */
    boolean contains(char c);

    /**
     * 通过字符数组查找节点路径的最后一个节点。
     *
     * @param keyWord 字符数组形式的键
     * @return 路径的最后一个节点，如果路径不存在则返回null
     */
    default BinTrieNode<T> findNode(char[] keyWord) {
        BinTrieNode<T> point = this;
        for (int j = 0; j < keyWord.length; j++) {
            point = point.findChild(keyWord[j]);
            if (point == null) {
                return null;
            }
        }
        return point;
    }

    /**
     * 通过字符序列查找节点路径的最后一个节点。
     * <p>
     * 该方法从当前节点开始，沿着字符序列的路径向下查找，
     * 返回路径的最后一个节点。
     *
     * @param key 字符序列形式的键
     * @return 路径的最后一个节点，如果路径不存在则返回null
     */
    default BinTrieNode<T> findNode(CharSequence key) {
        BinTrieNode<T> branch = this;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            char _char = key.charAt(i);
            if (branch == null) {
                return null;
            }
            branch = branch.findChild(_char);
        }
        return branch;
    }
}
