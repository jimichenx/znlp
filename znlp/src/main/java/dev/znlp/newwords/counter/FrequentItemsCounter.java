package dev.znlp.newwords.counter;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.newwords.entity.IntCount;
import dev.znlp.newwords.entity.WordCount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高频元素统计器，采用动态压缩机制控制内存使用。
 *
 * <p>该类用于在大量数据流中统计出现频次最高的元素（Top-K问题）。
 * 当内存中的元素数量达到设定的容量上限时，会自动触发压缩机制，
 * 删除低频元素以保持内存可控。</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>新词发现：统计文本中高频出现的词组</li>
 *   <li>热点分析：识别访问频繁的资源</li>
 *   <li>流式数据处理：在有限内存下处理海量数据</li>
 * </ul>
 *
 * <h3>核心机制</h3>
 * <ul>
 *   <li><b>动态阈值</b>：根据数据分布自动调整最小频次阈值</li>
 *   <li><b>分级压缩</b>：当容量达到上限时，渐进式删除低频元素</li>
 *   <li><b>内存可控</b>：保证最多占用 size 个元素的内存空间</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * FrequentItemsCounter counter = new FrequentItemsCounter(10000, 5);
 * for (String word : textStream) {
 *     counter.put(word);
 * }
 * List<WordCount> topWords = counter.getListResult();
 * }</pre>
 *
 * @author jimichan
 * @see WordCount
 * @see IntCount
 */
public class FrequentItemsCounter implements IFrequentItemsCounter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FrequentItemsCounter.class);

    /**
     * 容量上限：HashMap中最多保留的元素数量
     */
    private final int size;

    /**
     * 最小频次阈值：低于此值的元素会在压缩时被删除
     */
    private int minCount;

    /**
     * 调试模式：是否输出压缩过程的详细信息
     */
    private boolean verbose = false;

    /**
     * 核心存储结构：存储元素及其出现次数
     */
    private HashMap<String, IntCount> data;

    /**
     * 预留字段：当前未使用
     */
    private final List<String> topList = new ArrayList<>();

    /**
     * 上次压缩时使用的最小阈值，用于优化下次压缩的起始点
     */
    private int lastMinCount = 2;

    /**
     * 构造高频元素统计器。
     *
     * @param size     容量上限，当元素数量达到此值时触发压缩机制
     * @param minCount 初始最小频次阈值，频次低于此值的元素会被过滤
     */
    public FrequentItemsCounter(int size, int minCount) {
        this.size = size;
        this.minCount = minCount;
        this.data = new HashMap<>(size);
    }

    /**
     * 记录一个元素的出现。
     *
     * <p>如果是新元素则初始化计数为1，如果已存在则频次+1。
     * 当元素总数达到容量上限时，会自动触发压缩机制清理低频元素。</p>
     *
     * @param key 要统计的元素（通常是字符串形式的词或短语）
     */
    public void put(String key) {
        IntCount v = data.get(key);
        if (v == null) {
            // 首次出现，初始化计数器
            data.put(key, new IntCount());
        } else {
            // 已存在，递增计数
            v.increment();
        }

        // 达到容量上限，触发压缩
        if (data.size() >= size) {
            reduce();
        }
    }

    /**
     * 执行数据压缩，删除低频元素以释放内存。
     *
     * <p>压缩策略：</p>
     * <ol>
     *   <li>目标：将数据量压缩至容量的 1/4（size/4）</li>
     *   <li>方法：从 lastMinCount 开始递增阈值，逐步删除低频元素</li>
     *   <li>优化：记录本次最高阈值，下次从此值开始压缩</li>
     *   <li>兜底：如果压缩后仍超过容量一半，则提升全局最小阈值</li>
     * </ol>
     */
    private void reduce() {
        if (verbose) {
            logger.debug("清洗前有{}条数据", data.size());
        }

        // 压缩目标：降至容量的 1/4
        int targetSize = size / 4;
        int maxThresholdUsed = 0;

        // 渐进式提升阈值，删除低频元素
        for (int threshold = lastMinCount; threshold < minCount; threshold++) {
            if (data.size() > targetSize) {
                HashMap<String, IntCount> filteredData = new HashMap<>();
                for (Map.Entry<String, IntCount> entry : data.entrySet()) {
                    if (entry.getValue().getValue() > threshold) {
                        filteredData.put(entry.getKey(), entry.getValue());
                    }
                }
                data = filteredData;

                if (verbose) {
                    logger.debug("删除频次 ≤ {} 的元素，剩余 {}", threshold, data.size());
                }
                maxThresholdUsed = threshold;
            }
        }

        // 更新下次压缩的起始阈值（避免重复扫描）
        lastMinCount = Math.max(maxThresholdUsed - 1, 2);

        // 如果压缩后仍超过一半容量，提升全局最小阈值
        if (data.size() > size / 2) {
            minCount++;
        }
    }

    /**
     * 最终清理，删除所有低于最小阈值的元素。
     *
     * <p>该方法通常在获取结果前调用，确保返回的都是高频元素。</p>
     */
    public void clean() {
        HashMap<String, IntCount> filteredData = new HashMap<>();
        for (Map.Entry<String, IntCount> entry : data.entrySet()) {
            if (entry.getValue().getValue() > minCount) {
                filteredData.put(entry.getKey(), entry.getValue());
            }
        }
        data = filteredData;
    }

    /**
     * 获取按频次降序排列的结果列表。
     *
     * <p>该方法会先执行 {@link #clean()} 清理低频元素，
     * 然后将数据转换为 {@link WordCount} 列表并按频次降序排序。</p>
     *
     * @return 排序后的高频元素列表，频次从高到低
     */
    public List<WordCount> getListResult() {
        clean();
        List<WordCount> resultList = new ArrayList<>(data.size());

        for (Map.Entry<String, IntCount> entry : data.entrySet()) {
            resultList.add(new WordCount(entry.getKey(), entry.getValue().getValue()));
        }

        // 按频次降序排序（依赖 WordCount 的 Comparable 实现）
        resultList.sort(null);
        return resultList;
    }

    /**
     * 获取底层存储的原始数据。
     *
     * <p><b>警告</b>：直接修改返回的 HashMap 可能破坏统计器的内部状态。</p>
     *
     * @return 元素与计数器的映射表
     */
    public HashMap<String, IntCount> getData() {
        return data;
    }

    /**
     * 设置是否启用调试模式。
     *
     * <p>开启后，压缩过程会输出详细日志到标准输出，
     * 包括清洗前数据量、每步过滤的阈值和剩余数量。</p>
     *
     * @param verbose true 启用调试输出，false 关闭（默认）
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
