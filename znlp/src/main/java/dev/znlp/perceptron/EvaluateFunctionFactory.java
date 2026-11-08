package dev.znlp.perceptron;

import dev.znlp.perceptron.feature.ImmutableFeatureSet;

/**
 * 评估函数工厂接口，用于创建评估函数。
 * <p>
 * 该接口定义了创建评估函数的工厂方法，用于评估感知机模型在验证集或测试集上的性能。
 * 评估函数的具体实现由工厂方法创建，不同的任务可能需要不同的评估指标和策略。
 * <p>
 * 评估函数工厂的设计允许灵活配置不同的评估策略，使其适用于各种NLP任务，
 * 如分词、词性标注、命名实体识别等。
 *
 * @author jimichan
 * @see EvaluateFunction
 */
public interface EvaluateFunctionFactory {
    /**
     * 创建评估函数。
     * <p>
     * 根据给定的解码器和特征集，创建并返回相应的评估函数。
     *
     * @param decoder    感知机解码器，用于对测试数据进行解码
     * @param featureSet 不可变特征集，包含模型的所有特征信息
     * @return 创建的评估函数实例
     */
    EvaluateFunction create(PerceptronDecoder decoder, ImmutableFeatureSet featureSet);
}