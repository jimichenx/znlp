package dev.znlp.segment.impl.pos;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.EvaluateFunctionFactory;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.PerceptronInstance;
import dev.znlp.perceptron.feature.FeatureIdSeek;
import dev.znlp.perceptron.feature.FeatureVectors;
import dev.znlp.perceptron.AnnotateTextParser;
import dev.znlp.perceptron.utils.FastStringBuilder;
import dev.znlp.segment.Nature;

import java.util.Arrays;
import java.util.List;

/**
 * 词性标注感知机实例
 *
 * @author jimichan
 */
public class PosPerceptronInstance implements PerceptronInstance<String, List<String>> {

    private final String[] labels;
    private final PosAnnotateTextParser parser = new PosAnnotateTextParser();
    private final PosFeatureExtractor featureExtractor = new PosFeatureExtractor();
    private final int featureMaxSize = 50;

    public PosPerceptronInstance(String[] labels) {
        this.labels = labels;
    }

    public PosPerceptronInstance() {
        this.labels = Arrays.stream(Nature.values())
                .filter(n -> n != Nature.newWord && n != Nature.begin && n != Nature.end)
                .map(Nature::name)
                .sorted()
                .toArray(String[]::new);
    }

    @Override
    public int getFeatureMaxSize() {
        return featureMaxSize;
    }

    @Override
    public FeatureExtractor<List<String>> getFeatureFun() {
        return featureExtractor;
    }

    @Override
    public EvaluateFunctionFactory getEvaluateFunction() {
        return (decoder, featureSet) -> new PosEvaluateFunction(decoder);
    }

    @Override
    public AnnotateTextParser<String, List<String>> getAnnotateTextParser() {
        return parser;
    }

    @Override
    public String[] getLables() {
        return labels;
    }

    @Override
    public List<IntArrayList> tansform(List<String> input, FeatureIdSeek featureSet) {
        // buffer size estimate: featureMaxSize * avg_feature_len (say 32)
        FastStringBuilder builder = new FastStringBuilder(featureMaxSize * 32);
        int size = input.size();
        List<IntArrayList> out = FeatureVectors.borrow(size);
        FeatureVectors.Consumer consumer = new FeatureVectors.Consumer(featureSet);
        for (int i = 0; i < size; i++) {
            IntArrayList vector = out.get(i);
            consumer.vector = vector;
            featureExtractor.compute(input, i, builder, consumer);
            vector.add(0);
        }
        return out;
    }
}
