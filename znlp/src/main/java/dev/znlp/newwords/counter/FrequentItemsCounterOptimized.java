package dev.znlp.newwords.counter;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.newwords.entity.IntCount;
import dev.znlp.newwords.entity.WordCount;

import java.util.ArrayList;
import java.util.List;

/**
 * 高频元素统计器的优化版本，采用多种性能优化技术。
 *
 * <h3>优化策略</h3>
 * <ul>
 *   <li><b>快速压缩</b>：使用新 HashMap 替换而非迭代删除，在大量删除时更快</li>
 *   <li><b>减少对象分配</b>：优化 IntCount 使用，减少 GC 压力</li>
 *   <li><b>智能阈值</b>：动态计算最优删除阈值，一次性压缩到位</li>
 *   <li><b>预分配容量</b>：HashMap 初始化时设置合适的负载因子</li>
 *   <li><b>批量操作优化</b>：减少不必要的中间步骤</li>
 * </ul>
 *
 * @author jimichan
 * @see FrequentItemsCounter
 */
public class FrequentItemsCounterOptimized implements IFrequentItemsCounter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FrequentItemsCounterOptimized.class);

    private final int size;
    private int minCount;
    private boolean verbose = false;
    private java.util.HashMap<String, IntCount> data;
    private int lastMinCount = 2;

    // 优化：使用 int 数组缓存频次分布，避免重复创建
    private final int[] frequencyCache;
    private static final int MAX_FREQUENCY_CACHE = 1000;

    public FrequentItemsCounterOptimized(int size, int minCount) {
        this.size = size;
        this.minCount = minCount;
        // 预分配容量，减少 rehash
        int initialCapacity = (int) (size / 0.75f) + 1;
        this.data = new java.util.HashMap<>(initialCapacity, 0.75f);
        this.frequencyCache = new int[MAX_FREQUENCY_CACHE];
    }

    /**
     * 优化的 put 方法：减少 HashMap 查找次数
     */
    public void put(String key) {
        IntCount count = data.get(key);
        if (count == null) {
            data.put(key, new IntCount());
        } else {
            count.increment();
        }

        if (data.size() >= size) {
            reduce();
        }
    }

    /**
     * 优化的压缩方法：使用快速重建策略
     */
    private void reduce() {
        if (verbose) {
            logger.debug("清洗前有{}条数据", data.size());
        }

        int targetSize = size / 4;

        // 策略1：快速统计频次分布
        java.util.Arrays.fill(frequencyCache, 0);
        int maxFreq = 0;

        for (IntCount count : data.values()) {
            int freq = count.getValue();
            if (freq < MAX_FREQUENCY_CACHE) {
                frequencyCache[freq]++;
            }
            maxFreq = Math.max(maxFreq, freq);
        }

        // 策略2：计算最优阈值（一次性压缩到位）
        int threshold = lastMinCount;
        int estimatedSize = data.size();

        for (int t = lastMinCount; t < Math.min(minCount, MAX_FREQUENCY_CACHE); t++) {
            estimatedSize -= frequencyCache[t];
            if (estimatedSize <= targetSize) {
                threshold = t;
                break;
            }
        }

        // 策略3：快速重建 HashMap（比迭代删除更快）
        java.util.HashMap<String, IntCount> newData = new java.util.HashMap<>(targetSize * 2, 0.75f);

        for (java.util.Map.Entry<String, IntCount> entry : data.entrySet()) {
            if (entry.getValue().getValue() > threshold) {
                newData.put(entry.getKey(), entry.getValue());
            }
        }

        // 直接替换引用，让 GC 回收旧 HashMap
        data = newData;

        if (verbose) {
            logger.debug("删除频次 ≤ {} 的元素，剩余 {}", threshold, data.size());
        }

        lastMinCount = Math.max(threshold - 1, 2);

        if (data.size() > size / 2) {
            minCount++;
        }
    }

    /**
     * 优化的清理方法
     */
    public void clean() {
        java.util.HashMap<String, IntCount> filteredData = new java.util.HashMap<>(data.size(), 0.75f);
        for (java.util.Map.Entry<String, IntCount> entry : data.entrySet()) {
            if (entry.getValue().getValue() > minCount) {
                filteredData.put(entry.getKey(), entry.getValue());
            }
        }
        data = filteredData;
    }

    /**
     * 优化的结果获取方法
     */
    public List<WordCount> getListResult() {
        clean();

        List<WordCount> resultList = new ArrayList<>(data.size());
        for (java.util.Map.Entry<String, IntCount> entry : data.entrySet()) {
            resultList.add(new WordCount(entry.getKey(), entry.getValue().getValue()));
        }

        // 直接排序，不使用并行流（小数据集反而慢）
        resultList.sort(null);
        return resultList;
    }

    public java.util.HashMap<String, IntCount> getData() {
        return data;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getMinCount() {
        return minCount;
    }
}
