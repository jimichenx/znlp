package dev.znlp.examples.train;

import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.segment.impl.personname.PersonNamePerceptron;

import java.io.File;

import static dev.znlp.perceptron.train.PerceptronTrainModel.FeatureFormat.FeatureText;

/**
 * 单线程训练 Evaluate Iter 5 正确率(P) 99.62 , 召回率(R) 99.43 , F1 99.52
 * 并行5线程 Evaluate Iter 5 正确率(P) 99.55 , 召回率(R) 99.36 , F1 99.46
 */
public class PersonNamePerceptronTrainer {

    public static void main(String[] args) throws Exception {
        String trainFilePath = "data/source/train/name-nr/train";
        String evaluateFilePath = "data/source/train/name-nr/test"; // 可选
        File workingDir = new File("data/work/name-nr");


        var trainer = PersonNamePerceptron.getInstance().craeteTrainer(workingDir);

        PerceptronTrainModel model = trainer.train(
                new File(trainFilePath),
                new File(evaluateFilePath),
                23,      // 最大迭代次数，如 10
                1,    // 线程数，如 1（单线程）或 4（多线程）
                false   // 是否使用快速解码
        );

        model.saveModelToDir(new File("data/work/name-nr/model"));


        PerceptronTrainModel compressedModel = model.compress(0.5f, 0.01f);

        compressedModel.saveModelBinFile(
                new File("znlp-resources/src/main/resources/znlp/personname/nr-name.model"), FeatureText);

        PersonNamePerceptron d = new PersonNamePerceptron(compressedModel.getDecoder(), compressedModel.toModel().getFeatureSet());

        System.out.println(d.findPersonName("这个是陈宝奇的快递"));
    }

}