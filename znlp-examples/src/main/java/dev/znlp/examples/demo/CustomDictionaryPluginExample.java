package dev.znlp.examples.demo;

import dev.znlp.segment.impl.userdict.MemUserDictionary;
import dev.znlp.segment.impl.userdict.UserDictionaryPluginBuilder;

import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;

/**
 * CustomDictionaryPlugin usage examples
 * Demonstrates basic usage of custom dictionary plugin
 *
 * Run this example as a JUnit test or simply execute the main method in IDE
 * Note: Running as standalone java application requires proper classpath setup
 *
 * @author jimichan
 */
public class CustomDictionaryPluginExample {

    public static void main(String[] args) {
        System.out.println("=== CustomDictionaryPlugin Usage Examples ===\n");

        // Example 1: Basic usage - add custom words
        example1_BasicUsage();

        // Example 2: Dynamic add words
        example2_DynamicAddWords();

        // Example 3: Multiple dictionaries
        example3_MultipleDictionaries();

        // Example 4: Using builder pattern
        example4_UsingBuilder();

        // Example 5: Dynamic remove words
        example5_DynamicRemoveWords();

        // Example 6: Compare with and without plugin
        example6_CompareWithAndWithoutPlugin();
    }

    /**
     * Example 1: Basic usage - add custom words
     */
    private static void example1_BasicUsage() {
        System.out.println("--- Example 1: Basic Usage ---");

        // Create a memory-based custom dictionary
        MemUserDictionary customDict = new MemUserDictionary();
        customDict.addWord("人工智能");
        customDict.addWord("机器学习");
        customDict.addWord("深度学习");

        WordSegmenter segmenter = createSegmenter(customDict);

        String text = "人工智能和机器学习是深度学习的基础";
        List<Token> tokens = segmenter.segment(text);

        System.out.println("Input: " + text);
        System.out.println("Result: " + formatTokens(tokens));
        System.out.println();
    }

    /**
     * Example 2: Dynamic add words during runtime
     */
    private static void example2_DynamicAddWords() {
        System.out.println("--- Example 2: Dynamic Add Words ---");

        MemUserDictionary customDict = new MemUserDictionary();
        WordSegmenter segmenter = createSegmenter(customDict);

        String text = "自然语言处理技术很强大";

        // Segment before adding word
        System.out.println("Before adding custom word:");
        System.out.println("  Input: " + text);
        System.out.println("  Result: " + formatTokens(segmenter.segment(text)));

        // Dynamically add custom word
        customDict.addWord("自然语言处理");

        // Segment after adding word
        System.out.println("After adding '自然语言处理':");
        System.out.println("  Input: " + text);
        System.out.println("  Result: " + formatTokens(segmenter.segment(text)));
        System.out.println();
    }

    /**
     * Example 3: Using multiple dictionaries
     */
    private static void example3_MultipleDictionaries() {
        System.out.println("--- Example 3: Multiple Dictionaries ---");

        // Dictionary 1: AI terms
        MemUserDictionary aiDict = new MemUserDictionary();
        aiDict.addWord("神经网络");
        aiDict.addWord("卷积神经网络");

        // Dictionary 2: Company names
        MemUserDictionary companyDict = new MemUserDictionary();
        companyDict.addWord("谷歌公司");
        companyDict.addWord("微软公司");

        // Build segmenter with multiple dictionaries
        WordSegmenter segmenter = createSegmenter(aiDict, companyDict);

        String text = "谷歌公司研发了卷积神经网络";
        List<Token> tokens = segmenter.segment(text);

        System.out.println("Input: " + text);
        System.out.println("Result: " + formatTokens(tokens));
        System.out.println();
    }

    /**
     * Example 4: Using builder pattern
     */
    private static void example4_UsingBuilder() {
        System.out.println("--- Example 4: Using Builder Pattern ---");

        // Create plugin using builder
        UserDictionaryPluginBuilder builder = UserDictionaryPluginBuilder.create();
        MemUserDictionary memDict = builder.addMemDict();
        
        memDict.addWord("区块链技术");
        memDict.addWord("分布式系统");
        memDict.addWord("智能合约");

        WordSegmenter segmenter = SegmentRecipe.dictionary()
                .withUserDictionary(builder.dictionaries())
                .build();

        String text = "区块链技术基于分布式系统和智能合约";
        List<Token> tokens = segmenter.segment(text);

        System.out.println("Input: " + text);
        System.out.println("Result: " + formatTokens(tokens));
        System.out.println();
    }

    /**
     * Example 5: Dynamic remove words
     */
    private static void example5_DynamicRemoveWords() {
        System.out.println("--- Example 5: Dynamic Remove Words ---");

        MemUserDictionary customDict = new MemUserDictionary();
        customDict.addWord("大数据");
        customDict.addWord("云计算");

        WordSegmenter segmenter = createSegmenter(customDict);

        String text = "大数据和云计算技术";

        // Before removing
        System.out.println("Before removing:");
        System.out.println("  Input: " + text);
        System.out.println("  Result: " + formatTokens(segmenter.segment(text)));

        // Remove a word
        customDict.removeWord("大数据");

        // After removing
        System.out.println("After removing '大数据':");
        System.out.println("  Input: " + text);
        System.out.println("  Result: " + formatTokens(segmenter.segment(text)));
        System.out.println();
    }

    /**
     * Example 6: Compare segmentation with and without plugin
     */
    private static void example6_CompareWithAndWithoutPlugin() {
        System.out.println("--- Example 6: Compare With and Without Plugin ---");

        String text = "奥利奥饼干很好吃";

        // Without plugin
        WordSegmenter segmenterWithout = createSegmenter();

        System.out.println("Without custom dictionary:");
        System.out.println("  Input: " + text);
        System.out.println("  Result: " + formatTokens(segmenterWithout.segment(text)));

        // With plugin
        MemUserDictionary customDict = new MemUserDictionary();
        customDict.addWord("奥利奥饼干");

        WordSegmenter segmenterWith = createSegmenter(customDict);

        System.out.println("With custom dictionary:");
        System.out.println("  Input: " + text);
        System.out.println("  Result: " + formatTokens(segmenterWith.segment(text)));
        System.out.println();
    }

    /**
     * Format tokens to string
     */
    private static String formatTokens(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            sb.append(tokens.get(i).word);
        }
        return sb.toString();
    }

    /**
     * Helper method to create segmenter with optional custom dictionaries
     */
    private static WordSegmenter createSegmenter(MemUserDictionary... dicts) {
        SegmentRecipe recipe = SegmentRecipe.dictionary();
        if (dicts != null && dicts.length > 0) {
            recipe = recipe.withUserDictionary(dicts);
        }
        return recipe.build();
    }
}
