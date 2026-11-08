package dev.znlp.pinyin.spliter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.test.TestTags;
import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.utils.FastStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PinyinSplitFeatureExtractor 性能对比测试
 * <p>
 * 对比模板实现和手动实现的性能差异
 */

@Disabled
@Tag(TestTags.PERFORMANCE)
public class PinyinSplitFeatureExtractorPerformanceTest {

    private static final String TEMPLATE_RULES = "%[0]2;" +
            "%[-1]1;" +
            "%[-1]/%[0]5;" +
            "%[-2]/%[-1]4;" +
            "%[1]3;" +
            "%[0]/%[1]6;" +
            "%[1]/%[2]7;";

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;

    /**
     * 执行特征提取并测量时间
     */
    private long measureExtractionTime(FeatureExtractor<char[]> extractor, char[] sequence, int iterations) {
        FastStringBuilder buffer = new FastStringBuilder(100);
        FeatureConsumer consumer = feature -> {
            // 空操作，只测量提取时间
        };

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int pos = 0; pos < sequence.length; pos++) {
                extractor.compute(sequence, pos, buffer, consumer);
            }
        }
        long endTime = System.nanoTime();

        return endTime - startTime;
    }

    /**
     * 预热JVM
     */
    private void warmup(FeatureExtractor<char[]> extractor, char[] sequence) {
        FastStringBuilder buffer = new FastStringBuilder(100);
        FeatureConsumer consumer = feature -> {
        };

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (int pos = 0; pos < sequence.length; pos++) {
                extractor.compute(sequence, pos, buffer, consumer);
            }
        }
    }

    /**
     * 性能对比测试：短序列
     */
    @Test
    public void testPerformance_ShortSequence() {
        char[] sequence = "nihao".toCharArray();
        runPerformanceComparison("短序列(5字符)", sequence);
    }

    /**
     * 性能对比测试：中等序列
     */
    @Test
    public void testPerformance_MediumSequence() {
        char[] sequence = "wo shi zhong guo ren".toCharArray();
        runPerformanceComparison("中等序列(20字符)", sequence);
    }

    /**
     * 性能对比测试：长序列
     */
    @Test
    public void testPerformance_LongSequence() {
        char[] sequence = generateLongSequence(100);
        runPerformanceComparison("长序列(100字符)", sequence);
    }

    /**
     * 性能对比测试：超长序列
     */
    @Test
    public void testPerformance_VeryLongSequence() {
        char[] sequence = generateLongSequence(500);
        runPerformanceComparison("超长序列(500字符)", sequence);
    }

    /**
     * 性能对比测试：中文序列
     */
    @Test
    public void testPerformance_ChineseSequence() {
        char[] sequence = "世界你好我是中国人我爱我的祖国".toCharArray();
        runPerformanceComparison("中文序列(15字符)", sequence);
    }

    /**
     * 性能对比测试：混合序列
     */
    @Test
    public void testPerformance_MixedSequence() {
        char[] sequence = "wo shi zhong guo ren 我是中国人".toCharArray();
        runPerformanceComparison("混合序列(30字符)", sequence);
    }

    /**
     * 执行性能对比
     */
    private void runPerformanceComparison(String testName, char[] sequence) {
        FeatureExtractor<char[]> templateImpl = FeatureExtractor.buildByTemplate(TEMPLATE_RULES);
        FeatureExtractor<char[]> manualImpl = new PinyinSplitFeatureExtractor();

        // 预热
        System.out.println("\n========== " + testName + " 性能对比 ==========");
        System.out.println("序列长度: " + sequence.length);
        System.out.println("预热中...");
        warmup(templateImpl, sequence);
        warmup(manualImpl, sequence);

        // 测试模板实现
        long templateTime = measureExtractionTime(templateImpl, sequence, TEST_ITERATIONS);
        double templateTimeMs = templateTime / 1_000_000.0;
        double templateThroughput = (double) (TEST_ITERATIONS * sequence.length) / (templateTime / 1_000_000_000.0);

        // 测试手动实现
        long manualTime = measureExtractionTime(manualImpl, sequence, TEST_ITERATIONS);
        double manualTimeMs = manualTime / 1_000_000.0;
        double manualThroughput = (double) (TEST_ITERATIONS * sequence.length) / (manualTime / 1_000_000_000.0);

        // 计算性能提升
        double speedup = (double) templateTime / manualTime;
        double improvement = ((double) templateTime - manualTime) / templateTime * 100;

        // 输出结果
        System.out.println("\n模板实现:");
        System.out.println("  总耗时: " + String.format("%.2f", templateTimeMs) + " ms");
        System.out.println("  吞吐量: " + String.format("%.2f", templateThroughput) + " 位置/秒");

        System.out.println("\n手动实现:");
        System.out.println("  总耗时: " + String.format("%.2f", manualTimeMs) + " ms");
        System.out.println("  吞吐量: " + String.format("%.2f", manualThroughput) + " 位置/秒");

        System.out.println("\n性能提升:");
        System.out.println("  加速比: " + String.format("%.2f", speedup) + "x");
        System.out.println("  性能提升: " + String.format("%.2f", improvement) + "%");

        // 验证手动实现应该更快或至少不慢
        assertTrue(manualTime <= templateTime * 1.1,
                "手动实现应该不慢于模板实现（允许10%的误差）");
    }

    /**
     * 综合性能测试：多种序列长度
     */
    @Test
    public void testPerformance_Comprehensive() {
        System.out.println("\n\n========================================");
        System.out.println("综合性能对比测试");
        System.out.println("========================================\n");

        int[] sequenceLengths = {10, 50, 100, 200, 500, 1000};

        for (int length : sequenceLengths) {
            char[] sequence = generateLongSequence(length);
            runPerformanceComparison("序列长度(" + length + "字符)", sequence);
        }

        System.out.println("\n========================================");
        System.out.println("综合性能测试完成");
        System.out.println("========================================\n");
    }

    /**
     * 内存分配测试：对比两种实现的内存使用
     */
    @Test
    public void testMemoryAllocation() {
        char[] sequence = generateLongSequence(100);
        int iterations = 1000;

        FeatureExtractor<char[]> templateImpl = FeatureExtractor.buildByTemplate(TEMPLATE_RULES);
        FeatureExtractor<char[]> manualImpl = new PinyinSplitFeatureExtractor();

        // 预热
        warmup(templateImpl, sequence);
        warmup(manualImpl, sequence);

        // 强制GC
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 测试模板实现的内存分配
        long templateMemoryBefore = getUsedMemory();
        for (int i = 0; i < iterations; i++) {
            FastStringBuilder buffer = new FastStringBuilder(100);
            List<String> features = new ArrayList<>();
            FeatureConsumer consumer = features::add;
            for (int pos = 0; pos < sequence.length; pos++) {
                templateImpl.compute(sequence, pos, buffer, consumer);
            }
        }
        long templateMemoryAfter = getUsedMemory();
        long templateMemoryUsed = templateMemoryAfter - templateMemoryBefore;

        // 强制GC
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 测试手动实现的内存分配
        long manualMemoryBefore = getUsedMemory();
        for (int i = 0; i < iterations; i++) {
            FastStringBuilder buffer = new FastStringBuilder(100);
            List<String> features = new ArrayList<>();
            FeatureConsumer consumer = features::add;
            for (int pos = 0; pos < sequence.length; pos++) {
                manualImpl.compute(sequence, pos, buffer, consumer);
            }
        }
        long manualMemoryAfter = getUsedMemory();
        long manualMemoryUsed = manualMemoryAfter - manualMemoryBefore;

        System.out.println("\n========== 内存分配对比 ==========");
        System.out.println("序列长度: " + sequence.length);
        System.out.println("迭代次数: " + iterations);
        System.out.println("\n模板实现内存使用: " + templateMemoryUsed / 1024 + " KB");
        System.out.println("手动实现内存使用: " + manualMemoryUsed / 1024 + " KB");

        if (templateMemoryUsed > 0) {
            double memoryReduction = (1.0 - (double) manualMemoryUsed / templateMemoryUsed) * 100;
            System.out.println("内存减少: " + String.format("%.2f", memoryReduction) + "%");
        }
    }

    /**
     * 获取当前使用的内存
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 生成指定长度的测试序列
     */
    private char[] generateLongSequence(int length) {
        Random random = new Random(42); // 固定种子以便可重复
        char[] sequence = new char[length];
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int i = 0; i < length; i++) {
            sequence[i] = chars.charAt(random.nextInt(chars.length()));
        }

        return sequence;
    }

    /**
     * 单次提取性能测试：对比单次特征提取的开销
     */
    @Test
    public void testSingleExtractionPerformance() {
        char[] sequence = "wo shi zhong guo ren".toCharArray();
        int iterations = 100000;

        FeatureExtractor<char[]> templateImpl = FeatureExtractor.buildByTemplate(TEMPLATE_RULES);
        FeatureExtractor<char[]> manualImpl = new PinyinSplitFeatureExtractor();

        FastStringBuilder buffer = new FastStringBuilder(100);
        FeatureConsumer consumer = feature -> {
        };

        // 预热
        warmup(templateImpl, sequence);
        warmup(manualImpl, sequence);

        // 测试模板实现
        long templateTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int pos = i % sequence.length;
            templateImpl.compute(sequence, pos, buffer, consumer);
        }
        templateTime = System.nanoTime() - templateTime;
        double templateAvgNs = (double) templateTime / iterations;

        // 测试手动实现
        long manualTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int pos = i % sequence.length;
            manualImpl.compute(sequence, pos, buffer, consumer);
        }
        manualTime = System.nanoTime() - manualTime;
        double manualAvgNs = (double) manualTime / iterations;

        System.out.println("\n========== 单次提取性能对比 ==========");
        System.out.println("迭代次数: " + iterations);
        System.out.println("\n模板实现平均耗时: " + String.format("%.2f", templateAvgNs) + " ns");
        System.out.println("手动实现平均耗时: " + String.format("%.2f", manualAvgNs) + " ns");
        System.out.println("加速比: " + String.format("%.2f", templateAvgNs / manualAvgNs) + "x");
    }
}

