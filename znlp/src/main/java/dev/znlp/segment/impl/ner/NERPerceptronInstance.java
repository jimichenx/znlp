package dev.znlp.segment.impl.ner;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.perceptron.AnnotateTextParser;
import dev.znlp.perceptron.EvaluateFunctionFactory;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.PerceptronInstance;
import dev.znlp.perceptron.feature.FeatureIdSeek;
import dev.znlp.perceptron.feature.FeatureVectors;
import dev.znlp.perceptron.utils.FastStringBuilder;
import dev.znlp.segment.WordAndNature;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * NER Perceptron Instance
 *
 * @author jimichan
 */
public class NERPerceptronInstance implements PerceptronInstance<WordAndNature, List<WordAndNature>> {

    private final String[] labels;
    private final AnnotateTextParser<WordAndNature, List<WordAndNature>> annotateTextParser;
    private final FeatureExtractor<List<WordAndNature>> featureExtractor;

    // Defined targetPos field initialized with ns and nt
    private final Set<String> targetPos = new TreeSet<>(Set.of("ns", "nt"));

    public NERPerceptronInstance() {
        // Dynamically calculate labels based on targetPos
        List<String> labelList = new ArrayList<>();
        for (String pos : targetPos) {
            labelList.add("B-" + pos);
            labelList.add("M-" + pos);
            labelList.add("E-" + pos);
        }
        labelList.add("O");
        labelList.add("S");
        
        this.labels = labelList.toArray(new String[0]);

        // Pass targetPos to AnnotateTextParser
        this.annotateTextParser = new NERAnnotateTextParser(targetPos);
        this.featureExtractor = new NERFeatureExtractor();
    }

    @Override
    public int getFeatureMaxSize() {
        return 100; // Buffer size
    }

    @Override
    public FeatureExtractor<List<WordAndNature>> getFeatureFun() {
        return featureExtractor;
    }

    @Override
    public EvaluateFunctionFactory getEvaluateFunction() {
        return (decoder, featureSet) -> new NEREvaluateFunction(decoder, featureSet, getLables());
    }

    @Override
    public AnnotateTextParser<WordAndNature, List<WordAndNature>> getAnnotateTextParser() {
        return annotateTextParser;
    }

    @Override
    public String[] getLables() {
        return labels;
    }

    @Override
    public List<IntArrayList> tansform(List<WordAndNature> input, FeatureIdSeek featureSet) {
        FastStringBuilder builder = new FastStringBuilder(getFeatureMaxSize());
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
