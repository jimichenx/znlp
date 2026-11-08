package dev.znlp.perceptron.feature;

/**
 * 特征ID查找接口，用于将特征名称映射为特征ID。
 * <p>
 * 该接口定义了特征名称到特征ID的映射关系，是结构化感知机框架中连接特征提取和模型参数的关键组件。
 * 它允许模型在训练和推理过程中高效地查找特征对应的索引，从而访问模型参数。
 * <p>
 * 特征ID的分配和管理由具体实现类负责，不同的实现可以采用不同的策略，
 * 如哈希表、前缀树等，以平衡内存使用和查询效率。
 *
 * @author jimichan
 */
public interface FeatureIdSeek {

    /**
     * 根据特征名称获取特征ID。
     * <p>
     * 该方法是特征ID查找的核心功能，用于将特征名称转换为特征ID，
     * 以便在模型参数数组中快速定位对应的权重。
     *
     * @param feature 特征名称，通常是由特征提取函数生成的字符串
     * @return 特征ID，如果特征不存在则返回-1
     */
    int featureId(String feature);

    /**
     * 用字符缓冲查特征号。默认实现会先做成字符串，推理用的特征集应直接扫缓冲。
     */
    default int featureId(char[] chars, int length) {
        if (chars == null || length <= 0) {
            return -1;
        }
        return featureId(new String(chars, 0, length));
    }
}
