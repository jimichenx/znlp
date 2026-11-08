package dev.znlp.examples;

import dev.znlp.Znlp;
import dev.znlp.segment.impl.cws.CwsPerceptron;

import java.util.List;

/**
 * 独立运行 CWS 感知机分词，不走流水线。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run}
 */
public class CwsPerceptronExample {

    public static void main(String[] args) {
        System.out.println("Loading CwsPerceptron ...");
        long t0 = System.currentTimeMillis();
        CwsPerceptron cws = Znlp.getService(CwsPerceptron.class);
        System.out.println("Loaded in " + (System.currentTimeMillis() - t0) + " ms");
        System.out.println();

        String[] sentences = args.length > 0 ? args : new String[]{
                "自然语言处理技术是人工智能的重要领域",
                "南京市长江大桥",
                "这是张三的快递",
                "乒乓球拍卖完了",
                "上海浦东开发与建设同步推进",
                "他从马上下来",
                "美国会通过这项法案",
                "TRAE 正在积极准备于更多国家/地区开通付费相关的服务，敬请期待。"
        };

        for (String sentence : sentences) {
            long start = System.nanoTime();
            List<String> words = cws.split(sentence);
            long costMs = (System.nanoTime() - start) / 1_000_000;
            System.out.println(sentence);
            System.out.println("  -> " + String.join(" / ", words) + "  (" + costMs + " ms)");
            System.out.println();
        }
    }
}
