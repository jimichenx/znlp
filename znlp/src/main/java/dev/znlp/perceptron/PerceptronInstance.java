package dev.znlp.perceptron;


import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.feature.FeatureIdSeek;
import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.perceptron.train.PerceptronTrainer;

import java.io.File;
import java.util.List;

/**
 * 感知机实例接口，定义了特定任务的感知机模型配置和行为。
 * <p>
 * 该接口是结构化感知机框架的核心抽象，用于将特定任务（如分词、词性标注、命名实体识别等）
 * 与感知机训练和推理逻辑解耦。每个具体任务需要实现此接口，提供任务相关的配置和功能。
 *
 * @param <T>             标注数据类型，用于表示训练数据中的标注信息
 * @param <InputSequence> 输入序列类型，用于表示模型的输入数据
 * @author jimichan
 */
public interface PerceptronInstance<T, InputSequence> {

    /**
     * 获取单个位置的最大特征数量。
     * <p>
     * 用于预分配内存和优化性能，指示每个输入位置可能生成的最大特征数量。
     *
     * @return 单个位置的最大特征数量
     */
    int getFeatureMaxSize();

    /**
     * 获取特征提取函数。
     * <p>
     * 该函数用于从输入序列中提取特征，是连接输入数据和感知机模型的桥梁。
     *
     * @return 特征提取函数实例，用于将输入序列转换为特征向量
     */
    FeatureExtractor<InputSequence> getFeatureFun();

    /**
     * 获取评估函数工厂。
     * <p>
     * 用于创建评估函数，评估模型在验证集或测试集上的性能。
     *
     * @return 评估函数工厂实例，用于生成评估函数
     */
    EvaluateFunctionFactory getEvaluateFunction();

    /**
     * 获取标注文本解析器。
     * <p>
     * 用于解析训练数据中的标注文本，将其转换为感知机可处理的训练样本。
     *
     * @return 标注文本解析器实例，用于处理训练数据
     */
    AnnotateTextParser<T, InputSequence> getAnnotateTextParser();

    /**
     * 获取标签数组。
     * <p>
     * 返回当前任务的所有可能标签，数组索引与标签ID一一对应。
     *
     * @return 标签数组，索引对应标签ID，值对应标签名称
     */
    String[] getLables();

    /**
     * 将输入序列转换为特征序列。
     * <p>
     * 使用特征提取函数和特征ID查找工具，将输入序列转换为感知机模型可处理的特征序列。
     *
     * @param input 输入序列，如句子或文本片段
     * @param featureSet 特征ID查找工具，用于将特征名称映射为特征ID
     * @return 转换后的特征序列，每个元素是一个位置的稀疏特征向量
     */
    List<IntArrayList> tansform(InputSequence input, FeatureIdSeek featureSet);

    /**
     * 创建感知机训练器。
     * <p>
     * 根据当前实例的配置，创建并返回一个感知机训练器实例，用于模型训练。
     *
     * @param workingDir 工作目录，用于存储训练过程中的临时文件和最终模型
     * @return 感知机训练器实例
     */
    default PerceptronTrainer<T, InputSequence> craeteTrainer(File workingDir) {
        return new PerceptronTrainer<>(this, workingDir);
    }

    /**
     * 评估模型
     *
     * @param testFile 测试文件或目录
     * @param workDir  工作目录
     * @param model    训练模型
     * @return 评估结果
     */
    default EvaluateResult evaluate(File testFile, File workDir, PerceptronTrainModel model) {
        return PerceptronEvaluator.evaluate(this, testFile, workDir, model);
    }

    default EvaluateResult evaluate(File testFile, File workDir, PerceptronModel model) {
        return PerceptronEvaluator.evaluate(this, testFile, workDir, model);
    }
}


