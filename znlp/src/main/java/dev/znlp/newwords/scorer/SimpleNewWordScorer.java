package dev.znlp.newwords.scorer;

import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.entity.WordInfo;

/**
 * 默认新词打分器
 *
 * <p>基于平均互信息和左右熵的综合打分算法</p>
 * <p>公式: score = avg_mi + log₂((le·e^re + re·e^le) / |le - re|)</p>
 * <p>使用默认阈值进行过滤：互信息 > 3.0，熵 > 0.5</p>
 *
 * @author jimichan
 */
public class SimpleNewWordScorer implements NewWordScorer {

    /**
     * 默认互信息阈值
     */
    public static final float DEFAULT_MIN_MI = 3.0f;

    /**
     * 默认熵阈值
     */
    public static final float DEFAULT_MIN_ENTROPY = 0.5f;

    private static final SimpleNewWordScorer INSTANCE = new SimpleNewWordScorer();

    /**
     * 互信息阈值
     */
    private final float minMi;

    /**
     * 熵阈值
     */
    private final float minEntropy;

    /**
     * 获取默认实例（使用默认阈值）
     */
    public static SimpleNewWordScorer getInstance() {
        return INSTANCE;
    }

    /**
     * 创建使用指定阈值的新实例
     *
     * @param minMi      最小互信息阈值
     * @param minEntropy 最小熵阈值
     * @return 新的打分器实例
     */
    public static SimpleNewWordScorer create(float minMi, float minEntropy) {
        return new SimpleNewWordScorer(minMi, minEntropy);
    }

    /**
     * 使用默认阈值创建打分器
     */
    private SimpleNewWordScorer() {
        this(DEFAULT_MIN_MI, DEFAULT_MIN_ENTROPY);
    }

    /**
     * 使用指定阈值创建打分器
     *
     * @param minMi      最小互信息阈值
     * @param minEntropy 最小熵阈值
     */
    private SimpleNewWordScorer(float minMi, float minEntropy) {
        this.minMi = minMi;
        this.minEntropy = minEntropy;
    }

    @Override
    public float score(NewWord newWord) {
        float le = newWord.getLe();
        float re = newWord.getRe();
        float avg_mi = newWord.getAvg_mi();

        float ac = Math.abs(le - re);
        if (ac == 0.0f) {
            ac = 0.00000000001f;
        }
        return (float) (avg_mi + Math.log((le * Math.exp(re) + re * Math.exp(le)) / ac) / Math.log(2));
    }

    @Override
    public boolean accept(WordInfo wordInfo) {
        return wordInfo.getMi() > minMi && wordInfo.getEntropy() > minEntropy;
    }

    @Override
    public String getName() {
        return "DefaultNewWordScorer";
    }

    /**
     * 获取互信息阈值
     */
    public float getMinMi() {
        return minMi;
    }

    /**
     * 获取熵阈值
     */
    public float getMinEntropy() {
        return minEntropy;
    }
}
