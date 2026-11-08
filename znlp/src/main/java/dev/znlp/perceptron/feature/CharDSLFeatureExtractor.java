package dev.znlp.perceptron.feature;

import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.utils.FastStringBuilder;

public class CharDSLFeatureExtractor implements FeatureExtractor<char[]> {

    private final FeatureTemplate template;

    public CharDSLFeatureExtractor(String templateRules) {
        template = new FeatureTemplate(templateRules);
    }

    @Override
    public void compute(char[] sentence, int position, FastStringBuilder buffer, FeatureConsumer consumer) {
        template.features(sentence, position, buffer, consumer);
    }
}