package dev.znlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Znlp 门面类的基础功能测试。
 * <p>
 * 主要验证拼音相关静态方法的行为，包括正常转换结果和异常参数处理。
 */
public class ZnlpTest {

    /**
     * 测试 pinyinString 方法对 "复旦大学" 的拼音转换。
     */
    @Test
    void testPinyinStringForFudanUniversity() {
        String result = Znlp.pinyinString("复旦大学");

        assertNotNull(result, "拼音字符串不应为空");
        assertEquals("fu dan da xue", result, "复旦大学的拼音应为\"fu dan da xue\"");
    }

    /**
     * 测试 pinyinHeadString 方法对 "复旦大学" 的首字母拼音转换。
     */
    @Test
    void testPinyinHeadStringForFudanUniversity() {
        String result = Znlp.pinyinHeadString("复旦大学");

        assertNotNull(result, "拼音首字母字符串不应为空");
        assertEquals("f d d x", result, "复旦大学的首字母拼音应为\"f d d x\"");
    }

    /**
     * 测试 pinyin 方法在传入 null 时抛出 IllegalArgumentException。
     */
    @Test
    void testPinyinNullTextThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Znlp.pinyin(null));
    }

    @Test
    void mixedChineseAndAsciiPinyin() {
        String result = Znlp.pinyinString("你好znlp");
        assertNotNull(result);
        assertTrue(result.contains("ni"));
        assertTrue(result.contains("hao"));
        assertFalse(result.isBlank());
    }

    @Test
    void pinyinResultFormatting() {
        var result = Znlp.pinyin("复旦大学");
        assertEquals("fu dan da xue", result.asString());
        assertEquals("f d d x", result.asHeadString());
        assertEquals(4, result.asList().size());
    }
}
