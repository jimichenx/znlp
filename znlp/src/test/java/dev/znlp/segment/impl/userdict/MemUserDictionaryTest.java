package dev.znlp.segment.impl.userdict;

import org.junit.jupiter.api.Test;
import dev.znlp.base.dat.StringIntDATMap;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemCustomDictionary 测试类
 * 测试内存版本自定义词典的功能
 *
 * @author jimichan
 */
public class MemUserDictionaryTest {

    @Test
    public void testMemCustomDictionaryCreation() {
        MemUserDictionary dictionary = new MemUserDictionary();
        assertNotNull(dictionary);
        assertNull(dictionary.getTrie()); // 新创建的词典为空
    }

    @Test
    public void testAddWordAndRebuild() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("测试词汇");
        dictionary.rebuild();

        assertNotNull(dictionary.getTrie());

        StringIntDATMap trie = dictionary.getTrie();
        assertTrue(trie.containsKey("测试词汇"));
    }

    @Test
    public void testRemoveWord() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("需要删除的词汇");
        dictionary.rebuild();

        assertTrue(dictionary.getTrie().containsKey("需要删除的词汇"));

        dictionary.removeWord("需要删除的词汇");
        dictionary.rebuild();

        assertNull(dictionary.getTrie()); // 词典为空时应该返回null
    }

    @Test
    public void testClear() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("词汇1");
        dictionary.addWord("词汇2");
        dictionary.rebuild();

        assertNotNull(dictionary.getTrie());
        assertTrue(dictionary.getTrie().containsKey("词汇1"));
        assertTrue(dictionary.getTrie().containsKey("词汇2"));

        dictionary.clear();
        dictionary.rebuild();

        assertNull(dictionary.getTrie());
    }

    @Test
    public void testConstructorWithExistingDictionary() {
        TreeMap<String, Integer> initialDict = new TreeMap<>();
        initialDict.put("初始化词汇1", 1000);
        initialDict.put("初始化词汇2", 1000);

        MemUserDictionary dictionary = new MemUserDictionary(initialDict);

        assertNotNull(dictionary.getTrie());
        assertTrue(dictionary.getTrie().containsKey("初始化词汇1"));
        assertTrue(dictionary.getTrie().containsKey("初始化词汇2"));
    }

    @Test
    public void testCharNormalization() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("测试123");
        dictionary.rebuild();

        // 测试包含规范化字符的匹配
        assertTrue(dictionary.getTrie().containsKey("测试123"));
    }

    @Test
    public void testAddWordsPublishesEveryWord() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("原有词");
        dictionary.addWords(List.of("批量甲", "批量乙"));

        assertTrue(dictionary.getTrie().containsKey("原有词"));
        assertTrue(dictionary.getTrie().containsKey("批量甲"));
        assertTrue(dictionary.getTrie().containsKey("批量乙"));
    }

    @Test
    public void testEmptyAddWordsLeavesTrieUntouched() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("原有词");
        StringIntDATMap published = dictionary.getTrie();

        dictionary.addWords(List.of());

        assertSame(published, dictionary.getTrie());
    }

    @Test
    public void testReplaceAllSwapsTheWholeTable() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("旧词");

        dictionary.replaceAll(Stream.of("新词甲", "新词乙"));

        assertFalse(dictionary.getTrie().containsKey("旧词"));
        assertTrue(dictionary.getTrie().containsKey("新词甲"));
        assertTrue(dictionary.getTrie().containsKey("新词乙"));
    }

    @Test
    public void testFailedReplaceAllKeepsPublishedTrie() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("旧词");
        StringIntDATMap published = dictionary.getTrie();

        assertThrows(NullPointerException.class, () -> dictionary.replaceAll(Stream.of("新词", null)));

        assertSame(published, dictionary.getTrie());
        assertTrue(dictionary.getTrie().containsKey("旧词"));
    }

    @Test
    public void testEmptyDictionary() {
        MemUserDictionary dictionary = new MemUserDictionary();
        assertNull(dictionary.getTrie());

        dictionary.rebuild();
        assertNull(dictionary.getTrie());
    }
}
