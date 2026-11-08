package dev.znlp.segment.impl.ner;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.PerceptronModel;
import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.segment.WordAndNature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * NER Perceptron Runtime
 *
 * @author jimichan
 */
public class NERPerceptron {

    private final PerceptronModel model;
    private final String[] labels;
    private final NERPerceptronInstance instance;

    public NERPerceptron(PerceptronModel model) {
        this.model = model;
        // Use the same fixed labels as Instance
        this.instance = new NERPerceptronInstance();
        this.labels = instance.getLables();
    }

    public List<String> decode(List<WordAndNature> sentence) {
        List<IntArrayList> featureSequence = instance.tansform(sentence, model.getFeatureSet());
        int[] resultIds = model.getDecoder().decode(featureSequence);
        
        List<String> resultLabels = new ArrayList<>(resultIds.length);
        for(int id : resultIds) {
            resultLabels.add(labels[id]);
        }
        return resultLabels;
    }

    /**
     * Load NER perceptron model from directory
     *
     * @param modelDir model directory
     * @return NERPerceptron instance
     * @throws IOException if I/O error occurs
     */
    public static NERPerceptron loadFromModelDir(File modelDir) throws IOException {
        PerceptronTrainModel trainModel = PerceptronTrainModel.readModelFromDir(modelDir);
        PerceptronModel model = trainModel.toModel();
        // Labels are hardcoded now, no need to read label.txt
        return new NERPerceptron(model);
    }

    /**
     * Load NER perceptron model from classpath
     *
     * @param classPath model path in classpath (e.g., "models/ner-model.bin")
     * @return NERPerceptron instance
     * @throws IOException if I/O error occurs
     */
    public static NERPerceptron loadFromClasspath(String classPath) throws IOException {
        PerceptronModel model = PerceptronModel.readFromClasspath(classPath);
        return new NERPerceptron(model);
    }

    /**
     * Load NER perceptron model from NlpResourceLoader
     *
     * @param loader NlpResourceLoader instance
     * @param path model path (e.g., "models/ner-model.bin")
     * @return NERPerceptron instance
     * @throws IOException if I/O error occurs
     */
    public static NERPerceptron loadFromResource(NlpResourceLoader loader, String path) throws IOException {
        PerceptronModel model = PerceptronModel.readFromNlpResource(loader, path);
        return new NERPerceptron(model);
    }
}
