package dev.znlp.segment.impl.cws;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;

import java.io.IOException;

public final class CwsPerceptronFactory implements ServiceFactory<CwsPerceptron> {

    private final NlpResourceLoader resourceLoader;

    public CwsPerceptronFactory(NlpResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public CwsPerceptron create() {
        try {
            return CwsPerceptron.load(resourceLoader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load CWS perceptron model", e);
        }
    }
}
