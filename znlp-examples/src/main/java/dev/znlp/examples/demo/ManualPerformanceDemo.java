package dev.znlp.examples.demo;

import dev.znlp.newwords.counter.FrequentItemsCounter;
import dev.znlp.newwords.entity.WordCount;

import java.util.List;
import java.util.Random;

/**
 * 手动运行的性能测试，用于查看详细的性能数据
 *
 * @author jimichan
 */
public class ManualPerformanceDemo {

    private static String formatDuration(long nanos) {
        if (nanos < 1_000_000) {
            return String.format("%.2f μs", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }

    private static void printReport(String testName, long duration, int operations, int finalSize) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("性能测试: " + testName);
        System.out.println("=".repeat(70));
        System.out.println("总操作数: " + String.format("%,d", operations));
        System.out.println("总耗时: " + formatDuration(duration));
        System.out.println("平均每次操作: " + formatDuration(duration / operations));
        System.out.println("吞吐量: " + String.format("%,.0f ops/s", operations / (duration / 1_000_000_000.0)));
        System.out.println("最终数据量: " + String.format("%,d", finalSize));
        System.out.println("=".repeat(70));
    }

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("FrequentItemsCounter 性能测试报告");
        System.out.println("=".repeat(70));

        // 测试1: 小规模数据
        testSmallScale();

        // 测试2: 中规模数据
        testMediumScale();

        // 测试3: 大规模数据
        testLargeScale();

        // 测试4: 压缩性能
        testCompressionPerformance();

        // 测试5: 内存效率
        testMemoryEfficiency();

        // 测试6: 结果获取性能
        testResultRetrieval();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("所有性能测试完成！");
        System.out.println("=".repeat(70) + "\n");
    }

    private static void testSmallScale() {
        int operations = 10_000;
        int capacity = 1000;

        FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 2);
        Random random = new Random(42);

        long startTime = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            counter.put("word" + random.nextInt(500));
        }

        long duration = System.nanoTime() - startTime;

        printReport("小规模数据 (1万次操作)", duration, operations, counter.getData().size());
    }

    private static void testMediumScale() {
        int operations = 100_000;
        int capacity = 10_000;

        FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 5);
        Random random = new Random(42);

        long startTime = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            counter.put("word" + random.nextInt(5000));
        }

        long duration = System.nanoTime() - startTime;

        printReport("中规模数据 (10万次操作)", duration, operations, counter.getData().size());
    }

    private static void testLargeScale() {
        int operations = 1_000_000;
        int capacity = 50_000;

        FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 10);
        Random random = new Random(42);

        long startTime = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            counter.put("word" + random.nextInt(20000));
        }

        long duration = System.nanoTime() - startTime;

        printReport("大规模数据 (100万次操作)", duration, operations, counter.getData().size());
    }

    private static void testCompressionPerformance() {
        int capacity = 1000;
        int operations = 50_000;
        int vocabularySize = 2000;

        FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 3);
        Random random = new Random(42);

        long startTime = System.nanoTime();
        int maxSize = 0;

        for (int i = 0; i < operations; i++) {
            counter.put("word" + random.nextInt(vocabularySize));
            maxSize = Math.max(maxSize, counter.getData().size());
        }

        long duration = System.nanoTime() - startTime;

        System.out.println("\n" + "=".repeat(70));
        System.out.println("性能测试: 压缩性能 (频繁触发压缩)");
        System.out.println("=".repeat(70));
        System.out.println("总操作数: " + String.format("%,d", operations));
        System.out.println("词汇表大小: " + vocabularySize);
        System.out.println("容量上限: " + capacity);
        System.out.println("观察到的最大容量: " + maxSize);
        System.out.println("最终数据量: " + String.format("%,d", counter.getData().size()));
        System.out.println("总耗时: " + formatDuration(duration));
        System.out.println("平均每次操作: " + formatDuration(duration / operations));
        System.out.println("吞吐量: " + String.format("%,.0f ops/s", operations / (duration / 1_000_000_000.0)));
        System.out.println("=".repeat(70));
    }

    private static void testMemoryEfficiency() {
        int capacity = 5000;
        int operations = 200_000;

        FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 5);
        Random random = new Random(42);

        long startTime = System.nanoTime();
        int maxSize = 0;

        for (int i = 0; i < operations; i++) {
            counter.put("word" + random.nextInt(100000));
            maxSize = Math.max(maxSize, counter.getData().size());
        }

        long duration = System.nanoTime() - startTime;
        int finalSize = counter.getData().size();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("性能测试: 内存效率");
        System.out.println("=".repeat(70));
        System.out.println("容量上限: " + String.format("%,d", capacity));
        System.out.println("总操作数: " + String.format("%,d", operations));
        System.out.println("观察到的最大容量: " + String.format("%,d", maxSize));
        System.out.println("最终数据量: " + String.format("%,d", finalSize));
        System.out.println("容量利用率: " + String.format("%.1f%%", (finalSize * 100.0 / capacity)));
        System.out.println("总耗时: " + formatDuration(duration));
        System.out.println("平均每次操作: " + formatDuration(duration / operations));
        System.out.println("=".repeat(70));
    }

    private static void testResultRetrieval() {
        int capacity = 10_000;
        FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 5);
        Random random = new Random(42);

        // 填充数据
        for (int i = 0; i < 100_000; i++) {
            counter.put("word" + random.nextInt(5000));
        }

        // 测试 getListResult 性能
        long startTime = System.nanoTime();
        List<WordCount> result = counter.getListResult();
        long duration = System.nanoTime() - startTime;

        System.out.println("\n" + "=".repeat(70));
        System.out.println("性能测试: 结果获取");
        System.out.println("=".repeat(70));
        System.out.println("数据量: " + String.format("%,d", counter.getData().size()));
        System.out.println("结果数量: " + String.format("%,d", result.size()));
        System.out.println("获取耗时: " + formatDuration(duration));
        System.out.println("\nTop 10 高频词:");
        for (int i = 0; i < Math.min(10, result.size()); i++) {
            WordCount wc = result.get(i);
            System.out.printf("  %2d. %-15s: %,6d%n", i + 1, wc.word(), wc.count());
        }
        System.out.println("=".repeat(70));
    }
}
