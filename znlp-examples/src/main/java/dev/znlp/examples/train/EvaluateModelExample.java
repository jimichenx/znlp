package dev.znlp.examples.train;

import dev.znlp.perceptron.EvaluateResult;
import dev.znlp.perceptron.PerceptronModel;
import dev.znlp.segment.impl.personname.PersonNamePerceptron;

import java.io.File;

/**
 * 演示如何使用 PerceptronTrainer 的评估方法对已训练好的模型进行评估
 */
public class EvaluateModelExample {

    public static void main(String[] args) throws Exception {
        // 示例2: 加载已保存的模型进行评估
        evaluateSavedModel();
    }

    /**
     * 示例2: 加载已保存的模型进行评估
     */
    @SuppressWarnings("unused")
    private static void evaluateSavedModel() throws Exception {
        String modelFilePath = "data/output/train/name-nr/nr-name-model.bin";
        File evaluateFilePath = new File("data/source/train/name-nr/test");
        File workingDir = new File("data/output/train/name-nr/cache");

        // 加载已保存的模型
        PerceptronModel model = PerceptronModel.read(new File(modelFilePath));

        // 使用新增的评估方法
        System.out.println("\n========== 评估已保存的模型 ==========");
        EvaluateResult result =  PersonNamePerceptron.getInstance().evaluate(evaluateFilePath,workingDir, model);
    }
}
