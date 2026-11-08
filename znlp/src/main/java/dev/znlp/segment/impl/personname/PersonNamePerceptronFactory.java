package dev.znlp.segment.impl.personname;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;

import java.io.IOException;

final public class PersonNamePerceptronFactory implements ServiceFactory<PersonNamePerceptron> {

    private final static String PERSON_NAME_MODEL_PATH = "znlp/personname/nr-name.model";

    private final NlpResourceLoader resourceLoader;

    public PersonNamePerceptronFactory(NlpResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public PersonNamePerceptron create() {
        try {
            return PersonNamePerceptron.load(resourceLoader, PERSON_NAME_MODEL_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}