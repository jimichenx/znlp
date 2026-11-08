package dev.znlp.newwords.counter;

/**
 * 高频元素统计器工厂类。
 *
 * <p>该工厂类负责创建 {@link IFrequentItemsCounter} 的不同实现版本。
 * 通过工厂方法，可以在运行时选择使用标准版本还是优化版本，
 * 便于性能测试和版本对比。</p>
 *
 * <h3>可用实现版本</h3>
 * <ul>
 *   <li><b>STANDARD</b>: 标准版本 {@link FrequentItemsCounter}，采用渐进式压缩策略</li>
 *   <li><b>OPTIMIZED</b>: 优化版本 {@link FrequentItemsCounterOptimized}，采用快速重建策略和预分配优化</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用标准版本
 * IFrequentItemsCounter counter1 = FrequentItemsCounterFactory.create(
 *     FrequentItemsCounterFactory.Version.STANDARD,
 *     10000, 5
 * );
 *
 * // 使用优化版本
 * IFrequentItemsCounter counter2 = FrequentItemsCounterFactory.create(
 *     FrequentItemsCounterFactory.Version.OPTIMIZED,
 *     10000, 5
 * );
 * }</pre>
 *
 * @author jimichan
 * @see IFrequentItemsCounter
 * @see FrequentItemsCounter
 * @see FrequentItemsCounterOptimized
 */
public class FrequentItemsCounterFactory {

    /**
     * 高频元素统计器的实现版本。
     */
    public enum Version {
        /**
         * 标准版本：采用渐进式压缩策略，每次压缩时逐步提升阈值。
         * 适合内存充足、需要稳定性能的场景。
         */
        STANDARD,

        /**
         * 优化版本：采用快速重建策略、预分配优化和频次分布缓存。
         * 适合大规模数据处理、追求极致性能的场景。
         */
        OPTIMIZED
    }

    /**
     * 创建高频元素统计器实例。
     *
     * @param version  实现版本，选择 STANDARD 或 OPTIMIZED
     * @param size     容量上限，当元素数量达到此值时触发压缩机制
     * @param minCount 初始最小频次阈值，频次低于此值的元素会被过滤
     * @return 高频元素统计器实例
     * @throws IllegalArgumentException 如果版本参数为 null
     */
    public static IFrequentItemsCounter create(Version version, int size, int minCount) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }

        switch (version) {
            case STANDARD:
                return new FrequentItemsCounter(size, minCount);
            case OPTIMIZED:
                return new FrequentItemsCounterOptimized(size, minCount);
            default:
                throw new IllegalArgumentException("Unknown version: " + version);
        }
    }

    /**
     * 创建标准版本的高频元素统计器。
     *
     * <p>这是一个便捷方法，等价于 {@code create(Version.STANDARD, size, minCount)}。</p>
     *
     * @param size     容量上限
     * @param minCount 最小频次阈值
     * @return 标准版本的统计器实例
     */
    public static IFrequentItemsCounter createStandard(int size, int minCount) {
        return new FrequentItemsCounter(size, minCount);
    }

    /**
     * 创建优化版本的高频元素统计器。
     *
     * <p>这是一个便捷方法，等价于 {@code create(Version.OPTIMIZED, size, minCount)}。</p>
     *
     * @param size     容量上限
     * @param minCount 最小频次阈值
     * @return 优化版本的统计器实例
     */
    public static IFrequentItemsCounter createOptimized(int size, int minCount) {
        return new FrequentItemsCounterOptimized(size, minCount);
    }

}
