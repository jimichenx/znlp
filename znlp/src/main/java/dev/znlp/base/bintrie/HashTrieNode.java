package dev.znlp.base.bintrie;

import dev.znlp.base.common.CharObjectHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于哈希表存储的Trie树节点实现，使用快速哈希查找子节点。
 * <p>
 * 该类使用{@link CharObjectHashMap}存储子节点，
 * 通过哈希表实现O(1)平均时间复杂度的查找和插入操作。
 * 相比数组实现，哈希表实现在插入和查找方面性能更好，但内存占用略高。
 * <p>
 * <b>性能特点</b>：
 * <ul>
 *   <li><b>查找</b>：O(1)平均时间复杂度，使用哈希表直接定位</li>
 *   <li><b>插入</b>：O(1)平均时间复杂度，无需维护顺序</li>
 *   <li><b>内存</b>：比数组实现多约5%，主要用于哈希表的结构开销</li>
 *   <li><b>适用场景</b>：子节点数量较多（通常超过20个）的情况</li>
 * </ul>
 * <p>
 * <b>实际测试数据</b>（CRF数据测试）：
 * <ul>
 *   <li>内存占用：465M（数组实现为445M，增加约4.5%）</li>
 *   <li>插入时间：10秒（数组实现为15秒，提升约33%）</li>
 * </ul>
 * <p>
 * <b>与ArrayTrieNode的对比</b>：
 * <ul>
 *   <li>哈希实现：查找和插入更快，但内存占用略大，适合子节点多的情况</li>
 *   <li>数组实现：内存占用更小，但查找和插入稍慢，适合子节点少的情况</li>
 * </ul>
 *
 * @param <T> 节点存储的值的类型
 * @author jimichan
 */
class HashTrieNode<T> extends AbstractTrieNode<T> {

    private CharObjectHashMap<HashTrieNode<T>> map;

    /**
     * 构造一个哈希Trie节点。
     *
     * @param _char  节点代表的字符
     * @param status 节点的状态
     * @param param  节点关联的值
     */
    HashTrieNode(char _char, byte status, T param) {
        super(_char);
        this.status = status;
        this.value = param;
    }

    @Override
    public List<AbstractTrieNode<T>> getChildren() {
        if (map == null) {
            return null;
        }

        return new ArrayList<>(map.values());
    }

    /**
     * 插入子节点。
     * <p>
     * 如果子节点已存在，则更新其状态和值；如果不存在，则添加到哈希表中。
     *
     * @param c 要插入的子节点
     * @return 插入或更新后的子节点
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BinTrieNode<T> addChildNode(BinTrieNode<T> c) {
        AbstractTrieNode<T> child = ((AbstractTrieNode<T>) c);

        if (map == null) {
            map = new CharObjectHashMap<>();
        }
        HashTrieNode<T> branch = map.get(child._char);
        if (branch != null) {
            switch (child.status) {
                case -1:
                    branch.status = Status_Begin;
                    break;
                case 1:
                    if (branch.status == Status_End) {
                        branch.status = Status_Continue;
                    }
                    break;
                case 3:
                    if (branch.status != Status_End) {
                        branch.status = Status_Continue;
                    }
                    branch.value = child.value;
            }
        } else {
            map.put(child._char, (HashTrieNode) child);
        }

        return map.get(child._char);
    }

    /**
     * 查找指定字符的子节点。
     *
     * @param c 要查找的字符
     * @return 找到的子节点，如果不存在则返回null
     */
    @Override
    public HashTrieNode<T> findChild(char c) {
        if (map != null) {
            return map.get(c);
        }
        return null;
    }

    /**
     * 检查是否包含指定字符的子节点。
     *
     * @param c 要检查的字符
     * @return 如果包含则返回true，否则返回false
     */
    @Override
    public boolean contains(char c) {
        if (map != null) {
            return map.containsKey(c);
        }
        return false;
    }
}
