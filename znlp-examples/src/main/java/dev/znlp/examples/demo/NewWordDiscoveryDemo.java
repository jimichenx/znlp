package dev.znlp.examples.demo;

import dev.znlp.newwords.NewWordDiscoveryTool;
import dev.znlp.newwords.engine.INewWordFindEngine;
import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.scorer.NewWordScorer;
import dev.znlp.newwords.scorer.NewWordScorerFactory;
import dev.znlp.newwords.scorer.SimpleNewWordScorer;
import dev.znlp.segment.impl.dict.CoreDictionary;
import dev.znlp.segment.impl.dict.CoreDictionaryFactory;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * 新词发现功能测试
 *
 * <p>该测试类演示如何使用 {@link NewWordDiscoveryTool} 从文本文件中自动发现新词。
 * 测试数据应放置在 data/newword_test 目录下。</p>
 *
 * <h3>测试步骤</h3>
 * <ol>
 *   <li>加载系统配置和核心词典</li>
 *   <li>创建新词发现工具实例</li>
 *   <li>执行新词发现</li>
 *   <li>将结果保存到CSV文件</li>
 * </ol>
 *
 * @author jimichan
 */
public class NewWordDiscoveryDemo {

    private static final String Data_PATH = "data/newword_test";

    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(60));
            System.out.println("新词发现测试");
            System.out.println("=".repeat(60));

            // 步骤1: 初始化系统配置和核心词典
            CoreDictionary coreDictionary = initializeCoreDictionary();

            // 步骤2: 定位数据目录
            File dataDirectory = locateDataDirectory();
            System.out.println("[配置] 数据目录: " + dataDirectory.getAbsolutePath());

            // 步骤3: 创建新词发现工具
            NewWordDiscoveryTool tool = createDiscoveryTool(dataDirectory, coreDictionary);

            // 步骤4: 执行新词发现
            System.out.println();
            ArrayList<NewWord> results = tool.discover();

            // 步骤5: 保存结果
            File outputFile = new File(dataDirectory.getParent(), "newword_test_result.csv");
            NewWordDiscoveryTool.saveResults(results, outputFile, true, "\t", true, StandardCharsets.UTF_8);

            // 步骤6: 显示部分结果示例
            displaySampleResults(results);

            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("测试完成！");
            System.out.println("结果文件: " + outputFile.getAbsolutePath());
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("\n测试执行失败:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 初始化核心词典
     */
    private static CoreDictionary initializeCoreDictionary() throws Exception {
        System.out.println("[初始化] 正在加载核心词典...");

        ZnlpSystemConfig config = ZnlpSystemConfig.builder()
                .dataDir("data/dat")
                .cacheDir("data/cache")
                .build();
        NlpResourceLoader loader = new NlpResourceLoader(config);
        CoreDictionaryFactory factory = new CoreDictionaryFactory(config, loader);
        CoreDictionary dictionary = factory.create();

        System.out.println("[初始化] 核心词典加载完成");
        return dictionary;
    }

    /**
     * 定位数据目录
     */
    private static File locateDataDirectory() {
        File projectRoot = new File(System.getProperty("user.dir"));
        File dataDir = new File(projectRoot, Data_PATH);

        // 如果当前在znlp子目录，向上查找
        if (!dataDir.exists()) {
            File parentDir = projectRoot.getParentFile();
            if (parentDir != null) {
                dataDir = new File(parentDir, Data_PATH);
            }
        }

        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new RuntimeException(
                    "数据目录不存在: " + dataDir.getAbsolutePath() + "\n" +
                            "请确保 " + Data_PATH + " 目录存在并包含文本文件"
            );
        }

        return dataDir;
    }

    /**
     * 创建新词发现工具实例
     */
    private static NewWordDiscoveryTool createDiscoveryTool(
            File dataDirectory, CoreDictionary coreDictionary) {

        System.out.println("[配置] 创建新词发现工具...");

        // 创建配置对象
        INewWordFindEngine.Config engineConfig = new INewWordFindEngine.Config(3, 12, 5);
        boolean excludeCoreDict = true;
        NewWordScorer scorer = SimpleNewWordScorer.create(2.0f, 1.0f);


        NewWordScorer scorer2 = NewWordScorerFactory.createAdaptive();

        // 创建新词发现引擎并设置 scorer
        INewWordFindEngine engine = 
                INewWordFindEngine.create(
                        engineConfig,
                        excludeCoreDict,
                        coreDictionary
                );
        engine.setScorer(scorer2);

        return new NewWordDiscoveryTool.Builder(dataDirectory, engine)
                .charset(StandardCharsets.UTF_8)
                .verbose(true)
                .build();
    }

    /**
     * 显示部分结果示例
     */
    private static void displaySampleResults(ArrayList<NewWord> results) {
        if (results.isEmpty()) {
            System.out.println("\n未发现新词");
            return;
        }

        System.out.println();
        System.out.println("前10个新词示例:");
        System.out.println("-".repeat(60));
        System.out.printf("%-10s %6s %8s %8s %8s%n",
                "词语", "词长", "评分", "频次", "互信息");
        System.out.println("-".repeat(60));

        int displayCount = Math.min(10, results.size());
        for (int i = 0; i < displayCount; i++) {
            NewWord word = results.get(i);
            System.out.printf("%-10s %6d %8.2f %8d %8.2f%n",
                    word.getWord(),
                    word.getLen(),
                    word.getScore(),
                    word.getFreq(),
                    word.getMi()
            );
        }
        System.out.println("-".repeat(60));
    }
}
