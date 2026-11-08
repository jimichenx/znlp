package dev.znlp.duckling.ml;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.duckling.support.DkNode;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消歧用朴素贝叶斯。{@link #trueProbalog} 会缓存到 {@link DkNode} 上。
 */
public class MlModel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MlModel.class);

    private final Map<String, Classifier> classifiers;

    public MlModel(Map<String, Classifier> classifiers) {
        this.classifiers = classifiers;
    }

    public double trueProbalog(DkNode<?> node) {
        return classify(node, true);
    }

    public double classify(DkNode<?> node, boolean target) {
        if (node.getProbalog() != null) {
            return node.getProbalog();
        }

        FeatureInput input = node.getFeatureInput();
        Classifier classifier = classifiers.get(input.classifierId());

        if (classifier == null) {
            node.setProbalog(0.0);
            return 0.0;
        }

        Map<String, Integer> bagOfFeatures = feature2Count(input.features());
        ClassifierResult result = classifier.scores(bagOfFeatures);

        double probalog = target ? result.trueScore() : result.falseScore();

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (DkNode<?> child : node.getChildren()) {
                probalog += this.classify(child, target);
            }
        }

        node.setProbalog(probalog);
        return probalog;
    }

    private Map<String, Integer> feature2Count(List<String> features) {
        if (features == null || features.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Integer> countMap = new HashMap<>();
        for (String feature : features) {
            countMap.put(feature, countMap.getOrDefault(feature, 0) + 1);
        }
        return countMap;
    }

    public static MlModel load() {
        try (InputStream ins = MlModel.class.getClassLoader().getResourceAsStream("znlp/duckling.model")) {
            if (ins == null) {
                logger.warn("model.bin not found in classpath");
                return new MlModel(new HashMap<>());
            }
            try (DataInputStream dat = new DataInputStream(new BufferedInputStream(ins))) {
                int size = dat.readInt();
                Map<String, Classifier> classifiers = new HashMap<>(size);

                for (int i = 0; i < size; i++) {
                    String key = dat.readUTF();
                    Classifier info = Classifier.load(dat);
                    classifiers.put(key, info);
                }

                return new MlModel(classifiers);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model", e);
        }
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(classifiers.size());
        for (Map.Entry<String, Classifier> entry : classifiers.entrySet()) {
            out.writeUTF(entry.getKey());
            entry.getValue().write(out);
        }
    }
}
