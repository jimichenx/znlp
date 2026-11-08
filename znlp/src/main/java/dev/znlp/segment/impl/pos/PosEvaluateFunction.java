package dev.znlp.segment.impl.pos;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.train.EvaluationSample;

import java.util.List;

/**
 * 词性标注评估函数
 *
 * @author jimichan
 */
class PosEvaluateFunction implements EvaluateFunction {

    private final PerceptronDecoder decoder;

    PosEvaluateFunction(PerceptronDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public EvaluateResult evaluate(List<EvaluationSample> samples) {
        int totalTags = 0;
        int correctTags = 0;

        for (EvaluationSample sample : samples) {
            List<IntArrayList> featureSequence = sample.getFeatureSequence();
            int[] goldLabels = sample.getLabel();
            int[] predLabels = new int[goldLabels.length];

            decoder.decode(featureSequence, predLabels);

            totalTags += goldLabels.length;
            for (int i = 0; i < goldLabels.length; i++) {
                if (goldLabels[i] == predLabels[i]) {
                    correctTags++;
                }
            }
        }

        // 对于词性标注，预测总数等于实际总数，因此P=R=Accuracy
        return new EvaluateResult(totalTags, totalTags, correctTags);
    }
}
