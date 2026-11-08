package dev.znlp.examples.demo;

import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;

/**
 * 演示 {@link Wordnet} 的 toString / toMoreString 输出，便于直观看到词图结构。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run -PmainClass=demo.examples.dev.znlp.WordnetStringDemo}
 */
public class WordnetStringDemo {

    public static void main(String[] args) {
        System.out.println("========== WordnetToStringBuilder 输出效果演示 ==========\n");

        // 示例1: 基本词图
        System.out.println("【示例1】基本词图 - 简单分词");
        System.out.println("文本: 测试文本");
        char[] text1 = "测试文本".toCharArray();
        Wordnet wordnet1 = new Wordnet(text1);
        wordnet1.put(0, 2); // "测试"
        wordnet1.put(2, 2); // "文本"
        System.out.println(wordnet1);
        System.out.println();

        // 示例2: 包含多个候选词
        System.out.println("【示例2】包含多个候选词 - 歧义分词");
        System.out.println("文本: 品质和服");
        char[] text2 = "品质和服".toCharArray();
        Wordnet wordnet2 = new Wordnet(text2);
        wordnet2.put(0, 2); // "品质"
        wordnet2.put(0, 1); // "品"
        wordnet2.put(1, 1); // "质"
        wordnet2.put(2, 1); // "和"
        wordnet2.put(2, 2); // "和服"
        wordnet2.put(3, 1); // "服"
        System.out.println(wordnet2);
        System.out.println();

        // 示例3: 包含空行（未被覆盖）
        System.out.println("【示例3】包含空行 - 未覆盖位置");
        System.out.println("文本: 测试");
        char[] text3 = "测试".toCharArray();
        Wordnet wordnet3 = new Wordnet(text3);
        wordnet3.put(0, 2); // "测试" - 位置1会被跳过
        System.out.println(wordnet3);
        System.out.println();

        // 示例4: 详细模式（显示词性和权重）
        System.out.println("【示例4】详细模式 - 显示词性和权重");
        System.out.println("文本: 测试文本");
        char[] text4 = "测试文本".toCharArray();
        Wordnet wordnet4 = new Wordnet(text4);
        Vertex v1 = wordnet4.put(0, 2);
        v1.weight = 10.5;
        Vertex v2 = wordnet4.put(2, 2);
        v2.weight = 8.0;
        System.out.println(wordnet4.toMoreString());
        System.out.println();

        // 示例5: 空词图
        System.out.println("【示例5】空词图 - 只有BEGIN和END");
        System.out.println("文本: 测试");
        char[] text5 = "测试".toCharArray();
        Wordnet wordnet5 = new Wordnet(text5);
        System.out.println(wordnet5);
        System.out.println();

        System.out.println("========== 演示结束 ==========");
    }
}
