package dev.znlp.perceptron.dsl;

import org.junit.jupiter.api.Test;
import dev.znlp.perceptron.feature.FeatureTemplate;
import dev.znlp.perceptron.utils.FastStringBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FeatureTemplate测试类
 */
public class FeatureTemplateTest {

    @Test
    public void testParseSimpleRule() {
        // 测试简单规则的解析和特征生成
        String dsl = "%[0]2;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);
        List<String> features = new ArrayList<>();

        template.features(seq, 0, buffer, features::add);
        assertEquals(1, features.size());
        assertEquals("世2", features.get(0));
    }

    @Test
    public void testParseMultipleRules() {
        // 测试多个规则的解析
        String dsl = "%[0]2;%[-1]1;%[1]3;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界你".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);
        List<String> features = new ArrayList<>();

        template.features(seq, 1, buffer, features::add);
        // 位置1应该生成3个特征：%[0]2, %[-1]1, %[1]3
        assertEquals(3, features.size());
        assertTrue(features.contains("界2"));
        assertTrue(features.contains("世1"));
        assertTrue(features.contains("你3"));
    }

    @Test
    public void testParseComplexRule() {
        // 测试复杂规则（多个索引）
        String dsl = "%[-1]%[0]5;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);
        List<String> features = new ArrayList<>();

        template.features(seq, 1, buffer, features::add);
        assertEquals(1, features.size());
        assertEquals("世界5", features.get(0));
    }

    @Test
    public void testParseRuleWithSeparator() {
        // 测试带分隔符的规则
        String dsl = "%[-2]/%[-1]%[0]4;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界你好".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);
        List<String> features = new ArrayList<>();

        template.features(seq, 2, buffer, features::add);
        assertEquals(1, features.size());
        assertEquals("世/界你4", features.get(0));
    }

    @Test
    public void testParseEmptyDSL() {
        // 测试空DSL的行为
        FeatureTemplate template = new FeatureTemplate("");
        char[] seq = "世界".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);
        List<String> features = new ArrayList<>();
        template.features(seq, 0, buffer, features::add);
        assertEquals(0, features.size());

        template = new FeatureTemplate("   ");
        features.clear();
        template.features(seq, 0, buffer, features::add);
        assertEquals(0, features.size());

        template = new FeatureTemplate(null);
        features.clear();
        template.features(seq, 0, buffer, features::add);
        assertEquals(0, features.size());
    }

    @Test
    public void testParseWithWhitespace() {
        // 测试带空格的DSL
        String dsl = "  %[0]2;  %[-1]1;  ";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);
        List<String> features = new ArrayList<>();

        template.features(seq, 1, buffer, features::add);
        assertEquals(2, features.size());
        assertTrue(features.contains("界2"));
        assertTrue(features.contains("世1"));
    }

    @Test
    public void testFeaturesBasic() {
        String dsl = "%[0]2;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界你好".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);

        List<String> features = new ArrayList<>();
        template.features(seq, 0, buffer, features::add);

        assertEquals(1, features.size());
        assertEquals("世2", features.get(0));
    }

    @Test
    public void testFeaturesMultipleRules() {
        // 对应完整的DSL规则
        String dsl = "%[0]2;%[-1]1;%[-1]%[0]5;%[-2]/%[-1]%[0]4;%[1]3;%[0]/%[1]6;%[1]/%[2]7;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界你好".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);

        // 测试位置0（'世'）
        List<String> features = new ArrayList<>();
        template.features(seq, 0, buffer, features::add);

        // 位置0应该生成：%[0]2, %[1]3, %[0]/%[1]6, %[1]/%[2]7
        // 因为position=0，所以%[-1]和%[-2]相关的规则不满足
        assertEquals(4, features.size());
        assertTrue(features.contains("世2")); // %[0]2
        assertTrue(features.contains("界3")); // %[1]3
        assertTrue(features.contains("世/界6")); // %[0]/%[1]6
        assertTrue(features.contains("界/你7")); // %[1]/%[2]7

        // 测试位置1（'界'）
        features.clear();
        template.features(seq, 1, buffer, features::add);

        // 位置1应该生成：%[0]2, %[-1]1, %[-1]%[0]5, %[1]3, %[0]/%[1]6, %[1]/%[2]7
        assertEquals(6, features.size());
        assertTrue(features.contains("界2")); // %[0]2
        assertTrue(features.contains("世1")); // %[-1]1
        assertTrue(features.contains("世界5")); // %[-1]%[0]5
        assertTrue(features.contains("你3")); // %[1]3
        assertTrue(features.contains("界/你6")); // %[0]/%[1]6
        assertTrue(features.contains("你/好7")); // %[1]/%[2]7

        // 测试位置2（'你'）
        features.clear();
        template.features(seq, 2, buffer, features::add);

        // 位置2应该生成6个特征（%[1]/%[2]7会因为索引越界而失败）
        assertEquals(6, features.size());
        assertTrue(features.contains("你2")); // %[0]2
        assertTrue(features.contains("界1")); // %[-1]1
        assertTrue(features.contains("界你5")); // %[-1]%[0]5
        assertTrue(features.contains("世/界你4")); // %[-2]/%[-1]%[0]4
        assertTrue(features.contains("好3")); // %[1]3
        assertTrue(features.contains("你/好6")); // %[0]/%[1]6

        // 测试位置3（'好'）- 最后一个字符
        features.clear();
        template.features(seq, 3, buffer, features::add);

        // 位置3应该生成：%[0]2, %[-1]1, %[-1]%[0]5, %[-2]/%[-1]%[0]4
        assertEquals(4, features.size());
        assertTrue(features.contains("好2")); // %[0]2
        assertTrue(features.contains("你1")); // %[-1]1
        assertTrue(features.contains("你好5")); // %[-1]%[0]5
        assertTrue(features.contains("界/你好4")); // %[-2]/%[-1]%[0]4
    }

    @Test
    public void testFeaturesBoundaryConditions() {
        String dsl = "%[0]2;%[-1]1;%[1]3;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "A".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);

        List<String> features = new ArrayList<>();
        template.features(seq, 0, buffer, features::add);

        // 单字符，position=0，只能生成 %[0]2
        // %[-1]1 和 %[1]3 都会因为索引越界而失败
        assertEquals(1, features.size());
        assertEquals("A2", features.get(0));
    }

    @Test
    public void testFeaturesInvalidPosition() {
        String dsl = "%[0]2;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);

        List<String> features = new ArrayList<>();

        // 负数位置
        template.features(seq, -1, buffer, features::add);
        assertEquals(0, features.size());

        // 超出范围的位置
        template.features(seq, 2, buffer, features::add);
        assertEquals(0, features.size());
    }

    @Test
    public void testComplexDSL() {
        // 测试更复杂的DSL，包含多个分隔符
        String dsl = "%[0]/%[1]/%[2]abc;%[-1]-%[0]-%[1]xyz;";
        FeatureTemplate template = new FeatureTemplate(dsl);
        char[] seq = "世界你好".toCharArray();
        FastStringBuilder buffer = new FastStringBuilder(100);

        List<String> features = new ArrayList<>();
        template.features(seq, 1, buffer, features::add);

        // 应该生成：%[0]/%[1]/%[2]abc 和 %[-1]-%[0]-%[1]xyz
        assertEquals(2, features.size());
        assertTrue(features.contains("界/你/好abc"));
        assertTrue(features.contains("世-界-你xyz"));
    }
}
