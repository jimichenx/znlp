package dev.znlp.base.io;

import org.junit.jupiter.api.Test;
import dev.znlp.base.common.DataInOutputUtils;
import dev.znlp.base.common.DataInputWrapper;
import dev.znlp.base.common.DataOutputWrapper;

import java.io.*;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataInOutputUtils 工具类的测试
 *
 * @author jimichan
 */
class DataInOutputUtilsTest {

    @Test
    void testWriteAndReadFloatArray() throws IOException {
        // 测试普通 float 数组
        float[] original = {1.5f, 2.7f, 3.14159f, -4.2f, 0.0f, Float.MAX_VALUE, Float.MIN_VALUE};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeFloatArray(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        float[] result = inputWrapper.readFloatArray();

        // 验证
        assertNotNull(result);
        assertEquals(original.length, result.length);
        assertArrayEquals(original, result, 0.0001f);
    }

    @Test
    void testWriteAndReadNullFloatArray() throws IOException {
        // 测试 null 数组
        float[] original = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeFloatArray(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        float[] result = inputWrapper.readFloatArray();

        // 验证
        assertNull(result);
    }

    @Test
    void testWriteAndReadEmptyFloatArray() throws IOException {
        // 测试空数组
        float[] original = new float[0];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeFloatArray(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        float[] result = inputWrapper.readFloatArray();

        // 验证
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testWriteAndReadLargeFloatArray() throws IOException {
        // 测试大数组（超过分页大小）
        int size = 10000;
        float[] original = new float[size];
        for (int i = 0; i < size; i++) {
            original[i] = i * 0.123f;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeFloatArray(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        float[] result = inputWrapper.readFloatArray();

        // 验证
        assertNotNull(result);
        assertEquals(original.length, result.length);
        assertArrayEquals(original, result, 0.0001f);
    }

    @Test
    void testWriteAndReadFloatArrayWithSpecialValues() throws IOException {
        // 测试特殊值
        float[] original = {
                Float.NaN,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                0.0f,
                -0.0f,
                Float.MIN_VALUE,
                Float.MAX_VALUE,
                Float.MIN_NORMAL
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeFloatArray(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        float[] result = inputWrapper.readFloatArray();

        // 验证
        assertNotNull(result);
        assertEquals(original.length, result.length);
        for (int i = 0; i < original.length; i++) {
            if (Float.isNaN(original[i])) {
                assertTrue(Float.isNaN(result[i]), "Index " + i + " should be NaN");
            } else {
                assertEquals(original[i], result[i], 0.0f, "Index " + i + " should match");
            }
        }
    }

    @Test
    void testDirectStaticMethods() throws IOException {
        // 测试直接调用静态方法
        float[] original = {1.1f, 2.2f, 3.3f};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataInOutputUtils.writeFloatArray(original, dos);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        float[] result = DataInOutputUtils.readFloatArray(dis);

        // 验证
        assertNotNull(result);
        assertArrayEquals(original, result, 0.0001f);
    }

    @Test
    void testWriteAndReadStringList() throws IOException {
        // 测试普通字符串列表
        ArrayList<String> original = new ArrayList<>();
        original.add("hello");
        original.add("world");
        original.add("test");
        original.add("string");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        assertEquals(original, result);
    }

    @Test
    void testWriteAndReadNullStringList() throws IOException {
        // 测试 null 列表
        ArrayList<String> original = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNull(result);
    }

    @Test
    void testWriteAndReadEmptyStringList() throws IOException {
        // 测试空列表
        ArrayList<String> original = new ArrayList<>();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    void testWriteAndReadStringListWithNullElements() throws IOException {
        // 测试包含 null 元素的列表
        ArrayList<String> original = new ArrayList<>();
        original.add("first");
        original.add(null);
        original.add("third");
        original.add(null);
        original.add("last");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        for (int i = 0; i < original.size(); i++) {
            if (original.get(i) == null) {
                assertNull(result.get(i), "Index " + i + " should be null");
            } else {
                assertEquals(original.get(i), result.get(i), "Index " + i + " should match");
            }
        }
    }

    @Test
    void testWriteAndReadStringListWithChinese() throws IOException {
        // 测试中文字符串（UTF-8 编码）
        ArrayList<String> original = new ArrayList<>();
        original.add("你好");
        original.add("世界");
        original.add("测试");
        original.add("字符串列表");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        assertEquals(original, result);
    }

    @Test
    void testWriteAndReadLargeStringList() throws IOException {
        // 测试大量字符串（测试分页功能）
        int size = 1000;
        ArrayList<String> original = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            original.add("string_" + i + "_" + "x".repeat(50)); // 每个字符串约 60 字节
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        assertEquals(original, result);
    }

    @Test
    void testWriteAndReadStringListWithSpecialCharacters() throws IOException {
        // 测试特殊字符
        ArrayList<String> original = new ArrayList<>();
        original.add("line1\nline2");
        original.add("tab\there");
        original.add("quote\"test");
        original.add("backslash\\test");
        original.add("unicode: \u4e2d\u6587");
        original.add("emoji: 😀🎉");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataOutputWrapper wrapper = new DataOutputWrapper(dos);
        wrapper.writeStringList(original);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        DataInputWrapper inputWrapper = new DataInputWrapper(dis);
        ArrayList<String> result = inputWrapper.readStringList();

        // 验证
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        assertEquals(original, result);
    }

    @Test
    void testDirectStaticMethodsStringList() throws IOException {
        // 测试直接调用静态方法
        ArrayList<String> original = new ArrayList<>();
        original.add("test1");
        original.add("test2");
        original.add("test3");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 写入
        DataInOutputUtils.writeStringList(original, dos);

        // 读取
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        ArrayList<String> result = DataInOutputUtils.readStringList(dis);

        // 验证
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        assertEquals(original, result);
    }
}
