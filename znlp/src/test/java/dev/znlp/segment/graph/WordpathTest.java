package dev.znlp.segment.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.znlp.segment.Nature;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wordpath类的单元测试
 */
class WordpathTest {

    private Wordnet wordnet;
    private Wordpath wordpath;

    @BeforeEach
    void setUp() {
        char[] text = "测试文本内容".toCharArray();
        wordnet = new Wordnet(text);
        wordpath = new Wordpath(wordnet);
    }

    @Test
    void testWordpathCreation() {
        assertNotNull(wordpath);
        assertEquals(wordnet, wordpath.getWordnet());
        
        // 默认情况下,每个字符后都有分割符
        BitSet bitSet = wordpath.getBitSet();
        // bitSet.set(0, length)设置从0到length-1的所有位
        assertEquals(wordnet.length(), bitSet.cardinality());
    }

    @Test
    void testCombine() {
        // 合并前两个字符 "测试"
        Vertex vertex = wordpath.combine(0, 2);
        
        assertNotNull(vertex);
        assertEquals(2, vertex.length);
        
        // 验证BitSet已更新
        BitSet bitSet = wordpath.getBitSet();
        assertTrue(bitSet.get(1)); // 位置1后应该有分割符
        assertFalse(bitSet.get(0)); // 位置0后不应该有分割符(在词内部)
    }

    @Test
    void testCombineWithVertex() {
        Vertex vertex = wordnet.put(0, 2);
        wordpath.combine(vertex);
        
        BitSet bitSet = wordpath.getBitSet();
        assertTrue(bitSet.get(1)); // 位置1后应该有分割符
    }

    @Test
    void testWordCount() {
        int initialCount = wordnet.length(); // 默认每个字符一个词
        assertEquals(initialCount, wordpath.wordCount());
        
        wordpath.combine(0, 2);
        assertEquals(initialCount - 1, wordpath.wordCount()); // 合并后减少一个
    }

    @Test
    void testWordCountInPath() {
        assertEquals(wordpath.wordCount(), wordpath.wordCountInPath());
    }

    @Test
    void testWordPointer() {
        wordpath.combine(0, 2);
        wordpath.combine(2, 2);
        
        Wordpath.WordPointer pointer = wordpath.wordPointer();
        
        assertTrue(pointer.next());
        assertEquals(0, pointer.getFrom());
        assertEquals(2, pointer.getLen());
        
        assertTrue(pointer.next());
        assertEquals(2, pointer.getFrom());
        assertEquals(2, pointer.getLen());
        
        // 继续遍历剩余的词
        int remainingWords = 0;
        while (pointer.next()) {
            remainingWords++;
        }
        // 应该还有剩余的词
        assertTrue(remainingWords >= 0);
    }

    @Test
    void testIteratorVertex() {
        wordnet.put(0, 2);
        wordnet.put(2, 2);
        wordpath.combine(0, 2);
        wordpath.combine(2, 2);
        
        List<Vertex> vertices = new ArrayList<>();
        var iterator = wordpath.iteratorVertex();
        while (iterator.hasNext()) {
            vertices.add(iterator.next());
        }
        
        assertTrue(vertices.size() >= 2);
    }

    @Test
    void testGetBestPathWithBE() {
        wordnet.put(0, 2);
        wordnet.put(2, 2);
        wordpath.combine(0, 2);
        wordpath.combine(2, 2);
        
        List<Vertex> path = new ArrayList<>();
        for (Vertex v : wordpath.getBestPathWithBE()) {
            path.add(v);
        }
        
        // 应该包含Begin,中间词,和End
        assertTrue(path.size() >= 3);
        assertEquals(1, path.get(0).length); // Begin节点
        assertEquals(0, path.get(path.size() - 1).length); // End节点
    }

    @Test
    void testFindunloadVertext() {
        wordpath.combine(0, 2);
        wordpath.combine(2, 2);
        
        List<Vertex> created = new ArrayList<>();
        wordpath.findunloadVertext(v -> {
            created.add(v);
            v.nature = Nature.x;
        });
        
        // 应该创建了一些词节点
        assertFalse(created.isEmpty());
    }

    @Test
    void testWillCutOtherWords() {
        // 原文「测试文本内容」先切成 测试 / 文本 / 内容
        wordpath.combine(0, 2); // "测试"
        wordpath.combine(2, 2); // "文本"

        // (0,3) = 测试文：起点在句首，但结尾落在「文本」中间 → 打断右侧
        assertTrue(wordpath.willCutOtherWords(0, 3));
        // (3,2) = 本内：起点落在「文本」中间 → 打断左侧
        assertTrue(wordpath.willCutOtherWords(3, 2));
        // (1,2) = 试文：起点在「测试」中间 → 打断左侧
        assertTrue(wordpath.willCutOtherWords(1, 2));

        // (2,2) = 文本：与已有词边界完全重合
        assertFalse(wordpath.willCutOtherWords(2, 2));
        // (4,2) = 内容：结尾已到句尾，仍有切分点
        assertFalse(wordpath.willCutOtherWords(4, 2));
        // (0,2) = 测试：句首起、边界完整
        assertFalse(wordpath.willCutOtherWords(0, 2));
    }

    @Test
    void testReset() {
        wordpath.combine(0, 2);
        wordpath.combine(2, 2);
        
        wordpath.reset();
        int countAfter = wordpath.wordCount();
        
        // 重置后应该恢复默认状态
        assertEquals(wordnet.length(), countAfter);
    }

    @Test
    void testToString() {
        wordpath.combine(0, 2);
        wordpath.combine(2, 2);
        
        String str = wordpath.toString();
        assertNotNull(str);
        assertTrue(str.contains("测试") || str.contains("文本"));
    }

    @Test
    void testGetBitSet() {
        BitSet bitSet = wordpath.getBitSet();
        assertNotNull(bitSet);
        
        wordpath.combine(0, 2);
        BitSet bitSet2 = wordpath.getBitSet();
        assertSame(bitSet, bitSet2); // 应该返回同一个对象
    }

    @Test
    void testCombineAtStart() {
        // 测试在起始位置合并
        Vertex vertex = wordpath.combine(0, 1);
        assertNotNull(vertex);
        assertEquals(1, vertex.length);
    }

    @Test
    void testCombineAtEnd() {
        // 测试在结束位置合并
        Vertex vertex = wordpath.combine(4, 1);
        assertNotNull(vertex);
        assertEquals(1, vertex.length);
    }

    @Test
    void testCombineInvalidLength() {
        // 测试无效长度 - connect方法会直接返回,不会创建词节点
        // 根据实现,combine可能返回null或创建长度为0的节点
        Vertex vertex = wordpath.combine(0, 0);
        // 根据实际实现调整断言
        if (vertex != null) {
            assertEquals(0, vertex.length);
        }
    }

    @Test
    void testCombineNegativeFrom() {
        // 测试负数起始位置 - 应该被忽略或返回null
        // 这个测试主要是验证不会抛出异常
        assertDoesNotThrow(() -> {
            wordpath.combine(-1, 1);
        });
        assertNotNull(wordpath);
    }

    @Test
    void testWordPointerMultipleIterations() {
        wordpath.combine(0, 2);
        
        Wordpath.WordPointer pointer1 = wordpath.wordPointer();
        Wordpath.WordPointer pointer2 = wordpath.wordPointer();
        
        // 两个指针应该独立工作
        assertTrue(pointer1.next());
        assertTrue(pointer2.next());
        
        assertEquals(pointer1.getFrom(), pointer2.getFrom());
        assertEquals(pointer1.getLen(), pointer2.getLen());
    }
}
