package dev.znlp.segment.impl.ner;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;

import java.io.IOException;

public final class NERPerceptronFactory implements ServiceFactory<NERPerceptron> {

    private static final String NER_MODEL_PATH = "znlp/ner/ner.model";

    private final NlpResourceLoader resourceLoader;

    public NERPerceptronFactory(NlpResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public NERPerceptron create() {
        try {
            return NERPerceptron.loadFromResource(resourceLoader, NER_MODEL_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load NER perceptron model", e);
        }
    }
}
