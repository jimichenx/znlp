package dev.znlp.newwords.scorer;

import dev.znlp.newwords.NewWordDiscoveryTool;
import dev.znlp.newwords.engine.INewWordFindEngine;
import dev.znlp.newwords.entity.NewWord;

import java.io.File;
import java.util.List;

/**
 * AdaptiveNewWordScorer 使用示例
 * <p>
 * 展示如何使用新的自适应打分器进行智能新词发现
 * </p>
 *
 * @author jimichan
 */
public class AdaptiveScorerUsageExample {

    public static void main(String[] args) throws Exception {
        // 示例1：完全自适应模式（推荐）
        example1_FullyAdaptive();

        // 示例2：不同策略对比
        example2_CompareStrategies();

        // 示例3：半自适应模式
        example3_SemiAdaptive();

        // 示例4：对比旧的手动模式
        example4_CompareWithManual();
    }

    /**
     * 示例1：完全自适应模式（最简单、最智能）
     */
    public static void example1_FullyAdaptive() throws Exception {
        System.out.println("\n========== 示例1：完全自适应模式 ==========");

        // 创建自适应打分器（会自动学习最佳阈值）
        AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();

        // 使用新词发现工具
        File corpusDir = new File("data/corpus");
        if (!corpusDir.exists()) {
            System.out.println("语料目录不存在: " + corpusDir.getAbsolutePath());
            return;
        }

        // 创建引擎
        INewWordFindEngine.Config config = new INewWordFindEngine.Config(3, 12, 10);
        INewWordFindEngine engine = INewWordFindEngine.create(config, false, null);
        engine.setScorer(scorer);

        // 创建工具并执行发现
        NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(corpusDir, engine)
                .charset(java.nio.charset.StandardCharsets.UTF_8)
                .verbose(true)
                .build();

        List<NewWord> results = tool.discover();

        System.out.println("发现新词数量: " + results.size());
        System.out.println("\n前20个新词:");
        for (int i = 0; i < Math.min(20, results.size()); i++) {
            NewWord word = results.get(i);
            System.out.printf("%3d. %s (评分: %.2f, 频次: %d, 左熵: %.2f, 右熵: %.2f)%n",
                    i + 1, word.getWord(), word.getScore(),
                    word.getFreq(), word.getLe(), word.getRe());
        }
    }

    /**
     * 示例2：对比不同过滤策略
     */
    public static void example2_CompareStrategies() throws Exception {
        System.out.println("\n========== 示例2：对比不同过滤策略 ==========");

        File corpusDir = new File("data/corpus");
        if (!corpusDir.exists()) {
            System.out.println("语料目录不存在: " + corpusDir.getAbsolutePath());
            return;
        }

        INewWordFindEngine.Config config = new INewWordFindEngine.Config(3, 12, 10);

        // 严格模式：高精确度
        AdaptiveNewWordScorer strictScorer = AdaptiveNewWordScorer.createAdaptive(
                AdaptiveNewWordScorer.FilterStrategy.STRICT
        );
        INewWordFindEngine engine1 = INewWordFindEngine.create(config, false, null);
        engine1.setScorer(strictScorer);
        List<NewWord> strictResults = new NewWordDiscoveryTool.Builder(corpusDir, engine1).build().discover();
        System.out.println("严格模式 - 发现新词数量: " + strictResults.size());

        // 平衡模式：平衡准确率和召回率（推荐）
        AdaptiveNewWordScorer balancedScorer = AdaptiveNewWordScorer.createAdaptive(
                AdaptiveNewWordScorer.FilterStrategy.BALANCED
        );
        INewWordFindEngine engine2 = INewWordFindEngine.create(config, false, null);
        engine2.setScorer(balancedScorer);
        List<NewWord> balancedResults = new NewWordDiscoveryTool.Builder(corpusDir, engine2).build().discover();
        System.out.println("平衡模式 - 发现新词数量: " + balancedResults.size());

        // 宽松模式：高召回率
        AdaptiveNewWordScorer looseScorer = AdaptiveNewWordScorer.createAdaptive(
                AdaptiveNewWordScorer.FilterStrategy.LOOSE
        );
        INewWordFindEngine engine3 = INewWordFindEngine.create(config, false, null);
        engine3.setScorer(looseScorer);
        List<NewWord> looseResults = new NewWordDiscoveryTool.Builder(corpusDir, engine3).build().discover();
        System.out.println("宽松模式 - 发现新词数量: " + looseResults.size());

        System.out.println("\n策略对比:");
        System.out.println("  严格模式: " + strictResults.size() + " 个词（高质量）");
        System.out.println("  平衡模式: " + balancedResults.size() + " 个词（推荐）");
        System.out.println("  宽松模式: " + looseResults.size() + " 个词（高召回）");
    }

