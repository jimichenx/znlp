package dev.znlp.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.znlp.base.common.MaxHeapTopK;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MaxHeapTopK 数据结构的单元测试。
 * <p>
 * 测试覆盖以下场景：
 * - 基本插入和结果获取
 * - 堆容量满的情况
 * - 元素替换和堆化
 * - 边界条件（k=1, k=大数值）
 * - 分数排序
 * - 异常情况处理
 */
public class MaxHeapTopKTest {

    private MaxHeapTopK topK;
    private static final int K = 3;

    @BeforeEach
    public void setUp() {
        topK = new MaxHeapTopK(K);
    }

    @Test
    public void testInitialization() {
        assertNotNull(topK);
        // 验证初始化时结果为空
        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvalidKZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MaxHeapTopK(0);
        });
    }

    @Test
    public void testInvalidKNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MaxHeapTopK(-1);
        });
    }

    @Test
    public void testSinglePush() {
        topK.push(1, 0.5f);
        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).first());
        assertEquals(0.5f, result.get(0).second(), 0.001f);
    }

    @Test
    public void testMultiplePushBeforeFull() {
        topK.push(1, 0.3f);
        topK.push(2, 0.7f);
        topK.push(3, 0.5f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证结果按分数升序排列
        assertEquals(0.3f, result.get(0).second(), 0.001f);
        assertEquals(0.5f, result.get(1).second(), 0.001f);
        assertEquals(0.7f, result.get(2).second(), 0.001f);
    }

    @Test
    public void testKeepOnlyTopK() {
        // 堆容量为3，插入5个元素
        topK.push(1, 0.9f);
        topK.push(2, 0.1f);
        topK.push(3, 0.5f);
        topK.push(4, 0.3f);  // 0.3 < 0.9，应该替换
        topK.push(5, 0.7f);  // 0.7 > 0.5，应该被忽略

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 结果应该是最小的3个：0.1, 0.3, 0.5
        assertEquals(0.1f, result.get(0).second(), 0.001f);
        assertEquals(0.3f, result.get(1).second(), 0.001f);
        assertEquals(0.5f, result.get(2).second(), 0.001f);
    }

    @Test
    public void testIgnoreLargerScoresWhenFull() {
        topK.push(1, 0.1f);
        topK.push(2, 0.3f);
        topK.push(3, 0.5f);

        // 尝试插入比最大值更大的分数
        topK.push(4, 0.7f);
        topK.push(5, 0.6f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 应该保持原来的最小3个元素
        float[] scores = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            scores[i] = result.get(i).second();
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.1f, scores[0], 0.001f);
        assertEquals(0.3f, scores[1], 0.001f);
        assertEquals(0.5f, scores[2], 0.001f);
    }

    @Test
    public void testReplaceMaximumWhenFull() {
        topK.push(1, 0.1f);
        topK.push(2, 0.3f);
        topK.push(3, 0.9f);  // 最大值

        // 插入比最大值更小的分数
        topK.push(4, 0.5f);  // 应该替换0.9

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证包含0.1, 0.3, 0.5
        float[] scores = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            scores[i] = result.get(i).second();
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.1f, scores[0], 0.001f);
        assertEquals(0.3f, scores[1], 0.001f);
        assertEquals(0.5f, scores[2], 0.001f);
    }

    @Test
    public void testResultOrdering() {
        topK.push(1, 0.9f);
        topK.push(2, 0.1f);
        topK.push(3, 0.5f);
        topK.push(4, 0.3f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();

        // 验证分数严格递增
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).second() <= result.get(i + 1).second(),
                    "分数应该递增：" + result.get(i).second() + " <= " + result.get(i + 1).second());
        }
    }

    @Test
    public void testEqualScores() {
        topK.push(1, 0.5f);
        topK.push(2, 0.5f);
        topK.push(3, 0.5f);
        topK.push(4, 0.3f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证结果包含正确的分数
        int count05 = 0, count03 = 0;
        for (MaxHeapTopK.Pair<Integer, Float> p : result) {
            if (Math.abs(p.second() - 0.5f) < 0.001f) {
                count05++;
            } else if (Math.abs(p.second() - 0.3f) < 0.001f) {
                count03++;
            }
        }
        // 由于是最大堆，0.3会替换掉其中一个0.5
        assertTrue(count05 >= 2);
        assertEquals(1, count03);
    }

    @Test
    public void testWithK1() {
        MaxHeapTopK topK1 = new MaxHeapTopK(1);
        topK1.push(1, 0.5f);
        topK1.push(2, 0.1f);
        topK1.push(3, 0.3f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK1.result();
        assertEquals(1, result.size());
        assertEquals(0.1f, result.get(0).second(), 0.001f);
        assertEquals(Integer.valueOf(2), result.get(0).first());
    }

    @Test
    public void testWithLargeK() {
        MaxHeapTopK topKLarge = new MaxHeapTopK(100);

        for (int i = 0; i < 50; i++) {
            topKLarge.push(i, i * 0.01f);
        }

        List<MaxHeapTopK.Pair<Integer, Float>> result = topKLarge.result();
        assertEquals(50, result.size());

        // 验证第一个元素是最小的
        assertEquals(0.00f, result.get(0).second(), 0.001f);
    }

    @Test
    public void testNegativeAndZeroScores() {
        topK.push(1, -0.5f);
        topK.push(2, 0.0f);
        topK.push(3, 0.5f);
        topK.push(4, -1.0f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 应该保留最小的3个：-1.0, -0.5, 0.0
        assertEquals(-1.0f, result.get(0).second(), 0.001f);
        assertEquals(-0.5f, result.get(1).second(), 0.001f);
        assertEquals(0.0f, result.get(2).second(), 0.001f);
    }

    @Test
    public void testExtremeFloatValues() {
        topK.push(1, Float.MAX_VALUE);
        topK.push(2, Float.MIN_VALUE);
        topK.push(3, 0.0f);
        topK.push(4, -Float.MAX_VALUE);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        // 验证最小值被保留
        assertEquals(-Float.MAX_VALUE, result.get(0).second(), 0.001f);
    }

    @Test
    public void testMultipleReplacements() {
        // 初始化堆：[0.9, 0.5, 0.1]
        topK.push(1, 0.9f);
        topK.push(2, 0.5f);
        topK.push(3, 0.1f);

        // 替换最大值（0.9）
        topK.push(4, 0.7f);  // 堆变为[0.7, 0.5, 0.1]

        // 再次替换最大值（0.7）
        topK.push(5, 0.3f);  // 堆变为[0.5, 0.3, 0.1]

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertEquals(3, result.size());

        float[] scores = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            scores[i] = result.get(i).second();
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.1f, scores[0], 0.001f);
        assertEquals(0.3f, scores[1], 0.001f);
        assertEquals(0.5f, scores[2], 0.001f);
    }

    @Test
    public void testMixedOperations() {
        // 阶段1：填充堆
        topK.push(1, 0.3f);
        topK.push(2, 0.7f);
        topK.push(3, 0.5f);

        // 阶段2：查询结果
        List<MaxHeapTopK.Pair<Integer, Float>> result1 = topK.result();
        assertEquals(3, result1.size());
        assertEquals(0.3f, result1.get(0).second(), 0.001f);

        // 阶段3：继续插入（会触发替换）
        topK.push(4, 0.6f);
        topK.push(5, 0.4f);

        // 阶段4：再次查询
        List<MaxHeapTopK.Pair<Integer, Float>> result2 = topK.result();
        assertEquals(3, result2.size());

        // 验证结果是最小的3个值
        float[] scores = new float[result2.size()];
        for (int i = 0; i < result2.size(); i++) {
            scores[i] = result2.get(i).second();
        }
        java.util.Arrays.sort(scores);

        assertEquals(0.3f, scores[0], 0.001f);
        assertEquals(0.4f, scores[1], 0.001f);
        assertEquals(0.5f, scores[2], 0.001f);
    }

    @Test
    public void testPairStructure() {
        topK.push(101, 0.9f);

        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        MaxHeapTopK.Pair<Integer, Float> pair = result.get(0);

        assertNotNull(pair);
        assertEquals(Integer.valueOf(101), pair.first());
        assertEquals(0.9f, pair.second(), 0.001f);
    }

    @Test
    public void testClear() {
        topK.push(1, 0.5f);
        topK.push(2, 0.2f);
        topK.clear();
        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertTrue(result.isEmpty());
        topK.push(3, 0.1f);
        result = topK.result();
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get(0).first());
        assertEquals(0.1f, result.get(0).second(), 0.001f);
    }

    @Test
    public void testReleaseAndReuse() {
        topK.push(1, 0.5f);
        topK.push(2, 0.2f);
        topK.release();
        List<MaxHeapTopK.Pair<Integer, Float>> result = topK.result();
        assertTrue(result.isEmpty());
        topK.push(3, 0.1f);
        result = topK.result();
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get(0).first());
        assertEquals(0.1f, result.get(0).second(), 0.001f);
    }

    @Test
    public void testPerformance() {
        int localK = 128;
        int n = 200_000;
        int rounds = 3;

        long total = 0L;
        for (int r = 0; r < rounds; r++) {
            MaxHeapTopK localTopK = new MaxHeapTopK(localK);
            Random random = new Random(123);

            long start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                localTopK.push(i, random.nextFloat());
            }
            localTopK.result();
            long end = System.nanoTime();
            total += (end - start);
        }

        long avg = total / rounds;
        System.out.println("MaxHeapTopK performance: N=" + n + ", K=" + localK + ", avgTimeNs=" + avg);
    }
}
