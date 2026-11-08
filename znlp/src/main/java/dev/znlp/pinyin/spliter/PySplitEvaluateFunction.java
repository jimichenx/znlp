package dev.znlp.pinyin.spliter;

import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.train.EvaluationSample;
import dev.znlp.perceptron.utils.WordsSplitEvaluateUtils;

import java.util.Arrays;
import java.util.List;

class PySplitEvaluateFunction implements EvaluateFunction {

    private final PerceptronDecoder decoder;
    private final ImmutableFeatureSet featureSet;

    public PySplitEvaluateFunction(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
        this.decoder = decoder;
        this.featureSet = featureSet;
    }

    @Override
    public EvaluateResult evaluate(List<EvaluationSample> samples) {
        PinyinSplitPerceptron sp = new PinyinSplitPerceptron(decoder, featureSet);

        int goldTotal = 0;  // 正确答案中的拼音总数
        int predTotal = 0;  // 预测结果中的拼音总数
        int correct = 0;    // 正确匹配的拼音数量

        for (EvaluationSample evaluationSample : samples) {
            char[] text = (char[]) evaluationSample.getInputSequence();
            List<String> result = sp.split(text);
            List<String> answer = Arrays.stream(evaluationSample.getLine().split("﹍"))
                    .filter(s -> !s.isEmpty())
                    .toList();

            // 统计总数
            goldTotal += answer.size();
            predTotal += result.size();

            // 计算正确匹配的拼音数量，使用字符位置对齐的方式
            correct += WordsSplitEvaluateUtils.wordCorrect(answer, result);
        }

        // 使用 EvaluateResult 的构造函数，自动计算准确率和召回率
        return new EvaluateResult(goldTotal, predTotal, correct);
    }
}