    /**
     * 示例3：半自适应模式（可以微调百分位数）
     */
    public static void example3_SemiAdaptive() throws Exception {
        System.out.println("\n========== 示例3：半自适应模式 ==========");

        File corpusDir = new File("data/corpus");
        if (!corpusDir.exists()) {
            System.out.println("语料目录不存在: " + corpusDir.getAbsolutePath());
            return;
        }

        // 保留前30%的词（更严格）
        AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createSemiAdaptive(
                0.7f,  // 百分位数（0.7表示保留评分在前30%的词）
                AdaptiveNewWordScorer.FilterStrategy.BALANCED
        );

        INewWordFindEngine.Config config = new INewWordFindEngine.Config(3, 12, 10);
        INewWordFindEngine engine = INewWordFindEngine.create(config, false, null);
        engine.setScorer(scorer);

        List<NewWord> results = new NewWordDiscoveryTool.Builder(corpusDir, engine).build().discover();

        System.out.println("半自适应模式（保留前30%）- 发现新词数量: " + results.size());

        // 查看学习到的阈值
        System.out.println("\n学习到的阈值:");
        System.out.println("  最小互信息: " + scorer.getLearnedMinMi());
        System.out.println("  最小熵: " + scorer.getLearnedMinEntropy());
        System.out.println("  最小评分: " + scorer.getLearnedMinScore());
    }

    /**
     * 示例4：对比手动模式（展示改进效果）
     */
    public static void example4_CompareWithManual() throws Exception {
        System.out.println("\n========== 示例4：自适应 vs 手动模式 ==========");

        File corpusDir = new File("data/corpus");
        if (!corpusDir.exists()) {
            System.out.println("语料目录不存在: " + corpusDir.getAbsolutePath());
            return;
        }

        INewWordFindEngine.Config config = new INewWordFindEngine.Config(3, 12, 10);

        // 手动模式（旧方式）
        AdaptiveNewWordScorer manualScorer = AdaptiveNewWordScorer.createManual(
                3.0f,  // minMi
                2.0f,  // minEntropy
                2,     // minAccessorVariety
                5,     // minFreq
                2      // minDocFreq
        );
        INewWordFindEngine engine1 = INewWordFindEngine.create(config, false, null);
        engine1.setScorer(manualScorer);
        List<NewWord> manualResults = new NewWordDiscoveryTool.Builder(corpusDir, engine1).build().discover();

        // 自适应模式（新方式）
        AdaptiveNewWordScorer adaptiveScorer = AdaptiveNewWordScorer.createAdaptive();
        INewWordFindEngine engine2 = INewWordFindEngine.create(config, false, null);
        engine2.setScorer(adaptiveScorer);
        List<NewWord> adaptiveResults = new NewWordDiscoveryTool.Builder(corpusDir, engine2).build().discover();

        System.out.println("手动模式 - 发现新词数量: " + manualResults.size());
        System.out.println("自适应模式 - 发现新词数量: " + adaptiveResults.size());

        // 分析差异
        System.out.println("\n优势对比:");
        System.out.println("  手动模式:");
        System.out.println("    ❌ 需要人工设置多个阈值");
        System.out.println("    ❌ 不同语料需要不同阈值");
        System.out.println("    ❌ 难以找到最佳参数");
        System.out.println("  自适应模式:");
        System.out.println("    ✅ 完全自动学习阈值");
        System.out.println("    ✅ 适应不同语料特点");
        System.out.println("    ✅ 智能过滤低质量词");
        System.out.println("    ✅ 左右熵平衡性检查");
    }

    /**
     * 工具方法：展示新词详情
     */
    private static void printWordDetails(NewWord word) {
        System.out.printf("  词语: %s%n", word.getWord());
        System.out.printf("    评分: %.2f%n", word.getScore());
        System.out.printf("    频次: %d%n", word.getFreq());
        System.out.printf("    文档频次: %d%n", word.getDocFreq());
        System.out.printf("    互信息: %.2f%n", word.getAvg_mi());
        System.out.printf("    熵: %.2f (左: %.2f, 右: %.2f)%n",
                word.getEntropy(), word.getLe(), word.getRe());
        System.out.printf("    邻字多样性: %d (左: %d, 右: %d)%n",
                Math.min(word.getLeftVariety(), word.getRightVariety()),
                word.getLeftVariety(), word.getRightVariety());
    }
}
