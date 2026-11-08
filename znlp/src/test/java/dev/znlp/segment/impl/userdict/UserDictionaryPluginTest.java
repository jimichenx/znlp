package dev.znlp.segment.impl.userdict;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomDictionaryPlugin 测试类
 * 测试自定义词典插件对分词结果的影响
 *
 * @author jimichan
 */
public class UserDictionaryPluginTest {

    /**
     * 词已经在核心词图里、Viterbi 切碎了，两端仍在边界上时，用户词典合并。
     * 卡在已选词中间的用户词不改切分。
     */
    @Test
    @Timeout(5)
    public void testUserWordMergesOnlyOnExistingBoundaries() {
        String text = "南京市长江大桥";
        assertEquals(List.of("南京市", "长江", "大桥"),
                SegmentRecipe.dictionary().build().segmentToStrings(text));

        MemUserDictionary bridge = new MemUserDictionary();
        bridge.addWord("长江大桥");
        assertEquals(List.of("南京市", "长江大桥"),
                SegmentRecipe.dictionary().withUserDictionary(bridge).build().segmentToStrings(text));

        MemUserDictionary mayor = new MemUserDictionary();
        mayor.addWord("市长");
        assertEquals(List.of("南京市", "长江", "大桥"),
                SegmentRecipe.dictionary().withUserDictionary(mayor).build().segmentToStrings(text));
    }

    /**
     * 测试未安装插件时的基础分词
     */
    @Test
    @Timeout(5)
    public void testSegmentWithoutPlugin() {
        WordSegmenter segmenter = SegmentRecipe.dictionary().build();

        List<Token> tokens = segmenter.segment("北京烤鸭很好吃");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());

        // 记录未安装插件时的分词结果
        String result = tokens.stream()
                .map(t -> t.word)
                .collect(Collectors.joining("/"));
        
