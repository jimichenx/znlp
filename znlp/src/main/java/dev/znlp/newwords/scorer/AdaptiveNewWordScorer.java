package dev.znlp.newwords.scorer;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.entity.WordInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自适应新词评分器 - 智能过滤版本
 * <p>
 * 主要改进：
 * <ol>
 *   <li>自动学习阈值：基于统计分布，无需人工设置</li>
 *   <li>多级过滤策略：粗过滤+精过滤</li>
 *   <li>完整性检查：左右熵平衡性验证</li>
 *   <li>异常检测：使用百分位数识别高质量词</li>
 * </ol>
 * <p>
 * 使用方式：
 * <pre>
 * // 模式1：完全自适应（推荐）
 * AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
 *
 * // 模式2：半自适应（保留部分控制）
 * AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createSemiAdaptive(
 *     0.25f,  // 百分位数（保留前25%的词）
 *     FilterStrategy.BALANCED  // 过滤策略
 * );
 *
 * // 模式3：手动模式（兼容旧版）
 * AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createManual(
 *     minMi, minEntropy, minAccessorVariety, minFreq, minDocFreq
 * );
 * </pre>
 *
 * @author jimichan
 */
public class AdaptiveNewWordScorer implements NewWordScorer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AdaptiveNewWordScorer.class);

    /**
     * 过滤策略
     */
    public enum FilterStrategy {
        /**
         * 严格模式：只保留最高质量的词
         * 适用于：需要高精确度的场景
         */
        STRICT,

        /**
         * 平衡模式：平衡准确率和召回率
         * 适用于：通用场景（推荐）
         */
        BALANCED,

        /**
         * 宽松模式：尽可能多地召回新词
         * 适用于：需要高召回率的场景
         */
        LOOSE
    }

    /**
     * 工作模式
     */
    public enum WorkMode {
        /**
         * 自适应模式：完全自动学习阈值
         */
        ADAPTIVE,

        /**
         * 半自适应模式：使用百分位数，但可调整
         */
        SEMI_ADAPTIVE,

        /**
         * 手动模式：完全手动设置阈值
         */
        MANUAL
    }

    // ==================== 配置参数 ====================
    private final WorkMode workMode;
    private final FilterStrategy filterStrategy;
    private final float percentile; // 百分位数（0-1），用于自适应模式

    // ==================== 手动阈值（仅MANUAL模式使用） ====================
    private final float manualMinMi;
    private final float manualMinEntropy;
    private final int manualMinAccessorVariety;
    private final int manualMinFreq;
    private final int manualMinDocFreq;

    // ==================== 统计信息 ====================
    private int totalCharCount = 0;
    private int totalDocCount = 0;
    private int[] charFreqArray = null;

    // ==================== 学习到的阈值（自适应模式） ====================
    private volatile boolean thresholdsLearned = false;
    private float learnedMinMi = 0.0f;
    private float learnedMinEntropy = 0.0f;
    private float learnedMinScore = 0.0f;
    private int learnedMinAccessorVariety = 2;
    private int learnedMinFreq = 3;
    private int learnedMinDocFreq = 2;
    private float learnedMaxEntropyImbalance = 2.0f; // 左右熵最大比例差异

    // ==================== 工厂方法 ====================

    /**
     * 创建完全自适应的打分器（推荐）
     * 将自动学习最佳阈值
     */
    public static AdaptiveNewWordScorer createAdaptive() {
        return new AdaptiveNewWordScorer(
                WorkMode.ADAPTIVE,
                FilterStrategy.BALANCED,
                0.5f, // 默认保留前50%
                0, 0, 0, 0, 0
        );
    }

    /**
     * 创建自适应打分器，指定过滤策略
     *
     * @param strategy 过滤策略
     */
    public static AdaptiveNewWordScorer createAdaptive(FilterStrategy strategy) {
        float percentile = getDefaultPercentile(strategy);
        return new AdaptiveNewWordScorer(
                WorkMode.ADAPTIVE,
                strategy,
                percentile,
                0, 0, 0, 0, 0
        );
    }

    /**
     * 创建半自适应打分器
     *
     * @param percentile 百分位数（0-1），例如0.25表示保留前25%的词
     * @param strategy   过滤策略
     */
    public static AdaptiveNewWordScorer createSemiAdaptive(
            float percentile, FilterStrategy strategy) {
        return new AdaptiveNewWordScorer(
                WorkMode.SEMI_ADAPTIVE,
                strategy,
                percentile,
                0, 0, 0, 0, 0
        );
    }

    /**
     * 创建手动模式打分器（兼容旧版）
     *
     * @param minMi              最小互信息阈值
     * @param minEntropy         最小熵阈值
     * @param minAccessorVariety 最小左右邻字种类数
     * @param minFreq            最小词频
     * @param minDocFreq         最小文档频率
     */
    public static AdaptiveNewWordScorer createManual(
            float minMi, float minEntropy, int minAccessorVariety,
            int minFreq, int minDocFreq) {
        return new AdaptiveNewWordScorer(
                WorkMode.MANUAL,
                FilterStrategy.BALANCED,
                0.5f,
                minMi, minEntropy, minAccessorVariety, minFreq, minDocFreq
        );
    }

    /**
     * 根据策略获取默认百分位数
     */
    private static float getDefaultPercentile(FilterStrategy strategy) {
        switch (strategy) {
            case STRICT:
                return 0.75f; // 保留前25%
            case BALANCED:
                return 0.5f;  // 保留前50%
            case LOOSE:
                return 0.3f;  // 保留前70%
            default:
                return 0.5f;
        }
    }

    /**
     * 私有构造函数
     */
    private AdaptiveNewWordScorer(
            WorkMode workMode,
            FilterStrategy filterStrategy,
            float percentile,
            float manualMinMi,
            float manualMinEntropy,
            int manualMinAccessorVariety,
            int manualMinFreq,
            int manualMinDocFreq) {
        this.workMode = workMode;
        this.filterStrategy = filterStrategy;
        this.percentile = percentile;
        this.manualMinMi = manualMinMi;
        this.manualMinEntropy = manualMinEntropy;
        this.manualMinAccessorVariety = manualMinAccessorVariety;
        this.manualMinFreq = manualMinFreq;
        this.manualMinDocFreq = manualMinDocFreq;
    }

    // ==================== NewWordScorer 接口实现 ====================

    @Override
    public void setStatistics(int totalCharCount, int totalDocCount, int[] charFreqArray) {
        this.totalCharCount = totalCharCount;
        this.totalDocCount = totalDocCount;
        this.charFreqArray = charFreqArray;
    }

    /**
     * 学习阈值（在第一次过滤前调用）
     * <p>
     * 该方法需要在所有词的统计完成后、过滤之前调用
     * </p>
     *
     * @param allWords 所有候选词列表
     */
    public void learnThresholds(List<WordInfo> allWords) {
        if (workMode == WorkMode.MANUAL) {
            // 手动模式不需要学习
            return;
        }

        if (allWords == null || allWords.isEmpty()) {
            logger.warn("词列表为空，无法学习阈值");
            return;
        }

        logger.info("开始学习阈值...");
        logger.info("  - 候选词数量: {}", allWords.size());
        logger.info("  - 工作模式: {}", workMode);
        logger.info("  - 过滤策略: {}", filterStrategy);
        logger.info("  - 百分位数: {}", percentile);

        // 收集各项指标
        List<Float> miList = new ArrayList<>();
        List<Float> entropyList = new ArrayList<>();
        List<Float> scoreList = new ArrayList<>();
        List<Float> entropyImbalanceList = new ArrayList<>();

        for (WordInfo word : allWords) {
            // 先计算评分（需要临时使用一个NewWord对象）
            NewWord newWord = createNewWord(word);
            float score = this.score(newWord);

            miList.add(word.getMi());
            entropyList.add(word.getEntropy());
            scoreList.add(score);

            // 计算左右熵不平衡度
            float le = word.getLe();
            float re = word.getRe();
            if (le > 0 && re > 0) {
                float imbalance = Math.max(le, re) / Math.min(le, re);
                entropyImbalanceList.add(imbalance);
            }
        }

        // 排序
        Collections.sort(miList);
        Collections.sort(entropyList);
        Collections.sort(scoreList);
        Collections.sort(entropyImbalanceList);

        // 根据百分位数计算阈值
        int miIndex = (int) (miList.size() * percentile);
        int entropyIndex = (int) (entropyList.size() * percentile);
        int scoreIndex = (int) (scoreList.size() * percentile);
        int imbalanceIndex = (int) (entropyImbalanceList.size() * (1.0f - percentile)); // 反向

        learnedMinMi = miList.get(Math.max(0, miIndex));
        learnedMinEntropy = entropyList.get(Math.max(0, entropyIndex));
        learnedMinScore = scoreList.get(Math.max(0, scoreIndex));

        if (!entropyImbalanceList.isEmpty()) {
            learnedMaxEntropyImbalance = entropyImbalanceList.get(
                    Math.min(imbalanceIndex, entropyImbalanceList.size() - 1)
            );
        }

        // 根据策略调整基础阈值
        adjustThresholdsByStrategy();

        thresholdsLearned = true;

        logger.info("  - 学习到的阈值:");
        logger.info("    * 最小互信息: {}", learnedMinMi);
        logger.info("    * 最小熵: {}", learnedMinEntropy);
        logger.info("    * 最小评分: {}", learnedMinScore);
        logger.info("    * 最大熵不平衡度: {}", learnedMaxEntropyImbalance);
        logger.info("    * 最小邻字种类: {}", learnedMinAccessorVariety);
        logger.info("    * 最小词频: {}", learnedMinFreq);
        logger.info("    * 最小文档频率: {}", learnedMinDocFreq);
    }

    /**
     * 根据策略调整阈值
     */
    private void adjustThresholdsByStrategy() {
        switch (filterStrategy) {
            case STRICT:
                // 严格模式：提高所有阈值
                learnedMinMi = Math.max(learnedMinMi, 3.0f);
                learnedMinEntropy = Math.max(learnedMinEntropy, 2.0f);
                learnedMinAccessorVariety = 3;
                learnedMinFreq = 5;
                learnedMinDocFreq = 3;
                learnedMaxEntropyImbalance = 2.0f;
                break;

            case BALANCED:
                // 平衡模式：适中阈值
                learnedMinMi = Math.max(learnedMinMi, 1.0f);
                learnedMinEntropy = Math.max(learnedMinEntropy, 1.0f);
                learnedMinAccessorVariety = 2;
                learnedMinFreq = 3;
                learnedMinDocFreq = 2;
                learnedMaxEntropyImbalance = 3.0f;
                break;

            case LOOSE:
                // 宽松模式：较低阈值
                learnedMinMi = Math.max(learnedMinMi, 0.0f);
                learnedMinEntropy = Math.max(learnedMinEntropy, 0.5f);
                learnedMinAccessorVariety = 2;
                learnedMinFreq = 2;
                learnedMinDocFreq = 1;
                learnedMaxEntropyImbalance = 4.0f;
                break;
        }
    }

    /**
     * 创建NewWord对象（用于评分）
     */
    private NewWord createNewWord(WordInfo wordInfo) {
        return new NewWord(
                wordInfo.getWord(),
                wordInfo.getWord().length(),
                wordInfo.getCount(),
                wordInfo.getDoc(),
                wordInfo.getMi(),
                wordInfo.getMi_avg(),
                wordInfo.getEntropy(),
                wordInfo.getLe(),
                wordInfo.getRe(),
                wordInfo.getIdf(),
                wordInfo.getLeftVariety(),
                wordInfo.getRightVariety()
        );
    }

    @Override
    public boolean accept(WordInfo wordInfo) {
        if (workMode == WorkMode.MANUAL) {
            return acceptManual(wordInfo);
        } else {
            return acceptAdaptive(wordInfo);
        }
    }

    /**
     * 手动模式的过滤
     */
    private boolean acceptManual(WordInfo wordInfo) {
        if (wordInfo.getCount() < manualMinFreq) {
            return false;
        }
        if (wordInfo.getDoc() < manualMinDocFreq) {
            return false;
        }
        if (wordInfo.getEntropy() < manualMinEntropy) {
            return false;
        }

        int minAV = Math.min(wordInfo.getLeftVariety(), wordInfo.getRightVariety());
        if (minAV < manualMinAccessorVariety) {
            return false;
        }

        return !(wordInfo.getMi() < manualMinMi);
    }

    /**
     * 自适应模式的过滤
     */
    private boolean acceptAdaptive(WordInfo wordInfo) {
        if (!thresholdsLearned) {
            // 如果还没学习阈值，使用宽松的默认值
            return wordInfo.getCount() >= 2 && wordInfo.getDoc() >= 1;
        }

        // 第一级过滤：基本统计阈值
        if (wordInfo.getCount() < learnedMinFreq) {
            return false;
        }
        if (wordInfo.getDoc() < learnedMinDocFreq) {
            return false;
        }

        // 第二级过滤：熵和互信息
        if (wordInfo.getEntropy() < learnedMinEntropy) {
            return false;
        }
        if (wordInfo.getMi() < learnedMinMi) {
            return false;
        }

        // 第三级过滤：邻字多样性
        int minAV = Math.min(wordInfo.getLeftVariety(), wordInfo.getRightVariety());
        if (minAV < learnedMinAccessorVariety) {
            return false;
        }

        // 第四级过滤：词的完整性检查（左右熵平衡）
        if (!checkEntropyBalance(wordInfo)) {
            return false;
        }

        // 第五级过滤：综合评分
        NewWord newWord = createNewWord(wordInfo);
        float score = this.score(newWord);
        return !(score < learnedMinScore);
    }

    /**
     * 检查左右熵的平衡性
     * <p>
     * 如果左熵或右熵过低，说明词不完整
     * </p>
     */
    private boolean checkEntropyBalance(WordInfo wordInfo) {
        float le = wordInfo.getLe();
        float re = wordInfo.getRe();

        // 如果任意一边熵太低，直接拒绝
        if (le < 0.5f || re < 0.5f) {
            return false;
        }

        // 检查左右熵的比例
        if (le > 0 && re > 0) {
            float imbalance = Math.max(le, re) / Math.min(le, re);
            return !(imbalance > learnedMaxEntropyImbalance); // 不平衡度过高
        }

        return true;
    }

    @Override
    public float score(NewWord newWord) {
        // 1. 归一化互信息 (NPMI)
        float npmi = calculateNPMI(newWord);

        // 2. Accessor Variety
        int leftVariety = newWord.getLeftVariety();
        int rightVariety = newWord.getRightVariety();
        float av = Math.min(leftVariety, rightVariety);

        // 3. 分支熵得分（对数缩放）
        float entropy = newWord.getEntropy();
        float entropyScore = (float) (Math.log(entropy + 1) * 2.0);

        // 4. 文档分布得分
        float docSpreadScore = calculateDocSpreadScore(
                newWord.getFreq(),
                newWord.getDocFreq(),
                totalDocCount
        );

        // 5. 词频得分（对数缩放）
        float freqScore = (float) (Math.log(newWord.getFreq() + 1) / 5.0);

        // 6. 左右熵平衡性（改进：更强调平衡性）
        float entropyBalance = calculateEntropyBalance(newWord.getLe(), newWord.getRe());

        // 7. 词长惩罚（过长或过短的词降低权重）
        float lengthPenalty = calculateLengthPenalty(newWord.getWord().length());

        // 加权组合（调整权重，更强调完整性）
        float score =
                npmi * 3.5f +              // NPMI 权重最高
                        av * 2.0f +                // AV 权重提高
                        entropyScore * 2.5f +      // 熵权重提高
                        docSpreadScore +    // 文档分布
                        freqScore * 0.5f +         // 词频权重较低
                        entropyBalance * 2.0f +    // 熵平衡性权重大幅提高
                        lengthPenalty;      // 词长惩罚

        return score;
    }

    /**
     * 词长惩罚因子
     */
    private float calculateLengthPenalty(int length) {
        if (length == 2) {
            return 1.0f; // 2字词最常见，不惩罚
        } else if (length == 3 || length == 4) {
            return 1.2f; // 3-4字词加分
        } else if (length == 5 || length == 6) {
            return 1.0f; // 5-6字词正常
        } else if (length > 6) {
            return 0.5f; // 过长词降低权重
        } else {
            return 0.3f; // 单字词大幅降低
        }
    }

    /**
     * 归一化点互信息 (NPMI)
     */
    private float calculateNPMI(NewWord newWord) {
        if (totalCharCount == 0 || charFreqArray == null) {
            return newWord.getAvg_mi();
        }

        String word = newWord.getWord();
        int wordLen = word.length();

        float pAB = newWord.getFreq() / (float) totalCharCount;
        if (pAB <= 0) {
            return 0f;
        }

        float pIndep = 1.0f;
        for (int i = 0; i < wordLen; i++) {
            char c = word.charAt(i);
            int charFreq = (c < charFreqArray.length) ? charFreqArray[c] : 0;
            if (charFreq == 0) {
                pIndep *= 1.0f / totalCharCount;
            } else {
                pIndep *= charFreq / (float) totalCharCount;
            }
        }

        if (pIndep <= 0) {
            return 0f;
        }

        float pmi = (float) (Math.log(pAB / pIndep) / Math.log(2));
        float npmi = pmi / (float) (-Math.log(pAB) / Math.log(2));

        return Math.max(-1.0f, Math.min(1.0f, npmi));
    }

    /**
     * 文档分布得分
     */
    private float calculateDocSpreadScore(int freq, int docFreq, int totalDocCount) {
        if (docFreq == 0 || totalDocCount == 0) {
            return 0f;
        }

        float avgFreqPerDoc = freq / (float) docFreq;
        float docRatio = docFreq / (float) totalDocCount;

        if (avgFreqPerDoc >= 1.0f && avgFreqPerDoc <= 3.0f && docRatio > 0.1f) {
            return 2.0f;
        } else if (avgFreqPerDoc <= 5.0f && docRatio > 0.05f) {
            return 1.5f;
        } else if (docRatio > 0.02f) {
            return 1.0f;
        }

        return 0.5f;
    }

    /**
     * 左右熵平衡性（改进版）
     */
    private float calculateEntropyBalance(float le, float re) {
        if (le <= 0 || re <= 0) {
            return 0f;
        }

        // 惩罚过低的熵
        float minEntropy = Math.min(le, re);
        if (minEntropy < 1.0f) {
            return 0.5f; // 严重惩罚
        }

        float maxEntropy = Math.max(le, re);
        float balance = minEntropy / maxEntropy;

        // 转换为得分，更强调平衡性
        return balance * 3.0f;
    }

    @Override
    public String getName() {
        return "AdaptiveNewWordScorer[mode=" + workMode + ",strategy=" + filterStrategy + "]";
    }

    // ==================== Getter 方法 ====================

    public WorkMode getWorkMode() {
        return workMode;
    }

    public FilterStrategy getFilterStrategy() {
        return filterStrategy;
    }

    public float getPercentile() {
        return percentile;
    }

    public boolean isThresholdsLearned() {
        return thresholdsLearned;
    }

    public float getLearnedMinMi() {
        return learnedMinMi;
    }

    public float getLearnedMinEntropy() {
        return learnedMinEntropy;
    }

    public float getLearnedMinScore() {
        return learnedMinScore;
    }
}
