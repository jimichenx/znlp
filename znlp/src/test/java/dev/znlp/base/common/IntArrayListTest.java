package dev.znlp.base.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IntArrayList 的单元测试
 * <p>
 * 测试覆盖以下场景：
 * - 构造函数功能
 * - 基本的添加和获取操作
 * - 批量添加元素
 * - 修改元素
 * - 查找元素
 * - 移除元素
 * - 清空和大小操作
 * - 边界条件处理
 * - 异常情况处理
 * </p>
 */
public class IntArrayListTest {

    // ========== 构造函数测试 ==========

    @Test
    public void testConstructor_Default() {
        IntArrayList list = new IntArrayList();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructor_SpecifiedCapacity() {
        IntArrayList list = new IntArrayList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructor_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> {
            new IntArrayList(-1);
        });
    }

    @Test
    public void testConstructor_ZeroCapacity() {
        IntArrayList list = new IntArrayList(0);
        assertEquals(0, list.size());
        list.add(1); // 应该能够正常添加元素
        assertEquals(1, list.size());
    }

    // ========== 基本添加和获取操作测试 ==========

    @Test
    public void testAdd_SingleElement() {
        IntArrayList list = new IntArrayList();
        list.add(5);
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
        assertEquals(5, list.get(0));
    }

    @Test
    public void testAdd_MultipleElements() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testAdd_LargeNumberOfElements() {
        IntArrayList list = new IntArrayList(10);
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        assertEquals(count, list.size());
        for (int i = 0; i < count; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void testAdd_DynamicResizing() {
        IntArrayList list = new IntArrayList(2); // 初始容量为2
        list.add(1);
        list.add(2);
        list.add(3); // 应该自动扩容
        list.add(4);
        list.add(5);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testGet_IndexOutOfBounds() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);

        // 测试索引小于0
        try {
            list.get(-1);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 预期异常
        }

        // 测试索引等于大小
        try {
            list.get(2);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 预期异常
        }

        // 测试索引大于大小
        try {
            list.get(10);
            fail("应该抛出IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // 预期异常
        }
    }

    // ========== 批量添加功能测试 ==========

    @Test
    public void testAddAll_Array() {
        IntArrayList list = new IntArrayList();
        int[] elements = {1, 2, 3, 4, 5};
        list.addAll(elements);
        assertEquals(5, list.size());
        for (int i = 0; i < elements.length; i++) {
            assertEquals(elements[i], list.get(i));
        }
    }

    @Test
    public void testAddAll_EmptyArray() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        int[] elements = {};
        list.addAll(elements);
        assertEquals(1, list.size());
        assertEquals(1, list.get(0));
    }

    @Test
    public void testAddAll_NullArray() {
        IntArrayList list = new IntArrayList();
        assertThrows(NullPointerException.class, () -> {
            list.addAll((int[]) null);
        });
    }

    @Test
    public void testAddAll_IntArrayList() {
        IntArrayList source = new IntArrayList();
        source.add(1);
        source.add(2);
        source.add(3);

        IntArrayList target = new IntArrayList();
        target.add(0);
        target.addAll(source);

        assertEquals(4, target.size());
        assertEquals(0, target.get(0));
        assertEquals(1, target.get(1));
        assertEquals(2, target.get(2));
        assertEquals(3, target.get(3));
    }

    @Test
    public void testAddAll_EmptyIntArrayList() {
        IntArrayList source = new IntArrayList();
        IntArrayList target = new IntArrayList();
        target.add(1);
        target.addAll(source);
        assertEquals(1, target.size());
        assertEquals(1, target.get(0));
    }

    @Test
    public void testAddAll_NullIntArrayList() {
        IntArrayList list = new IntArrayList();
        assertThrows(NullPointerException.class, () -> {
            list.addAll((IntArrayList) null);
        });
    }

    // ========== 修改元素测试 ==========

    @Test
    public void testSet() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);

