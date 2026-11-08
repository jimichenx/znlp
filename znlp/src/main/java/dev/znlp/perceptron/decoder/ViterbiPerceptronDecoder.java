package dev.znlp.perceptron.decoder;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.PerceptronDecoder;

import java.util.List;

/**
 * Viterbi解码感知机模型实现。
 * <p>
 * 考虑转移概率，使用动态规划（Viterbi算法）找到全局最优的标签序列，
 * 用于需要全局最优的场景。
 * <p>
 * <h3>解码原理：</h3>
 * <p>
 * 使用动态规划算法，考虑标签间的转移概率，找到全局最优的标签序列。
 * 对于位置i和标签j，得分计算包括：
 * <ul>
 *   <li><b>特征得分</b>：当前特征在该标签上的得分</li>
 *   <li><b>转移得分</b>：从前一个标签转移到当前标签的得分</li>
 *   <li><b>历史得分</b>：到达前一个位置的最优路径得分</li>
 * </ul>
 * <p>
 * 得分计算公式：
 * <pre>
 * score(i, j) = max_k(score(i-1, k) + feature_score(i, j) + transition(k, j))
 * </pre>
 * <p>
 * 其中：
 * <ul>
 *   <li>score(i-1, k)：到达位置i-1、标签k的最优路径得分</li>
 *   <li>feature_score(i, j)：位置i的特征在标签j上的得分</li>
 *   <li>transition(k, j)：从标签k转移到标签j的转移得分</li>
 * </ul>
 * <p>
 * 对于序列的第一个位置，使用BOS（Begin of Sequence）特征计算转移得分。
 * <p>
 * 时间复杂度为O(n * m² * k)，其中n是序列长度，m是标签数量，k是平均特征数量。
 * <p>
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>命名实体识别：标签间存在强依赖关系（如BIO标注）</li>
 *   <li>需要全局最优解的序列标注任务</li>
 *   <li>对标签间依赖关系敏感的场景</li>
 * </ul>
 * <p>
 * <h3>与快速解码的区别：</h3>
 * <p>
 * Viterbi解码考虑转移概率和全局最优，能获得全局最优解，但计算速度较慢。
 * 如果标签间依赖关系较弱或对速度要求较高，可以考虑使用
 * {@link QuickPerceptronDecoder}。
 *
 * @author jimichan
 * @see PerceptronDecoder
 * @see QuickPerceptronDecoder
 */
public class ViterbiPerceptronDecoder implements PerceptronDecoder {
    /**
     * 用于初始化的最小得分值
     */
    private static final double MAX_SCORE = Integer.MIN_VALUE;

    /**
     * 标签数量
     */
    private final int labelCount;
    /**
     * 模型参数数组，结构为 [featureIndex * labelCount + labelIndex]
     */
    private final float[] parameter;

    /**
     * 构造Viterbi解码感知机模型。
     *
     * @param labelCount 标签数量，必须大于0
     * @param parameter  模型参数数组，长度必须为 featureCount * labelCount。
     *                   参数数组的结构：parameter[featureIndex * labelCount + labelIndex]
     *                   表示特征featureIndex在标签labelIndex上的权重。
     *                   前labelCount个特征位置用于标签间转移特征，
     *                   第labelCount个特征位置用于BOS（序列开始）特征。
     *                   不能为null，且长度必须正确。
     * @throws IllegalArgumentException 如果labelCount &lt;= 0 或 parameter为null
     */
    public ViterbiPerceptronDecoder(int labelCount, float[] parameter) {
        this.labelCount = labelCount;
        this.parameter = parameter;
    }

    public ViterbiPerceptronDecoder(int labelCount, int featureSize) {
        this.labelCount = labelCount;
        this.parameter = new float[featureSize * labelCount];
    }

    @Override
    public int parameterSize() {
        return parameter.length;
    }

    /**
     * 执行Viterbi解码。
     * <p>
     * 使用动态规划算法，考虑标签间的转移概率，找到全局最优的标签序列。
     * <p>
     * 解码过程分为三个阶段：
     * <ol>
     *   <li><b>初始化</b>：计算序列第一个位置的所有标签得分，使用BOS特征</li>
     *   <li><b>前向传播</b>：使用动态规划计算每个位置每个标签的最优路径得分，
     *       并记录最优路径的前驱标签</li>
     *   <li><b>回溯</b>：从最后一个位置开始，根据记录的前驱信息回溯找到最优路径</li>
     * </ol>
     *
     * @param featureSequence 特征序列，每个元素表示一个位置的稀疏特征向量。
     *                        特征向量的最后一位是留给转移特征使用的。
     * @param guessLabel      用于保存解码结果的数组，长度必须至少等于featureSequence的长度。
     *                        解码完成后，guessLabel[i]将包含位置i的预测标签ID。
     */
    @Override
    public void decode(List<IntArrayList> featureSequence, int[] guessLabel) {
        int sentenceLength = featureSequence.size();
        int[] preMatrix = new int[sentenceLength * labelCount];

        double[] scoreMLast = new double[labelCount];
        double[] scoreMNow = new double[labelCount];

        IntArrayList firstFeature = featureSequence.get(0);

        int bos = labelCount;
        int bosBase = bos * labelCount;
        for (int j = 0; j < labelCount; j++) {
            preMatrix[j] = j;
            double score = scoreBase(firstFeature, j) + parameter[bosBase + j];
            scoreMLast[j] = score;
        }

        for (int i = 1; i < sentenceLength; i++) {
            IntArrayList allFeature = featureSequence.get(i);
            int base = i * labelCount;

            for (int curLabel = 0; curLabel < labelCount; curLabel++) {
                double maxScore = MAX_SCORE;
                double baseScore = scoreBase(allFeature, curLabel);

                for (int preLabel = 0; preLabel < labelCount; preLabel++) {
                    double curScore = scoreMLast[preLabel] + baseScore + parameter[preLabel * labelCount + curLabel];

                    if (curScore > maxScore) {
                        maxScore = curScore;
                        preMatrix[base + curLabel] = preLabel;
                        scoreMNow[curLabel] = maxScore;
                    }
                }
            }

            double[] temp = scoreMLast;
            scoreMLast = scoreMNow;
            scoreMNow = temp;
        }

        int maxIndex = 0;
        double maxScore = scoreMLast[0];

        for (int index = 1; index < labelCount; index++) {
            double x = scoreMLast[index];
            if (maxScore < x) {
                maxIndex = index;
                maxScore = x;
            }
        }

        int k = (sentenceLength - 1) * labelCount;
        for (int i = sentenceLength - 1; i >= 0; i--) {
            guessLabel[i] = maxIndex;
            maxIndex = preMatrix[k + maxIndex];
            k -= labelCount;
        }
    }

    /**
     * 计算特征向量在指定标签上的基础得分。
     * <p>
     * 对特征向量中除最后一位（转移特征）外的所有特征，累加它们在当前标签上的权重。
     *
     * @param featureVector 特征向量，最后一位是转移特征，不参与计算
     * @param currentTag    当前标签ID
     * @return 特征向量在当前标签上的得分
     */
    private double scoreBase(IntArrayList featureVector, int currentTag) {
        double score = 0.0;
        int[] buffer = featureVector.getBuffer();
        for (int i = 0; i < featureVector.size() - 1; i++) {
            int index = buffer[i];
            score += parameter[index * labelCount + currentTag];
        }
        return score;
    }
}
