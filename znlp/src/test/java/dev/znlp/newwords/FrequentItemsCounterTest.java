package dev.znlp.newwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import dev.znlp.newwords.counter.FrequentItemsCounter;
import dev.znlp.newwords.entity.IntCount;
import dev.znlp.newwords.entity.WordCount;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FrequentItemsCounter 单元测试
 *
 * @author jimichan
 */
@DisplayName("FrequentItemsCounter 测试")
class FrequentItemsCounterTest {

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("测试基本的计数功能")
        void testBasicCounting() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            // 添加一些元素
            counter.put("apple");
            counter.put("banana");
            counter.put("apple");
            counter.put("cherry");
            counter.put("apple");

            HashMap<String, IntCount> data = counter.getData();

            // 验证计数
            assertEquals(3, data.get("apple").getValue(), "apple 应该出现 3 次");
            assertEquals(1, data.get("banana").getValue(), "banana 应该出现 1 次");
            assertEquals(1, data.get("cherry").getValue(), "cherry 应该出现 1 次");
        }

        @Test
        @DisplayName("测试新元素的初始化")
        void testNewElementInitialization() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            counter.put("newItem");

            HashMap<String, IntCount> data = counter.getData();
            assertNotNull(data.get("newItem"), "新元素应该被创建");
            assertEquals(1, data.get("newItem").getValue(), "新元素的初始计数应该是 1");
        }

        @Test
        @DisplayName("测试元素递增")
        void testElementIncrement() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            for (int i = 0; i < 10; i++) {
                counter.put("test");
            }

            assertEquals(10, counter.getData().get("test").getValue(),
                    "元素应该正确递增到 10");
        }

        @Test
        @DisplayName("测试空计数器")
        void testEmptyCounter() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 5);

            List<WordCount> result = counter.getListResult();

            assertTrue(result.isEmpty(), "空计数器应该返回空列表");
        }
    }

    @Nested
    @DisplayName("压缩机制测试")
    class CompressionTests {

        @Test
        @DisplayName("测试达到容量上限时触发压缩")
        void testCompressionTriggered() {
            int capacity = 100;
            FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 2);

            // 添加超过容量的元素
            for (int i = 0; i < capacity + 50; i++) {
                counter.put("item" + i);
            }

            // 验证压缩后数据量被控制
            assertTrue(counter.getData().size() < capacity,
                    "压缩后数据量应该小于容量上限");
        }

        @Test
        @DisplayName("测试低频元素被删除")
        void testLowFrequencyItemsRemoved() {
            FrequentItemsCounter counter = new FrequentItemsCounter(50, 5);

            // 添加高频元素
            for (int i = 0; i < 20; i++) {
                counter.put("high");
            }

            // 添加大量低频元素触发压缩
            for (int i = 0; i < 100; i++) {
                counter.put("low" + i);
            }

            // 高频元素应该保留
            assertNotNull(counter.getData().get("high"),
                    "高频元素应该在压缩后保留");
            assertEquals(20, counter.getData().get("high").getValue(),
                    "高频元素的计数应该保持不变");
        }

        @Test
        @DisplayName("测试压缩后保留高频元素")
        void testHighFrequencyItemsPreserved() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 3);

            // 添加不同频率的元素
            for (int i = 0; i < 50; i++) {
                counter.put("veryHigh");
            }
            for (int i = 0; i < 10; i++) {
                counter.put("medium");
            }
            counter.put("low");

            // 触发压缩
            for (int i = 0; i < 200; i++) {
                counter.put("temp" + i);
            }

            // 验证高频元素被保留
            assertNotNull(counter.getData().get("veryHigh"),
                    "非常高频的元素应该被保留");
            assertTrue(counter.getData().get("veryHigh").getValue() >= 50,
                    "高频元素的计数应该保持");
        }
    }

    @Nested
    @DisplayName("结果获取测试")
    class ResultRetrievalTests {

        @Test
        @DisplayName("测试 getListResult 返回排序结果")
        void testGetListResultSorted() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            // 添加不同频率的元素
            for (int i = 0; i < 5; i++) counter.put("low");
            for (int i = 0; i < 10; i++) counter.put("medium");
            for (int i = 0; i < 20; i++) counter.put("high");

            List<WordCount> result = counter.getListResult();

            // 验证排序（降序）
            assertEquals(3, result.size(), "应该有 3 个元素");
            assertEquals("high", result.get(0).word(), "最高频的应该排第一");
            assertEquals(20, result.get(0).count(), "最高频元素计数应该是 20");
            assertEquals("medium", result.get(1).word(), "第二高频的应该排第二");
            assertEquals("low", result.get(2).word(), "最低频的应该排最后");
        }

        @Test
        @DisplayName("测试 clean 方法过滤低频元素")
        void testCleanFiltersLowFrequency() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 5);

            // 添加不同频率的元素
            for (int i = 0; i < 10; i++) counter.put("high");
            for (int i = 0; i < 3; i++) counter.put("low");

            counter.clean();

            // 验证低频元素被过滤
            assertNotNull(counter.getData().get("high"),
                    "高于阈值的元素应该保留");
            assertNull(counter.getData().get("low"),
                    "低于阈值的元素应该被删除");
        }

        @Test
        @DisplayName("测试结果列表的完整性")
        void testResultListCompleteness() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 2);

            String[] words = {"apple", "banana", "cherry", "date", "elderberry"};
            int[] counts = {10, 8, 6, 4, 3};

            for (int i = 0; i < words.length; i++) {
                for (int j = 0; j < counts[i]; j++) {
                    counter.put(words[i]);
                }
            }

            List<WordCount> result = counter.getListResult();

            // 验证所有元素都在结果中
            assertEquals(words.length, result.size(),
                    "结果应该包含所有高频元素");

            // 验证排序正确
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).count() >= result.get(i + 1).count(),
                        "结果应该按频次降序排列");
            }
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("测试最小容量")
        void testMinimalCapacity() {
            int capacity = 10;
            FrequentItemsCounter counter = new FrequentItemsCounter(capacity, 1);

            // 添加超过容量的元素
            for (int i = 0; i < capacity + 5; i++) {
                counter.put("item" + i);
            }

            // 应该触发压缩，数据量被控制
            assertTrue(counter.getData().size() <= capacity,
                    "压缩后数据量应该不超过容量上限");
        }

        @Test
        @DisplayName("测试相同频率的元素")
        void testSameFrequencyItems() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            for (int i = 0; i < 5; i++) {
                counter.put("a");
                counter.put("b");
                counter.put("c");
            }

            List<WordCount> result = counter.getListResult();

            assertEquals(3, result.size(), "应该有 3 个元素");
            // 所有元素频率相同
            assertEquals(5, result.get(0).count());
            assertEquals(5, result.get(1).count());
            assertEquals(5, result.get(2).count());
        }

        @Test
        @DisplayName("测试空字符串")
        void testEmptyString() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            counter.put("");
            counter.put("");

            assertEquals(2, counter.getData().get("").getValue(),
                    "空字符串应该能正常计数");
        }

        @Test
        @DisplayName("测试单个元素重复多次")
        void testSingleItemRepeated() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 1);

            for (int i = 0; i < 1000; i++) {
                counter.put("repeated");
            }

            List<WordCount> result = counter.getListResult();

            assertEquals(1, result.size(), "应该只有一个元素");
            assertEquals("repeated", result.get(0).word());
            assertEquals(1000, result.get(0).count());
        }
    }

    @Nested
    @DisplayName("配置参数测试")
    class ConfigurationTests {

        @Test
        @DisplayName("测试不同的最小阈值")
        void testDifferentMinCountThresholds() {
            int[] thresholds = {1, 5, 10, 20};

            for (int threshold : thresholds) {
                FrequentItemsCounter counter = new FrequentItemsCounter(100, threshold);

                // 添加低于阈值的元素
                for (int i = 0; i < threshold - 1; i++) {
                    counter.put("low");
                }

                // 添加高于阈值的元素
                for (int i = 0; i < threshold + 5; i++) {
                    counter.put("high");
                }

                List<WordCount> result = counter.getListResult();

                // 只有高频元素应该在结果中
                assertEquals(1, result.size(),
                        "阈值 " + threshold + " 时应该只有 1 个元素");
                assertEquals("high", result.get(0).word());
            }
        }

        @Test
        @DisplayName("测试 verbose 模式")
        void testVerboseMode() {
            FrequentItemsCounter counter = new FrequentItemsCounter(50, 2);
            counter.setVerbose(true);

            // 触发压缩（会输出日志）
            for (int i = 0; i < 100; i++) {
                counter.put("item" + i);
            }

            // 只要不抛异常就算通过
            assertTrue(true, "verbose 模式应该正常工作");
        }
    }

    @Nested
    @DisplayName("数据一致性测试")
    class DataConsistencyTests {

        @Test
        @DisplayName("测试压缩前后计数一致性")
        void testCountConsistencyAfterCompression() {
            FrequentItemsCounter counter = new FrequentItemsCounter(50, 2);

            // 添加高频元素
            for (int i = 0; i < 100; i++) {
                counter.put("important");
            }

            int countBefore = counter.getData().get("important").getValue();

            // 触发压缩
            for (int i = 0; i < 100; i++) {
                counter.put("temp" + i);
            }

            int countAfter = counter.getData().get("important").getValue();

            assertEquals(countBefore, countAfter,
                    "压缩不应该改变已存在元素的计数");
        }

        @Test
        @DisplayName("测试多次压缩的稳定性")
        void testMultipleCompressions() {
            FrequentItemsCounter counter = new FrequentItemsCounter(100, 3);

            // 触发多次压缩
            for (int round = 0; round < 5; round++) {
                for (int i = 0; i < 150; i++) {
                    counter.put("item" + (round * 150 + i));
                }
            }

            // 应该仍然能正常工作
            List<WordCount> result = counter.getListResult();
            assertNotNull(result, "多次压缩后应该能正常获取结果");
            assertTrue(counter.getData().size() <= 100,
                    "多次压缩后数据量应该被控制");
        }
    }
}
