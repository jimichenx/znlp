package dev.znlp.perceptron.train;

import dev.znlp.base.common.IntArrayList;

import java.util.List;

/**
 * 感知机训练的样本
 * FeatureVector最后多一位是留个转移特征使用
 */
public class EvaluationSample {
    /**
     * 样本原始的内容
     */
    private final String line;
    private final List<IntArrayList> featureSequence;
    private final int[] label;
    private final int size;
    private final Object inputSequence;

    public EvaluationSample(
            String line,
            List<IntArrayList> featureSequence, int[] label,
                          Object inputSequence) {
        this.line = line;
        this.featureSequence = featureSequence;
        this.label = label;
        this.size = featureSequence.size();
        this.inputSequence = inputSequence; // 输入样本
    }

    public String getLine() {
        return line;
    }

    public List<IntArrayList> getFeatureSequence() {
        return featureSequence;
    }

    public int[] getLabel() {
        return label;
    }

    public Object getInputSequence() {
        return inputSequence;
    }

    public int getSize() {
        return size;
    }
}