package dev.znlp.pinyin.spliter;

import org.junit.jupiter.api.Test;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.utils.FastStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PinyinSplitFeatureExtractor 的单元测试
 * <p>
 * 验证手动实现的特征提取器与模板实现产生相同的结果
 */
public class PinyinSplitFeatureExtractorTest {

    private static final String TEMPLATE_RULES = "%[0]2;" +
            "%[-1]1;" +
            "%[-1]/%[0]5;" +
            "%[-2]/%[-1]4;" +
            "%[1]3;" +
            "%[0]/%[1]6;" +
            "%[1]/%[2]7;";

    /**
     * 收集特征的工具方法
     */
    private List<String> extractFeatures(FeatureExtractor<char[]> extractor, char[] sequence, int position) {
        List<String> features = new ArrayList<>();
        FastStringBuilder buffer = new FastStringBuilder(100);
        FeatureConsumer consumer = features::add;
        extractor.compute(sequence, position, buffer, consumer);
        Collections.sort(features); // 排序以便比较
        return features;
    }

    /**
     * 对比模板实现和手动实现的结果
     */
    private void compareImplementations(char[] sequence, int position) {
        FeatureExtractor<char[]> templateImpl = FeatureExtractor.buildByTemplate(TEMPLATE_RULES);
        FeatureExtractor<char[]> manualImpl = new PinyinSplitFeatureExtractor();

        List<String> templateFeatures = extractFeatures(templateImpl, sequence, position);
        List<String> manualFeatures = extractFeatures(manualImpl, sequence, position);

        assertEquals(templateFeatures.size(), manualFeatures.size(),
                String.format("位置 %d 的特征数量应该相同", position));

        for (int i = 0; i < templateFeatures.size(); i++) {
            assertEquals(templateFeatures.get(i), manualFeatures.get(i),
                    String.format("位置 %d 的第 %d 个特征应该相同", position, i));
        }
    }

    // ========== 正确性验证：对比模板实现 ==========

    @Test
    public void testCorrectness_CompareWithTemplate_ShortSequence() {
        char[] seq = "abc".toCharArray();
        for (int i = 0; i < seq.length; i++) {
            compareImplementations(seq, i);
        }
    }

    @Test
    public void testCorrectness_CompareWithTemplate_MediumSequence() {
        char[] seq = "世界你好".toCharArray();
        for (int i = 0; i < seq.length; i++) {
            compareImplementations(seq, i);
        }
    }

    @Test
    public void testCorrectness_CompareWithTemplate_LongSequence() {
        char[] seq = "wo shi zhong guo ren".toCharArray();
        for (int i = 0; i < seq.length; i++) {
            compareImplementations(seq, i);
        }
    }

    @Test
    public void testCorrectness_CompareWithTemplate_PinyinSequence() {
        char[] seq = "nihao".toCharArray();
        for (int i = 0; i < seq.length; i++) {
            compareImplementations(seq, i);
        }
    }

    // ========== 特征规则验证 ==========

    @Test
    public void testFeatureRule1_CurrentChar() {
        // 特征1: %[0]2 - 当前位置字符 + '2'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 0);

        assertTrue(features.contains("世2"), "应该包含特征：世2");
    }

    @Test
    public void testFeatureRule2_PreviousChar() {
        // 特征2: %[-1]1 - 前1个字符 + '1'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 1);

        assertTrue(features.contains("世1"), "应该包含特征：世1");
    }

    @Test
    public void testFeatureRule3_PreviousAndCurrent() {
        // 特征3: %[-1]/%[0]5 - 前1个字符 + '/' + 当前字符 + '5'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 1);

        assertTrue(features.contains("世/界5"), "应该包含特征：世/界5");
    }

    @Test
    public void testFeatureRule4_PreviousTwoAndOne() {
        // 特征4: %[-2]/%[-1]4 - 前2个字符 + '/' + 前1个字符 + '4'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界你好".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 2);

        assertTrue(features.contains("世/界4"), "应该包含特征：世/界4");
    }

    @Test
    public void testFeatureRule5_NextChar() {
        // 特征5: %[1]3 - 后1个字符 + '3'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 0);

        assertTrue(features.contains("界3"), "应该包含特征：界3");
    }

    @Test
    public void testFeatureRule6_CurrentAndNext() {
        // 特征6: %[0]/%[1]6 - 当前字符 + '/' + 后1个字符 + '6'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 0);

        assertTrue(features.contains("世/界6"), "应该包含特征：世/界6");
    }

    @Test
    public void testFeatureRule7_NextTwo() {
        // 特征7: %[1]/%[2]7 - 后1个字符 + '/' + 后2个字符 + '7'
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界你好".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 0);

        assertTrue(features.contains("界/你7"), "应该包含特征：界/你7");
    }

    // ========== 边界条件测试 ==========

    @Test
    public void testBoundary_SingleChar() {
        // 单字符序列，只能生成特征1（%[0]2）
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "a".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 0);

