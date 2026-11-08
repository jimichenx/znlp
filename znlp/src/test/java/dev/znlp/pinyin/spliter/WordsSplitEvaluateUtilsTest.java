package dev.znlp.pinyin.spliter;

import org.junit.jupiter.api.Test;
import dev.znlp.perceptron.utils.WordsSplitEvaluateUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PinyinSplitEvaluateUtils 的单元测试
 */
public class WordsSplitEvaluateUtilsTest {

    // ========== 完全匹配的测试 ==========

    @Test
    public void testWordCorrect_ExactMatch() {
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = Arrays.asList("ni", "hao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(2, correct, "完全匹配时应该返回2");
    }

    @Test
    public void testWordCorrect_SinglePinyin() {
        List<String> gold = List.of("ni");
        List<String> pred = List.of("ni");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(1, correct, "单个拼音匹配时应该返回1");
    }

    @Test
    public void testWordCorrect_MultiplePinyins() {
        List<String> gold = Arrays.asList("wo", "shi", "zhong", "guo", "ren");
        List<String> pred = Arrays.asList("wo", "shi", "zhong", "guo", "ren");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(5, correct, "多个拼音完全匹配时应该返回5");
    }

    // ========== 完全错误的测试 ==========

    @Test
    public void testWordCorrect_AllWrong() {
        // 字符序列相同但切分完全不同
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = Arrays.asList("nih", "ao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "完全错误时应该返回0");
    }

    // ========== 部分匹配的测试 ==========

    @Test
    public void testWordCorrect_PartialMatch_First() {
        // 第一个匹配，后面切分错误
        List<String> gold = Arrays.asList("ni", "hao", "ma");
        List<String> pred = Arrays.asList("ni", "haom", "a");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(1, correct, "只有第一个匹配时应该返回1");
    }

    @Test
    public void testWordCorrect_PartialMatch_Middle() {
        // 前面切分错误，中间和后面正确
        List<String> gold = Arrays.asList("wo", "shi", "zhong", "guo");
        List<String> pred = Arrays.asList("wosh", "i", "zhong", "guo");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(2, correct, "中间两个匹配时应该返回2");
    }

    @Test
    public void testWordCorrect_PartialMatch_Last() {
        // 前面切分错误，最后一个正确
        List<String> gold = Arrays.asList("wo", "shi", "zhong");
        List<String> pred = Arrays.asList("wos", "hi", "zhong");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(1, correct, "只有最后一个匹配时应该返回1");
    }

    // ========== 切分错误的测试 ==========

    @Test
    public void testWordCorrect_SplitError_OverSplit() {
        // 正确答案: ni hao
        // 预测结果: n i ha o (过度切分)
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = Arrays.asList("n", "i", "ha", "o");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "过度切分时应该返回0");
    }

    @Test
    public void testWordCorrect_SplitError_UnderSplit() {
        // 正确答案: ni hao
        // 预测结果: nihao (切分不足)
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = List.of("nihao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "切分不足时应该返回0");
    }

    @Test
    public void testWordCorrect_SplitError_Mixed() {
        // 正确答案: ni hao ma
        // 预测结果: nihao ma (第一个和第二个合并)
        List<String> gold = Arrays.asList("ni", "hao", "ma");
        List<String> pred = Arrays.asList("nihao", "ma");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(1, correct, "部分切分错误时，后续正确的应该被统计");
    }

    // ========== 长度不匹配的测试 ==========

    @Test
    public void testWordCorrect_LengthMismatch_GoldLonger() {
        // 正确答案: ni hao ma
        // 预测结果: ni hao (缺少最后一个)
        List<String> gold = Arrays.asList("ni", "hao", "ma");
        List<String> pred = Arrays.asList("ni", "hao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(2, correct, "正确答案更长时，前面匹配的应该被统计");
    }

    @Test
    public void testWordCorrect_LengthMismatch_PredLonger() {
        // 正确答案: ni hao
        // 预测结果: ni hao ma (多了一个)
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = Arrays.asList("ni", "hao", "ma");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(2, correct, "预测结果更长时，前面匹配的应该被统计");
    }

    // ========== 边界条件测试 ==========

    @Test
    public void testWordCorrect_EmptyGold() {
        List<String> gold = List.of();
        List<String> pred = Arrays.asList("ni", "hao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "正确答案为空时应该返回0");
    }

    @Test
    public void testWordCorrect_EmptyPred() {
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = List.of();
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "预测结果为空时应该返回0");
    }

    @Test
    public void testWordCorrect_BothEmpty() {
        List<String> gold = List.of();
        List<String> pred = List.of();
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "两者都为空时应该返回0");
    }

    @Test
    public void test2() {
        List<String> gold = Arrays.asList("wo", "zai", "xi", "an", "da", "jie");
        List<String> pred = Arrays.asList("wo", "zai", "xian", "da", "jie");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(4, correct, "两者都为空时应该返回0");
    }

    // ========== 复杂场景测试 ==========

    @Test
    public void testWordCorrect_ComplexScenario1() {
        // 正确答案: wo shi zhong guo ren
        // 预测结果: woshi zhong guo ren (前两个合并)
        // 匹配: zhong, guo, ren
        List<String> gold = Arrays.asList("wo", "shi", "zhong", "guo", "ren");
        List<String> pred = Arrays.asList("woshi", "zhong", "guo", "ren");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(3, correct, "复杂场景1：应该返回3（zhong, guo, ren）");
    }

    @Test
    public void testWordCorrect_ComplexScenario2() {
        // 正确答案: ni hao wo shi zhong guo ren
        // 预测结果: nihao wo shi zhong guo
        // 匹配: wo, shi, zhong, guo
        List<String> gold = Arrays.asList("ni", "hao", "wo", "shi", "zhong", "guo", "ren");
        List<String> pred = Arrays.asList("nihao", "wo", "shi", "zhong", "guo");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(4, correct, "复杂场景2：应该返回4（wo, shi, zhong, guo）");
    }

    // ========== 字符位置对齐测试 ==========

    @Test
    public void testWordCorrect_CharacterAlignment() {
        // 测试字符位置对齐逻辑
        // 正确答案: ni hao (n-i- -h-a-o)
        // 预测结果: n ihao (n- -i-h-a-o)
        // 虽然字符相同，但切分不同，应该返回0
        List<String> gold = Arrays.asList("ni", "hao");
        List<String> pred = Arrays.asList("n", "ihao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "字符位置不对齐时应该返回0");
    }

    @Test
    public void testWordCorrect_CharacterAlignment2() {
        // 正确答案: nihao (n-i-h-a-o)
        // 预测结果: ni hao (n-i- -h-a-o)
        // 字符相同但切分不同，应该返回0
        List<String> gold = List.of("nihao");
        List<String> pred = Arrays.asList("ni", "hao");
        int correct = WordsSplitEvaluateUtils.wordCorrect(gold, pred);
        assertEquals(0, correct, "字符相同但切分不同时应该返回0");
    }
}

