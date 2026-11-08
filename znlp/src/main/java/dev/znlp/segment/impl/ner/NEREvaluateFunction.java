package dev.znlp.segment.impl.ner;

import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.train.EvaluationSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * NER Evaluate Function
 *
 * @author jimichan
 */
public class NEREvaluateFunction implements EvaluateFunction {

    private final PerceptronDecoder decoder;
    private final ImmutableFeatureSet featureSet;
    private final String[] labels;

    public NEREvaluateFunction(PerceptronDecoder decoder, ImmutableFeatureSet featureSet, String[] labels) {
        this.decoder = decoder;
        this.featureSet = featureSet;
        this.labels = labels;
    }

    @Override
    public EvaluateResult evaluate(List<EvaluationSample> samples) {
        Map<String, double[]> scores = new TreeMap<>();
        double[] avg = new double[]{0.0, 0.0, 0.0};
        scores.put("avg.", avg);

        for (EvaluationSample sample : samples) {
            // Decode
            int[] predIds = decoder.decode(sample.getFeatureSequence());
            List<String> predLabels = new ArrayList<>(predIds.length);
            for (int id : predIds) {
                predLabels.add(labels[id]);
            }

            int[] goldIds = sample.getLabel();
            List<String> goldLabels = new ArrayList<>(goldIds.length);
            for (int id : goldIds) {
                goldLabels.add(labels[id]);
            }

            Set<String> goldEntities = NEREvaluateUtils.combineNER(goldLabels);
            Set<String> predEntities = NEREvaluateUtils.combineNER(predLabels);

            for (String p : predEntities) {
                String type = p.split("\t")[2];
                double[] s = scores.computeIfAbsent(type, k -> new double[]{0.0, 0.0, 0.0});
                
                if (goldEntities.contains(p)) {
                    s[2]++; // Correct count
                    avg[2]++;
                }
                s[0]++; // Pred count
                avg[0]++;
            }

            for (String g : goldEntities) {
                String type = g.split("\t")[2];
                double[] s = scores.computeIfAbsent(type, k -> new double[]{0.0, 0.0, 0.0});
                s[1]++; // Gold count
                avg[1]++;
            }
        }

        // Calculate P, R, F1
        for (double[] s : scores.values()) {
            if (s[2] == 0.0) {
                s[0] = 0.0; // P
                s[1] = 0.0; // R
                continue;
            }
            double recall = s[2] / s[1] * 100;
            double precision = s[2] / s[0] * 100;
            double f1 = 2.0 * precision * recall / (precision + recall);
            
            s[0] = precision;
            s[1] = recall;
            s[2] = f1;
        }

        // Print result
        NEREvaluateUtils.printNERScore(scores);

        // Return a summary string
        double[] avgScore = scores.get("avg.");
        return new EvaluateResult((float) avgScore[0], (float) avgScore[1]);
    }
}
