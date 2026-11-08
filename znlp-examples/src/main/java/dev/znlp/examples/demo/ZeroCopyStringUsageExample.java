package dev.znlp.examples.demo;

import dev.znlp.base.common.ZeroCopyString;

/**
 * ZeroCopyString使用示例
 * <p>
 * 展示如何在分词场景中使用ZeroCopyString来避免字符串复制，提高性能
 */
public class ZeroCopyStringUsageExample {

    /**
     * 示例1: 基本的分词场景
     * 将输入文本转换为ZeroCopyString，然后进行分词处理
     */
    public static void basicSegmentationExample() {
        String input = "我爱自然语言处理技术";

        // 将String转换为ZeroCopyString
        ZeroCopyString text = new ZeroCopyString(input);

        // 模拟分词结果的位置信息 [start, end)
        int[][] segments = {
                {0, 1},  // 我
                {1, 2},  // 爱
                {2, 4},  // 自然
                {4, 6},  // 语言
                {6, 8},  // 处理
                {8, 10}  // 技术
        };

        // 使用zero-copy方式提取每个词
        for (int[] seg : segments) {
            ZeroCopyString word = text.subSequence(seg[0], seg[1]);
            System.out.println(word);
            // 注意：所有的word都共享同一个char数组，没有复制开销
        }
    }

    /**
     * 示例2: 字符规范化场景
     * 在分词前对文本进行规范化处理（如大小写转换）
     */
    public static void characterNormalizationExample() {
        String input = "Hello World 123";

        // 创建ZeroCopyString
        ZeroCopyString text = new ZeroCopyString(input);

        // 直接修改字符（规范化为小写）
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                text.setCharAt(i, (char) (c + 32));
            }
        }

        System.out.println("Normalized: " + text);
        // 输出: "hello world 123"
    }

    /**
     * 示例3: 避免String.substring的复制开销
     * 在需要频繁切分文本的场景中性能优势明显
     */
    public static void performanceComparisonExample() {
        String longText = "这是一段很长的文本".repeat(1000);

        // 方式1: 使用ZeroCopyString (zero-copy)
        long start = System.nanoTime();
        ZeroCopyString zcs = new ZeroCopyString(longText);
        for (int i = 0; i < longText.length() - 10; i++) {
            ZeroCopyString sub = zcs.subSequence(i, i + 10);
            // 每次subSequence只创建一个新的ZeroCopyString对象
            // 共享底层char数组，无复制开销
        }
        long zeroCopyTime = System.nanoTime() - start;

        // 方式2: 使用String.substring (会复制char数组)
        start = System.nanoTime();
        for (int i = 0; i < longText.length() - 10; i++) {
            String sub = longText.substring(i, i + 10);
            // 每次substring会复制一个新的char数组
        }
        long stringTime = System.nanoTime() - start;

        System.out.println("ZeroCopyString: " + zeroCopyTime / 1_000_000 + "ms");
        System.out.println("String: " + stringTime / 1_000_000 + "ms");
        System.out.println("Speed up: " + (double) stringTime / zeroCopyTime + "x");
    }

    /**
     * 示例4: 与char[]的互操作
     * 当底层算法需要char[]时，可以直接获取
     */
    public static void charArrayInteropExample() {
        String input = "自然语言处理";
        ZeroCopyString text = new ZeroCopyString(input);

        // 获取底层char数组（无复制）
        char[] chars = text.getChars();
        int offset = text.getOffset();
        int length = text.length();

        // 直接对char数组进行操作
        System.out.println("Chars from offset " + offset + ", length " + length);
        for (int i = 0; i < length; i++) {
            System.out.print(chars[offset + i]);
        }
        System.out.println();
    }

    /**
     * 示例5: 分词过程中的完整流程
     * 展示如何在实际分词器中使用ZeroCopyString
     */
    public static void fullSegmentationWorkflow() {
        String input = "我在学习自然语言处理";

        // Step 1: 转换输入为ZeroCopyString
        ZeroCopyString text = new ZeroCopyString(input);

        // Step 2: 字符规范化（如果需要）
        // normalizeCharacters(text);

        // Step 3: 分词算法处理（这里简化为固定切分）
        // 实际应用中，这里会调用具体的分词算法
        int[][] tokenPositions = {
                {0, 1},   // 我
                {1, 2},   // 在
                {2, 4},   // 学习
                {4, 8}    // 自然语言处理
        };

        // Step 4: 生成分词结果（zero-copy）
        System.out.println("分词结果:");
        for (int[] pos : tokenPositions) {
            ZeroCopyString token = text.subSequence(pos[0], pos[1]);
            System.out.println("  " + token + " [" + pos[0] + "," + pos[1] + ")");

            // 所有token都共享同一个char数组
            assert token.getChars() == text.getChars();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 示例1: 基本分词场景 ===");
        basicSegmentationExample();

        System.out.println("\n=== 示例2: 字符规范化 ===");
        characterNormalizationExample();

        System.out.println("\n=== 示例3: 性能对比 ===");
        performanceComparisonExample();

        System.out.println("\n=== 示例4: char[]互操作 ===");
        charArrayInteropExample();

        System.out.println("\n=== 示例5: 完整分词流程 ===");
        fullSegmentationWorkflow();
    }
}
