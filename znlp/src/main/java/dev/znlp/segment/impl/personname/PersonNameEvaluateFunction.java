package dev.znlp.segment.impl.personname;

import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.train.EvaluationSample;

import java.util.ArrayList;
import java.util.List;

/**
 * 人名识别评估函数
 */
class PersonNameEvaluateFunction implements EvaluateFunction {

    private final PerceptronDecoder decoder;
    private final ImmutableFeatureSet featureSet;

    public PersonNameEvaluateFunction(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
        this.decoder = decoder;
        this.featureSet = featureSet;
    }

    @Override
    public EvaluateResult evaluate(List<EvaluationSample> samples) {
        PersonNamePerceptron perceptron = new PersonNamePerceptron(decoder, featureSet);

        int goldTotal = 0;  // 正确答案中的人名总数
        int predTotal = 0;  // 预测结果中的人名总数
        int correct = 0;    // 正确匹配的人名数量

        for (EvaluationSample evaluationSample : samples) {
            char[] text = (char[]) evaluationSample.getInputSequence();
            List<PersonName> predicted = perceptron.findPersonName(text);
            List<PersonName> answer = extractPersonNames(evaluationSample.getLine());

            // 统计总数
            goldTotal += answer.size();
            predTotal += predicted.size();

            // 计算正确匹配的人名数量
            correct += countCorrectMatches(answer, predicted);
        }

        return new EvaluateResult(goldTotal, predTotal, correct);
    }

    /**
     * 从标注文本中提取人名
     * 标注格式: 字B﹍字M﹍字E (表示人名)
     */
    private List<PersonName> extractPersonNames(String annotatedText) {
        List<PersonName> names = new ArrayList<>();
        String[] items = annotatedText.split(String.valueOf(PersonNameAnnotateTextParser.SEPARATOR));

        StringBuilder nameBuilder = new StringBuilder();
        int offset = -1;
        int currentOffset = 0;

        for (String item : items) {
            if (item.isEmpty()) {
                continue;
            }
            if (item.length() >= 2) {
                char zi = item.charAt(0);
                char label = item.charAt(1);

                switch (label) {
                    case 'B':
                        // 开始新的人名
                        nameBuilder = new StringBuilder();
                        nameBuilder.append(zi);
                        offset = currentOffset;
                        break;
                    case 'M':
                        // 人名中间
                        if (offset >= 0) {
                            nameBuilder.append(zi);
                        }
                        break;
                    case 'E':
                        // 人名结束
                        if (offset >= 0) {
                            nameBuilder.append(zi);
                            names.add(new PersonName(offset, nameBuilder.length()));
                            offset = -1;
                        }
                        break;
                    default:
                        // O, X, Y, Z - 重置
                        offset = -1;
                        break;
                }
                currentOffset++;
            }
        }

        return names;
    }

    /**
     * 计算正确匹配的人名数量
     */
    private int countCorrectMatches(List<PersonName> gold, List<PersonName> predicted) {
        int count = 0;
        for (PersonName p : predicted) {
            for (PersonName g : gold) {
                if (p.offset() == g.offset() && p.length() == g.length()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}
