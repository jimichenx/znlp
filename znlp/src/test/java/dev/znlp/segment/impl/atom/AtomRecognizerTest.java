package dev.znlp.segment.impl.atom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.segment.Nature;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AtomRecognizer 单元测试
 * <p>
 * 测试覆盖以下场景：
 * - 时间识别
 * - 数字识别
 * - 数量词识别
 * - 单词识别
 * - Email识别
 * - 字符归一化
 * - 中文数字检查
 * - 边界条件（空文本、null等）
 * - Callback方式识别
 * - 返回List方式识别
 *
 * @author jimichan
 */
public class AtomRecognizerTest {

    private AtomRecognizer recognizer;
    private StringIntDATMap testDat;

    @BeforeEach
    public void setUp() {
        // 创建测试用的模板数据
        TreeMap<String, Integer> treeMap = new TreeMap<>();

        // 添加一些简单的测试模板
        treeMap.put("N年", TemplateType.TIME.getValue());
        treeMap.put("NN年", TemplateType.TIME.getValue());
        treeMap.put("NNNN年", TemplateType.TIME.getValue());
        treeMap.put("N月", TemplateType.TIME.getValue());
        treeMap.put("NN月", TemplateType.TIME.getValue());
        treeMap.put("N日", TemplateType.TIME.getValue());
        treeMap.put("NN日", TemplateType.TIME.getValue());

        treeMap.put("N", TemplateType.NUMBER.getValue());
        treeMap.put("NN", TemplateType.NUMBER.getValue());
        treeMap.put("NNN", TemplateType.NUMBER.getValue());
        treeMap.put("-N", TemplateType.NUMBER.getValue());

        treeMap.put("N个", TemplateType.MQ.getValue());
        treeMap.put("NN个", TemplateType.MQ.getValue());
        treeMap.put("Z个", TemplateType.MQ.getValue());

        treeMap.put("AA", TemplateType.WORD.getValue());
        treeMap.put("AAA", TemplateType.WORD.getValue());

        treeMap.put("-", TemplateType.CONNECT.getValue());

        testDat = new StringIntDATMap(treeMap);
        recognizer = new AtomRecognizer(testDat);
    }

    // ==================== 时间识别测试 ====================

