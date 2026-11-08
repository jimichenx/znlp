package dev.znlp.segment.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VertexRow类的单元测试
 */
class VertexRowTest {

    private Wordnet wordnet;
    private VertexRow vertexRow;

    @BeforeEach
    void setUp() {
        char[] text = "测试文本内容".toCharArray();
        wordnet = new Wordnet(text);
        vertexRow = wordnet.getRow(0);
    }

    @Test
    void testSubString() {
        String result = vertexRow.subString(2);
        assertEquals("测试", result);
        
        // 测试起始行
        VertexRow beginRow = wordnet.getBeginRow();
        assertEquals("", beginRow.subString(1));
    }

    @Test
    void testGetOrCreate() {
        Vertex vertex = vertexRow.getOrCrete(2);
        assertNotNull(vertex);
        assertEquals(2, vertex.length);
        
        // 再次获取应该返回同一个节点
        Vertex vertex2 = vertexRow.getOrCrete(2);
        assertSame(vertex, vertex2);
    }

    @Test
    void testClear() {
        vertexRow.put(new Vertex(1));
        vertexRow.put(new Vertex(2));
        assertFalse(vertexRow.isEmpty());
        
        vertexRow.clear();
        assertTrue(vertexRow.isEmpty());
        assertEquals(0, vertexRow.size());
    }

    @Test
    void testGet() {
        Vertex vertex1 = new Vertex(1);
        Vertex vertex2 = new Vertex(2);
        vertexRow.put(vertex1);
        vertexRow.put(vertex2);
        
        assertEquals(vertex1, vertexRow.get(1));
        assertEquals(vertex2, vertexRow.get(2));
        assertNull(vertexRow.get(3));
    }

    @Test
    void testGetEmptyRow() {
        assertNull(vertexRow.get(1));
    }

    @Test
    void testGetSingleNode() {
        Vertex vertex = new Vertex(2);
        vertexRow.put(vertex);
        assertEquals(vertex, vertexRow.get(2));
        assertNull(vertexRow.get(1));
    }

    @Test
    void testRemove() {
        Vertex vertex = new Vertex(2);
        vertexRow.put(vertex);
        assertTrue(vertexRow.contains(2));
        
        Vertex removed = vertexRow.remove((short) 2);
        assertSame(vertex, removed);
        assertFalse(vertexRow.contains(2));
    }

    @Test
    void testRemoveNonExistent() {
        assertNull(vertexRow.remove((short) 5));
    }

    @Test
    void testPut() {
        Vertex vertex = new Vertex(2);
        Vertex result = vertexRow.put(vertex);
        
        assertNull(result); // 第一次插入应该返回null
        assertTrue(vertexRow.contains(2));
        assertEquals(vertexRow, vertex.getVertexRow());
    }

    @Test
    void testPutReplace() {
        Vertex vertex1 = new Vertex(2);
        Vertex vertex2 = new Vertex(2);
        vertexRow.put(vertex1);
        
        Vertex replaced = vertexRow.put(vertex2);
        assertSame(vertex1, replaced);
        assertEquals(vertex2, vertexRow.get(2));
    }

    @Test
    void testPutOrdering() {
        // 测试插入顺序: 应该按长度从小到大排序
        vertexRow.put(new Vertex(3));
        vertexRow.put(new Vertex(1));
        vertexRow.put(new Vertex(2));
        
        List<Integer> lengths = new ArrayList<>();
        for (Vertex v : vertexRow) {
            lengths.add(v.length);
        }
        
        assertEquals(List.of(1, 2, 3), lengths);
    }

    @Test
    void testIsEmpty() {
        assertTrue(vertexRow.isEmpty());
        vertexRow.put(new Vertex(1));
        assertFalse(vertexRow.isEmpty());
    }

    @Test
    void testContains() {
        assertFalse(vertexRow.contains(2));
        vertexRow.put(new Vertex(2));
        assertTrue(vertexRow.contains(2));
    }

    @Test
    void testSize() {
        assertEquals(0, vertexRow.size());
        vertexRow.put(new Vertex(1));
        assertEquals(1, vertexRow.size());
        vertexRow.put(new Vertex(2));
        assertEquals(2, vertexRow.size());
    }

    @Test
    void testKeys() {
        vertexRow.put(new Vertex(2));
        vertexRow.put(new Vertex(1));
        vertexRow.put(new Vertex(3));
        
        Set<Integer> keys = vertexRow.keys();
        assertEquals(3, keys.size());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
        assertTrue(keys.contains(3));
    }

    @Test
    void testLastLen() {
        assertEquals(0, vertexRow.lastLen());
        
        vertexRow.put(new Vertex(1));
        assertEquals(1, vertexRow.lastLen());
        
        vertexRow.put(new Vertex(3));
        assertEquals(3, vertexRow.lastLen());
    }

    @Test
    void testIterator() {
        vertexRow.put(new Vertex(1));
        vertexRow.put(new Vertex(2));
        vertexRow.put(new Vertex(3));
        
        List<Vertex> vertices = new ArrayList<>();
        for (Vertex v : vertexRow) {
            vertices.add(v);
        }
        
        assertEquals(3, vertices.size());
        assertEquals(1, vertices.get(0).length);
        assertEquals(2, vertices.get(1).length);
        assertEquals(3, vertices.get(2).length);
    }

    @Test
    void testValues() {
        Vertex v1 = new Vertex(1);
        Vertex v2 = new Vertex(2);
        vertexRow.put(v1);
        vertexRow.put(v2);
        
        List<Vertex> values = vertexRow.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(v1));
        assertTrue(values.contains(v2));
    }

    @Test
    void testGetFirst() {
        assertNull(vertexRow.getFirst());
        
        Vertex vertex = new Vertex(2);
        vertexRow.put(vertex);
        assertEquals(vertex, vertexRow.getFirst());
    }

    @Test
    void testFirst() {
        Vertex vertex = new Vertex(1);
        vertexRow.put(vertex);
        assertEquals(vertex, vertexRow.first());
    }

    @Test
    void testTheChar() {
        char ch = vertexRow.theChar();
        assertEquals('测', ch);
    }

    @Test
    void testGetRowNum() {
        assertEquals(0, vertexRow.getRowNum());
        
        VertexRow row2 = wordnet.getRow(2);
        assertEquals(2, row2.getRowNum());
    }

    @Test
    void testIsNotEmpty() {
        assertFalse(vertexRow.isNotEmpty());
        vertexRow.put(new Vertex(1));
        assertTrue(vertexRow.isNotEmpty());
    }

    @Test
    void testToString() {
        vertexRow.put(new Vertex(1));
        vertexRow.put(new Vertex(2));
        String str = vertexRow.toString();
        assertTrue(str.contains("Slot"));
        assertTrue(str.contains("0"));
    }

    @Test
    void testArrayOrder() {
        Vertex v1 = new Vertex(1);
        Vertex v2 = new Vertex(2);
        Vertex v3 = new Vertex(3);

        vertexRow.put(v3);
        vertexRow.put(v1);
        vertexRow.put(v2);

        assertEquals(3, vertexRow.size());
        assertSame(v1, vertexRow.vertexAt(0));
        assertSame(v2, vertexRow.vertexAt(1));
        assertSame(v3, vertexRow.vertexAt(2));
        assertSame(v1, vertexRow.first());
    }
}
