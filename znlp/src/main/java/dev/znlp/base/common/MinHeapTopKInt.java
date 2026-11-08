package dev.znlp.base.common;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;

/**
 * 用最小堆实现的Top K最大值查询数据结构（int 特化版本）。
 * <p>
 * 通过维护一个大小为k的最小堆，可以在O(log k)时间内完成元素的插入/更新操作，
 * 最终获得最大的k个元素。该实现特别适合流式数据处理场景，其中k远小于数据总量。
 * 
 * <p>本类专门针对 int 类型的标识符进行了特化，避免了泛型版本的拆装箱开销。
 *
 * <p>使用示例：
 * <pre>
 * MinHeapTopKInt topK = new MinHeapTopKInt(10);
 * topK.push(1, 0.9f);
 * topK.push(2, 0.8f);
 * // ...
 * List&lt;Pair&lt;Integer, Float&gt;&gt; results = topK.result(); // 返回按分数降序排列的结果
 * </pre>
 *
 * <p><strong>时间复杂度：</strong>
 * <ul>
 *   <li>push(id, score): O(log k) - 每次插入需要堆化</li>
 *   <li>result(): O(k log k) - 排序k个元素</li>
 * </ul>
 *
 * <p><strong>空间复杂度：</strong> O(k)
 *
 * @author jimichan
 */
public class MinHeapTopKInt {
    /**
     * 堆的容量大小，即所维护的Top K的K值
     */
    private final int k;

    /**
     * 最小堆数组，存储分数
     */
    private final float[] heap;

    /**
     * 与heap数组一一对应的元素标识数组（int类型，避免拆装箱）
     */
    private final int[] idIndex;

    /**
     * 当前堆中实际的元素数量
     */
    private int size = 0;

    /**
     * 简单的键值对类，用于存储结果中的元素及其分数。
     *
     * @param <T>    第一个元素的类型
     * @param <S>    第二个元素的类型
     * @param first  第一个元素
     * @param second 第二个元素
     */
        public record Pair<T, S>(T first, S second) {
        /**
         * 构造键值对。
         *
         * @param first  第一个元素
         * @param second 第二个元素
         */
        public Pair {
        }

            /**
             * 获取第一个元素
             *
             * @return 第一个元素
             */
            @Override
            public T first() {
                return first;
            }

            /**
             * 获取第二个元素
             *
             * @return 第二个元素
             */
            @Override
            public S second() {
                return second;
            }
        }

    /**
     * 构造MinHeapTopKInt实例。
     *
     * @param k 需要维护的最大元素个数
     * @throws IllegalArgumentException 如果k &lt;= 0
     */
    public MinHeapTopKInt(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be greater than 0, got: " + k);
        }
        this.k = k;
        this.heap = new float[k];
        this.idIndex = new int[k];
    }

    /**
     * 向Top K集合中添加或更新一个元素。
     *
     * <p>如果堆未满，直接添加元素。如果堆满且新元素分数大于堆顶（最小值）元素，
     * 则替换堆顶元素并进行堆化以保持堆的性质。否则忽略该元素。
     *
     * <p>时间复杂度: O(log k)
     *
     * @param id    元素的标识/键（int类型，避免拆装箱）
     * @param score 元素的分数值，用于比较大小
     */
    public void push(int id, float score) {
        if (size < k) {
            heap[size] = score;
            idIndex[size] = id;
            size++;

            if (size == k) {
                buildMinHeap();
            }
        } else {
            // 如果这个数据大于最小值，那么有资格进入
            if (score > heap[0]) {
                heap[0] = score;
                idIndex[0] = id;
                minHeapify(0);
            }
        }
    }

    /**
     * 获取当前Top K集合中的所有元素，按分数降序排列。
     *
     * <p>返回的结果列表包含当前堆中的所有元素（最多k个），按照分数从高到低排序。
     * 如果已插入的元素总数少于k，则返回所有已插入的元素。
     *
     * <p>时间复杂度: O(k log k) - 由排序操作主导
     *
     * @return 包含所有Top K元素的列表，每个元素是(标识, 分数)的键值对，按分数降序排列
     */
    public List<Pair<Integer, Float>> result() {
        int top = min(k, size);
        List<Pair<Integer, Float>> list = new ArrayList<>(top);
        for (int i = 0; i < top; i++) {
            list.add(new Pair<>(idIndex[i], heap[i]));
        }

        list.sort((a, b) -> Float.compare(b.second(), a.second()));
        return list;
    }

    /**
     * 获取排序后的结果（与 result() 方法相同，提供 resort() 方法以保持 API 一致性）。
     *
     * @return 结果列表，按分数降序排序
     */
    public List<Pair<Integer, Float>> resort() {
        return result();
    }

    /**
     * 将无序的heap数组构建成最小堆。
     *
     * <p>使用线性时间算法（从最后一个非叶子节点开始向下堆化），
     * 时间复杂度为O(k)，优于逐个插入的O(k log k)。
     * 在push方法中堆满时调用。
     */
    private void buildMinHeap() {
        for (int i = k / 2 - 1; i >= 0; i--) {
            minHeapify(i);
        }
    }

    /**
     * 对指定位置的元素进行最小堆化操作。
     *
     * <p>将位置i的元素与其子节点比较，如果找到比i更小的子节点，则交换并递归处理
     * 被交换到的位置，直到满足最小堆的性质（父节点 <= 子节点）。
     *
     * @param i 要堆化的节点位置
     */
    private void minHeapify(int i) {
        int l = 2 * i + 1;
        int r = 2 * i + 2;
        int min;

        if (l < k && heap[l] < heap[i]) {
            min = l;
        } else {
            min = i;
        }

        if (r < k && heap[r] < heap[min]) {
            min = r;
        }

        if (min == i || min >= k) {
            return;
        }

        swap(i, min);
        minHeapify(min);
    }

    /**
     * 交换堆中两个位置的元素（同时交换heap和idIndex中的对应值）。
     *
     * @param i 第一个位置的索引
     * @param j 第二个位置的索引
     */
    private void swap(int i, int j) {
        float tmpScore = heap[i];
        heap[i] = heap[j];
        heap[j] = tmpScore;

        int tmpId = idIndex[i];
        idIndex[i] = idIndex[j];
        idIndex[j] = tmpId;
    }
}
