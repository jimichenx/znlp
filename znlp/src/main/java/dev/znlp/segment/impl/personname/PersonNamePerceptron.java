package dev.znlp.segment.impl.personname;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.*;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 人名识别感知机
 * <p>
 * 标签说明：
 * B(0) - Begin（人名开始）
 * M(1) - Middle（人名中间）
 * E(2) - End（人名结束）
 * O(3) - Other（非人名）
 * X(4) - 人名前的字符
 * Y(5) - 人名后的字符
 * Z(6) - 其他标记
 */
public class PersonNamePerceptron {

    /**
     * 标签定义
     */
    public static final int B = 0;  // Begin - 人名开始
    public static final int M = 1;  // Middle - 人名中间
    public static final int E = 2;  // End - 人名结束
    public static final int O = 3;  // Other - 非人名

    private static final String[] LABELS = new String[]{"B", "M", "E", "O", "X", "Y", "Z"};

    private final PerceptronDecoder decoder;
    private final ImmutableFeatureSet featureSet;

    public PersonNamePerceptron(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
        this.decoder = decoder;
        this.featureSet = featureSet;
    }

    /**
     * 识别句子中的人名
     *
     * @param sentence 输入句子
     * @return 识别到的人名列表
     */
    public List<PersonName> findPersonName(char[] sentence) {
        if (sentence == null || sentence.length == 0) {
            return new ArrayList<>(0);
        }
        return findPersonName(sentence, 0, sentence.length);
    }

    /**
     * 在 {@code [origin, origin + length)} 里识别人名。偏移相对这个窗口。
     */
    public List<PersonName> findPersonName(char[] sentence, int origin, int length) {
        List<PersonName> result = new ArrayList<>();
        int[] labels = decode(sentence, origin, length);

        int nameStart = -1;
        for (int i = 0; i < labels.length; i++) {
            switch (labels[i]) {
                case B:
                    nameStart = i;
                    break;
                case E:
                    if (nameStart != -1) {
                        result.add(new PersonName(nameStart, i - nameStart + 1));
                        nameStart = -1;
                    }
                    break;
                case O:
                    nameStart = -1;
                    break;
            }
        }

        return result;
    }

    /**
     * 识别句子中的人名
     *
     * @param sentence 输入句子字符串
     * @return 识别到的人名列表
     */
    public List<PersonName> findPersonName(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return new ArrayList<>(0);
        }
        return findPersonName(sentence.toCharArray());
    }

    /**
     * 解码获取标签序列
     *
     * @param sentence 输入句子
     * @return 标签序列
     */
    public int[] decode(char[] sentence) {
        if (sentence == null || sentence.length == 0) {
            return new int[0];
        }
        return decode(sentence, 0, sentence.length);
    }

    public int[] decode(char[] sentence, int origin, int length) {
        if (sentence == null || length <= 0) {
            return new int[0];
        }
        CharWindow.bind(origin, length);
        try {
            return decodeBound(sentence);
        } finally {
            CharWindow.clear();
        }
    }

    private int[] decodeBound(char[] sentence) {
        List<IntArrayList> featureSequence = instance.tansform(sentence, featureSet);

        int[] labelsCode = new int[featureSequence.size()];
        decoder.decode(featureSequence, labelsCode);

        return labelsCode;
    }

    /**
     * 从 classpath 加载模型
     *
     * @param classPath 模型路径
     * @return PersonNamePerceptron 实例
     * @throws IOException 加载失败时抛出
     */
    public static PersonNamePerceptron load(String classPath) throws IOException {
        PerceptronModel model = PerceptronModel.readFromClasspath(classPath);
        return new PersonNamePerceptron(model.getDecoder(), model.getFeatureSet());
    }


    public static PersonNamePerceptron load(NlpResourceLoader loader, String path) throws IOException {
        PerceptronModel model = PerceptronModel.readFromNlpResource(loader,path);
        return new PersonNamePerceptron(model.getDecoder(), model.getFeatureSet());
    }

    static CharBasedPerceptronInstance instance = initInstance();

    public static CharBasedPerceptronInstance getInstance() {
        return instance;
    }

    private static CharBasedPerceptronInstance initInstance() {
        // 使用高性能的手动实现替代模板解析
        FeatureExtractor<char[]> featureFun = new PersonNameFeatureExtractor();

        PersonNameAnnotateTextParser parser = new PersonNameAnnotateTextParser();

        return new CharBasedPerceptronInstance(
                LABELS,
                32,
                parser,
                new PersonNameEvaluateFunctionFactory(),
                featureFun
        );
    }

    private static class PersonNameEvaluateFunctionFactory implements EvaluateFunctionFactory {
        @Override
        public EvaluateFunction create(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
            return new PersonNameEvaluateFunction(decoder, featureSet);
        }
    }
}
