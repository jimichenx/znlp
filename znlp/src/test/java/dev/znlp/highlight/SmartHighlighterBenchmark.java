package dev.znlp.highlight;

import org.junit.jupiter.api.Tag;
import dev.znlp.ZnlpSystem;
import dev.znlp.test.TestTags;

import java.util.ArrayList;
import java.util.List;

/**
 * SmartHighlighter 性能基准测试
 *
 * 手动执行方式：
 * ./gradlew :znlp:test --tests highlight.dev.znlp.SmartHighlighterBenchmark
 */
@Tag(TestTags.BENCHMARK)
public class SmartHighlighterBenchmark {

    // 模拟 JUnit 测试方法以便可以通过 gradlew test 运行，
    // 但实际上这是一个 main 方法风格的性能测试
    @org.junit.jupiter.api.Test
    public void benchmark() {
        System.out.println("=================================");
        System.out.println("Starting SmartHighlighter Benchmark");
        System.out.println("=================================");

        // 初始化系统
        SmartHighlighter highlighter = ZnlpSystem.getInstance().getService(SmartHighlighter.class);
        SmartHighlightConfig config = new SmartHighlightConfig();

        // 准备数据
        // 模拟一个典型的搜索结果片段（约50-100字）
        String baseText = "这是一个关于理财产品的介绍，我们提供最好的理财服务。理财不仅仅是赚钱，更是对未来的规划。现在购买理财产品，可以享受超高收益。注意：投资有风险，理财需谨慎。";
        
        // 构造一个包含 1000 条数据的列表，模拟一次批量处理或高并发场景
        List<String> texts = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < batchSize; i++) {
            texts.add(baseText + " [ID:" + i + "]");
        }

        System.out.println("Test Data Ready: " + batchSize + " docs, avg length: " + baseText.length());

        // 预热 (Warmup)
        // 让 JIT 充分编译热点代码
        System.out.println("Warming up...");
        for (int i = 0; i < 50; i++) {
            for (String t : texts) {
                highlighter.highlight(t, "理财", config);
                highlighter.highlight(t, "licai", config);
            }
        }
        System.out.println("Warmup done.");

        // 测试配置
        int loops = 100; // 总共处理 loops * batchSize 个文档
        long totalDocs = (long) loops * batchSize;

        // ==========================================
        // 场景 1: 中文精确匹配 (Text Mode)
        // ==========================================
        long start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            for (String t : texts) {
                highlighter.highlight(t, "理财", config);
            }
        }
        long end = System.nanoTime();
        printResult("中文匹配 (Query='理财')", totalDocs, end - start);

        // ==========================================
        // 场景 2: 拼音匹配 (Pinyin Mode)
        // ==========================================
        start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            for (String t : texts) {
                highlighter.highlight(t, "licai", config);
            }
        }
        end = System.nanoTime();
        printResult("拼音匹配 (Query='licai')", totalDocs, end - start);

        // ==========================================
        // 场景 3: 模糊拼音匹配 (Fuzzy Pinyin)
        // 模拟 "理A财" 这种非连续情况
        // ==========================================
        List<String> fuzzyTexts = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            fuzzyTexts.add("这是一个理A财产品的介绍，即使中间有干扰字符也能匹配。");
        }
        
        start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            for (String t : fuzzyTexts) {
                highlighter.highlight(t, "licai", config);
            }
        }
        end = System.nanoTime();
        printResult("模糊拼音 (Text='理A财', Query='licai')", totalDocs, end - start);
        
        // ==========================================
        // 场景 4: 混合长文本匹配
        // ==========================================
         start = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            for (String t : texts) {
                highlighter.highlight(t, "理财产品", config);
            }
        }
        end = System.nanoTime();
        printResult("中文长词 (Query='理财产品')", totalDocs, end - start);

        System.out.println("=================================");
        System.out.println("Benchmark Finished");
        System.out.println("=================================");
    }

    private void printResult(String label, long count, long nanoTime) {
        double ms = nanoTime / 1_000_000.0;
        double qps = (count * 1_000_000_000.0) / nanoTime;
        System.out.printf("Scenario: %-35s | Count: %d | Time: %8.2f ms | QPS: %10.2f%n", label, count, ms, qps);
    }
}
