package dev.znlp.segment.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wordnet类的单元测试
 */
class WordnetTest {

    private char[] text;
    private Wordnet wordnet;

    @BeforeEach
    void setUp() {
        text = "测试文本内容".toCharArray();
        wordnet = new Wordnet(text);
    }

    @Test
    void testWordnetCreation() {
        assertNotNull(wordnet);
        assertEquals(text.length, wordnet.length());
        assertSame(text, wordnet.getCharArray());
        assertEquals(text.length, wordnet.getCharSizeLength());
    }

    @Test
    void testGetRow() {
        // 测试普通行
        VertexRow row0 = wordnet.getRow(0);
        assertNotNull(row0);
        assertEquals(0, row0.rowNum);
        
        // 测试起始行
        VertexRow beginRow = wordnet.getRow(-1);
        assertNotNull(beginRow);
        assertEquals(-1, beginRow.rowNum);
        
        // 测试结束行
        VertexRow endRow = wordnet.getRow(5);
        assertNotNull(endRow);
        assertEquals(5, endRow.rowNum);
    }

    @Test
    void testRow() {
        VertexRow row = wordnet.row(1);
        assertNotNull(row);
        assertEquals(1, row.rowNum);
    }

    @Test
    void testGetBeginRow() {
        VertexRow beginRow = wordnet.getBeginRow();
        assertNotNull(beginRow);
        assertEquals(-1, beginRow.rowNum);
        assertFalse(beginRow.isEmpty()); // 应该包含起始节点
    }

    @Test
    void testGetEndRow() {
        VertexRow endRow = wordnet.getEndRow();
        assertNotNull(endRow);
        assertEquals(text.length, endRow.rowNum);
        assertFalse(endRow.isEmpty()); // 应该包含结束节点
    }

    @Test
    void testPutVertex() {
        Vertex vertex = new Vertex(2);
        Vertex result = wordnet.put(0, vertex);
        
        assertNull(result); // 第一次插入
        assertEquals(vertex, wordnet.getVertex(0, 2));
    }

    @Test
    void testPutWithLength() {
        Vertex vertex = wordnet.put(0, 2);
        assertNotNull(vertex);
        assertEquals(2, vertex.length);
        assertEquals(wordnet.getRow(0), vertex.getVertexRow());
    }

    @Test
    void testGetVertex() {
        wordnet.put(0, 2);
        Vertex vertex = wordnet.getVertex(0, 2);
        assertNotNull(vertex);
        assertEquals(2, vertex.length);
    }

    @Test
    void testGetVertexNonExistent() {
        assertNull(wordnet.getVertex(0, 5));
    }

    @Test
    void testIsNotContains() {
        assertTrue(wordnet.isNotContains(0, 2));
        wordnet.put(0, 2);
        assertFalse(wordnet.isNotContains(0, 2));
    }

    @Test
    void testSizeInRow() {
        assertEquals(0, wordnet.sizeInRow(0));
        wordnet.put(0, 1);
        wordnet.put(0, 2);
        assertEquals(2, wordnet.sizeInRow(0));
    }

    @Test
    void testSize() {
        int initialSize = wordnet.size(); // 包含Begin和End节点
        
        wordnet.put(0, 1);
        wordnet.put(0, 2);
        wordnet.put(2, 2);
        
        assertTrue(wordnet.size() > initialSize);
    }

    @Test
    void testNotNullRowNums() {
        int initialCount = wordnet.notNullRowNums(); // 至少包含Begin和End
        
        wordnet.put(0, 1);
        wordnet.put(2, 2);
        
        assertTrue(wordnet.notNullRowNums() >= initialCount);
    }

    @Test
    void testCharAt() {
        assertEquals('测', wordnet.charAt(0));
        assertEquals('试', wordnet.charAt(1));
        assertEquals('文', wordnet.charAt(2));
    }

    @Test
    void testCharAtOutOfBounds() {
        assertEquals(' ', wordnet.charAt(-1));
        assertEquals(' ', wordnet.charAt(10));
    }

    @Test
    void testSubSequence() {
        CharSequence sub = wordnet.subSequence(0, 2);
        assertEquals("测试", sub.toString());
    }

    @Test
    void testAccessAllVertex() {
        wordnet.put(0, 1);
        wordnet.put(0, 2);
        wordnet.put(2, 2);
        
        List<Vertex> vertices = new ArrayList<>();
        wordnet.accessAllVertex(vertices::add);
        
        assertTrue(vertices.size() >= 3);
    }

    @Test
    void testFillNill() {
        // 添加一些词,但留一些空位
        char[] text2 = "测试".toCharArray();
        Wordnet wordnet2 = new Wordnet(text2);
        wordnet2.put(0, 2); // 只添加一个2长度的词,位置1会是空的
        
        wordnet2.fillNill();
        
        // 验证所有位置都有词
        assertFalse(wordnet2.getRow(1).isEmpty());
    }

    @Test
    void testFindNoOverWords() {
        wordnet.put(0, 2); // "测试"
        wordnet.put(2, 2); // "文本"
        
        BitSet noOverWords = wordnet.findNoOverWords();
        
        // 位置0,1,2,3应该被覆盖
        assertTrue(noOverWords.get(0));
        assertTrue(noOverWords.get(1));
        assertTrue(noOverWords.get(2));
        assertTrue(noOverWords.get(3));
    }

    @Test
    void testSubWordnet() {
        wordnet.put(0, 2);
        wordnet.put(2, 2);
        
        Wordnet subWordnet = wordnet.subWordnet(0, 2);
        assertNotNull(subWordnet);
        assertEquals(2, subWordnet.length());
        // 验证子词图包含正确的字符
        assertEquals('测', subWordnet.charAt(0));
        assertEquals('试', subWordnet.charAt(1));
    }

    @Test
    void testGetRowFirst() {
        wordnet.put(0, 1);
        wordnet.put(0, 2);
        
        Vertex first = wordnet.getRowFirst(0);
        assertNotNull(first);
        assertEquals(1, first.length); // 应该是最短的
    }

    @Test
    void testContext() {
        assertNull(wordnet.get("key1"));
        
        wordnet.set("key1", "value1");
        assertEquals("value1", wordnet.get("key1"));
        
        wordnet.set("key2", 123);
        Integer value = wordnet.get("key2");
        assertEquals(123, value.intValue());
    }

    @Test
    void testToString() {
        wordnet.put(0, 2);
        String str = wordnet.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    void testToMoreString() {
        wordnet.put(0, 2);
        String str = wordnet.toMoreString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    void testGetSlotList() {
        VertexRow[] slotList = wordnet.getSlotList();
        assertNotNull(slotList);
        assertEquals(text.length, slotList.length);
    }

    @Test
    void testCharSequenceInterface() {
        // 测试CharSequence接口的实现
        assertEquals(text.length, wordnet.length());
        assertEquals('测', wordnet.charAt(0));
        
        if (text.length >= 3) {
            CharSequence sub = wordnet.subSequence(1, 3);
            assertEquals("试文", sub.toString());
        }
    }
}
