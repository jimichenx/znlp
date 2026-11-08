package dev.znlp.segment.impl.userdict;

import org.junit.jupiter.api.Test;
import dev.znlp.base.dat.StringIntDATMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileCustomDictionary 测试类
 * 测试从文件加载自定义词典的功能
 *
 * @author jimichan
 */
public class FileUserDictionaryTest {

    @Test
    public void testFileCustomDictionaryLoading() throws IOException {
        // 创建临时测试文件
        File tempFile = File.createTempFile("custom-dict-test", ".txt");
        tempFile.deleteOnExit();

        // 写入测试数据
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("测试词汇1 1000\n");
            writer.write("测试词汇2 2000\n");
            writer.write("测试词汇3\n");
        }

        // 测试加载词典
        FileUserDictionary dictionary = new FileUserDictionary(tempFile, StandardCharsets.UTF_8);

        assertNotNull(dictionary.getTrie());

        StringIntDATMap trie = dictionary.getTrie();
        assertTrue(trie.containsKey("测试词汇1"));
        assertTrue(trie.containsKey("测试词汇2"));
        assertTrue(trie.containsKey("测试词汇3"));
    }

    @Test
    public void testFileNotFound() {
        File nonExistentFile = new File("non-existent-dictionary.txt");
        assertThrows(IOException.class, () -> {
            new FileUserDictionary(nonExistentFile, StandardCharsets.UTF_8);
        });
    }

    @Test
    public void testEmptyDictionaryFile() throws IOException {
        File tempFile = File.createTempFile("empty-dict-test", ".txt");
        tempFile.deleteOnExit();

        FileUserDictionary dictionary = new FileUserDictionary(tempFile, StandardCharsets.UTF_8);

        assertNull(dictionary.getTrie());
    }

    @Test
    public void testDictionaryFileWithComments() throws IOException {
        File tempFile = File.createTempFile("dict-with-comments", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("# 这是一个注释\n");
            writer.write("有效词汇1 1000\n");
            writer.write("   # 缩进的注释\n");
            writer.write("有效词汇2\n");
        }

        FileUserDictionary dictionary = new FileUserDictionary(tempFile, StandardCharsets.UTF_8);

        assertNotNull(dictionary.getTrie());

        StringIntDATMap trie = dictionary.getTrie();
        assertTrue(trie.containsKey("有效词汇1"));
        assertTrue(trie.containsKey("有效词汇2"));
        assertFalse(trie.containsKey("#"));
    }

    @Test
    public void testNormalization() throws IOException {
        File tempFile = File.createTempFile("dict-normalization", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("测试123 1000\n");
        }

        FileUserDictionary dictionary = new FileUserDictionary(tempFile, StandardCharsets.UTF_8);

        assertNotNull(dictionary.getTrie());

        StringIntDATMap trie = dictionary.getTrie();
        assertTrue(trie.containsKey("测试123"));
    }
}
