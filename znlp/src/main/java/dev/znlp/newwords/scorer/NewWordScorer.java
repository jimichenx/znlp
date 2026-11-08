package dev.znlp.newwords.scorer;

import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.entity.WordInfo;

/**
 * 新词打分和过滤接口
 *
 * <p>定义新词的评分计算和过滤逻辑，不同的实现可以采用不同的评分和过滤策略</p>
 *
 * @author jimichan
 */
public interface NewWordScorer {

    /**
     * 计算新词的综合得分
     *
     * @param newWord 新词对象，包含各项统计指标
     * @return 综合得分
     */
    float score(NewWord newWord);

    /**
     * 判断候选词是否通过过滤
     *
     * @param wordInfo 候选词信息
     * @return true 表示通过过滤，false 表示被过滤掉
     */
    boolean accept(WordInfo wordInfo);

    /**
     * 获取打分器名称
     *
     * @return 打分器名称
     */
    String getName();

    /**
     * 设置全局统计信息
     * <p>
     * 该方法由引擎在第二轮扫描完成后（endSecond）自动调用，
     * 为打分器提供语料的全局统计数据。
     * </p>
     * <p>
     * 打分器实现可以选择使用或忽略这些统计信息，
     * 例如高级打分器可能需要字符频率数组来计算NPMI，
     * 而简单打分器可能完全不需要这些信息。
     * </p>
     *
     * @param totalCharCount 语料总字符数
     * @param totalDocCount  总文档数
     * @param charFreqArray  字符频率数组（长度为65535，索引为字符的int值）
     */
    default void setStatistics(int totalCharCount, int totalDocCount, int[] charFreqArray) {
        // 默认实现为空，不需要统计信息的打分器可以不实现此方法
    }
}
