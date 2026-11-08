package dev.znlp.perceptron;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.utils.PerceptronModelLoadUtils;

import java.io.File;
import java.io.IOException;

/**
 * 感知机模型类，封装了感知机模型的所有组件和配置。
 * <p>
 * 该类是结构化感知机框架的核心数据结构，包含了模型的所有必要信息：
 * 特征集、标签数量、模型参数和解码策略。
 * <p>
 * 感知机模型类负责管理模型的生命周期，包括模型的创建、解码和参数访问。
 *
 * @author jimichan
 */
public class PerceptronModel {
    private final ImmutableFeatureSet featureSet;
    private final int labelCount;
    private final float[] parameter;
    private final boolean decodeQuickModel;

    private final PerceptronDecoder decoder;

    /**
     * 构造感知机模型。
     *
     * @param featureSet       不可变特征集，包含模型的所有特征信息
     * @param labelCount       标签总数
     * @param parameter        模型参数数组，包含所有特征-标签组合的权重
     * @param decodeQuickModel 是否使用快速解码策略，true表示使用快速解码，false表示使用Viterbi解码
     */
    public PerceptronModel(ImmutableFeatureSet featureSet, int labelCount, float[] parameter, boolean decodeQuickModel) {
        this.featureSet = featureSet;
        this.labelCount = labelCount;
        this.parameter = parameter;
        this.decodeQuickModel = decodeQuickModel;
        decoder = PerceptronDecoder.create(decodeQuickModel, labelCount, parameter);
    }

    /**
     * 获取感知机解码器。
     * <p>
     * 解码器负责将特征序列转换为标签序列，根据构造时指定的解码策略创建。
     *
     * @return 感知机解码器实例
     */
    public PerceptronDecoder getDecoder() {
        return decoder;
    }

    /**
     * 获取不可变特征集。
     * <p>
     * 特征集包含模型的所有特征信息，用于特征ID和特征名称的映射。
     *
     * @return 不可变特征集实例
     */
    public ImmutableFeatureSet getFeatureSet() {
        return featureSet;
    }

    /**
     * 获取是否使用快速解码模式。
     * <p>
     * 快速解码模式使用贪心算法，速度更快但可能不是全局最优解。
     * Viterbi解码模式使用动态规划，速度较慢但保证全局最优解。
     *
     * @return true表示使用快速解码，false表示使用Viterbi解码
     */
    public boolean isDecodeQuickModel() {
        return decodeQuickModel;
    }

    /**
     * 从文件读取感知机模型。
     * <p>
     * 支持压缩和非压缩格式，自动检测文件格式。
     * <p>
     * 此方法委托给 {@link PerceptronModelLoadUtils#read(File)} 实现。
     *
     * @param inputFile 模型文件路径
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误或文件格式不正确
     */
    public static PerceptronModel read(File inputFile) throws IOException {
        return PerceptronModelLoadUtils.read(inputFile);
    }

    /**
     * 从classpath资源读取感知机模型。
     * <p>
     * 从类路径中加载模型文件，支持压缩和非压缩格式，自动检测文件格式。
     * <p>
     * 资源路径可以是绝对路径（以"/"开头）或相对路径。
     * 例如："/models/pinyin-split.bin" 或 "models/pinyin-split.bin"
     * <p>
     * 此方法委托给 {@link PerceptronModelLoadUtils#readFromClasspath(String)} 实现。
     *
     * @param resourcePath classpath资源路径（例如："/models/pinyin-split.bin"）
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误、资源不存在或文件格式不正确
     */
    public static PerceptronModel readFromClasspath(String resourcePath) throws IOException {
        return PerceptronModelLoadUtils.readFromClasspath(resourcePath);
    }

    public static PerceptronModel readFromNlpResource(NlpResourceLoader loader , String resourcePath) throws IOException {
        return PerceptronModelLoadUtils.read(loader,resourcePath);
    }
}
