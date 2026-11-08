package dev.znlp.pinyin.spliter;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.*;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼音切分感知机
 */
public class PinyinSplitPerceptron {

    private static final String[] LABLES = new String[]{"B", "M", "E", "S"};

    private final PerceptronDecoder decoder;

    private final ImmutableFeatureSet featureSet;

    public PinyinSplitPerceptron(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
        this.decoder = decoder;
        this.featureSet = featureSet;
    }

    public List<String> split(String text) {
        int len = text.length();
        if (len == 0) {
            return new ArrayList<>(0);
        }

        char[] textArray = text.toCharArray();
        return split(textArray);
    }

    public List<String> split(char[] textArray) {
        int len = textArray.length;
        if (len == 0) {
            return new ArrayList<>(0);
        }

        List<IntArrayList> featureSequence = instance.tansform(textArray, featureSet);

        int[] labelsCode = new int[featureSequence.size()];
        // 使用模型对输入文本进行预测
        decoder.decode(featureSequence, labelsCode);

        // 预估结果数量，避免 ArrayList 频繁扩容
        List<String> result = new ArrayList<>(len / 3 + 1);
        int wordStart = 0;

        for (int i = 0; i < len; i++) {
            int label = labelsCode[i];

            // 根据标签决定是否切分
            // label == 2(E) 或 label == 3(S) 表示词结束
            if (label == 2 || label == 3) {
                // 从 char[] 创建字符串
                result.add(new String(textArray, wordStart, i + 1 - wordStart));
                wordStart = i + 1;
            }
        }

        // 添加最后一个词（如果有的话）
        if (wordStart < len) {
            result.add(new String(textArray, wordStart, len - wordStart));
        }

        return result;
    }

    public static PinyinSplitPerceptron load(String classPath) throws IOException {
        PerceptronModel model = PerceptronModel.readFromClasspath(classPath);
        return new PinyinSplitPerceptron(model.getDecoder(), model.getFeatureSet());
    }

    public static PinyinSplitPerceptron load(NlpResourceLoader loader,String path) throws IOException {
        PerceptronModel model = PerceptronModel.readFromNlpResource(loader,path);
        return new PinyinSplitPerceptron(model.getDecoder(), model.getFeatureSet());
    }


    static CharBasedPerceptronInstance instance = initInstance();

    public static CharBasedPerceptronInstance getInstance() {
        return instance;
    }

    private static CharBasedPerceptronInstance initInstance() {

        // 使用高性能的手动实现替代模板解析
        FeatureExtractor<char[]> featureFun = new PinyinSplitFeatureExtractor();

        PinyinSplitAnnotateTextParser parser = new PinyinSplitAnnotateTextParser();

        return new CharBasedPerceptronInstance(
                LABLES,
                8,
                parser,
                new PyEvaluateFunctionFactory(),
                featureFun
        );
    }

    private static class PyEvaluateFunctionFactory implements EvaluateFunctionFactory {
        @Override
        public EvaluateFunction create(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
            return new PySplitEvaluateFunction(decoder, featureSet);
        }
    }
}