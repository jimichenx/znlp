package dev.znlp.perceptron;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.decoder.OptimizedViterbiPerceptronDecoder;
import dev.znlp.perceptron.decoder.QuickPerceptronDecoder;
import dev.znlp.perceptron.decoder.ViterbiPerceptronDecoder;

import java.util.List;

/**
 * 感知机解码器接口，用于将特征序列解码为标签序列。
 * <p>
 * 该接口定义了感知机模型的核心解码功能，提供了两种不同的解码策略：
 * 快速解码和Viterbi解码。感知机解码器直接操作底层的特征序列和参数数组，
 * 负责将特征序列转换为最终的标签预测结果。
 * <p>
 * <h3>参数数组结构：</h3>
 * <p>
 * parameter是一个很长的float数组，逻辑上按以下结构组织：
 * <pre>
 * [ [F0], [F1], [label_0, label_1, ..., label_n], ..., [FN] ]
 * </pre>
 * <p>
 * Fn表示一个特征在每个label的权重。假设Label有4个，特征总数是10000，那么参数数组的总长度为40000，
 * 每个特征占据4个连续位置，分别对应4个标签的权重。
 * <p>
 * 一个输入序列包含多个位置，每个位置有多个激活特征，这些特征在对应标签上的权重之和，
 * 就是该位置对应标签的得分。
 * <p>
 * <h3>解码策略：</h3>
 * <p>
 * 本接口提供两种解码实现：
 * <ul>
 *   <li><b>快速解码</b>（{@link QuickPerceptronDecoder}）：
 *       不考虑转移概率，每个位置独立选择得分最高的标签。
 *       适用于词性标注等对标签间依赖关系要求不高的场景，计算速度快。</li>
 *   <li><b>Viterbi解码</b>（{@link ViterbiPerceptronDecoder}）：
 *       考虑转移概率，使用动态规划找到全局最优的标签序列。
 *       适用于命名实体识别等需要考虑标签间依赖关系的场景，能获得全局最优解。</li>
 * </ul>
 * <p>
 * <h3>特征向量格式：</h3>
 * <p>
 * decode方法中使用的特征序列，每个元素是一个稀疏特征向量（IntArrayList），
 * 包含当前位置所有激活特征的索引。
 * <p>
 * <h3>使用示例：</h3>
 * <pre>
 * // 创建解码器
 * PerceptronDecoder decoder = PerceptronDecoder.create(true, labelCount, parameter);
 *
 * // 解码特征序列
 * int[] labels = decoder.decode(featureSequence);
 * </pre>
 *
 * @author jimichan
 * @see QuickPerceptronDecoder
 * @see ViterbiPerceptronDecoder
 */
public interface PerceptronDecoder {
    /**
     * 解码方法，将结果保存到guessLabel数组中。
     * <p>
     * 这是核心的抽象方法，由具体实现类提供快速解码或Viterbi解码的实现。
     * <p>
     * <b>注意：</b>guessLabel数组的长度必须至少等于featureSequence的长度，
     * 否则可能抛出数组越界异常。
     *
     * @param featureSequence 特征序列，每个元素是一个浓缩的稀疏特征向量（IntArrayList），
     *                        表示一个位置的所有激活特征。
     *                        不能为null，且不能包含null元素。
     * @param guessLabel      用于保存解码结果的数组，长度必须至少等于featureSequence的长度。
     *                        解码完成后，guessLabel[i]将包含位置i的预测标签ID。
     *                        不能为null。
     */
    void decode(List<IntArrayList> featureSequence, int[] guessLabel);

    /**
     * 获取参数数组的大小。
     * <p>
     * 返回当前解码器使用的参数数组的总长度，即所有特征和标签组合的权重数量。
     *
     * @return 参数数组的大小
     */
    int parameterSize();

    /**
     * 解码，返回label对应的ID数组。
     * <p>
     * 这是默认方法，内部调用 {@link #decode(List, int[])} 方法。
     * 如果featureSequence为空，则返回空数组。
     *
     * @param featureSequence 特征序列，每个元素是一个浓缩的稀疏特征向量（IntArrayList），
     *                        表示一个位置的所有激活特征。
     *                        不能为null，但可以为空列表。
     * @return label对应的ID数组，数组长度等于featureSequence的长度。
     * 如果featureSequence为空，则返回空数组。
     * 数组中的每个元素表示对应位置的预测标签ID。
     */
    default int[] decode(List<IntArrayList> featureSequence) {
        int[] result = new int[featureSequence.size()];

        if (result.length == 0) {
            return result;
        }

        decode(featureSequence, result);

        return result;
    }

    /**
     * 创建感知机解码器实例。
     * <p>
     * 根据指定的解码策略，创建并返回相应的感知机解码器实例。
     *
     * @param decodeQuickModel 是否使用快速解码策略，true表示使用快速解码，false表示使用Viterbi解码
     * @param labelCount       标签总数
     * @param parameter        模型参数数组
     * @return 创建的感知机解码器实例
     */
    static PerceptronDecoder create(boolean decodeQuickModel, int labelCount, float[] parameter) {
        if (decodeQuickModel) {
            return new QuickPerceptronDecoder(labelCount, parameter);
        } else {
            return new OptimizedViterbiPerceptronDecoder(labelCount, parameter);
        }
    }
}
