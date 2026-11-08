package dev.znlp.newwords.scorer;

/**
 * 新词打分器工厂类
 *
 * <p>提供默认的打分器实例和常用打分器对象</p>
 * <p>
 * 推荐使用：
 * <ul>
 *   <li>{@link #createAdaptive()} - 自适应打分器（推荐，零配置）</li>
 *   <li>{@link #createAdaptiveManual(float, float, int, int, int)} - 改进打分器（需手动设置阈值）</li>
 *   <li>{@link #createDefault()} - 默认打分器（基础版）</li>
 * </ul>
 * </p>
 *
 * @author jimichan
 */
public class NewWordScorerFactory {

    /**
     * 默认打分器实例
     * 基于平均互信息和左右熵的综合打分算法
     */
    public static final NewWordScorer DEFAULT_SCORER = AdaptiveNewWordScorer.createAdaptive();

    /**
     * 创建默认打分器
     *
     * @return 默认打分器实例
     */
    public static NewWordScorer createDefault() {
        return DEFAULT_SCORER;
    }

    // ==================== 自适应打分器（推荐） ====================

    /**
     * 创建自适应打分器（推荐使用）
     * <p>
     * 特点：
     * <ul>
     *   <li>✅ 零配置：无需手动设置阈值</li>
     *   <li>✅ 自动适应：根据语料特点自动学习最佳阈值</li>
     *   <li>✅ 智能过滤：基于统计分布的多级过滤</li>
     *   <li>✅ 完整性检查：左右熵平衡性验证</li>
     * </ul>
     * </p>
     *
     * @return 自适应打分器实例（使用平衡策略）
     */
    public static NewWordScorer createAdaptive() {
        return AdaptiveNewWordScorer.createAdaptive();
    }

    /**
     * 创建自适应打分器，指定过滤策略
     *
     * @param strategy 过滤策略
     *                 <ul>
     *                   <li>STRICT - 严格模式：只保留最高质量词（保留前25%）</li>
     *                   <li>BALANCED - 平衡模式：平衡准确率和召回率（保留前50%，推荐）</li>
     *                   <li>LOOSE - 宽松模式：尽可能多召回（保留前70%）</li>
     *                 </ul>
     * @return 自适应打分器实例
     */
    public static NewWordScorer createAdaptive(AdaptiveNewWordScorer.FilterStrategy strategy) {
        return AdaptiveNewWordScorer.createAdaptive(strategy);
    }

    /**
     * 创建半自适应打分器
     * <p>
     * 允许微调百分位数和策略，在自动化和控制力之间取得平衡
     * </p>
     *
     * @param percentile 百分位数（0-1）
     *                   例如：0.7表示保留评分在前30%的词，0.5表示保留前50%
     * @param strategy   过滤策略
     * @return 半自适应打分器实例
     */
    public static NewWordScorer createSemiAdaptive(
            float percentile, AdaptiveNewWordScorer.FilterStrategy strategy) {
        return AdaptiveNewWordScorer.createSemiAdaptive(percentile, strategy);
    }

    /**
     * 创建自适应打分器（手动模式）
     * <p>
     * 兼容旧版 AdvancedNewWordScorer 的使用方式，
     * 但使用 AdaptiveNewWordScorer 的改进评分公式
     * </p>
     *
     * @param minMi              最小互信息阈值
     * @param minEntropy         最小熵阈值
     * @param minAccessorVariety 最小左右邻字种类数
     * @param minFreq            最小词频
     * @param minDocFreq         最小文档频率
     * @return 自适应打分器实例（手动模式）
     */
    public static NewWordScorer createAdaptiveManual(
            float minMi, float minEntropy, int minAccessorVariety,
            int minFreq, int minDocFreq) {
        return AdaptiveNewWordScorer.createManual(
                minMi, minEntropy, minAccessorVariety, minFreq, minDocFreq
        );
    }
}
