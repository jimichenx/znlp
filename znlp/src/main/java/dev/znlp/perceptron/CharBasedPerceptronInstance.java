package dev.znlp.perceptron;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.feature.FeatureIdSeek;
import dev.znlp.perceptron.feature.FeatureVectors;
import dev.znlp.perceptron.utils.FastStringBuilder;

import java.util.List;

/**
 * 字符级感知机实例类，实现了针对字符序列的感知机实例接口。
 * <p>
 * 该类是PerceptronInstance接口的具体实现，专门用于处理字符级别的任务，
 * 如分词、词性标注、命名实体识别等。它封装了字符级任务所需的所有配置和功能。
 * <p>
 * 该类的设计允许灵活配置不同的特征提取函数、评估函数和标注文本解析器，
 * 使其适用于各种字符级NLP任务。
 *
 * @author jimichan
 * @see PerceptronInstance
 */
public class CharBasedPerceptronInstance implements PerceptronInstance<Character, char[]> {

    private final String[] lables;

    private final int featureMaxSize;

    private final AnnotateTextParser<Character, char[]> annotateTextParser;

    private final EvaluateFunctionFactory evaluateFunction;

    private final FeatureExtractor<char[]> featureFun;

    /**
     * 构造字符级感知机实例。
     *
     * @param lables             标签数组，索引对应标签ID，值对应标签名称
     * @param featureMaxSize     单个位置的最大特征数量，用于预分配内存
     * @param annotateTextParser 标注文本解析器，用于处理训练数据
     * @param evaluateFunction   评估函数工厂，用于生成评估函数
     * @param featureFun         特征提取函数，用于从字符序列中提取特征
     */
    public CharBasedPerceptronInstance(String[] lables,
                                       int featureMaxSize,
                                       AnnotateTextParser<Character, char[]> annotateTextParser,
                                       EvaluateFunctionFactory evaluateFunction,
                                       FeatureExtractor<char[]> featureFun) {
        this.lables = lables;
        this.featureMaxSize = featureMaxSize;
        this.annotateTextParser = annotateTextParser;
        this.evaluateFunction = evaluateFunction;
        this.featureFun = featureFun;
    }

    @Override
    public int getFeatureMaxSize() {
        return featureMaxSize;
    }

    @Override
    public FeatureExtractor<char[]> getFeatureFun() {
        return featureFun;
    }

    @Override
    public EvaluateFunctionFactory getEvaluateFunction() {
        return evaluateFunction;
    }

    @Override
    public AnnotateTextParser<Character, char[]> getAnnotateTextParser() {
        return annotateTextParser;
    }

    @Override
    public String[] getLables() {
        return lables;
    }

    /**
     * 将字符数组转换为特征序列。
     * <p>
     * 遍历字符数组的每个位置，使用特征提取函数提取特征，并将特征名称转换为特征ID，
     * 最终生成一个特征序列，每个元素是一个位置的稀疏特征向量。
     *
     * @param input 输入字符数组
     * @param featureSet 特征ID查找工具，用于将特征名称映射为特征ID
     * @return 转换后的特征序列，每个元素是一个位置的稀疏特征向量
     */
    @Override
    public List<IntArrayList> tansform(char[] input, FeatureIdSeek featureSet) {
        FastStringBuilder builder = new FastStringBuilder(featureMaxSize);
        int size = CharWindow.length(input);
        List<IntArrayList> out = FeatureVectors.borrow(size);
        FeatureVectors.Consumer consumer = new FeatureVectors.Consumer(featureSet);
        for (int i = 0; i < size; i++) {
            IntArrayList vector = out.get(i);
            consumer.vector = vector;
            featureFun.compute(input, i, builder, consumer);
            vector.add(0);
        }
        return out;
    }

}


