package dev.znlp.base.bintrie;

import java.util.List;

/**
 * Trie树节点的抽象基类，提供了节点的基本属性和通用方法。
 * <p>
 * 该类实现了{@link BinTrieNode}接口，定义了节点的核心属性：
 * <ul>
 *   <li><b>字符</b>：节点代表的字符，用于在Trie树中定位</li>
 *   <li><b>状态</b>：节点在Trie树中的状态（开始、继续、结束等），用于标识词语的完整性</li>
 *   <li><b>值</b>：节点关联的值对象，可以存储词语的附加信息</li>
 *   <li><b>层级</b>：节点在树中的层级深度，用于遍历和路径重建</li>
 * </ul>
 * <p>
 * 子类需要实现{@link #getChildren()}方法来提供子节点的访问方式。
 * 不同的子类实现可以使用不同的数据结构存储子节点，以优化不同场景下的性能。
 * <p>
 * <b>线程安全性</b>：该类不是线程安全的。如果需要在多线程环境中使用，需要外部同步。
 *
 * @param <V> 节点存储的值的类型
 * @author jimichan
 */
abstract class AbstractTrieNode<V> implements BinTrieNode<V> {

    /**
     * 节点状态：开始节点（中间节点，不是词的结尾）
     */
    public static final byte Status_Begin = 1;

    /**
     * 节点状态：继续节点（既是中间节点也是词的结尾）
     */
    public static final byte Status_Continue = 2;

    /**
     * 节点状态：结束节点（词的结尾）
     */
    public static final byte Status_End = 3;

    /**
     * 节点状态：无效节点（用于删除操作）
     */
    public static final byte Status_Null = -1;

    /**
     * 节点代表的字符
     */
    public final char _char;

    /**
     * 节点的状态
     */
    public byte status;

    /**
     * 节点关联的值对象
     */
    public V value = null;

    /**
     * 节点在Trie树中的层级深度
     */
    public short level;

    /**
     * 构造一个Trie树节点。
     *
     * @param _char 节点代表的字符
     */
    public AbstractTrieNode(char _char) {
        this._char = _char;
    }

    /**
     * 获取节点的所有子节点列表。
     * <p>
     * 子类必须实现此方法来提供子节点的访问。
     *
     * @return 子节点列表，如果没有子节点则返回null或空列表
     */
    public abstract List<AbstractTrieNode<V>> getChildren();

    @Override
    public String toString() {
        return this._char + "[" + this.status + "]";
    }

    /**
     * 获取值对象中指定索引的参数。
     * <p>
     * 如果值对象是数组或列表类型，则返回指定索引的元素；
     * 否则返回null。
     *
     * @param i 参数索引
     * @return 指定索引的参数值，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public V getParam(int i) {
        if (value != null) {
            if (value instanceof String[] _p) {
                if (_p.length > i) {
                    return (V) _p[i];
                }
            } else if (value instanceof List) {
                List<V> list = (List<V>) value;
                return list.get(i);
            }
        }
        return null;
    }

    /**
     * 检查节点的字符是否等于指定字符。
     *
     * @param c 要比较的字符
     * @return 如果相等则返回true，否则返回false
     */
    public boolean equals(char c) {
        return this._char == c;
    }

    @Override
    public int hashCode() {
        return this._char;
    }

    @Override
    public int compareTo(char c) {
        if (this._char > c) {
            return 1;
        }
        if (this._char < c) {
            return -1;
        }
        return 0;
    }

    @Override
    public byte getStatus() {
        return status;
    }

    @Override
    public V getValue() {
        return this.value;
    }

    /**
     * 使用二分查找在有序的子节点数组中查找指定字符。
     * <p>
     * 该方法要求子节点数组已经按照字符顺序排序。
     *
     * @param branches 有序的子节点数组
     * @param _char    要查找的字符
     * @return 如果找到则返回数组索引（非负数），如果未找到则返回负值（-(插入点+1)）
     */
    static int binarySearch(BinTrieNode<?>[] branches, char _char) {
        int high = branches.length - 1;
        if (branches.length < 1) {
            return high;
        }
        int low = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = branches[mid].compareTo(_char);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1); // key not found.
    }
}
