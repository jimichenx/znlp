package dev.znlp.segment.impl.pos;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;

import java.io.IOException;

public final class PosPerceptronFactory implements ServiceFactory<PosPerceptron> {

    private static final String POS_MODEL_PATH = "znlp/pos-model/pos.model";

    private final NlpResourceLoader resourceLoader;

    public PosPerceptronFactory(NlpResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public PosPerceptron create() {
        try {
            return PosPerceptron.load(resourceLoader, POS_MODEL_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load POS perceptron model", e);
        }
    }
}
