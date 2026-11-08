package dev.znlp.perceptron.utils;

import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateFunctionFactory;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.train.EvaluationSample;

import java.util.List;

/**
 * 简单评估。只考虑标签相等性
 */
public class EvaluateUtils {

    public static EvaluateFunctionFactory simpleEvaluateFactory() {
        return (decoder, featureSet) ->
                new SimpleEvaluateFunction(decoder, featureSet);
    }

    static class SimpleEvaluateFunction implements EvaluateFunction {
        private final PerceptronDecoder decoder;
        private final ImmutableFeatureSet featureSet;

        public SimpleEvaluateFunction(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
            this.decoder = decoder;
            this.featureSet = featureSet;
        }


        @Override
        public EvaluateResult evaluate(List<EvaluationSample> samples) {
            int total = 0;
            int right = 0;
            for (EvaluationSample sample : samples) {
                total += sample.getLabel().length;
                int[] gold = sample.getLabel();
                int[] pred = decoder.decode(sample.getFeatureSequence());
                for (int i = 0; i < sample.getLabel().length; i++) {
                    if (gold[i] == pred[i]) {
                        right++;
                    }
                }
            }

            return new EvaluateResult(total, total, right);
        }
    }

    /**
     * 创建分词评估函数
     */
//    public static EvaluateFunction segmentEvaluateFunction(
//            Function<String, List<String>> textSegment,
//            String split,
//            boolean verbose) {
//
//        return (decoder, evaluateSample) -> {
//            int goldTotal = 0;
//            int predTotal = 0;
//            int correct = 0;
//
//            if (verbose) {
//                System.out.print("Evaluating 0%");
//            }
//
//            long t1 = System.currentTimeMillis();
//
//            int count = 0;
//            for (String line : evaluateSample) {
//                // 简单的字符串分割实现，替代CharNormUtils
//                String normalizedLine = normalizeString(line);
//                String[] wordArray = normalizedLine.split(split);
//                List<String> wordList = new ArrayList<>();
//                for (String word : wordArray) {
//                    if (word != null && !word.trim().isEmpty()) {
//                        wordList.add(word.trim());
//                    }
//                }
//                goldTotal += wordList.size();
//
//                String text = String.join("", wordList);
//                List<String> predArray = textSegment.apply(text);
//                predTotal += predArray.size();
//
//                correct += wordCorrect(wordList, predArray);
//
//                count++;
//
//                if (count % 2000 == 0 && verbose) {
//                    System.out.print(String.format("\rEvaluating %.2f%%", count * 100.0 / evaluateSample.size()));
//                }
//            }
//
//            double[] result = prf(goldTotal, predTotal, correct);
//
//            if (verbose) {
//                System.out.print("\r");
//                long t2 = System.currentTimeMillis();
//                System.out.println(String.format("正确率(P) %.2f , 召回率(R) %.2f , F1 %.2f",
//                        result[0], result[1], result[2]));
//                System.out.println("Evaluate use time " + (t2 - t1) + " ms");
//            }
//
//            return new EvaluateResult((float) result[0], (float) result[1]);
//        };
//    }

    /**
     * 计算分词正确的词数
     */
    public static int wordCorrect(List<String> gold, List<String> pred) {
        int goldIndex = 0;
        int predIndex = 0;
        int goldLen = 0;
        int predLen = 0;

        int correct = 0;
        while (goldIndex < gold.size() && predIndex < pred.size()) {
            if (goldLen == predLen) {
                if (gold.get(goldIndex).equals(pred.get(predIndex))) {
                    correct++;
                    goldLen += gold.get(goldIndex).length();
                    predLen += gold.get(goldIndex).length();
                    goldIndex++;
                    predIndex++;
                } else {
                    goldLen += gold.get(goldIndex).length();
                    predLen += pred.get(predIndex).length();
                    goldIndex++;
                    predIndex++;
                }
            } else if (goldLen < predLen) {
                goldLen += gold.get(goldIndex).length();
                goldIndex++;
            } else {
                predLen += pred.get(predIndex).length();
                predIndex++;
            }
        }
        return correct;
    }

    private static double[] prf(int goldTotal, int predTotal, int correct) {
        double precision = correct * 100.0 / predTotal;
        double recall = correct * 100.0 / goldTotal;
        double[] performance = new double[3];
        performance[0] = precision;
        performance[1] = recall;
        performance[2] = 2.0 * precision * recall / (precision + recall);
        return performance;
    }

    /**
     * 简单的字符串规范化，替代CharNormUtils.convert
     * 这里只做基本处理，实际使用中可以根据需要扩展
     */
    private static String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        // 这里可以添加字符规范化逻辑
        // 目前只做基本的trim处理
        return input.trim();
    }
}
