package dev.znlp.segment.impl.pos;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.PerceptronModel;
import dev.znlp.perceptron.AnnotateTextParser;
import dev.znlp.perceptron.train.PerceptronTrainer;
import dev.znlp.perceptron.train.PerceptronTrainModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * 词性标注感知机
 *
 * @author jimichan
 */
public class PosPerceptron {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PosPerceptron.class);

    private final PerceptronModel model;
    private final PerceptronDecoder decoder;
    private final PosPerceptronInstance instance;

    public PosPerceptron(PerceptronModel model) {
        this.model = model;
        this.decoder = model.getDecoder();
        this.instance = new PosPerceptronInstance();
    }

    /**
     * 对分词结果进行词性标注
     *
     * @param sentence 分词列表
     * @return 词性列表
     */
    public List<String> decode(List<String> sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return new ArrayList<>();
        }

        List<IntArrayList> featureSequence = instance.tansform(sentence, model.getFeatureSet());
        int[] result = new int[featureSequence.size()];
        decoder.decode(featureSequence, result);

        List<String> tags = new ArrayList<>(result.length);
        String[] labels = instance.getLables();
        for (int id : result) {
            if (id >= 0 && id < labels.length) {
                tags.add(labels[id]);
            } else {
                tags.add("x"); // 未知或默认词性
            }
        }
        return tags;
    }

    public PosPerceptronInstance getInstance() {
        return instance;
    }

    public static PosPerceptron loadFromClasspath(String path) throws IOException {
        PerceptronModel model = PerceptronModel.readFromClasspath(path);
        return new PosPerceptron(model);
    }

    public static PosPerceptron load(NlpResourceLoader loader, String path) throws IOException {
        PerceptronModel model = PerceptronModel.readFromNlpResource(loader, path);
        return new PosPerceptron(model);
    }

    public static PerceptronTrainModel train(File trainFile, File evalFile,File workDir, int iter, int threadNum) throws Exception {
        // 1. Scan labels
        logger.info("Scanning labels from {}", trainFile.getAbsolutePath());
        Set<String> labelSet = new HashSet<>();
        PosAnnotateTextParser parser = new PosAnnotateTextParser();

        try (Stream<Path> paths = Files.walk(trainFile.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(path -> {
                        try (Stream<String> lines = Files.lines(path)) {
                            lines.forEach(line -> {
                                List<AnnotateTextParser.Pair<String>> pairs = parser.parse(line);
                                for (AnnotateTextParser.Pair<String> p : pairs) {
                                    labelSet.add(p.second());
                                }
                            });
                        } catch (IOException e) {
                            logger.warn("读取文件失败: {}", path, e);
                        }
                    });
        }

        String[] labels = labelSet.stream().sorted().toArray(String[]::new);
        logger.info("Scanned {} labels: {}", labels.length, Arrays.toString(labels));

        // 2. Train
        PosPerceptronInstance instance = new PosPerceptronInstance(labels);
        PerceptronTrainer<String, List<String>> trainer = new PerceptronTrainer<>(instance, workDir);
        return trainer.train(trainFile, evalFile, iter, threadNum, true);
    }
}
