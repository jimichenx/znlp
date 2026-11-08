package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.ModelName;
import dev.znlp.fasttext.dictionary.Dictionary;
import dev.znlp.fasttext.dictionary.EntryType;

/**
 * 损失函数工厂类，用于根据配置创建不同类型的损失函数实例。
 * 支持多种损失函数：分层Softmax、负采样、标准Softmax和一对一分类。
 */
public class LossFactory {

    /**
     * 根据配置创建损失函数实例
     *
     * @param args 模型参数配置，包含损失函数类型等信息
     * @param output 输出矩阵
     * @param modelName 模型类型（监督或非监督）
     * @param dictionary 词典对象，用于获取目标计数
     * @return 对应类型的损失函数实例
     * @throws RuntimeException 当指定的损失函数类型未知时抛出
     */
    public static Loss createLoss(Args args, Matrix output, ModelName modelName, Dictionary dictionary) {
        long[] targetCounts;
        if (modelName == ModelName.sup) {
            targetCounts = dictionary.getCounts(EntryType.label);
        } else {
            targetCounts = dictionary.getCounts(EntryType.word);
        }

        switch (args.loss) {
            case hs:
                return new HierarchicalSoftmaxLoss(output, targetCounts);
            case ns:
                return new NegativeSamplingLoss(output, args.neg, targetCounts);
            case softmax:
                return new SoftmaxLoss(output);
            case ova:
                return new OneVsAllLoss(output);
            default:
                throw new RuntimeException("unknown loss");
        }
    }
}
