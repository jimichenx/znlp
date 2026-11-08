package dev.znlp.base.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CharNormUtils 的单元测试
 * <p>
 * 测试覆盖以下场景：
 * - 单字符转换（有转换规则和无转换规则）
 * - 字符数组原地转换
 * - 字符串转换（包含转换字符和不包含转换字符）
 * - 空值和边界条件处理
 * - 性能特性（延迟复制）
 * - 异常情况处理
 * </p>
 *
 * @author jimichan
 */
public class CharNormUtilsTest {

    // ========== 单字符转换测试 ==========

    @Test
    public void testConvertChar_WithRule() {
        // 测试全角字符转半角（同时转小写）
        assertEquals('a', CharNormUtils.convert('Ａ'));
        assertEquals('b', CharNormUtils.convert('Ｂ'));
        assertEquals('0', CharNormUtils.convert('０'));
        assertEquals('1', CharNormUtils.convert('１'));
        assertEquals('2', CharNormUtils.convert('２'));
    }

    @Test
    public void testConvertChar_ChineseCharacters() {
        // 测试大写字母转小写
        assertEquals('a', CharNormUtils.convert('A'));
        assertEquals('b', CharNormUtils.convert('B'));
        assertEquals('z', CharNormUtils.convert('Z'));
    }

    @Test
    public void testConvertChar_Punctuation() {
        // 测试没有转换规则的标点符号，返回原字符
        assertEquals(',', CharNormUtils.convert(','));
        assertEquals('.', CharNormUtils.convert('.'));
        assertEquals(' ', CharNormUtils.convert(' '));
    }

    @Test
    public void testConvertChar_NoRule() {
        // 测试没有转换规则的字符，应该返回原字符
        assertEquals('中', CharNormUtils.convert('中'));
        assertEquals('文', CharNormUtils.convert('文'));
        assertEquals('@', CharNormUtils.convert('@'));
        assertEquals('#', CharNormUtils.convert('#'));
        assertEquals('!', CharNormUtils.convert('!'));
    }

    @Test
    public void testConvertChar_SpecialCharacters() {
        // 测试特殊字符
        assertEquals('\n', CharNormUtils.convert('\n'));
        assertEquals('\t', CharNormUtils.convert('\t'));
        assertEquals('\r', CharNormUtils.convert('\r'));
        assertEquals(' ', CharNormUtils.convert(' '));
    }

    // ========== 字符数组转换测试 ==========

    @Test
    public void testConvertCharArray_AllNeedConversion() {
        char[] chars = "ＡＢ０１２".toCharArray();
        CharNormUtils.convert(chars);
        assertArrayEquals("ab012".toCharArray(), chars);
    }

    @Test
    public void testConvertCharArray_PartialConversion() {
        char[] chars = "ＡbcＢde".toCharArray();
        CharNormUtils.convert(chars);
        assertArrayEquals("abcbde".toCharArray(), chars);
    }

    @Test
    public void testConvertCharArray_NoConversion() {
        char[] original = "xyz123中文".toCharArray();
        char[] chars = original.clone();
        CharNormUtils.convert(chars);
        assertArrayEquals(original, chars);
    }