        assertEquals(1, features.size(), "单字符应该只生成1个特征");
        assertTrue(features.contains("a2"), "应该包含特征：a2");
    }

    @Test
    public void testBoundary_FirstPosition() {
        // 第一个位置，不能生成需要前向索引的特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界你好".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 0);

        // 应该包含：%[0]2, %[1]3, %[0]/%[1]6, %[1]/%[2]7
        assertEquals(4, features.size(), "第一个位置应该生成4个特征");
        assertTrue(features.contains("世2"), "应该包含特征：世2");
        assertTrue(features.contains("界3"), "应该包含特征：界3");
        assertTrue(features.contains("世/界6"), "应该包含特征：世/界6");
        assertTrue(features.contains("界/你7"), "应该包含特征：界/你7");
    }

    @Test
    public void testBoundary_LastPosition() {
        // 最后一个位置，不能生成需要后向索引的特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界你好".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 3);

        // 应该包含：%[0]2, %[-1]1, %[-1]/%[0]5, %[-2]/%[-1]4
        assertEquals(4, features.size(), "最后一个位置应该生成4个特征");
        assertTrue(features.contains("好2"), "应该包含特征：好2");
        assertTrue(features.contains("你1"), "应该包含特征：你1");
        assertTrue(features.contains("你/好5"), "应该包含特征：你/好5");
        assertTrue(features.contains("界/你4"), "应该包含特征：界/你4");
    }

    @Test
    public void testBoundary_SecondPosition() {
        // 第二个位置，可以生成大部分特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界你好".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 1);

        // 应该包含：%[0]2, %[-1]1, %[-1]/%[0]5, %[1]3, %[0]/%[1]6, %[1]/%[2]7
        // 注意：%[-2]/%[-1]4 需要 position >= 2，所以这里不包含
        assertEquals(6, features.size(), "第二个位置应该生成6个特征");
        assertTrue(features.contains("界2"), "应该包含特征：界2");
        assertTrue(features.contains("世1"), "应该包含特征：世1");
        assertTrue(features.contains("世/界5"), "应该包含特征：世/界5");
        assertTrue(features.contains("你3"), "应该包含特征：你3");
        assertTrue(features.contains("界/你6"), "应该包含特征：界/你6");
        assertTrue(features.contains("你/好7"), "应该包含特征：你/好7");
    }

    @Test
    public void testBoundary_MiddlePosition() {
        // 中间位置，可以生成所有可能的特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界你好".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 2);

        // 应该包含所有7个特征（除了%[1]/%[2]7因为%[2]会越界）
        assertEquals(6, features.size(), "中间位置应该生成6个特征");
        assertTrue(features.contains("你2"), "应该包含特征：你2");
        assertTrue(features.contains("界1"), "应该包含特征：界1");
        assertTrue(features.contains("界/你5"), "应该包含特征：界/你5");
        assertTrue(features.contains("世/界4"), "应该包含特征：世/界4");
        assertTrue(features.contains("好3"), "应该包含特征：好3");
        assertTrue(features.contains("你/好6"), "应该包含特征：你/好6");
    }

    @Test
    public void testBoundary_InvalidPosition_Negative() {
        // 负数位置应该不生成任何特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, -1);

        assertEquals(0, features.size(), "负数位置应该不生成特征");
    }

    @Test
    public void testBoundary_InvalidPosition_OutOfRange() {
        // 超出范围的位置应该不生成任何特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "世界".toCharArray();
        List<String> features = extractFeatures(extractor, seq, 10);

        assertEquals(0, features.size(), "超出范围的位置应该不生成特征");
    }

    @Test
    public void testBoundary_EmptySequence() {
        // 空序列应该不生成任何特征
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = new char[0];
        List<String> features = extractFeatures(extractor, seq, 0);

        assertEquals(0, features.size(), "空序列应该不生成特征");
    }

    // ========== 完整特征集验证 ==========

    @Test
    public void testCompleteFeatureSet_AllRules() {
        // 验证所有7个特征规则都能正确生成
        PinyinSplitFeatureExtractor extractor = new PinyinSplitFeatureExtractor();
        char[] seq = "abcdefg".toCharArray();
        int position = 3; // 位置3（'d'），可以生成所有特征

        List<String> features = extractFeatures(extractor, seq, position);

        // 验证所有7个特征都存在
        assertTrue(features.contains("d2"), "应该包含特征：d2 (%[0]2)");
        assertTrue(features.contains("c1"), "应该包含特征：c1 (%[-1]1)");
        assertTrue(features.contains("c/d5"), "应该包含特征：c/d5 (%[-1]/%[0]5)");
        assertTrue(features.contains("b/c4"), "应该包含特征：b/c4 (%[-2]/%[-1]4)");
        assertTrue(features.contains("e3"), "应该包含特征：e3 (%[1]3)");
        assertTrue(features.contains("d/e6"), "应该包含特征：d/e6 (%[0]/%[1]6)");
        assertTrue(features.contains("e/f7"), "应该包含特征：e/f7 (%[1]/%[2]7)");

        assertEquals(7, features.size(), "应该生成7个特征");
    }

    // ========== 性能对比测试（可选） ==========

    @Test
    public void testConsistencyWithTemplate_AllPositions() {
        // 对多个序列和位置进行全面对比
        char[][] testSequences = {
                "a".toCharArray(),
                "ab".toCharArray(),
                "abc".toCharArray(),
                "世界".toCharArray(),
                "世界你好".toCharArray(),
                "nihao".toCharArray(),
                "wo shi zhong guo ren".toCharArray()
        };

        for (char[] seq : testSequences) {
            for (int i = 0; i < seq.length; i++) {
                compareImplementations(seq, i);
            }
        }
    }
}

