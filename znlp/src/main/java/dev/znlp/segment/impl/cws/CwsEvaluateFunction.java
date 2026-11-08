package dev.znlp.segment.impl.cws;

import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.train.EvaluationSample;
import dev.znlp.perceptron.utils.WordsSplitEvaluateUtils;

import java.util.Arrays;
import java.util.List;

class CwsEvaluateFunction implements EvaluateFunction {

    private final PerceptronDecoder decoder;
    private final ImmutableFeatureSet featureSet;

    CwsEvaluateFunction(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
        this.decoder = decoder;
        this.featureSet = featureSet;
    }

    @Override
    public EvaluateResult evaluate(List<EvaluationSample> samples) {
        CwsPerceptron cws = new CwsPerceptron(decoder, featureSet);
        int goldTotal = 0;
        int predTotal = 0;
        int correct = 0;

        for (EvaluationSample sample : samples) {
            char[] text = (char[]) sample.getInputSequence();
            List<String> result = cws.split(text, false);
            List<String> answer = Arrays.stream(sample.getLine().split("﹍"))
                    .filter(s -> !s.isEmpty())
                    .toList();
            goldTotal += answer.size();
            predTotal += result.size();
            correct += WordsSplitEvaluateUtils.wordCorrect(answer, result);
        }
        return new EvaluateResult(goldTotal, predTotal, correct);
    }
}
