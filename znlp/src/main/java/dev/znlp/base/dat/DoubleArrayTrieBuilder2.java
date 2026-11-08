package dev.znlp.base.dat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * 双数组Trie构建器的优化版本，采用智能扩容和缓存优化，提高构建效率。
 */
public class DoubleArrayTrieBuilder2 extends AbstractDoubleArrayTrieBuilder {

    /**
     * 默认构造方法，使用默认容量。
     */
    public DoubleArrayTrieBuilder2() {
        super();
    }

    /**
     * 带初始容量的构造方法。
     *
     * @param initCapacity 初始容量
     */
    public DoubleArrayTrieBuilder2(int initCapacity) {
        super(initCapacity);
    }

    /**
     * 插入节点，采用优化的算法提高构建效率。
     *
     * @param siblings 等待插入的兄弟节点
     * @return 插入位置
     */
    @Override
    protected int insert(List<Node> siblings) {
        if (error < 0) {
            return 0;
        }

        int begin = 0;
        int pos = Math.max(siblings.get(0).code + 1, nextCheckPos) - 1;
        int nonzeroNum = 0;
        boolean first = false;

        if (arrayCapacity <= pos) {
            resize(pos + 1);
        }

        // 此循环体的目标是找出满足base[begin + a1...an] == 0的n个空闲空间,a1...an是siblings中的n个节点
        final int firstSiblingCode = siblings.get(0).code;
        final int lastSiblingCode = siblings.get(siblings.size() - 1).code;
        final int lastSiblingCodeAndMax = lastSiblingCode + Character.MAX_VALUE;
        final int siblingsSize = siblings.size();

        // 缓存变量，减少字段访问开销
        final BitSet localUsed = used;
        final int localLastSiblingCode = lastSiblingCode;
        final int localFirstSiblingCode = firstSiblingCode;
        final int localLastSiblingCodeAndMax = lastSiblingCodeAndMax;
        final int localSiblingsSize = siblingsSize;
        final List<Node> localSiblings = siblings;

        while (true) {
            ++pos;

            // pos 是下标，扩容参数是新的长度
            if (pos >= arrayCapacity) {
                resize(pos + 1);
            }

            // 直接使用check数组，避免final变量重新赋值问题
            if (check[pos] != 0) {
                nonzeroNum++;
                continue;
            } else if (!first) {
                nextCheckPos = pos;
                first = true;
            }

            begin = pos - localFirstSiblingCode; // 当前位置离第一个兄弟节点的距离

            // 检查begin + lastSiblingCode是否超出数组容量
            final int beginPlusLastSiblingCode = begin + localLastSiblingCode;
            if (beginPlusLastSiblingCode >= arrayCapacity) {
                final int nsize = begin + localLastSiblingCodeAndMax;
                resize(nsize);
            }

            if (localUsed.get(begin)) {
                continue;
            }

            // 缓存begin值，减少重复计算
            final int currentBegin = begin;
            // 检查所有兄弟节点是否都有空闲位置
            boolean allFree = true;
            for (int i = 1; i < localSiblingsSize; i++) {
                if (check[currentBegin + localSiblings.get(i).code] != 0) {
                    allFree = false;
                    break;
                }
            }

            if (allFree) {
                break;
            }
        }

        // 如果已占用的空间在95%以上，下次插入节点时直接从pos位置开始查找
        final int interval = pos - nextCheckPos + 1;
        if (interval > 0 && nonzeroNum >= 0.95 * interval) {
            nextCheckPos = pos;
        }

        used.set(begin);

        size = Math.max(size, begin + lastSiblingCode + 1);

        for (Node s : siblings) {
            check[begin + s.code] = begin;
        }

        // 预分配new_siblings的大小，减少内存分配
        final int estimatedMaxNewSiblings = Math.min(dataSize, 128);
        for (int i = 0; i < siblingsSize; i++) {
            final Node theSib = siblings.get(i);
            // 预分配大小，减少ArrayList扩容开销
            List<Node> newSiblings = new ArrayList<>(Math.min(estimatedMaxNewSiblings, theSib.right - theSib.left));

            // fetch也是newSiblings添加的过程。一个词的终止且不为其他词的前缀
            int x = fetch(theSib, newSiblings);

            if (x == 0) {
                base[begin + theSib.code] = (-theSib.left - 1);
                progress++;
            } else {
                int h = insert(newSiblings); // dfs
                base[begin + theSib.code] = h;
            }
        }

        return begin;
    }

    /**
     * 获取直接相连的子节点，采用优化的算法提高构建效率。
     *
     * @param parent   父节点
     * @param siblings （子）兄弟节点
     * @return 兄弟节点个数
     */
    @Override
    protected int fetch(Node parent, List<Node> siblings) {
        if (error < 0) {
            return 0;
        }

        final int parentDepth = parent.depth;
        final int parentRight = parent.right;
        final int parentLeft = parent.left;
        int prev = 0;
        Node lastNode = null;

        for (int i = parentLeft; i < parentRight; i++) {
            final String currentString = data.get(i);
            final int currentLength = currentString.length();

            // 合并条件检查
            if (currentLength < parentDepth) {
                continue;
            }

            int cur = 0;
            // 只有当字符串长度大于当前深度时才需要获取字符
            if (currentLength > parentDepth) {
                // 直接访问char并+1，避免不必要的操作
                cur = currentString.charAt(parentDepth) + 1;
            }

            // 检查顺序是否正确
            if (prev > cur) {
                error = -3;
                return 0;
            }

            // 只有当字符变化或没有兄弟节点时创建新节点
            if (cur != prev || siblings.isEmpty()) {
                // 创建新节点
                Node newNode = new Node();
                newNode.depth = parentDepth + 1;
                newNode.code = cur;
                newNode.left = i;

                // 设置前一个节点的right值
                if (!siblings.isEmpty()) {
                    lastNode.right = i;
                }

                siblings.add(newNode);
                lastNode = newNode;
            }

            prev = cur;
        }

        // 设置最后一个节点的right值
        if (!siblings.isEmpty()) {
            lastNode.right = parentRight;
        }

        return siblings.size();
    }
}