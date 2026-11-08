package dev.znlp.segment.impl.cws;

import dev.znlp.base.common.CharNormUtils;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.CharWindow;
import dev.znlp.perceptron.CharBasedPerceptronInstance;
import dev.znlp.perceptron.EvaluateFunction;
import dev.znlp.perceptron.EvaluateFunctionFactory;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.PerceptronModel;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.utils.PerceptronBinModelLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于结构化感知机的中文分词（CWS），字级 BMES 标注。
 * <p>
 * 标签：B=词首，M=词中，E=词尾，S=单字词。遇到 E 或 S 切一刀。
 */
public class CwsPerceptron {

    public static final int B = 0;
    public static final int M = 1;
    public static final int E = 2;
    public static final int S = 3;

    public static final String DEFAULT_MODEL_DIR = "znlp/cws-model";

    private static final String[] LABELS = new String[]{"B", "M", "E", "S"};

    private final PerceptronDecoder decoder;
    private final ImmutableFeatureSet featureSet;

    public CwsPerceptron(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
        this.decoder = decoder;
        this.featureSet = featureSet;
    }

    public CwsPerceptron(PerceptronModel model) {
        this(model.getDecoder(), model.getFeatureSet());
    }

    /**
     * 分词。默认先做字符规范化。
     */
    public List<String> split(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>(0);
        }
        return split(text.toCharArray(), true);
    }

    /**
     * 分词。
     *
     * @param text        输入字符，{@code convertChar=true} 时会原地规范化
     * @param convertChar 是否调用 {@link CharNormUtils#convert(char[])}
     */
    public List<String> split(char[] text, boolean convertChar) {
        int[] labels = decode(text, convertChar);
        return labelsToWords(text, labels);
    }

    /**
     * 解码为 BMES 标签 ID 序列。
     */
    public int[] decode(char[] text, boolean convertChar) {
        if (text == null || text.length == 0) {
            return new int[0];
        }
        if (convertChar) {
            CharNormUtils.convert(text);
        }
        return decode(text, 0, text.length);
    }

    /**
     * 解码 {@code [origin, origin + length)}。不改原文。
     */
    public int[] decode(char[] text, int origin, int length) {
        if (text == null || length <= 0) {
            return new int[0];
        }
        CharWindow.bind(origin, length);
        try {
            List<IntArrayList> featureSequence = instance.tansform(text, featureSet);
            int[] labels = new int[featureSequence.size()];
            decoder.decode(featureSequence, labels);
            return labels;
        } finally {
            CharWindow.clear();
        }
    }

    public static List<String> labelsToWords(char[] text, int[] labels) {
        int len = text.length;
        List<String> result = new ArrayList<>(len / 2 + 1);
        int wordStart = 0;
        for (int i = 0; i < len; i++) {
            int label = labels[i];
            if (label < 0) {
                label = S;
            }
            if (label == E || label == S) {
                result.add(new String(text, wordStart, i + 1 - wordStart));
                wordStart = i + 1;
            }
        }
        if (wordStart < len) {
            result.add(new String(text, wordStart, len - wordStart));
        }
        return result;
    }

    public static CwsPerceptron load(NlpResourceLoader loader) throws IOException {
        return load(loader, DEFAULT_MODEL_DIR);
    }

    public static CwsPerceptron load(NlpResourceLoader loader, String modelDir) throws IOException {
        PerceptronModel model = PerceptronBinModelLoader.load(loader, modelDir);
        return new CwsPerceptron(model);
    }

    public static CwsPerceptron loadFromDir(File dir) throws IOException {
        PerceptronModel model = PerceptronBinModelLoader.load(dir);
        return new CwsPerceptron(model);
    }

    public static CharBasedPerceptronInstance getInstance() {
        return instance;
    }

    static final CharBasedPerceptronInstance instance = initInstance();

    private static CharBasedPerceptronInstance initInstance() {
        FeatureExtractor<char[]> featureFun = new CwsFeatureExtractor();
        return new CharBasedPerceptronInstance(
                LABELS,
                8,
                new CwsAnnotateTextParser(),
                new CwsEvaluateFunctionFactory(),
                featureFun
        );
    }

    private static class CwsEvaluateFunctionFactory implements EvaluateFunctionFactory {
        @Override
        public EvaluateFunction create(PerceptronDecoder decoder, ImmutableFeatureSet featureSet) {
            return new CwsEvaluateFunction(decoder, featureSet);
        }
    }
}
