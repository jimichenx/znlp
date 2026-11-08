package dev.znlp.starspace;

import org.junit.jupiter.api.Test;
import dev.znlp.starspace.StarSpacePrediction.Prediction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StarSpaceMicroTrainTest {

    @Test
    void trainSaveLoadAndPredict() throws Exception {
        Path train = Files.createTempFile("ss-train", ".txt");
        Path modelDir = Files.createTempDirectory("ss-model");
        try {
            Files.writeString(train, """
                    __label__pos 服务 很好 房间 干净
                    __label__neg 服务 很差 房间 脏
                    __label__pos 态度 热情 体验 满意
                    __label__neg 态度 冷淡 体验 糟糕
                    """.repeat(20), StandardCharsets.UTF_8);

            Args args = new Args();
            args.lr = 0.05;
            args.dim = 16;
            args.epoch = 2;
            args.loss = Args.LossFunction.Hinge;
            args.trainMode = Args.TrainMode.Mode0;
            args.label = "__label__";
            args.thread = 1;
            args.adagrad = true;

            ModelTrainer trainer = new ModelTrainer(args, train.toString(), null);
            StarSpace model = trainer.train();
            model.saveModel(modelDir.toString());

            StarSpace loaded = StarSpace.loadModel(modelDir.toString(), false);
            StarSpacePrediction predictor = loaded.prediction();
            List<Prediction> predictions = predictor.predictOne("服务 很好", 1);
            assertFalse(predictions.isEmpty());
            assertTrue(predictions.get(0).score > Float.NEGATIVE_INFINITY);
        } finally {
            Files.deleteIfExists(train);
            Files.walk(modelDir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