    @Test
    public void testRecognizeTime_Year() {
        char[] text = "2024年".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size(), "应该识别到一个时间");
        AtomRecognitionResult result = results.get(0);
        assertEquals(0, result.getOffset());
        assertEquals(5, result.getLength());
        assertEquals(TemplateType.TIME, result.getTemplateType());
        assertEquals(Nature.t, result.getNature());
    }

    @Test
    public void testRecognizeTime_Month() {
        char[] text = "3月".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size());
        assertEquals(TemplateType.TIME, results.get(0).getTemplateType());
    }

    @Test
    public void testRecognizeTime_Date() {
        char[] text = "15日".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size());
        assertEquals(TemplateType.TIME, results.get(0).getTemplateType());
    }

    // ==================== 数字识别测试 ====================

    @Test
    public void testRecognizeNumber_SingleDigit() {
        char[] text = "5".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size());
        AtomRecognitionResult result = results.get(0);
        assertEquals(TemplateType.NUMBER, result.getTemplateType());
        assertEquals(Nature.m, result.getNature());
    }

    @Test
    public void testRecognizeNumber_MultipleDigits() {
        char[] text = "123".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size());
        assertEquals(TemplateType.NUMBER, results.get(0).getTemplateType());
        assertEquals(0, results.get(0).getOffset());
        assertEquals(3, results.get(0).getLength());
    }

    @Test
    public void testRecognizeNumber_Negative() {
        char[] text = "-5".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size());
        assertEquals(TemplateType.NUMBER, results.get(0).getTemplateType());
        assertEquals(0, results.get(0).getOffset());
        assertEquals(2, results.get(0).getLength());
    }

    @Test
    public void testRecognizeNumber_SkipSingleDot() {
        char[] text = ".".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        // 单独的点应该被跳过
        assertEquals(0, results.size());
    }

    // ==================== 数量词识别测试 ====================

    @Test
    public void testRecognizeMQ_Single() {
        char[] text = "5个".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(1, results.size());
        assertEquals(TemplateType.MQ, results.get(0).getTemplateType());
        assertEquals(Nature.mq, results.get(0).getNature());
    }

    // ==================== 单词识别测试 ====================

    @Test
    public void testRecognizeWord_English() {
        char[] text = "hello".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        // 应该识别到单词（至少2个字母）
        assertTrue(results.size() > 0);
        boolean foundWord = results.stream()
                .anyMatch(r -> r.getTemplateType() == TemplateType.WORD);
        assertTrue(foundWord, "应该识别到单词");
    }

    // ==================== Email识别测试 ====================

    @Test
    public void testRecognizeEmail() {
        char[] text = "test@example.com".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        // 应该识别到email（作为WORD类型）
        boolean foundEmail = results.stream()
                .anyMatch(r -> r.getTemplateType() == TemplateType.WORD &&
                        r.getOffset() == 0 &&
                        r.getLength() == text.length);
        assertTrue(foundEmail, "应该识别到email");
    }

    // ==================== 字符归一化测试 ====================

    @Test
    public void testNormalizeChars_Digits() {
        char[] text = "123".toCharArray();
        char[] normalized = recognizer.normalizeChars(text);

        assertEquals('N', normalized[0]);
        assertEquals('N', normalized[1]);
        assertEquals('N', normalized[2]);
    }

    @Test
    public void testNormalizeChars_Letters() {
        char[] text = "abc".toCharArray();
        char[] normalized = recognizer.normalizeChars(text);

        assertEquals('A', normalized[0]);
        assertEquals('A', normalized[1]);
        assertEquals('A', normalized[2]);
    }

    @Test
    public void testNormalizeChars_ChineseNumbers() {
        char[] text = "一二三".toCharArray();
        char[] normalized = recognizer.normalizeChars(text);

        assertEquals('Z', normalized[0]);
        assertEquals('Z', normalized[1]);
        assertEquals('Z', normalized[2]);
    }

    @Test
    public void testNormalizeChars_Mixed() {
        char[] text = "2024年3月".toCharArray();
        char[] normalized = recognizer.normalizeChars(text);

        assertEquals('N', normalized[0]);
        assertEquals('N', normalized[1]);
        assertEquals('N', normalized[2]);
        assertEquals('N', normalized[3]);
        // '年' 和 '月' 不是中文数字，所以不会被归一化
    }

    @Test
    public void testNormalizeChars_Dot() {
        char[] text = "3.14".toCharArray();
        char[] normalized = recognizer.normalizeChars(text);

        assertEquals('N', normalized[0]);
        assertEquals('N', normalized[1]); // 点被归一化为N
        assertEquals('N', normalized[2]);
        assertEquals('N', normalized[3]);
    }

    // ==================== 中文数字检查测试 ====================

    @Test
    public void testIsChineseNumber() {
        assertTrue(recognizer.isChineseNumber('一'));
        assertTrue(recognizer.isChineseNumber('二'));
        assertTrue(recognizer.isChineseNumber('三'));
        assertTrue(recognizer.isChineseNumber('零'));
        assertTrue(recognizer.isChineseNumber('万'));
        assertTrue(recognizer.isChineseNumber('亿'));

        assertFalse(recognizer.isChineseNumber('a'));
        assertFalse(recognizer.isChineseNumber('1'));
        assertFalse(recognizer.isChineseNumber('年'));
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testRecognize_EmptyArray() {
        char[] text = new char[0];
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertEquals(0, results.size());
    }

    @Test
    public void testRecognize_NullArray() {
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(null, results::add);

        assertEquals(0, results.size());
    }

    @Test
    public void testRecognize_NoNumbersOrLetters() {
        char[] text = "中文文本".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        // 没有数字和字母，应该返回空结果
        assertEquals(0, results.size());
    }

    // ==================== Callback方式测试 ====================

    @Test
    public void testRecognizeWithCallback() {
        char[] text = "2024年3月15日".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        assertTrue(results.size() > 0, "应该识别到至少一个结果");

        // 验证所有结果都有正确的属性
        for (AtomRecognitionResult result : results) {
            assertNotNull(result);
            assertTrue(result.getOffset() >= 0);
            assertTrue(result.getLength() > 0);
            assertNotNull(result.getTemplateType());
        }
    }

    @Test
    public void testRecognizeWithCallback_Count() {
        char[] text = "2024年3月".toCharArray();
        final int[] count = {0};

        recognizer.recognize(text, result -> count[0]++);

        assertTrue(count[0] > 0, "callback应该被调用");
    }

    // ==================== 返回List方式测试 ====================

    @Test
    public void testRecognizeReturnList() {
        char[] text = "2024年".toCharArray();

        List<AtomRecognitionResult> results = recognizer.recognize(text);

        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    @Test
    public void testRecognizeReturnList_Empty() {
        char[] text = "中文".toCharArray();

        List<AtomRecognitionResult> results = recognizer.recognize(text);

        assertNotNull(results);
        assertEquals(0, results.size());
    }

    // ==================== 综合测试 ====================

    @Test
    public void testRecognize_MultiplePatterns() {
        char[] text = "2024年3月15日有5个苹果".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        // 应该识别到多个模式
        assertTrue(results.size() > 0);

        // 验证至少有一个时间识别
        boolean hasTime = results.stream()
                .anyMatch(r -> r.getTemplateType() == TemplateType.TIME);
        assertTrue(hasTime, "应该识别到时间");
    }

    @Test
    public void testRecognizeResult_Properties() {
        char[] text = "2024年".toCharArray();
        List<AtomRecognitionResult> results = new ArrayList<>();

        recognizer.recognize(text, results::add);

        if (!results.isEmpty()) {
            AtomRecognitionResult result = results.get(0);

            // 验证所有属性都不为null（除了nature可能为null的情况）
            assertNotNull(result.getTemplateType());
            assertTrue(result.getOffset() >= 0);
            assertTrue(result.getLength() > 0);

            // 验证toString方法
            String str = result.toString();
            assertNotNull(str);
            assertFalse(str.isEmpty());
        }
    }

    @Test
    public void testRecognizeResult_EqualsAndHashCode() {
        AtomRecognitionResult result1 = new AtomRecognitionResult(0, 5, TemplateType.TIME);
        AtomRecognitionResult result2 = new AtomRecognitionResult(0, 5, TemplateType.TIME);
        AtomRecognitionResult result3 = new AtomRecognitionResult(1, 5, TemplateType.TIME);

        assertEquals(result1, result2, "相同的结果应该相等");
        assertNotEquals(result1, result3, "不同的结果应该不相等");
        assertEquals(result1.hashCode(), result2.hashCode(), "相同的结果应该有相同的hashCode");
    }
}