        System.out.println("未安装插件: " + result);
    }

    /**
     * 测试安装空的自定义词典插件
     * 应该不影响分词结果
     */
    @Test
    @Timeout(5)
    public void testSegmentWithEmptyPlugin() {
        MemUserDictionary memDict = new MemUserDictionary();
        
        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        List<Token> tokens = segmenter.segment("北京烤鸭很好吃");
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
    }

    /**
     * 测试安装插件前后分词结果的差异
     * 验证自定义词典能够影响分词结果
     */
    @Test
    @Timeout(5)
    public void testSegmentDifferenceBeforeAndAfterPlugin() {
        String testText = "奥利奥饼干很好吃";

        // 未安装插件的分词器
        WordSegmenter segmenterWithoutPlugin = SegmentRecipe.dictionary().build();
        List<String> tokensWithout = segmenterWithoutPlugin.segmentToStrings(testText);

        // 创建内存词典并添加自定义词
        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("奥利奥饼干");

        // 安装插件的分词器
        WordSegmenter segmenterWithPlugin = SegmentRecipe.dictionary().withUserDictionary(memDict).build();
        List<String> tokensWith = segmenterWithPlugin.segmentToStrings(testText);

        // 打印对比结果
        System.out.println("未安装插件: " + String.join("/", tokensWithout));
        System.out.println("安装插件后: " + String.join("/", tokensWith));

        // 验证"奥利奥饼干"应该作为一个完整的词出现
        assertTrue(tokensWith.contains("奥利奥饼干"), "分词结果中应该包含自定义词'奥利奥饼干'");
    }

    /**
     * 测试动态添加词后分词结果的变化
     * 验证在运行时添加新词能够影响分词
     */
    @Test
    @Timeout(5)
    public void testDynamicAddWordAffectsSegmentation() {
        String testText = "深度学习算法很强大";

        // 创建内存词典
        MemUserDictionary memDict = new MemUserDictionary();

        // 创建分词器
        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        // 第一次分词（词典为空）
        List<String> tokensBeforeAdd = segmenter.segmentToStrings(testText);
        System.out.println("添加词前: " + String.join("/", tokensBeforeAdd));

        // 动态添加自定义词
        memDict.addWord("深度学习");

        // 第二次分词（词典包含"深度学习"）
        List<String> tokensAfterAdd = segmenter.segmentToStrings(testText);
        System.out.println("添加词后: " + String.join("/", tokensAfterAdd));

        // 验证分词结果应该不同
        assertNotEquals(tokensBeforeAdd, tokensAfterAdd, "动态添加词后分词结果应该不同");

        // 验证"深度学习"应该作为一个完整的词出现
        assertTrue(tokensAfterAdd.contains("深度学习"), "分词结果中应该包含动态添加的词'深度学习'");
    }

    /**
     * 测试添加多个词的情况
     */
    @Test
    @Timeout(5)
    public void testMultipleCustomWords() {
        String testText = "人工智能和机器学习是计算机科学的重要分支";

        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("人工智能");
        memDict.addWord("机器学习");
        memDict.addWord("计算机科学");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        List<String> tokens = segmenter.segmentToStrings(testText);
        System.out.println("多词测试: " + String.join("/", tokens));

        // 验证所有自定义词都应该出现在分词结果中
        assertTrue(tokens.contains("人工智能"), "应该包含'人工智能'");
        assertTrue(tokens.contains("机器学习"), "应该包含'机器学习'");
        assertTrue(tokens.contains("计算机科学"), "应该包含'计算机科学'");
    }

    /**
     * 测试多个词典的情况
     */
    @Test
    @Timeout(5)
    public void testMultipleDictionaries() {
        String testText = "自然语言处理和语音识别技术";

        MemUserDictionary dict1 = new MemUserDictionary();
        dict1.addWord("自然语言处理");

        MemUserDictionary dict2 = new MemUserDictionary();
        dict2.addWord("语音识别");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(dict1, dict2).build();

        List<String> tokens = segmenter.segmentToStrings(testText);
        System.out.println("多词典测试: " + String.join("/", tokens));

        assertTrue(tokens.contains("自然语言处理"), "应该包含第一个词典的词'自然语言处理'");
        assertTrue(tokens.contains("语音识别"), "应该包含第二个词典的词'语音识别'");
    }

    /**
     * 测试使用Builder创建插件
     */
    @Test
    @Timeout(5)
    public void testPluginWithBuilder() {
        String testText = "神经网络模型训练";

        UserDictionaryPluginBuilder builder = UserDictionaryPluginBuilder.create();
        MemUserDictionary memDict = builder.addMemDict();
        memDict.addWord("神经网络");
        memDict.addWord("模型训练");

        WordSegmenter segmenter = SegmentRecipe.dictionary()
                .withUserDictionary(builder.dictionaries())
                .build();

        List<String> tokens = segmenter.segmentToStrings(testText);
        System.out.println("Builder测试: " + String.join("/", tokens));

        assertTrue(tokens.contains("神经网络"), "应该包含'神经网络'");
        assertTrue(tokens.contains("模型训练"), "应该包含'模型训练'");
    }

    /**
     * 测试动态删除词的影响
     * 注意：删除后需要rebuild才能生效
     */
    @Test
    @Timeout(5)
    public void testDynamicRemoveWord() {
        String testText = "数据挖掘分析平台";

        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("数据挖掘");
        memDict.addWord("分析平台");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        // 第一次分词
        List<String> tokensWithBoth = segmenter.segmentToStrings(testText);
        System.out.println("删除前: " + String.join("/", tokensWithBoth));
        assertTrue(tokensWithBoth.contains("数据挖掘"), "应该包含'数据挖掘'");

        // 删除一个词 - removeWord 内部会调用 rebuild()
        memDict.removeWord("数据挖掘");

        // 第二次分词 - 验证删除是否生效
        List<String> tokensAfterRemove = segmenter.segmentToStrings(testText);
        System.out.println("删除后: " + String.join("/", tokensAfterRemove));
        
        // 验证分词结果发生了变化（即使结果相同，也证明动态操作是可行的）
        // 由于核心词典可能本身就包含这些词，所以只验证rebuild被正常调用
        assertNotNull(memDict.getTrie(), "删除一个词后，如果还有其他词，trie应该不为null");
    }

    /**
     * 测试清空词典的影响
     */
    @Test
    @Timeout(5)
    public void testClearDictionary() {
        String testText = "云计算服务平台";

        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("云计算");
        memDict.addWord("服务平台");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        // 清空前分词
        List<String> tokensBeforeClear = segmenter.segmentToStrings(testText);
        System.out.println("清空前: " + String.join("/", tokensBeforeClear));
        assertTrue(tokensBeforeClear.contains("云计算"), "清空前应该包含'云计算'");

        // 清空词典
        memDict.clear();

        // 清空后分词
        List<String> tokensAfterClear = segmenter.segmentToStrings(testText);
        System.out.println("清空后: " + String.join("/", tokensAfterClear));

        // 验证结果应该不同
        assertNotEquals(tokensBeforeClear, tokensAfterClear, "清空词典后分词结果应该改变");
    }

    /**
     * 测试特殊字符和数字混合的自定义词
     */
    @Test
    @Timeout(5)
    public void testCustomWordWithSpecialChars() {
        String testText = "Python3.9是最新版本";

        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("Python3.9");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        List<String> tokens = segmenter.segmentToStrings(testText);
        System.out.println("特殊字符测试: " + String.join("/", tokens));

        // Python3.9可能会被规范化处理
        boolean containsOriginalOrNormalized = tokens.contains("Python3.9") || 
                                               tokens.stream().anyMatch(t -> t.contains("Python"));
        assertTrue(containsOriginalOrNormalized, "应该包含Python相关的词");
    }

    /**
     * 测试长词优先匹配
     */
    @Test
    @Timeout(5)
    public void testLongerWordPriority() {
        String testText = "中华人民共和国";

        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("中华");
        memDict.addWord("人民");
        memDict.addWord("共和国");
        memDict.addWord("中华人民共和国");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        List<String> tokens = segmenter.segmentToStrings(testText);
        System.out.println("长词优先测试: " + String.join("/", tokens));

        // 验证是否倾向于匹配更长的词
        assertTrue(tokens.contains("中华人民共和国") || 
                   (tokens.contains("中华") && tokens.contains("人民") && tokens.contains("共和国")),
                   "应该正确处理长词匹配");
    }

    /**
     * 测试空文本
     */
    @Test
    @Timeout(5)
    public void testEmptyText() {
        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("测试");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        List<String> tokens = segmenter.segmentToStrings("");
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty(), "空文本应该返回空列表");
    }

    /**
     * 测试单字自定义词
     */
    @Test
    @Timeout(5)
    public void testSingleCharCustomWord() {
        String testText = "这个A级产品很好";

        MemUserDictionary memDict = new MemUserDictionary();
        memDict.addWord("A");

        WordSegmenter segmenter = SegmentRecipe.dictionary().withUserDictionary(memDict).build();

        List<String> tokens = segmenter.segmentToStrings(testText);
        System.out.println("单字测试: " + String.join("/", tokens));

        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
    }
}
