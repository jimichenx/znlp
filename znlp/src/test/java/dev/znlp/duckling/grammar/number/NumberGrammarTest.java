package dev.znlp.duckling.grammar.number;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import dev.znlp.duckling.DucklingParser;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.support.Dimension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NumberGrammar 识别契约。只走数字规则，避免时间/金额抢同一段文本。
 */
public class NumberGrammarTest {

    private final DucklingParser parser = new DucklingParser(new DefaultRuleSet(new NumberGrammar().rules()));

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "0, 〇",
            "0, 零",
            "1, 一",
            "1, 壹",
            "2, 二",
            "2, 两",
            "2, 贰",
            "10, 十",
            "10, 一十",
            "10, 拾",
            "11, 十一",
            "12, 十二",
            "12, 一十二",
            "13, 十三",
            "20, 二十",
            "30, 三十",
            "90, 九十",
            "33, 三十三",
            "33, 叁拾叁",
            "60, 陆拾",
            "96, 九十六",
            "101, 一百零一",
            "110, 一百一十",
            "203, 二百零三",
            "534, 五百三十四",
            "1001, 一千零一",
            "2000, 两千",
            "2000, 二千",
            "10040, 一万零四十",
            "14000, 一万零四千",
            "10040, 壹万零肆拾",
            "20, 贰拾",
            "34567, 三万四千五百六十七",
    })
    public void canonicalChineseIntegers(double expected, String text) {
        assertSingle(text, expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "1, 1",
            "10, 10",
            "101, 101",
            "123, 123",
            "1998, 1998",
    })
    public void arabicIntegers(double expected, String text) {
        assertSingle(text, expected);
    }

    @Test
    public void thousandsSeparatedInteger() {
        assertSingle("34,567", 34567);
        assertSingle("1,200,000", 1_200_000);
        assertSingle("1,234.56", 1234.56);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "1.1, 1.1",
            "0.77, 0.77",
            "0.77, .77",
            "3.0, 3.0",
    })
    public void arabicDecimals(double expected, String text) {
        assertSingle(text.replace("'", ""), expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "0.5, 零点五",
            "0.05, 零点零五",
            "5.05, 五点零五",
            "10.5, 十点五",
            "3.14, 三点一四",
            "0.618, 零点六一八",
            "13.5, 十三点五",
    })
    public void chineseDecimals(double expected, String text) {
        assertSingle(text, expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "-100, 负一百",
            "-11, 负十一",
            "-11, 負十一",
            "-1200000, 负一百二十万",
            "-1200000, -1200000",
    })
    public void negatives(double expected, String text) {
        assertSingle(text, expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "100000, 十万",
            "1200000, 一百二十万",
            "1040000, 一百零四万",
            "1.0E9, 十亿",
            "1.0E9, 一十亿",
            "3.0E10, 三百亿",
            "100000001, 一亿零一",
            "123456789, 一亿二千三百四十五万六千七百八十九",
    })
    public void largeUnits(double expected, String text) {
        assertSingle(text, expected);
    }

    @Test
    public void yearLikeDigitSequence_isConcatenated() {
        assertSingle("一九九八", 1998);
        assertSingle("二〇二四", 2024);
    }

    @Test
    public void zeroAsGapMarker_acceptsBothForms() {
        assertSingle("一百零三", 103);
        assertSingle("一百〇三", 103);
        assertSingle("一千零十", 1010);
    }

    @Test
    public void similarLookingChineseNumbers_areNotConfused() {
        assertSingle("十三", 13);
        assertSingle("三十", 30);
        assertSingle("三十一", 31);
        assertSingle("十九", 19);
        assertSingle("九十", 90);
        assertSingle("九十一", 91);
        assertSingle("十二", 12);
        assertSingle("二十一", 21);
        assertSingle("一百零一", 101);
        assertSingle("一百一十", 110);
        assertSingle("一千零一", 1001);
        assertSingle("两千五百", 2500);
        assertSingle("一万二千", 12000);
        assertSingle("二万五千", 25000);
    }

    @Test
    public void twoAndLiang_areTheSameInteger() {
        assertSingle("二", 2);
        assertSingle("两", 2);
        assertSingle("兩", 2);
        assertSingle("二千", 2000);
        assertSingle("两千", 2000);
        assertSingle("两万", 20000);
    }

    @Test
    public void scatteredNumbersInSentence_keepSeparate() {
        assertValues("买了三苹果和五个梨", 3, 5);
        assertValues("一, 2, 三", 1, 2, 3);
        assertValues("我有 123 个苹果", 123);
    }

    @Test
    public void longSentence_extractsEmbeddedNumbersInOrder() {
        String text = "我有123个苹果，还有四十五个梨，以及一千零一夜那么多故事，最后剩下十一。";
        assertValues(text, 123, 45, 1001, 11);
    }

    @Test
    public void longChineseParagraph_extractsMixedNumbersInOrder() {
        String filler = "甲乙丙丁戊己庚辛".repeat(30);
        String text = filler
                + "统计显示共四十五人到场，提交材料123份，合计金额一万零四十，完成率三点一四。"
                + "对照年份一九九八，剩余名额十一。清单编号为 1、2、3。"
                + filler
                + "结尾编号999。";
        assertValues(text, 45, 123, 10040, 3.14, 1998, 11, 1, 2, 3, 999);
    }

    @Test
    public void longPadding_stillFindsChineseAndArabic() {
        String pad = "甲乙丙丁".repeat(80);
        String text = pad + "合计四十五件，单价3.14，共一万零四十。" + pad;
        assertValues(text, 45, 3.14, 10040);
        List<NumberDimension> nums = numbers(text);
        assertEquals("四十五", nums.get(0).getText());
        assertEquals("3.14", nums.get(1).getText());
        assertEquals("一万零四十", nums.get(2).getText());
    }

    @Test
    public void manyArabicNumbersSeparated_areNotGlued() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 25; i++) {
            if (i > 1) {
                sb.append('、');
            }
            sb.append(i);
        }
        List<Double> values = values(sb.toString());
        assertEquals(25, values.size(), values.toString());
        for (int i = 0; i < 25; i++) {
            assertEquals(i + 1.0, values.get(i), 1e-9, "index " + i);
        }
    }

    @Test
    public void longArabicInteger_isOneToken() {
        assertSingle("123456789012345", 123456789012345.0);
    }

    @Test
    public void chineseDigitRun_doesNotExplode() {
        List<?> nodes = parser.generateAll("一二三四五六七八九十");
        assertTrue(nodes.size() < 80, "中文数位拼接节点膨胀: " + nodes.size());
        assertSingle("一九九八", 1998);
    }

    @Test
    public void mergedNumericRegex_coversIntegerFloatAndThousands() {
        assertSingle("101", 101);
        assertSingle(".77", 0.77);
        assertSingle("3.0", 3.0);
        assertSingle("34,567", 34567);
        assertSingle("1,234.56", 1234.56);
    }

    private void assertSingle(String text, double expected) {
        List<Double> values = values(text);
        assertEquals(1, values.size(), () -> text + " => " + parser.parse(text));
        assertEquals(expected, values.get(0), 1e-6, text);
    }

    private void assertValues(String text, double... expected) {
        List<Double> values = values(text);
        assertEquals(expected.length, values.size(), () -> text + " => " + parser.parse(text));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], values.get(i), 1e-6, text + " #" + i);
        }
    }

    private List<Double> values(String text) {
        List<Double> list = new ArrayList<>();
        for (NumberDimension n : numbers(text)) {
            list.add(n.getValue());
        }
        return list;
    }

    private List<NumberDimension> numbers(String text) {
        List<NumberDimension> list = new ArrayList<>();
        for (Dimension d : parser.parse(text)) {
            if (d instanceof NumberDimension n) {
                list.add(n);
            }
        }
        return list;
    }
}
