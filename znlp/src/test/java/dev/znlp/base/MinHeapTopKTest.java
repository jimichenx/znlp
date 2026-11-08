package dev.znlp.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.znlp.base.common.MinHeapTopK;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TopMaxK 数据结构的单元测试。
 * <p>
 * 测试覆盖以下场景：
 * - 基本插入和结果获取
 * - 堆容量满的情况
 * - 元素替换和堆化
 * - 边界条件（k=1, k=大数值）
 * - 分数排序
 * - 异常情况处理
 */
public class MinHeapTopKTest {

    private MinHeapTopK<String> topK;
    private static final int K = 3;

    @BeforeEach
    public void setUp() {
        topK = new MinHeapTopK<>(K, String.class);
    }

    @Test
    public void testInitialization() {
        assertNotNull(topK);
        // 验证初始化时结果为空
        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvalidKZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MinHeapTopK<>(0, String.class);
        });
    }

    @Test
    public void testInvalidKNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MinHeapTopK<>(-1, String.class);
        });
    }

    @Test
    public void testSinglePush() {
        topK.push("item1", 0.5f);
        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();

        assertEquals(1, result.size());
        assertEquals("item1", result.get(0).first);
        assertEquals(0.5f, result.get(0).second, 0.001f);
    }

    @Test
    public void testMultiplePushBeforeFull() {
        topK.push("item1", 0.3f);
        topK.push("item2", 0.7f);
        topK.push("item3", 0.5f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证结果按分数降序排列
        assertEquals(0.7f, result.get(0).second, 0.001f);
        assertEquals(0.5f, result.get(1).second, 0.001f);
        assertEquals(0.3f, result.get(2).second, 0.001f);
    }

    @Test
    public void testKeepOnlyTopK() {
        // 堆容量为3，插入5个元素
        topK.push("item1", 0.1f);
        topK.push("item2", 0.9f);
        topK.push("item3", 0.5f);
        topK.push("item4", 0.3f);  // 0.3 > 0.1，应该替换
        topK.push("item5", 0.2f);  // 0.2 < 0.3，应该被忽略

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 结果应该是最大的3个：0.9, 0.5, 0.3（或0.4）
        assertEquals(0.9f, result.get(0).second, 0.001f);
        assertEquals(0.5f, result.get(1).second, 0.001f);
        assertEquals(0.3f, result.get(2).second, 0.001f);
    }

    @Test
    public void testIgnoreSmallerScoresWhenFull() {
        topK.push("item1", 0.9f);
        topK.push("item2", 0.7f);
        topK.push("item3", 0.5f);

        // 尝试插入比最小值更小的分数
        topK.push("item4", 0.3f);
        topK.push("item5", 0.2f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 应该保持原来的最大3个元素
        float[] scores = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            scores[i] = result.get(i).second;
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.5f, scores[0], 0.001f);
        assertEquals(0.7f, scores[1], 0.001f);
        assertEquals(0.9f, scores[2], 0.001f);
    }

    @Test
    public void testReplaceMinimumWhenFull() {
        topK.push("item1", 0.9f);
        topK.push("item2", 0.7f);
        topK.push("item3", 0.5f);  // 最小值

        // 插入比最小值更大的分数
        topK.push("item4", 0.8f);  // 应该替换0.5

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证包含0.9, 0.8, 0.7
        float[] scores = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            scores[i] = result.get(i).second;
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.7f, scores[0], 0.001f);
        assertEquals(0.8f, scores[1], 0.001f);
        assertEquals(0.9f, scores[2], 0.001f);
    }

    @Test
    public void testResultOrdering() {
        topK.push("item1", 0.1f);
        topK.push("item2", 0.9f);
        topK.push("item3", 0.5f);
        topK.push("item4", 0.7f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();

        // 验证分数严格递减
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).second >= result.get(i + 1).second,
                    "分数应该递减：" + result.get(i).second + " >= " + result.get(i + 1).second);
        }
    }

    @Test
    public void testEqualScores() {
        topK.push("item1", 0.5f);
        topK.push("item2", 0.5f);
        topK.push("item3", 0.5f);
        topK.push("item4", 0.6f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证结果包含正确的分数
        int count06 = 0, count05 = 0;
        for (MinHeapTopK.Pair<String, Float> p : result) {
            if (Math.abs(p.second - 0.6f) < 0.001f) {
                count06++;
            } else if (Math.abs(p.second - 0.5f) < 0.001f) {
                count05++;
            }
        }
        assertEquals(1, count06);
        assertEquals(2, count05);
    }

    @Test
    public void testWithK1() {
        MinHeapTopK<String> topK1 = new MinHeapTopK<>(1, String.class);
        topK1.push("item1", 0.5f);
        topK1.push("item2", 0.9f);
        topK1.push("item3", 0.7f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK1.result();
        assertEquals(1, result.size());
        assertEquals(0.9f, result.get(0).second, 0.001f);
        assertEquals("item2", result.get(0).first);
    }

    @Test
    public void testWithLargeK() {
        MinHeapTopK<String> topKLarge = new MinHeapTopK<>(100, String.class);

        for (int i = 0; i < 50; i++) {
            topKLarge.push("item" + i, i * 0.01f);
        }

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topKLarge.result();
        assertEquals(50, result.size());

        // 验证第一个元素是最大的
        assertEquals(0.49f, result.get(0).second, 0.001f);
    }

    @Test
    public void testNegativeAndZeroScores() {
        topK.push("item1", -0.5f);
        topK.push("item2", 0.0f);
        topK.push("item3", 0.5f);
        topK.push("item4", -1.0f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 应该保留最大的3个：0.5, 0.0, -0.5
        assertEquals(0.5f, result.get(0).second, 0.001f);
        assertEquals(0.0f, result.get(1).second, 0.001f);
        assertEquals(-0.5f, result.get(2).second, 0.001f);
    }

    @Test
    public void testExtremeFloatValues() {
        topK.push("item1", Float.MAX_VALUE);
        topK.push("item2", Float.MIN_VALUE);
        topK.push("item3", 0.0f);
        topK.push("item4", -Float.MAX_VALUE);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证最大值被保留
        assertEquals(Float.MAX_VALUE, result.get(0).second, 0.001f);
    }

    @Test
    public void testMultipleReplacements() {
        // 初始化堆：[0.1, 0.5, 0.9]
        topK.push("a", 0.1f);
        topK.push("b", 0.5f);
        topK.push("c", 0.9f);

        // 替换最小值（0.1）
        topK.push("d", 0.7f);  // 堆变为[0.5, 0.7, 0.9]

        // 再次替换最小值（0.5）
        topK.push("e", 0.8f);  // 堆变为[0.7, 0.8, 0.9]

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        assertEquals(3, result.size());

        float[] scores = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            scores[i] = result.get(i).second;
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.7f, scores[0], 0.001f);
        assertEquals(0.8f, scores[1], 0.001f);
        assertEquals(0.9f, scores[2], 0.001f);
    }

    @Test
    public void testMixedOperations() {
        // 阶段1：填充堆
        topK.push("item1", 0.3f);
        topK.push("item2", 0.7f);
        topK.push("item3", 0.5f);

        // 阶段2：查询结果
        ArrayList<MinHeapTopK.Pair<String, Float>> result1 = topK.result();
        assertEquals(3, result1.size());
        assertEquals(0.7f, result1.get(0).second, 0.001f);

        // 阶段3：继续插入（会触发替换）
        topK.push("item4", 0.6f);
        topK.push("item5", 0.4f);

        // 阶段4：再次查询
        ArrayList<MinHeapTopK.Pair<String, Float>> result2 = topK.result();
        assertEquals(3, result2.size());

        // 验证结果是最大的3个值
        float[] scores = new float[result2.size()];
        for (int i = 0; i < result2.size(); i++) {
            scores[i] = result2.get(i).second;
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.5f, scores[0], 0.001f);
        assertEquals(0.6f, scores[1], 0.001f);
        assertEquals(0.7f, scores[2], 0.001f);
    }

    @Test
    public void testWithIntegerIdentifiers() {
        MinHeapTopK<Integer> topKInt = new MinHeapTopK<>(3, Integer.class);

        topKInt.push(101, 0.1f);
        topKInt.push(102, 0.9f);
        topKInt.push(103, 0.5f);

        ArrayList<MinHeapTopK.Pair<Integer, Float>> result = topKInt.result();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(102), result.get(0).first);  // 最高分对应的id
    }

    @Test
    public void testWithCustomObjectIdentifiers() {
        MinHeapTopK<Item> topKItem = new MinHeapTopK<>(2, Item.class);
        Item item1 = new Item("A");
        Item item2 = new Item("B");
        Item item3 = new Item("C");

        topKItem.push(item1, 0.5f);
        topKItem.push(item2, 0.8f);
        topKItem.push(item3, 0.3f);

        ArrayList<MinHeapTopK.Pair<Item, Float>> result = topKItem.result();
        assertEquals(2, result.size());
        assertEquals("B", result.get(0).first.name);
    }

    @Test
    public void testPairStructure() {
        topK.push("key1", 0.9f);

        ArrayList<MinHeapTopK.Pair<String, Float>> result = topK.result();
        MinHeapTopK.Pair<String, Float> pair = result.get(0);

        assertNotNull(pair);
        assertEquals("key1", pair.first);
        assertEquals(0.9f, pair.second, 0.001f);
    }

    /**
     * 简单的测试辅助类
     */
    static class Item {
        String name;

        Item(String name) {
            this.name = name;
        }
    }
}
