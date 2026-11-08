package dev.znlp.base.common;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static java.lang.Math.min;


/**
 * 用最小堆实现的Top K最大值查询数据结构。
 * <p>
 * 通过维护一个大小为k的最小堆，可以在O(log k)时间内完成元素的插入/更新操作，
 * 最终获得最大的k个元素。该实现特别适合流式数据处理场景，其中k远小于数据总量。
 *
 * <p>使用示例：
 * <pre>
 * TopMaxK&lt;String&gt; topK = new TopMaxK&lt;&gt;(10, String.class);
 * topK.push("item1", 0.9f);
 * topK.push("item2", 0.8f);
 * // ...
 * List&lt;Pair&lt;String, Float&gt;&gt; results = topK.result(); // 返回按分数降序排列的结果
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
 * @param <T> 元素的标识类型，需要是可实例化的类型
 * @author jimichan
 */
public class MinHeapTopK<T> {
    /**
     * 用于反射创建泛型数组
     */
    private final Class<T> clazz;

    /**
     * 堆的容量大小，即所维护的Top K的K值
     */
    private int k = 10;

    /**
     * 最小堆数组，存储分数
     */
    private final float[] heap;

    /**
     * 与heap数组一一对应的元素标识数组
     */
    private final T[] idIndex;

    /**
     * 当前堆中实际的元素数量
     */
    int size = 0;

    /**
     * 简单的键值对类，用于存储结果中的元素及其分数。
     *
     * @param <T> 第一个元素的类型
     * @param <S> 第二个元素的类型
     */
    public static class Pair<T, S> {
        /**
         * 第一个元素
         */
        public T first;

        /**
         * 第二个元素
         */
        public S second;

        /**
         * 构造键值对。
         *
         * @param first  第一个元素
         * @param second 第二个元素
         */
        public Pair(T first, S second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * 构造TopMaxK实例。
     *
     * @param k     需要维护的最大元素个数
     * @param clazz 元素标识的类型，用于创建泛型数组
     * @throws IllegalArgumentException 如果k &lt;= 0
     */
    public MinHeapTopK(int k, Class<T> clazz) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be greater than 0, got: " + k);
        }
        this.k = k;
        heap = new float[k];
        this.clazz = clazz;
        @SuppressWarnings("unchecked")
        T[] tmpArray = (T[]) Array.newInstance(clazz, k);
        idIndex = tmpArray;
    }

    /**
     * 向Top K集合中添加或更新一个元素。
     *
     * <p>如果堆未满，直接添加元素。如果堆满且新元素分数大于堆顶（最小值）元素，
     * 则替换堆顶元素并进行堆化以保持堆的性质。否则忽略该元素。
     *
     * <p>时间复杂度: O(log k)
     *
     * @param id    元素的标识/键
     * @param score 元素的分数值，用于比较大小
     */
    public void push(T id, float score) {
        if (size < k) {
            heap[size] = score;
            idIndex[size] = id;
            size++;

            if (size == k) {
                buildMinHeap();
            }
        } else {
            // 如果这个数据大于最下值，那么有资格进入
            if (score > heap[0]) {
                heap[0] = score;
                idIndex[0] = id;
                mintopify(0);
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
    public ArrayList<Pair<T, Float>> result() {
        int top = min(k, size);
        ArrayList<Pair<T, Float>> list = new ArrayList<>(top);
        for (int i = 0; i < top; i++) {
            list.add(new Pair<>(idIndex[i], heap[i]));
        }

        list.sort((a, b) -> -1 * Float.compare(a.second, b.second));
        return list;
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
            // 依次向上将当前子树堆化
            mintopify(i);
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
    private void mintopify(int i) {
        int l = 2 * i + 1;
        int r = 2 * i + 2;
        int min = 0;

        if (l < k && heap[l] < heap[i]) {
            min = l;
        } else {
            min = i;
        }

        if (r < k && heap[r] < heap[min]) {
            min = r;
        }

        if (min == i || min >= k) {
            // 如果largest等于i说明i是最大元素
            // largest超出heap范围说明不存在比i节点大的子女
            return;
        }

        swap(i, min);
        mintopify(min);
    }

    /**
     * 交换堆中两个位置的元素（同时交换heap和idIndex中的对应值）。
     *
     * @param i 第一个位置的索引
     * @param j 第二个位置的索引
     */
    private void swap(int i, int j) {
        float tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;

        T tmp2 = idIndex[i];
        idIndex[i] = idIndex[j];
        idIndex[j] = tmp2;
    }

}
