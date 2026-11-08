package dev.znlp.perceptron;

import dev.znlp.perceptron.train.EvaluationSample;

import java.util.List;

/**
 * 评估函数接口，用于评估感知机模型的性能。
 * <p>
 * 该接口是一个函数式接口，用于评估感知机模型在给定测试样本上的性能。
 * 它定义了模型评估的核心逻辑，允许使用不同的评估指标和策略。
 * <p>
 * 在结构化感知机框架中，评估函数通常用于：
 * 1. 验证模型在训练过程中的性能
 * 2. 比较不同模型或不同参数设置的性能
 * 3. 生成模型的评估报告
 * <p>
 * 该接口的设计允许使用Lambda表达式或方法引用，使代码更加简洁和易读。
 *
 * @author jimichan
 * @see EvaluateResult
 * @see EvaluationSample
 */
@FunctionalInterface
public interface EvaluateFunction {
    /**
     * 评估模型在给定测试样本上的性能。
     * <p>
     * 该方法是评估函数的核心功能，用于计算模型在测试样本上的性能指标，
     * 如准确率、召回率、F1值等。
     *
     * @param samples 测试样本列表，每个样本包含输入数据和真实标签
     * @return 评估结果，包含模型的性能指标
     */
    EvaluateResult evaluate(List<EvaluationSample> samples);
}
