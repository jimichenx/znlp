package dev.znlp.perceptron;

import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.feature.CharDSLFeatureExtractor;
import dev.znlp.perceptron.utils.FastStringBuilder;

/**
 * 特征提取函数接口，用于从输入序列中提取特征。
 * <p>
 * 该接口定义了特征提取的核心逻辑，是结构化感知机框架中连接输入数据和模型参数的关键组件。
 * 每个具体任务需要实现此接口，定义如何从输入序列的特定位置提取有效特征。
 *
 * @param <InputSequence> 输入序列类型，用于表示模型的输入数据
 * @author jimichan
 */
public interface FeatureExtractor<InputSequence> {
    /**
     * 计算指定位置的特征。
     * <p>
     * 从输入序列的指定位置提取特征，并通过FeatureConsumer接口将特征传递给调用者。
     * 使用FastStringBuilder来构建特征字符串，提高性能。
     *
     * @param sentence 输入序列，如句子或文本片段
     * @param position 当前处理的位置索引
     * @param buffer   用于构建特征字符串的缓冲区，避免频繁创建字符串对象
     * @param consumer 特征消费者，用于接收和处理提取的特征
     */
    void compute(InputSequence sentence,
                 int position,
                 FastStringBuilder buffer,
                 FeatureConsumer consumer);

    /**
     * 根据模板规则构建特征提取函数。
     * <p>
     * 用于从模板规则字符串创建特征提取函数，支持DSL（领域特定语言）形式的特征模板定义。
     * 目前仅支持char[]类型的输入序列。
     *
     * @param templateRules 特征模板规则，使用DSL语法定义
     * @return 构建的特征提取函数实例
     */
    static FeatureExtractor<char[]> buildByTemplate(String templateRules) {
        return new CharDSLFeatureExtractor(templateRules);
    }
}