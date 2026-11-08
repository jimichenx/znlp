package dev.znlp.newwords.counter;

import dev.znlp.newwords.entity.IntCount;
import dev.znlp.newwords.entity.WordCount;

import java.util.HashMap;
import java.util.List;

/**
 * 高频元素统计器接口。
 *
 * <p>该接口定义了统计高频元素的核心方法，支持在大量数据流中识别出现频次最高的元素。
 * 实现类需要在内存受限的情况下，采用压缩机制控制内存使用。</p>
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>元素统计</b>：记录元素出现的次数</li>
 *   <li><b>内存控制</b>：自动压缩低频元素以控制内存占用</li>
 *   <li><b>结果排序</b>：按频次降序返回统计结果</li>
 *   <li><b>数据清理</b>：过滤低于阈值的元素</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>新词发现：统计文本中高频出现的词组</li>
 *   <li>热点分析：识别访问频繁的资源</li>
 *   <li>流式数据处理：在有限内存下处理海量数据</li>
 * </ul>
 *
 * @author jimichan
 * @see FrequentItemsCounter
 * @see FrequentItemsCounterOptimized
 * @see FrequentItemsCounterFactory
 */
public interface IFrequentItemsCounter {

    /**
     * 记录一个元素的出现。
     *
     * <p>如果是新元素则初始化计数为1，如果已存在则频次+1。
     * 当元素总数达到容量上限时，会自动触发压缩机制清理低频元素。</p>
     *
     * @param key 要统计的元素（通常是字符串形式的词或短语）
     */
    void put(String key);

    /**
     * 最终清理，删除所有低于最小阈值的元素。
     *
     * <p>该方法通常在获取结果前调用，确保返回的都是高频元素。</p>
     */
    void clean();

    /**
     * 获取按频次降序排列的结果列表。
     *
     * <p>该方法会先执行 {@link #clean()} 清理低频元素，
     * 然后将数据转换为 {@link WordCount} 列表并按频次降序排序。</p>
     *
     * @return 排序后的高频元素列表，频次从高到低
     */
    List<WordCount> getListResult();

    /**
     * 获取底层存储的原始数据。
     *
     * <p><b>警告</b>：直接修改返回的 HashMap 可能破坏统计器的内部状态。</p>
     *
     * @return 元素与计数器的映射表
     */
    HashMap<String, IntCount> getData();

    /**
     * 设置是否启用调试模式。
     *
     * <p>开启后，压缩过程会输出详细日志到标准输出，
     * 包括清洗前数据量、每步过滤的阈值和剩余数量。</p>
     *
     * @param verbose true 启用调试输出，false 关闭（默认）
     */
    void setVerbose(boolean verbose);
}
