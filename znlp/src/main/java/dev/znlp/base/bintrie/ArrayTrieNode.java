package dev.znlp.base.bintrie;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于数组存储的Trie树节点实现，使用二分查找进行子节点查找。
 * <p>
 * 该类使用有序数组存储子节点，通过二分查找算法快速定位子节点。
 * 相比哈希表实现，数组实现具有更好的内存局部性，适合子节点数量较少的情况。
 * <p>
 * <b>性能特点</b>：
 * <ul>
 *   <li><b>查找</b>：O(log n)时间复杂度，使用二分查找算法</li>
 *   <li><b>插入</b>：O(n)时间复杂度，需要移动数组元素以保持有序</li>
 *   <li><b>内存</b>：每个子节点占用一个数组槽位，内存占用相对较小</li>
 *   <li><b>适用场景</b>：子节点数量较少（通常少于20个）的情况</li>
 * </ul>
 * <p>
 * <b>与HashTrieNode的对比</b>：
 * <ul>
 *   <li>数组实现：查找稍慢但内存占用更小，适合子节点少的情况</li>
 *   <li>哈希实现：查找更快但内存占用略大，适合子节点多的情况</li>
 * </ul>
 *
 * @param <T> 节点存储的值的类型
 * @author jimichan
 */
class ArrayTrieNode<T> extends AbstractTrieNode<T> {

    private ArrayTrieNode<T>[] children = null;

    /**
     * 构造一个数组Trie节点。
     *
     * @param _char  节点代表的字符
     * @param status 节点的状态
     * @param value  节点关联的值
     */
    ArrayTrieNode(char _char, byte status, T value) {
        super(_char);
        this.status = status;
        this.value = value;
    }

    @Override
    public List<AbstractTrieNode<T>> getChildren() {
        if (children == null) {
            return null;
        }
        List<AbstractTrieNode<T>> list = new ArrayList<>();
        for (ArrayTrieNode<T> e : children) {
            if (e != null) {
                list.add(e);
            }
        }
        return list;
    }

    /**
     * 插入子节点。
     * <p>
     * 如果子节点已存在，则更新其状态和值；如果不存在，则插入到有序数组的适当位置。
     *
     * @param c 要插入的子节点
     * @return 插入或更新后的子节点
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BinTrieNode<T> addChildNode(BinTrieNode<T> c) {

        AbstractTrieNode<T> child = ((AbstractTrieNode<T>) c);

        if (children == null) {
            children = new ArrayTrieNode[0];
        }
        int bs = binarySearch(children, child._char);
        if (bs >= 0) {
            ArrayTrieNode<T> branch = this.children[bs];// 单独查找出来的对象
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
            return branch;
        } else {
            ArrayTrieNode[] newBranches = new ArrayTrieNode[children.length + 1];
            int insert = -(bs + 1);
            System.arraycopy(children, 0, newBranches, 0, insert);
            System.arraycopy(children, insert, newBranches, insert + 1,
                    children.length - insert);
            newBranches[insert] = (ArrayTrieNode) child;
            children = newBranches;
            return child;
        }
    }

    /**
     * 查找指定字符的子节点。
     *
     * @param c 要查找的字符
     * @return 找到的子节点，如果不存在则返回null
     */
    @Override
    public ArrayTrieNode<T> findChild(char c) {
        if (this.children == null) {
            return null;
        }
        int i = binarySearch(children, c);
        if (i < 0) {
            return null;
        }
        return this.children[i];
    }

    /**
     * 检查是否包含指定字符的子节点。
     *
     * @param c 要检查的字符
     * @return 如果包含则返回true，否则返回false
     */
    @Override
    public boolean contains(char c) {
        if (this.children == null) {
            return false;
        }
        return binarySearch(this.children, c) > -1;
    }
}
