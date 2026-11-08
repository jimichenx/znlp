package dev.znlp.base.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FastCharReader 类的单元测试
 * <p>
 * 测试覆盖以下场景：
 * - 基本字符读取功能
 * - 缓冲区管理
 * - 偏移量跟踪
 * - 流结束处理
 * - 重置功能
 * - 不同容量设置
 * - 边界条件（空字符串、单字符等）
 * </p>
 *
 * @author jimichan
 */
public class FastCharReaderTest {

    // ==================== 基本读取测试 ====================

    @Test
    public void testRead_SimpleString() throws IOException {
        String text = "Hello";
        FastCharReader reader = new FastCharReader(text);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
        assertEquals(text.length() - 1, reader.offset());
    }

    @Test
    public void testRead_EmptyString() throws IOException {
        FastCharReader reader = new FastCharReader("");
        assertEquals(-1, reader.read());
        assertEquals(-1, reader.offset());
    }

    @Test
    public void testRead_SingleChar() throws IOException {
        FastCharReader reader = new FastCharReader("A");
        assertEquals('A', reader.read());
        assertEquals(-1, reader.read());
        assertEquals(0, reader.offset());
    }

    @Test
    public void testRead_ChineseText() throws IOException {
        String text = "你好世界";
        FastCharReader reader = new FastCharReader(text);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    @Test
    public void testRead_LongText() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("A");
        }
        String text = sb.toString();

        FastCharReader reader = new FastCharReader(text);
        int count = 0;
        while (reader.read() != -1) {
            count++;
        }

        assertEquals(1000, count);
        assertEquals(999, reader.offset());
    }

    // ==================== 偏移量测试 ====================

    @Test
    public void testOffset_InitialState() {
        FastCharReader reader = new FastCharReader("test");
        assertEquals(-1, reader.offset());
    }

    @Test
    public void testOffset_AfterRead() throws IOException {
        FastCharReader reader = new FastCharReader("ABC");
        reader.read();
        assertEquals(0, reader.offset());
        reader.read();
        assertEquals(1, reader.offset());
        reader.read();
        assertEquals(2, reader.offset());
    }

    // ==================== 重置功能测试 ====================

    @Test
    public void testReset() throws IOException {
        FastCharReader reader = new FastCharReader("ABC");
        reader.read();
        reader.read();
        assertEquals(1, reader.offset());

        reader.reset(new StringReader("XYZ"));
        assertEquals(-1, reader.offset());

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }
        assertEquals("XYZ", result.toString());
    }

    @Test
    public void testReset_EmptyString() throws IOException {
        FastCharReader reader = new FastCharReader("ABC");
        reader.read();
        reader.reset(new StringReader(""));
        assertEquals(-1, reader.read());
    }

    // ==================== 容量设置测试 ====================

    @Test
    public void testCustomCapacity() throws IOException {
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        FastCharReader reader = new FastCharReader(new StringReader(text), 10);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    @Test
    public void testShortString_AutoCapacity() throws IOException {
        String text = "ABC";
        FastCharReader reader = new FastCharReader(text);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    @Test
    public void testMinCapacity() throws IOException {
        String text = "AB";
        FastCharReader reader = new FastCharReader(new StringReader(text), 1);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    // ==================== Reader 输入测试 ====================

    @Test
    public void testRead_FromReader() throws IOException {
        String text = "Hello World";
        FastCharReader reader = new FastCharReader(new StringReader(text));

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testRead_Newlines() throws IOException {
        String text = "A\nB\r\nC";
        FastCharReader reader = new FastCharReader(text);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    @Test
    public void testRead_SpecialCharacters() throws IOException {
        String text = "!@#$%^&*()";
        FastCharReader reader = new FastCharReader(text);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }

    @Test
    public void testRead_Unicode() throws IOException {
        String text = "αβγδε 中文 🚀";
        FastCharReader reader = new FastCharReader(text);

        StringBuilder result = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            result.append((char) ch);
        }

        assertEquals(text, result.toString());
    }
}
