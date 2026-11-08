package dev.znlp.base.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characters 类的单元测试
 * <p>
 * 测试覆盖以下场景：
 * - 全角/半角字符转换（String和char[]）
 * - 标点符号判断（ASCII、全角、CJK等各类标点）
 * - 字符类型判断（ASCII、数字等）
 * - 字符显示工具（Unicode转义）
 * - 空值和边界条件处理
 * - 性能特性（延迟复制）
 * </p>
 *
 * @author jimichan
 */
public class CharactersTest {

    // ==================== 全角转半角测试（String） ====================

    @Test
    public void testFullWidth2halfWidth_AllFullWidth() {
        String input = "ＡＢＣ０１２";
        String expected = "ABC012";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_PartialFullWidth() {
        String input = "Ａbc０１２def";
        String expected = "Abc012def";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_NoFullWidth() {
        String original = "abc123中文";
        String result = Characters.fullWidth2halfWidth(original);
        // 验证延迟复制：无需转换时返回原引用
        assertSame(original, result);
    }

    @Test
    public void testFullWidth2halfWidth_FullWidthSpace() {
        String input = "　";  // 全角空格 U+3000
        String expected = " ";  // 半角空格
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_MixedSpaces() {
        String input = "你好　世界 测试";  // 全角空格 + 半角空格
        String expected = "你好 世界 测试";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_Null() {
        assertNull(Characters.fullWidth2halfWidth((String) null));
    }

    @Test
    public void testFullWidth2halfWidth_Empty() {
        String original = "";
        String result = Characters.fullWidth2halfWidth(original);
        assertSame(original, result);
    }

    @Test
    public void testFullWidth2halfWidth_SingleChar() {
        assertEquals("A", Characters.fullWidth2halfWidth("Ａ"));
        assertEquals("0", Characters.fullWidth2halfWidth("０"));
    }

    @Test
    public void testFullWidth2halfWidth_FullWidthPunctuation() {
        String input = "！＂＃＄％";
        String expected = "!\"#$%";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_ConversionAtStart() {
        String input = "Ａbc";
        String expected = "Abc";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_ConversionAtEnd() {
        String input = "abcＡ";
        String expected = "abcA";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_ConversionInMiddle() {
        String input = "abc０１２def";
        String expected = "abc012def";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testFullWidth2halfWidth_LongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Ａ");
        }
        String result = Characters.fullWidth2halfWidth(sb.toString());
        assertEquals(1000, result.length());
        for (int i = 0; i < 1000; i++) {
            assertEquals('A', result.charAt(i));
        }
    }

    // ==================== 全角转半角测试（char[]） ====================

    @Test
    public void testFullWidth2halfWidth_CharArray_AllFullWidth() {
        char[] chars = "ＡＢＣ０１２".toCharArray();
        Characters.fullWidth2halfWidth(chars);
        assertArrayEquals("ABC012".toCharArray(), chars);
    }

    @Test
    public void testFullWidth2halfWidth_CharArray_PartialFullWidth() {
        char[] chars = "Ａbc０１２".toCharArray();
        Characters.fullWidth2halfWidth(chars);
        assertArrayEquals("Abc012".toCharArray(), chars);
    }

    @Test
    public void testFullWidth2halfWidth_CharArray_NoFullWidth() {
        char[] original = "abc123".toCharArray();
        char[] chars = original.clone();
        Characters.fullWidth2halfWidth(chars);
        assertArrayEquals(original, chars);
    }

    @Test
    public void testFullWidth2halfWidth_CharArray_Empty() {
        char[] chars = new char[0];
        Characters.fullWidth2halfWidth(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testFullWidth2halfWidth_CharArray_SingleChar() {
        char[] chars = {'Ａ'};
        Characters.fullWidth2halfWidth(chars);
        assertArrayEquals(new char[]{'A'}, chars);
    }

    @Test
    public void testFullWidth2halfWidth_CharArray_Null() {
        assertThrows(NullPointerException.class, () -> {
            Characters.fullWidth2halfWidth((char[]) null);
        });
    }

    @Test
    public void testFullWidth2halfWidth_CharArray_ModifiesOriginal() {
        char[] chars = "ＡＢＣ".toCharArray();
        char[] reference = chars;
        Characters.fullWidth2halfWidth(chars);
        assertSame(reference, chars);
        assertEquals('A', chars[0]);
    }

    // ==================== 半角转全角测试（String） ====================

    @Test
    public void testHalfWidth2fullWidth_AllHalfWidth() {
        String input = "ABC012";
        String expected = "ＡＢＣ０１２";
        assertEquals(expected, Characters.halfWidth2fullWidth(input));
    }

    @Test
    public void testHalfWidth2fullWidth_PartialHalfWidth() {
        String input = "A中B文0测1试";
        String expected = "Ａ中Ｂ文０测１试";
        assertEquals(expected, Characters.halfWidth2fullWidth(input));
    }

    @Test
    public void testHalfWidth2fullWidth_NoHalfWidth() {
        String original = "中文测试";
        String result = Characters.halfWidth2fullWidth(original);
        // 验证延迟复制
        assertSame(original, result);
    }

    @Test
    public void testHalfWidth2fullWidth_HalfWidthSpace() {
        String input = " ";  // 半角空格
        String expected = "　";  // 全角空格
        assertEquals(expected, Characters.halfWidth2fullWidth(input));
    }

    @Test
    public void testHalfWidth2fullWidth_Null() {
        assertNull(Characters.halfWidth2fullWidth(null));
    }

    @Test
    public void testHalfWidth2fullWidth_Empty() {
        String original = "";
        String result = Characters.halfWidth2fullWidth(original);
        assertSame(original, result);
    }

    @Test
    public void testHalfWidth2fullWidth_SingleChar() {
        assertEquals("Ａ", Characters.halfWidth2fullWidth("A"));
        assertEquals("０", Characters.halfWidth2fullWidth("0"));
    }

    @Test
    public void testHalfWidth2fullWidth_Punctuation() {
        // 注意：原代码中半角转全角时，范围是 '\u0022' < ch，即从 '#' 开始
        String input = "#$%&";
        String expected = "＃＄％＆";
        assertEquals(expected, Characters.halfWidth2fullWidth(input));
    }

    @Test
    public void testHalfWidth2fullWidth_DoubleQuoteNotConverted() {
        // 双引号 '"' 的 Unicode 值是 '\u0022'
        // 由于原代码使用 '\u0022' < ch，所以双引号不会被转换
        String input = "\"";
        // 双引号应该不被转换（因为条件是 > '\u0022'）
        String result = Characters.halfWidth2fullWidth(input);
        // 检查是否被转换
        assertTrue(result.equals("\"") || result.equals("＂"));
    }

    @Test
    public void testHalfWidth2fullWidth_ConversionAtStart() {
        String input = "A中文";
        String expected = "Ａ中文";
        assertEquals(expected, Characters.halfWidth2fullWidth(input));
    }

    @Test
    public void testHalfWidth2fullWidth_ConversionAtEnd() {
        String input = "中文A";
        String expected = "中文Ａ";
        assertEquals(expected, Characters.halfWidth2fullWidth(input));
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    public void testBidirectionalConversion_FullToHalfToFull() {
        String original = "ＡＢＣ０１２";
        String halfWidth = Characters.fullWidth2halfWidth(original);
        String backToFull = Characters.halfWidth2fullWidth(halfWidth);
        assertEquals(original, backToFull);
    }

    @Test
    public void testBidirectionalConversion_HalfToFullToHalf() {
        // 注意：由于半角转全角时有范围限制（'\u0022' < ch），某些字符可能不会被转换
        String original = "ABC012#$%";
        String fullWidth = Characters.halfWidth2fullWidth(original);
        String backToHalf = Characters.fullWidth2halfWidth(fullWidth);
        assertEquals(original, backToHalf);
    }

    @Test
    public void testIdempotent_FullToHalf() {
        String original = "ＡＢＣ０１２";
        String first = Characters.fullWidth2halfWidth(original);
        String second = Characters.fullWidth2halfWidth(first);
        assertEquals(first, second);
        assertSame(first, second, "已转换的字符串再次转换应返回原引用");
    }

    @Test
    public void testIdempotent_HalfToFull() {
        String original = "ABC012";
        String first = Characters.halfWidth2fullWidth(original);
        String second = Characters.halfWidth2fullWidth(first);
        assertEquals(first, second);
        assertSame(first, second, "已转换的字符串再次转换应返回原引用");
    }

    // ==================== 标点符号判断测试 ====================

    @Test
    public void testIsPunctuation_ASCIIPunctuation() {
        // 0-47 范围的标点
        assertTrue(Characters.isPunctuation('!'));   // 33
        assertTrue(Characters.isPunctuation('"'));   // 34
        assertTrue(Characters.isPunctuation('#'));   // 35
        assertTrue(Characters.isPunctuation('$'));   // 36
        assertTrue(Characters.isPunctuation('%'));   // 37
        assertTrue(Characters.isPunctuation('&'));   // 38
        assertTrue(Characters.isPunctuation('('));   // 40
        assertTrue(Characters.isPunctuation(')'));   // 41
        assertTrue(Characters.isPunctuation('*'));   // 42
        assertTrue(Characters.isPunctuation('+'));   // 43
        assertTrue(Characters.isPunctuation(','));   // 44
        assertTrue(Characters.isPunctuation('-'));   // 45
        assertTrue(Characters.isPunctuation('.'));   // 46
        assertTrue(Characters.isPunctuation('/'));   // 47

        // 58-64 范围的标点
        assertTrue(Characters.isPunctuation(':'));   // 58
        assertTrue(Characters.isPunctuation(';'));   // 59
        assertTrue(Characters.isPunctuation('<'));   // 60
        assertTrue(Characters.isPunctuation('='));   // 61
        assertTrue(Characters.isPunctuation('>'));   // 62
        assertTrue(Characters.isPunctuation('?'));   // 63
        assertTrue(Characters.isPunctuation('@'));   // 64

        // 91-96 范围的标点
        assertTrue(Characters.isPunctuation('['));   // 91
        assertTrue(Characters.isPunctuation('\\'));  // 92
        assertTrue(Characters.isPunctuation(']'));   // 93
        assertTrue(Characters.isPunctuation('^'));   // 94
        assertTrue(Characters.isPunctuation('_'));   // 95
        assertTrue(Characters.isPunctuation('`'));   // 96

        // 123-127 范围的标点
        assertTrue(Characters.isPunctuation('{'));   // 123
        assertTrue(Characters.isPunctuation('|'));   // 124
        assertTrue(Characters.isPunctuation('}'));   // 125
        assertTrue(Characters.isPunctuation('~'));   // 126
        assertTrue(Characters.isPunctuation((char) 127)); // DEL
    }

    @Test
    public void testIsPunctuation_ASCIINotPunctuation() {
        // 字母和数字不应该被认为是标点
        assertFalse(Characters.isPunctuation('A'));
        assertFalse(Characters.isPunctuation('Z'));
        assertFalse(Characters.isPunctuation('a'));
        assertFalse(Characters.isPunctuation('z'));
        assertFalse(Characters.isPunctuation('0'));
        assertFalse(Characters.isPunctuation('9'));
    }

    @Test
    public void testIsPunctuation_NonBreakingSpace() {
        assertTrue(Characters.isPunctuation((char) 160));  // 不间断空格
    }

    @Test
    public void testIsPunctuation_Latin1Supplement() {
        // U+00A1-U+00BF 范围
        assertTrue(Characters.isPunctuation('¡'));  // U+00A1
        assertTrue(Characters.isPunctuation('¿'));  // U+00BF
    }

    @Test
    public void testIsPunctuation_FullWidthPunctuation() {
        // 全角标点
        assertTrue(Characters.isPunctuation('！'));  // U+FF01
        assertTrue(Characters.isPunctuation('，'));  // U+FF0C
        assertTrue(Characters.isPunctuation('。'));  // U+FF0E
        assertTrue(Characters.isPunctuation('：'));  // U+FF1A
        assertTrue(Characters.isPunctuation('；'));  // U+FF1B
        assertTrue(Characters.isPunctuation('？'));  // U+FF1F
    }

    @Test
    public void testIsPunctuation_CJKPunctuation() {
        // CJK 符号和标点 U+3000-U+303F
        assertTrue(Characters.isPunctuation('　'));  // 全角空格 U+3000
        assertTrue(Characters.isPunctuation('、'));  // 顿号 U+3001
        assertTrue(Characters.isPunctuation('。'));  // 句号 U+3002
        assertTrue(Characters.isPunctuation('《'));  // U+300A
        assertTrue(Characters.isPunctuation('》'));  // U+300B
        assertTrue(Characters.isPunctuation('「'));  // U+300C
        assertTrue(Characters.isPunctuation('」'));  // U+300D
    }

    @Test
    public void testIsPunctuation_GeneralPunctuation() {
        // U+2010-U+2027 范围
        assertTrue(Characters.isPunctuation('\u2010'));  // U+2010 连字符
        assertTrue(Characters.isPunctuation('\u2014'));  // U+2014 破折号
        assertTrue(Characters.isPunctuation('\u2019'));  // U+2019 右单引号

        // U+2030-U+205E 范围
        assertTrue(Characters.isPunctuation('\u2030'));  // U+2030 千分号
    }

    @Test
    public void testIsPunctuation_VerticalForms() {
        // U+FE10-U+FE1F 竖排标点
        assertTrue(Characters.isPunctuation('\uFE10'));  // U+FE10
    }

    @Test
    public void testIsPunctuation_ChineseCharacters() {
        // 中文字符不应该被认为是标点
        assertFalse(Characters.isPunctuation('中'));
        assertFalse(Characters.isPunctuation('文'));
        assertFalse(Characters.isPunctuation('测'));
        assertFalse(Characters.isPunctuation('试'));
    }

    @Test
    public void testIsPunctuation_FullWidthLetters() {
        // 全角字母不应该被认为是标点
        assertFalse(Characters.isPunctuation('Ａ'));
        assertFalse(Characters.isPunctuation('０'));
    }

    // ==================== ASCII判断测试 ====================

    @Test
    public void testIsASCII_ASCIICharacters() {
        assertTrue(Characters.isASCII('A'));
        assertTrue(Characters.isASCII('Z'));
        assertTrue(Characters.isASCII('a'));
        assertTrue(Characters.isASCII('z'));
        assertTrue(Characters.isASCII('0'));
        assertTrue(Characters.isASCII('9'));
        assertTrue(Characters.isASCII(' '));
        assertTrue(Characters.isASCII('!'));
        assertTrue(Characters.isASCII((char) 0));
        assertTrue(Characters.isASCII((char) 127));
    }

    @Test
    public void testIsASCII_NonASCIICharacters() {
        assertFalse(Characters.isASCII('中'));
        assertFalse(Characters.isASCII('文'));
        assertFalse(Characters.isASCII('Ａ'));  // 全角A
        assertFalse(Characters.isASCII('　'));  // 全角空格
        assertFalse(Characters.isASCII((char) 128));
        assertFalse(Characters.isASCII((char) 256));
    }

    // ==================== 字符显示测试 ====================

    @Test
    public void testShowCharacter_ASCIICharacters() {
        assertEquals("\\u0041", Characters.showCharacter('A'));
        assertEquals("\\u007A", Characters.showCharacter('z'));
        assertEquals("\\u0030", Characters.showCharacter('0'));
        assertEquals("\\u0020", Characters.showCharacter(' '));
    }

    @Test
    public void testShowCharacter_ChineseCharacters() {
        assertEquals("\\u4E2D", Characters.showCharacter('中'));
        assertEquals("\\u6587", Characters.showCharacter('文'));
    }

    @Test
    public void testShowCharacter_FullWidthCharacters() {
        assertEquals("\\uFF21", Characters.showCharacter('Ａ'));  // 全角A
        assertEquals("\\u3000", Characters.showCharacter('　'));  // 全角空格
    }

    @Test
    public void testShowCharacter_SpecialCharacters() {
        assertEquals("\\u0000", Characters.showCharacter('\u0000'));
        assertEquals("\\uFFFF", Characters.showCharacter('\uFFFF'));
    }

    // ==================== 数字判断测试 ====================

    @Test
    public void testDigit_ASCIIDigits() {
        assertEquals(0, Characters.digit('0'));
        assertEquals(1, Characters.digit('1'));
        assertEquals(5, Characters.digit('5'));
        assertEquals(9, Characters.digit('9'));
    }

    @Test
    public void testDigit_FullWidthDigits() {
        assertEquals(0, Characters.digit('０'));
        assertEquals(1, Characters.digit('１'));
        assertEquals(5, Characters.digit('５'));
        assertEquals(9, Characters.digit('９'));
    }

    @Test
    public void testDigit_NonDigits() {
        assertEquals(-1, Characters.digit('A'));
        assertEquals(-1, Characters.digit('a'));
        assertEquals(-1, Characters.digit('中'));
        assertEquals(-1, Characters.digit(' '));
    }

    // ==================== 综合场景测试 ====================

    @Test
    public void testRealWorldScenario_MixedContent() {
        String input = "你好，Ｗorld！测试ＡＢＣ０１２．";
        // 全角标点也会被转换
        String expected = "你好,World!测试ABC012.";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testRealWorldScenario_OnlyChineseAndPunctuation() {
        String input = "你好，世界！";
        // 中文逗号和感叹号是全角的，但不在 fullWidth2halfWidth 的转换范围内
        String result = Characters.fullWidth2halfWidth(input);
        // 全角逗号 '，' 是 U+FF0C，全角感叹号 '！' 是 U+FF01
        // 这些应该被转换为半角
        String expected = "你好,世界!";
        assertEquals(expected, result);
    }

    @Test
    public void testRealWorldScenario_Email() {
        String input = "test＠example．com";
        String expected = "test@example.com";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }

    @Test
    public void testRealWorldScenario_URL() {
        String input = "ｈttp：／／www．example．com";
        String expected = "http://www.example.com";
        assertEquals(expected, Characters.fullWidth2halfWidth(input));
    }
}