        int oldValue = list.set(1, 10);
        assertEquals(2, oldValue);
        assertEquals(10, list.get(1));
        assertEquals(3, list.size());
    }

    @Test
    public void testSet_IndexOutOfBounds() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            list.set(1, 10); // 索引超出范围
        });
    }

    @Test
    public void testSet_NegativeIndex() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            list.set(-1, 10); // 负索引
        });
    }

    // ========== 查找元素测试 ==========

    @Test
    public void testIndexOf() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(2);

        assertEquals(1, list.indexOf(2)); // 第一次出现的位置
        assertEquals(0, list.indexOf(1));
        assertEquals(2, list.indexOf(3));
        assertEquals(-1, list.indexOf(4)); // 不存在的元素
    }

    @Test
    public void testIndexOf_EmptyList() {
        IntArrayList list = new IntArrayList();
        assertEquals(-1, list.indexOf(1));
    }

    @Test
    public void testIndexOf_MultipleOccurrences() {
        IntArrayList list = new IntArrayList();
        list.add(5);
        list.add(5);
        list.add(5);
        assertEquals(0, list.indexOf(5)); // 只返回第一次出现的位置
    }

    // ========== 移除元素测试 ==========

    @Test
    public void testRemove() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);

        int removedValue = list.remove(1);
        assertEquals(2, removedValue);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    public void testRemove_IndexOutOfBounds() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            list.remove(1); // 索引超出范围
        });
    }

    @Test
    public void testRemove_NegativeIndex() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> {
            list.remove(-1); // 负索引
        });
    }

    @Test
    public void testRemove_FirstElement() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);

        int removedValue = list.remove(0);
        assertEquals(1, removedValue);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    public void testRemove_LastElement() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);

        int removedValue = list.remove(2);
        assertEquals(3, removedValue);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    public void testRemove_SingleElementList() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        int removedValue = list.remove(0);
        assertEquals(1, removedValue);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ========== 清空和大小操作测试 ==========

    @Test
    public void testSize() {
        IntArrayList list = new IntArrayList();
        assertEquals(0, list.size());
        list.add(1);
        assertEquals(1, list.size());
        list.add(2);
        assertEquals(2, list.size());
        list.clear();
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty() {
        IntArrayList list = new IntArrayList();
        assertTrue(list.isEmpty());
        list.add(1);
        assertFalse(list.isEmpty());
        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());

        // 清空后应该能够继续添加元素
        list.add(4);
        assertEquals(1, list.size());
        assertEquals(4, list.get(0));
    }

    @Test
    public void testClear_EmptyList() {
        IntArrayList list = new IntArrayList();
        list.clear(); // 应该没有副作用
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ========== 转换为数组测试 ==========

    @Test
    public void testToArray() {
        IntArrayList list = new IntArrayList();
        list.add(1);
        list.add(2);
        list.add(3);

        int[] array = list.toArray();
        assertEquals(3, array.length);
        assertArrayEquals(new int[]{1, 2, 3}, array);

        // 修改数组不应该影响列表
        array[0] = 10;
        assertEquals(1, list.get(0));
    }

    @Test
    public void testToArray_EmptyList() {
        IntArrayList list = new IntArrayList();
        int[] array = list.toArray();
        assertEquals(0, array.length);
        assertArrayEquals(new int[0], array);
    }

    // ========== 边界条件测试 ==========

    @Test
    public void testLargeNumberOfElements() {
        IntArrayList list = new IntArrayList();
        int count = 10000;
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        assertEquals(count, list.size());
        for (int i = 0; i < count; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void testCapacityGrowth() {
        IntArrayList list = new IntArrayList(2);

        // 添加元素超过初始容量
        list.add(1);
        list.add(2);
        list.add(3); // 应该自动扩容
        list.add(4);
        list.add(5);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    // ========== 综合功能测试 ==========

    @Test
    public void testCombinedOperations() {
        IntArrayList list = new IntArrayList(5);

        // 添加元素
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size());

        // 修改元素
        list.set(1, 20);
        assertEquals(20, list.get(1));

        // 批量添加
        int[] moreElements = {4, 5, 6};
        list.addAll(moreElements);
        assertEquals(6, list.size());
        assertEquals(6, list.get(5));

        // 查找元素
        assertEquals(5, list.indexOf(6));

        // 移除元素
        list.remove(0);
        assertEquals(5, list.size());
        assertEquals(20, list.get(0));

        // 转换为数组
        int[] array = list.toArray();
        assertEquals(5, array.length);
        assertArrayEquals(new int[]{20, 3, 4, 5, 6}, array);

        // 清空列表
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }
}