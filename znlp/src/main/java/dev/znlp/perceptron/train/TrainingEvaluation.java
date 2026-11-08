package dev.znlp.perceptron.train;

import dev.znlp.perceptron.EvaluateFunctionFactory;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.utils.EvaluateUtils;

import java.util.List;

/**
 * 训练过程中的模型评估回调类
 * <p>
 * 该类用于在感知机训练过程中，对每个迭代的模型进行评估。
 * 评估结果可以被用于监控训练进度、早停策略等。
 *
 * @author jimi
 */
public class TrainingEvaluation {
    private final EvaluateFunctionFactory factory;
    private final List<EvaluationSample> evaluationSampleList;

    /**
     * 创建评估回调对象
     *
     * @param factory   评估函数工厂，如果为null则使用默认的简单评估
     * @param evaluationSampleList 评估样本列表
     */
    public TrainingEvaluation(EvaluateFunctionFactory factory,
                              List<EvaluationSample> evaluationSampleList) {
        if (factory == null) {
            this.factory = EvaluateUtils.simpleEvaluateFactory();
        } else {
            this.factory = factory;
        }
        this.evaluationSampleList = evaluationSampleList == null ? List.of() : evaluationSampleList;
    }

    /**
     * 在训练迭代过程中评估模型
     *
     * @param iteration 当前迭代次数（从1开始）
     * @param model     当前训练中的模型
     * @return 评估结果，如果返回null表示不进行评估或评估被跳过
     */
    public EvaluateResult evaluate(int iteration, PerceptronTrainModel model) {
        if (evaluationSampleList.isEmpty()) {
            return null;
        }
        PerceptronDecoder decoder = model.getDecoder();
        return factory.create(decoder, model.toModel().getFeatureSet()).evaluate(evaluationSampleList);
    }
}
