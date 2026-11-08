package dev.znlp.examples;

import dev.znlp.fasttext.FastText;
import dev.znlp.fasttext.FastTextTestResult;
import dev.znlp.fasttext.Model;
import dev.znlp.fasttext.ScoreLabelPair;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.LossName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * FastText 文本分类示例：训练、预测、P@k 评估、保存/加载。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.FastTextExample}
 */
public class FastTextExample {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("znlp-fasttext-example");
        Path trainFile = dir.resolve("train.txt");
        Path testFile = dir.resolve("test.txt");
        Path modelFile = dir.resolve("hotel.bin");

        Files.writeString(trainFile, """
                __label__pos 服务 很好 房间 干净 态度 热情
                __label__pos 体验 满意 早餐 丰富 位置 方便
                __label__pos 设施 齐全 床铺 舒适 环境 安静
                __label__neg 服务 很差 房间 脏 态度 冷淡
                __label__neg 体验 糟糕 噪音 很大 位置 偏僻
                __label__neg 设施 老旧 床铺 硬 环境 嘈杂
                """.repeat(40), StandardCharsets.UTF_8);

        Files.writeString(testFile, """
                __label__pos 服务 很好 环境 安静
                __label__pos 床铺 舒适 早餐 丰富
                __label__neg 房间 脏 噪音 很大
                __label__neg 态度 冷淡 体验 糟糕
                """, StandardCharsets.UTF_8);

        Args trainArgs = new Args();
        trainArgs.dim = 32;
        trainArgs.epoch = 10;
        trainArgs.lr = 0.1;
        trainArgs.loss = LossName.softmax;
        trainArgs.minCount = 1;
        trainArgs.wordNgrams = 2;
        trainArgs.minn = 3;
        trainArgs.maxn = 6;
        trainArgs.thread = 1;
        trainArgs.bucket = 1000;
        trainArgs.seed = 1;

        System.out.println("训练有监督分类模型（保留字符 n-gram）...");
        FastText model = FastText.trainSupervised(trainFile.toFile(), trainArgs);

        section("预测");
        show(model, "服务 很好 房间 干净");
        show(model, "房间 脏 态度 冷淡");

        section("无限 k（返回全部标签）");
        List<ScoreLabelPair> all = model.predict(List.of("服务", "很好"), Model.kUnlimitedPredictions, 0.0f);
        for (ScoreLabelPair pair : all) {
            System.out.printf("  %s  %.4f%n", pair.label, pair.score);
        }

        section("测试集 P@1 / R@1");
        FastTextTestResult metrics = model.test(testFile.toFile(), 1, 0.0f, true);
        System.out.printf("nfound=%d / nexamples=%d%n", metrics.nfound(), metrics.nexamples());

        section("保存并重新加载");
        model.saveModel(modelFile.toFile());
        FastText loaded = FastText.loadModel(modelFile.toFile());
        System.out.println("模型文件: " + modelFile.toAbsolutePath());
        show(loaded, "服务 很好 房间 干净");

        System.out.println();
        System.out.println("临时目录: " + dir.toAbsolutePath());
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    private static void show(FastText model, String text) {
        List<ScoreLabelPair> predictions = model.predict(List.of(text.split("\\s+")), 2, 0.0f);
        System.out.println(text);
        for (ScoreLabelPair pair : predictions) {
            System.out.printf("  %s  %.4f%n", pair.label, pair.score);
        }
    }
}
