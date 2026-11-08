package dev.znlp.examples.train;

import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.pinyin.spliter.PinyinSplitPerceptron;

import java.io.File;

import static dev.znlp.perceptron.train.PerceptronTrainModel.FeatureFormat.FeatureText;

/**
 * 单线程训练 Evaluate Iter 5 正确率(P) 99.62 , 召回率(R) 99.43 , F1 99.52
 * 并行5线程 Evaluate Iter 5 正确率(P) 99.55 , 召回率(R) 99.36 , F1 99.46
 */
public class PinyinSplitPerceptronTrainer {

    public static void main(String[] args) throws Exception {
        String trainFilePath = "data/source/train/pinyin-split/yuliao_train.txt";
        String evaluateFilePath = "data/source/train/pinyin-split/yuliao_test.txt"; // 可选
        File workingDir =  new File("data/work/pinyin-split");


        var trainer = PinyinSplitPerceptron.getInstance().craeteTrainer(workingDir);

        PerceptronTrainModel model = trainer.train(
                new File(trainFilePath),
                new File(evaluateFilePath),
                5,      // 最大迭代次数，如 10
                1,    // 线程数，如 1（单线程）或 4（多线程）
                false   // 是否使用快速解码
        );

        model.saveModelToDir(new File("data/work/pinyin-split/model"));

        model.saveModelBinFile(new File("znlp-resources/src/main/resources/znlp/pinyin/pinyin-split.model"), FeatureText);


        PinyinSplitPerceptron d = new PinyinSplitPerceptron(model.getDecoder(), model.toModel().getFeatureSet());

        System.out.println(d.split("nihaoshijiezheshiwodejiqiren,wozaixianshidengni"));
    }

}