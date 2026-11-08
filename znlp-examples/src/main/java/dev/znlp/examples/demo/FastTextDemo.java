package dev.znlp.examples.demo;

import dev.znlp.fasttext.FastText;

import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.LossName;
import dev.znlp.fasttext.args.ModelName;

import java.io.File;

/**
 * FastText演示类，用于展示FastText模型的基本使用方法。
 * 该类演示了如何初始化模型参数、训练模型以及在测试集上评估模型性能。
 */
public class FastTextDemo {

    /**
     * 主方法，演示FastText模型的训练和评估流程
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 训练和测试数据文件路径
        File trainFile = new File("data/fasttext/hotel/hotel-train-seg.txt");
        File testFile = new File("data/fasttext/hotel/hotel-test-seg.txt");

        // 检查训练文件是否存在
        if (!trainFile.exists()) {
            System.err.println("Train file not found: " + trainFile.getAbsolutePath());
            return;
        }

        System.out.println("Training FastText model...");
        
        // 初始化训练参数
        Args trainArgs = new Args();
        trainArgs.model = ModelName.sup;         // 监督学习模式
        trainArgs.loss = LossName.hs;           // 分层Softmax损失
        trainArgs.lr = 0.2;                     // 学习率
        trainArgs.dim = 100;                    // 词向量维度
        trainArgs.epoch = 10;                   // 训练轮数
        trainArgs.wordNgrams = 2;               // 词n-gram大小
        trainArgs.minCount = 1;                 // 词的最小出现次数
        
        // 训练FastText模型
        FastText fastText = FastText.train(trainFile, trainArgs);
        
        System.out.println("Training complete.");
        System.out.println("Evaluating on test set...");
        
        // 在测试集上评估模型
        fastText.test(testFile, 1, 0.0f, true);
    }
}