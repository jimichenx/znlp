package dev.znlp.perceptron.decoder;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.PerceptronDecoder;

import java.util.List;

/**
 * 快速解码感知机模型实现。
 * <p>
 * 不考虑转移概率，每个位置独立选择得分最高的标签，比较适合词性模型解码。
 * <p>
 * <h3>解码原理：</h3>
 * <p>
 * 对于序列中的每个位置，独立计算所有可能标签的得分：
 * <pre>
 * score(label) = Σ(parameter[featureIndex * labelCount + label])
 * </pre>
 * 然后选择得分最高的标签作为该位置的预测结果。
 * <p>
 * 这种解码方式不考虑标签之间的依赖关系，计算速度快，时间复杂度为O(n * m * k)，
 * 其中n是序列长度，m是标签数量，k是平均特征数量。
 * <p>
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>词性标注：标签间依赖关系较弱</li>
 *   <li>对解码速度要求较高的场景</li>
 *   <li>不需要全局最优解的场景</li>
 * </ul>
 * <p>
 * <h3>与Viterbi解码的区别：</h3>
 * <p>
 * 快速解码不考虑转移概率，每个位置独立决策，可能无法获得全局最优解。
 * 如果标签间存在强依赖关系（如命名实体识别中的BIO标注），建议使用
 * {@link ViterbiPerceptronDecoder}。
 *
 * @author jimichan
 * @see PerceptronDecoder
 * @see ViterbiPerceptronDecoder
 */
public class QuickPerceptronDecoder implements PerceptronDecoder {
    /**
     * 标签数量
     */
    private final int labelCount;
    /**
     * 模型参数数组，结构为 [featureIndex * labelCount + labelIndex]
     */
    private final float[] parameter;

    /**
     * 构造快速解码感知机模型。
     *
     * @param labelCount 标签数量，必须大于0
     * @param parameter  模型参数数组，长度必须为 featureCount * labelCount。
     *                   参数数组的结构：parameter[featureIndex * labelCount + labelIndex]
     *                   表示特征featureIndex在标签labelIndex上的权重。
     *                   不能为null，且长度必须正确。
     * @throws IllegalArgumentException 如果labelCount &lt;= 0 或 parameter为null
     */
    public QuickPerceptronDecoder(int labelCount, float[] parameter) {
        this.labelCount = labelCount;
        this.parameter = parameter;
    }

    @Override
    public int parameterSize() {
        return parameter.length;
    }

    /**
     * 执行快速解码。
     * <p>
     * 对序列中的每个位置，独立计算所有标签的得分，选择得分最高的标签。
     * 不考虑标签间的转移概率，因此计算速度快但可能不是全局最优解。
     * <p>
     * 如果某个位置没有特征（特征向量为空或只有转移特征），则预测标签为-1。
     *
     * @param featureSequence 特征序列，每个元素表示一个位置的稀疏特征向量
     * @param guessLabel      用于保存解码结果的数组，长度必须至少等于featureSequence的长度
     */
    @Override
    public void decode(List<IntArrayList> featureSequence, int[] guessLabel) {
        int index = 0;
        float[] parameterArray = parameter;

        for (IntArrayList feature : featureSequence) {
            int[] buffer = feature.getBuffer();
            int sizeM1 = feature.size() - 1;

            float maxScore = Float.MIN_VALUE;
            int maxIndex = -1;

            if (sizeM1 != 0) {
                for (int label = 0; label < labelCount; label++) {
                    float score = 0.0f;

                    for (int i = 0; i < sizeM1; i++) {
                        score += parameterArray[buffer[i] * labelCount + label];
                    }

                    if (score > maxScore) {
                        maxIndex = label;
                        maxScore = score;
                    }
                }
            }

            guessLabel[index++] = maxIndex;
        }
    }
}
