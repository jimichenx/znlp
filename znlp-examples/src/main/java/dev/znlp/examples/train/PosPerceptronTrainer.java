package dev.znlp.examples.train;

import dev.znlp.base.common.ProjectPathLocator;
import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.segment.impl.pos.PosPerceptron;
import dev.znlp.segment.impl.pos.PosPerceptronInstance;

import java.io.File;

public class PosPerceptronTrainer {

    static File trainDir = ProjectPathLocator.getProjectPath("data/work/pos.data/train");
    static File testDir = ProjectPathLocator.getProjectPath("data/work/pos.data/test");

    static File modelDir = ProjectPathLocator.ensureProjectPath("data/work/pos-model");
    static File workDir = modelDir;


    public static void main(String[] args) throws Exception {
        // 阶段：train 训练、compress 压缩并评估、save 压缩后写入 resources（默认）
        String stage = args.length > 0 ? args[0] : "save";
        switch (stage) {
            case "train" -> train();
            case "compress" -> compress();
            case "save" -> saveToBinFile();
            default -> throw new IllegalArgumentException("未知阶段: " + stage + "，可选 train | compress | save");
        }
    }

    private static void train() throws Exception {

        if (!trainDir.exists()) {
            System.err.println("训练数据未找到: " + trainDir.getAbsolutePath());
            return;
        }

        System.out.println("开始训练 POS 模型...");
        // 迭代5次，1线程 (单线程以避免OOM)
        PerceptronTrainModel model = PosPerceptron.train(trainDir, testDir, workDir, 5, 1);

        System.out.println("保存模型到: " + modelDir.getAbsolutePath());
        model.saveModelToDir(modelDir);
    }

    private static void compress() throws Exception {
        PerceptronTrainModel model = PerceptronTrainModel.readModelFromDir(modelDir);
        PosPerceptronInstance instance = new PosPerceptronInstance();

        for (int i = 0; i < 12; i++) {
            float ratio = 0.1f + i * 0.05f;
            if (ratio > 0.8f) {
                continue;
            }

            try{
                System.out.println("========Ratio: " + ratio + "======");
                PerceptronTrainModel newModel = model.compress(ratio, 0.001f);
                EvaluateResult evaluateResult = instance.evaluate(testDir, null, newModel);
            }catch(Exception e){
                break;
            }
        }

    }

    private static void saveToBinFile() throws Exception {

        PerceptronTrainModel model = PerceptronTrainModel.readModelFromDir(modelDir);

        PerceptronTrainModel newModel = model.compress(0.6f, 0.001f);

        File binFile = new File(ProjectPathLocator.
                ensureProjectPath("znlp-resources/src/main/resources/znlp/pos-model"),
                "pos.model");

        newModel.saveModelBinFile(binFile, PerceptronTrainModel.FeatureFormat.FeatureText);
    }
}