    @Test
    public void testConvertCharArray_Empty() {
        char[] chars = new char[0];
        CharNormUtils.convert(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testConvertCharArray_SingleChar() {
        char[] chars = {'Ａ'};
        CharNormUtils.convert(chars);
        assertArrayEquals(new char[]{'a'}, chars);
    }

    @Test
    public void testConvertCharArray_UpperCaseToLowerCase() {
        char[] chars = "ABC".toCharArray();
        CharNormUtils.convert(chars);
        assertArrayEquals("abc".toCharArray(), chars);
    }

    @Test
    public void testConvertCharArray_Null() {
        assertThrows(NullPointerException.class, () -> {
            CharNormUtils.convert((char[]) null);
        });
    }

    @Test
    public void testConvertCharArray_ModifiesOriginal() {
        // 验证原地修改
        char[] chars = "ＡＢＣ".toCharArray();
        char[] reference = chars;
        CharNormUtils.convert(chars);

        // 应该是同一个数组对象
        assertSame(reference, chars);
        assertEquals('a', chars[0]);
    }

    // ========== 字符串转换测试 ==========

    @Test
    public void testConvertString_AllNeedConversion() {
        String result = CharNormUtils.convert("ＡＢ０１２");
        assertEquals("ab012", result);
    }

    @Test
    public void testConvertString_PartialConversion() {
        String result = CharNormUtils.convert("ＡbcＢde");
        assertEquals("abcbde", result);
    }

    @Test
    public void testConvertString_NoConversion() {
        String original = "xyz123中文";
        String result = CharNormUtils.convert(original);

        // 验证延迟复制：如果没有字符需要转换，返回原字符串引用
        assertSame(original, result);
        assertEquals("xyz123中文", result);
    }

    @Test
    public void testConvertString_Empty() {
        String original = "";
        String result = CharNormUtils.convert(original);
        assertSame(original, result);
        assertEquals("", result);
    }

    @Test
    public void testConvertString_SingleChar_WithConversion() {
        String result = CharNormUtils.convert("Ａ");
        assertEquals("a", result);
    }

    @Test
    public void testConvertString_SingleChar_NoConversion() {
        String original = "x";
        String result = CharNormUtils.convert(original);
        assertSame(original, result);
    }

    @Test
    public void testConvertString_UpperCaseToLowerCase() {
        String result = CharNormUtils.convert("ABC");
        assertEquals("abc", result);
    }

    @Test
    public void testConvertString_MixedContent() {
        String result = CharNormUtils.convert("HelloＡorldＢ０１２");
        assertEquals("helloaorldb012", result);
    }

    @Test
    public void testConvertString_ConversionAtStart() {
        // 转换字符在开头
        String result = CharNormUtils.convert("ＡbcdeF");
        assertEquals("abcdef", result);
    }

    @Test
    public void testConvertString_ConversionAtEnd() {
        // 转换字符在末尾
        String result = CharNormUtils.convert("xyzＡ");
        assertEquals("xyza", result);
    }

    @Test
    public void testConvertString_ConversionInMiddle() {
        // 转换字符在中间
        String result = CharNormUtils.convert("xyzＡ中文");
        assertEquals("xyza中文", result);
    }

    @Test
    public void testConvertString_MultipleConversionsSpread() {
        // 多个转换字符分散在字符串中
        String result = CharNormUtils.convert("xＡyＢz０d１e２");
        assertEquals("xaybz0d1e2", result);
    }

    @Test
    public void testConvertString_Null() {
        assertThrows(NullPointerException.class, () -> {
            CharNormUtils.convert((String) null);
        });
    }

    // ========== 性能和优化测试 ==========

    @Test
    public void testLazyCopy_ReturnsSameReferenceWhenNoConversionNeeded() {
        // 验证延迟复制优化：无需转换时返回同一引用
        String original = "xyz中文测试";
        String result = CharNormUtils.convert(original);

        assertSame(original, result, "应该返回相同的字符串引用");
    }

    @Test
    public void testLazyCopy_ReturnsNewStringWhenConversionNeeded() {
        // 需要转换时返回新字符串
        String original = "testＡstring";
        String result = CharNormUtils.convert(original);

        assertNotSame("应该返回新的字符串对象", original, result);
        assertEquals("testastring", result);
    }

    // ========== 边界条件测试 ==========

    @Test
    public void testLongString() {
        // 测试较长的字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Ａ");
        }
        String result = CharNormUtils.convert(sb.toString());

        assertEquals(1000, result.length());
        for (int i = 0; i < 1000; i++) {
            assertEquals('a', result.charAt(i));
        }
    }

    @Test
    public void testLongStringNoConversion() {
        // 测试较长的字符串且无需转换
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("x");
        }
        String original = sb.toString();
        String result = CharNormUtils.convert(original);

        assertSame(original, result, "长字符串无需转换时也应返回原引用");
    }

    @Test
    public void testOnlySpaces() {
        // 空格没有转换规则，返回原样
        String original = "   ";
        String result = CharNormUtils.convert(original);
        assertSame(original, result);
    }

    @Test
    public void testMixedSpaces() {
        // 空格没有转换规则
        String original = "     ";
        String result = CharNormUtils.convert(original);
        assertSame(original, result);
    }

    // ========== 实际应用场景测试 ==========

    @Test
    public void testRealWorldScenario_FullWidthToHalfWidth() {
        String input = "ＡＢＣ０１２";
        String expected = "abc012";
        assertEquals(expected, CharNormUtils.convert(input));
    }

    @Test
    public void testRealWorldScenario_UpperToLower() {
        String input = "HELLO WORLD";
        String expected = "hello world";
        assertEquals(expected, CharNormUtils.convert(input));
    }

    @Test
    public void testRealWorldScenario_MixedLanguage() {
        String input = "HelloＷorldＡＢＣ";
        String expected = "helloworldabc";
        assertEquals(expected, CharNormUtils.convert(input));
    }

    // ========== 一致性测试 ==========

    @Test
    public void testConsistency_CharVsString() {
        // 验证单字符转换和字符串转换的一致性
        char testChar = 'Ａ';
        char convertedChar = CharNormUtils.convert(testChar);
        String convertedString = CharNormUtils.convert(String.valueOf(testChar));

        assertEquals(convertedChar, convertedString.charAt(0));
    }

    @Test
    public void testConsistency_CharArrayVsString() {
        // 验证字符数组转换和字符串转换的一致性
        String original = "ＡＢＣ０１２test";

        char[] chars = original.toCharArray();
        CharNormUtils.convert(chars);
        String fromArray = new String(chars);

        String fromString = CharNormUtils.convert(original);

        assertEquals(fromArray, fromString);
    }

    @Test
    public void testIdempotent() {
        // 验证幂等性：多次转换结果应该相同
        String original = "ＡＢＣ０１２";
        String first = CharNormUtils.convert(original);
        String second = CharNormUtils.convert(first);
        String third = CharNormUtils.convert(second);

        assertEquals(first, second);
        assertEquals(second, third);
        assertSame(first, second, "已转换的字符串再次转换应返回原引用");
    }
}
