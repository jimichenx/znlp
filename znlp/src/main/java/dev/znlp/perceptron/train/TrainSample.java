package dev.znlp.perceptron.train;

import dev.znlp.base.common.IntArrayList;

import java.util.List;

/**
 * 感知机训练的样本
 * FeatureVector最后多一位是留个转移特征使用
 */
public class TrainSample {
    private final List<IntArrayList> featureSequence;
    private final int[] label;
    private final int size;

    public TrainSample(List<IntArrayList> featureSequence, int[] label) {
        this.featureSequence = featureSequence;
        this.label = label;
        this.size = featureSequence.size();
    }

    public List<IntArrayList> getFeatureSequence() {
        return featureSequence;
    }

    public int[] getLabel() {
        return label;
    }

    public int getSize() {
        return size;
    }
}