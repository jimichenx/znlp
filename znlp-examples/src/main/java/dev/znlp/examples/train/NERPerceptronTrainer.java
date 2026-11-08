package dev.znlp.examples.train;

import dev.znlp.base.common.ProjectPathLocator;
import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.perceptron.train.PerceptronTrainer;
import dev.znlp.segment.WordAndNature;
import dev.znlp.segment.impl.ner.NERPerceptronInstance;

import java.io.File;
import java.util.List;

/**
 * NER Perceptron Trainer
 *
 * @author jimichan
 */
public class NERPerceptronTrainer {

    public static void main(String[] args) throws Exception {
        // 1. 训练数据目录：可用第一个参数覆盖，默认取仓库同级的 znlp-data
        File dataDir = args.length > 0
                ? new File(args[0])
                : ProjectPathLocator.getProjectPath("../../znlp-data/data_train/ner/");
        if (!dataDir.isDirectory()) {
            throw new IllegalArgumentException("找不到 NER 训练数据目录: " + dataDir.getAbsolutePath()
                    + "（可用第一个参数指定，目录下需有 train/ 与 test/）");
        }

        System.out.println("Training data directory: " + dataDir.getAbsolutePath());

        File trainDir = new File(dataDir, "train");
        File testDir = new File(dataDir, "test");

        // 2. Create Instance (Labels and targetPos are now handled internally)
        NERPerceptronInstance instance = new NERPerceptronInstance();

        // 3. Create Trainer - 使用 ensureDir 自动创建工作目录
        File workingDir = ProjectPathLocator.ensureProjectPath("data/work/ner-model-dynamic");
        System.out.println("Working directory: " + workingDir.getAbsolutePath());
        PerceptronTrainer<WordAndNature, List<WordAndNature>> trainer =
                new PerceptronTrainer<>(instance, workingDir);

        // 4. Train
        PerceptronTrainModel trainModel = trainer.train(trainDir, testDir, 10, 4, false);

        // 5. Save Model - 使用 ensureDir 自动创建模型目录
        File dir  = ProjectPathLocator.ensureProjectPath(
                "znlp-resources/src/main/resources/znlp/ner/");
        dir.mkdirs();

        System.out.println("Saving model to: " + dir.getAbsolutePath());
        trainModel.saveModelToDir(dir);

        System.out.println("Model saved successfully.");
    }
}
