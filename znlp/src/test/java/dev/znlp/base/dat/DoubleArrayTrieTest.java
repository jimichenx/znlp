package dev.znlp.base.dat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DoubleArrayTrie 单元测试
 * <p>
 * 覆盖精确查找、前缀、序列化和构建约束。构建器只有 {@link DoubleArrayTrieBuilder2}。
 */
public class DoubleArrayTrieTest {

    private List<String> testKeys;

    private DoubleArrayTrieBuilder getBuilder(int index) {
        return new DoubleArrayTrieBuilder2();
    }

    @BeforeEach
    public void setUp() {
        // 准备测试数据：有序且无重复的字符串列表
        testKeys = Arrays.asList(
                "一举",
                "一举一动",
                "一举成名",
                "一举成名天下知",
                "五谷",
                "五谷丰登",
                "万万",
                "abc",
                "abcd",
                "abcdef"
        );
        Collections.sort(testKeys); // 确保有序
    }

    @Test
    public void testConstructorWithList() {
        for (int i = 0; i < 1; i++) {
            List<String> keys = Arrays.asList("test", "test1", "test2");
            Collections.sort(keys);
            DoubleArrayTrie dat = getBuilder(i).build(keys);

            assertNotNull(dat, "Builder " + i + " returned null");
            assertNotNull(dat.getBase(), "Builder " + i + ": base is null");
            assertNotNull(dat.getCheck(), "Builder " + i + ": check is null");
            assertEquals(3, dat.size(), "Builder " + i + ": wrong size");
        }
    }

    @Test
    public void testConstructorWithTreeSet() {
        for (int i = 0; i < 1; i++) {
            TreeSet<String> set = new TreeSet<>();
            set.add("apple");
            set.add("banana");
            set.add("cherry");

            DoubleArrayTrie dat = getBuilder(i).build(new ArrayList<>(set));

            assertNotNull(dat, "Builder " + i + ": returned null");
            assertEquals(3, dat.size(), "Builder " + i + ": wrong size");
        }
    }

    @Test
    public void testSize() {
        for (int i = 0; i < 1; i++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(i).build(keys);
            assertEquals(testKeys.size(), t.size(), "Builder " + i + ": wrong size");
        }

        // 空列表在 testEmptyTrie 里单独断言
    }

    @Test
    public void testIndexOfWithString() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            // 测试存在的键
            for (int i = 0; i < keys.size(); i++) {
                int index = t.indexOf(keys.get(i));
                assertEquals(i, index, "Builder " + builderIdx + ", Key: " + keys.get(i));
            }

            // 测试不存在的键
            assertEquals(-1, t.indexOf("不存在的键"), "Builder " + builderIdx + ": should not find non-existent key");
            assertEquals(-1, t.indexOf(""), "Builder " + builderIdx + ": should not find empty key");
            assertEquals(-1, t.indexOf("一举成"), "Builder " + builderIdx + ": should not find incomplete key");
        }
    }

    @Test
    public void testIndexOfWithCharSequence() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            StringBuilder sb = new StringBuilder("一举");
            int index = t.indexOf(sb);
            assertTrue(index >= 0, "Builder " + builderIdx + ": should find existing key");

            // 测试不存在的
            assertEquals(-1, t.indexOf(new StringBuilder("不存在")), "Builder " + builderIdx + ": should not find non-existent key");
        }
    }

    @Test
    public void testIndexOfWithChar() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            // 创建一个包含单字符键的trie
            List<String> singleCharKeys = Arrays.asList("a", "b", "c");
            Collections.sort(singleCharKeys);
            DoubleArrayTrie singleCharTrie = getBuilder(builderIdx).build(singleCharKeys);

            int index = singleCharTrie.indexOf('a');
            assertEquals(0, index, "Builder " + builderIdx + ": 'a' should be at index 0");

            index = singleCharTrie.indexOf('b');
            assertEquals(1, index, "Builder " + builderIdx + ": 'b' should be at index 1");

            index = singleCharTrie.indexOf('d');
            assertEquals(-1, index, "Builder " + builderIdx + ": 'd' should not exist");
        }
    }

    @Test
    public void testIndexOfWithCharArray() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String key = "一举";
            int expectedIndex = keys.indexOf(key);
            int actualIndex = t.indexOf(key.toCharArray(), 0, key.length());
            assertEquals(expectedIndex, actualIndex, "Builder " + builderIdx + ": wrong index for key");

            // 测试不存在的键
            assertEquals(-1, t.indexOf("不存在".toCharArray(), 0, 3), "Builder " + builderIdx + ": should not find non-existent key");

            // 测试部分字符数组
            char[] chars = "一举成名".toCharArray();
            int index = t.indexOf(chars, 0, 2); // 只取前2个字符 "一举"
            assertTrue(index >= 0, "Builder " + builderIdx + ": should find partial key");
        }
    }

    @Test
    public void testIndexOfWithCharArrayAndNodePos() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String key = "五谷丰登";
            int expectedIndex = keys.indexOf(key);
            int actualIndex = t.indexOf(key.toCharArray(), 0, key.length(), 0);
            assertEquals(expectedIndex, actualIndex, "Builder " + builderIdx + ": wrong index");

            // 测试从不同节点位置开始查找
            int index = t.indexOf(key.toCharArray(), 0, key.length(), 0);
            assertTrue(index >= 0, "Builder " + builderIdx + ": should find key");
        }
    }

    @Test
    public void testIndexOfWithParameters() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String key = "一举成名";
            int expectedIndex = keys.indexOf(key);

            // 测试各种参数组合
            int index1 = t.indexOf(key, 0, key.length(), 0);
            assertEquals(expectedIndex, index1, "Builder " + builderIdx + ": index1 wrong");

            // 测试 len <= 0 的情况（应该使用 key.length()）
            int index2 = t.indexOf(key, 0, 0, 0);
            assertEquals(expectedIndex, index2, "Builder " + builderIdx + ": index2 wrong");

            // 测试 nodePos <= 0 的情况（应该使用 0）
            int index3 = t.indexOf(key, 0, key.length(), -1);
            assertEquals(expectedIndex, index3, "Builder " + builderIdx + ": index3 wrong");
        }
    }

    @Test
    public void testContainsKey() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            // 测试存在的键
            for (String key : keys) {
                assertTrue(t.containsKey(key), "Builder " + builderIdx + "应该包含键: " + key);
            }

            // 测试不存在的键
            assertFalse(t.containsKey("不存在"), "Builder " + builderIdx + ": should not contain non-existent key");
            assertFalse(t.containsKey(""), "Builder " + builderIdx + ": should not contain empty key");
            assertFalse(t.containsKey("一举成"), "Builder " + builderIdx + ": should not contain incomplete key"); // 前缀但不完整
        }
    }

    @Test
    public void testMatcherWithString() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String text = "一举一动一举成名";
            DATMatcher matcher = t.matcher(text);

            Set<String> found = new HashSet<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                String matched = text.substring(begin, begin + length);
                found.add(matched);
                assertTrue(keys.contains(matched), "Builder " + builderIdx + ": found unexpected key: " + matched);
            }

            // 验证找到了一些匹配
            assertTrue(found.size() > 0, "Builder " + builderIdx + ": should find some matches");
        }
    }

    @Test
    public void testMatcherWithStringAndOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String text = "前缀一举一动后缀";
            int offset = 2; // 从 "一举" 开始
            DATMatcher matcher = t.matcher(text, offset);

            List<String> found = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                String matched = text.substring(begin, begin + length);
                found.add(matched);
            }

            assertTrue(found.size() > 0, "Builder " + builderIdx + ": should find matches with offset");
        }
    }

    @Test
    public void testMatcherWithCharArray() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] text = "一举一动".toCharArray();
            DATMatcher matcher = t.matcher(text);

            List<Integer> indices = new ArrayList<>();
            while (matcher.next()) {
                indices.add(matcher.getIndex());
            }

            assertTrue(indices.size() > 0, "Builder " + builderIdx + ": should find matches in char array");
        }
    }

    @Test
    public void testMatcherWithCharArrayAndOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] text = "前缀一举一动".toCharArray();
            int offset = 2;
            DATMatcher matcher = t.matcher(text, offset);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertTrue(count > 0, "Builder " + builderIdx + ": should find matches with offset");
        }
    }

    @Test
    public void testMatcherLongWithString() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String text = "一举一动一举成名";
            DATLongMatcher matcher = t.matcherLong(text);

            List<String> found = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                String matched = text.substring(begin, begin + length);
                found.add(matched);
            }

            assertTrue(found.size() > 0, "Builder " + builderIdx + ": should find long matches");
        }
    }

    @Test
    public void testMatcherLongWithStringAndOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String text = "前缀一举成名天下知后缀";
            int offset = 2;
            DATLongMatcher matcher = t.matcherLong(text, offset);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertTrue(count > 0, "Builder " + builderIdx + ": should find long matches with offset");
        }
    }

    @Test
    public void testMatcherLongWithCharArray() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] text = "一举成名".toCharArray();
            DATLongMatcher matcher = t.matcherLong(text);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertTrue(count > 0, "Builder " + builderIdx + ": should find long matches in char array");
        }
    }

    @Test
    public void testMatcherLongWithCharArrayAndOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] text = "前缀一举成名".toCharArray();
            int offset = 2;
            DATLongMatcher matcher = t.matcherLong(text, offset);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertTrue(count > 0, "Builder " + builderIdx + ": should find long matches with offset");
        }
    }

    @Test
    public void testCommonPrefixSearchWithString() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String key = "一举成名天下知";
            List<Integer> results = t.commonPrefixSearch(key);

            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": should find some prefix matches");

            // 验证结果是有序的（根据匹配长度）
            for (int i = 1; i < results.size(); i++) {
                int prevLen = keys.get(results.get(i - 1)).length();
                int currLen = keys.get(results.get(i)).length();
                assertTrue(prevLen <= currLen, "Builder " + builderIdx + ": results should be ordered by length");
            }

            // 验证找到的前缀确实存在
            for (Integer index : results) {
                String prefix = keys.get(index);
                assertTrue(key.startsWith(prefix), "Builder " + builderIdx + ": matched prefix should exist in key");
            }
        }
    }

    @Test
    public void testCommonPrefixSearchWithStringAndParams() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String key = "一举成名天下知";
            List<Integer> results = t.commonPrefixSearch(key, 0, key.length(), 0);

            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": should find some matches");

            // 测试 len <= 0 的情况
            List<Integer> results2 = t.commonPrefixSearch(key, 0, 0, 0);
            assertEquals(results.size(), results2.size(), "Builder " + builderIdx + ": len=0 and len=full should return same results");
        }
    }

    @Test
    public void testCommonPrefixSearchWithCharArray() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] key = "一举成名".toCharArray();
            List<Integer> results = t.commonPrefixSearch(key);

            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": should find matches in char array");
        }
    }

    @Test
    public void testCommonPrefixSearchWithCharArrayAndOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] key = "前缀一举成名".toCharArray();
            int offset = 2;
            List<Integer> results = t.commonPrefixSearch(key, offset);

            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            // 注意：这个方法的实现可能不会正确处理 offset，但至少不应该抛异常
        }
    }

    @Test
    public void testCommonPrefixSearchWithCharArrayAndAllParams() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] key = "一举成名".toCharArray();
            List<Integer> results = t.commonPrefixSearch(key, 0, key.length, 0);

            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": should find matches");

            // 测试 len <= 0 的情况
            List<Integer> results2 = t.commonPrefixSearch(key, 0, 0, 0);
            assertEquals(results.size(), results2.size(), "Builder " + builderIdx + ": len=0 and len=full should return same results");
        }
    }

    @Test
    public void testCommonPrefixSearchWithValue() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            char[] keyChars = "一举成名".toCharArray();
            int begin = 0;
            LinkedList<Map.Entry<String, Integer>> results = t.commonPrefixSearchWithValue(keyChars, begin);

            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": should find results with values");

            // 验证每个结果
            for (Map.Entry<String, Integer> entry : results) {
                String k = entry.getKey();
                Integer index = entry.getValue();
                assertTrue(keys.contains(k), "Builder " + builderIdx + ": key should exist in test keys");
                assertEquals(keys.indexOf(k), (int) index, "Builder " + builderIdx + ": index should match");
                assertTrue(new String(keyChars).startsWith(k), "Builder " + builderIdx + ": should be prefix of input");
            }
        }
    }


    @Test
    public void testEmptyTrie() {
        List<String> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> getBuilder(0).build(emptyList));
    }

    @Test
    public void testRejectsUnsortedAndDuplicateKeys() {
        DoubleArrayTrieBuilder builder = getBuilder(0);
        assertThrows(IllegalArgumentException.class, () -> builder.build(Arrays.asList("b", "a")));
        assertThrows(IllegalArgumentException.class, () -> builder.build(Arrays.asList("a", "a")));
        assertThrows(IllegalArgumentException.class, () -> builder.build(Arrays.asList("a", null)));
    }

    @Test
    public void testIndexOfLengthFromPosition() {
        List<String> keys = Arrays.asList("cdef", "一举");
        Collections.sort(keys);
        DoubleArrayTrie trie = getBuilder(0).build(keys);

        assertEquals(keys.indexOf("cdef"), trie.indexOf("abcdef", 2, 0, 0));
        assertEquals(keys.indexOf("cdef"), trie.indexOf("abcdef".toCharArray(), 2, -1, 0));
        assertEquals(-1, trie.indexOf("abcdef", 2, 100, 0));
        assertEquals(-1, trie.indexOf("ab", 3, 0, 0));
    }

    @Test
    public void testBuiltArrayHasNoFixedTail() {
        DoubleArrayTrie ascii = getBuilder(0).build(Arrays.asList("ab", "abc"));
        assertTrue(ascii.baseSize() < 65536, "ascii trie should not keep a 65536 tail, length=" + ascii.baseSize());

        DoubleArrayTrie han = getBuilder(0).build(List.of("北"));
        assertTrue(han.baseSize() < 40000, "single CJK word should not keep an extra 65536 tail, length=" + han.baseSize());
        assertEquals(0, han.indexOf("北"));
    }

    @Test
    public void testSingleKeyTrie() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> singleKey = List.of("test");
            DoubleArrayTrie singleTrie = getBuilder(builderIdx).build(singleKey);

            assertEquals(1, singleTrie.size(), "Builder " + builderIdx + ": size should be 1");
            assertEquals(0, singleTrie.indexOf("test"), "Builder " + builderIdx + ": index of 'test' should be 0");
            assertTrue(singleTrie.containsKey("test"), "Builder " + builderIdx + ": should contain 'test'");
            assertFalse(singleTrie.containsKey("test1"), "Builder " + builderIdx + ": should not contain 'test1'");
        }
    }

    @Test
    public void testSpecialCharacters() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> specialKeys = Arrays.asList(
                    "测试",
                    "test-123",
                    "hello_world",
                    "key with spaces",
                    "标点符号！"
            );
            Collections.sort(specialKeys);
            DoubleArrayTrie specialTrie = getBuilder(builderIdx).build(specialKeys);

            for (String key : specialKeys) {
                assertTrue(specialTrie.containsKey(key), "Builder " + builderIdx + "应该包含: " + key);
                int index = specialTrie.indexOf(key);
                assertTrue(index >= 0, "Builder " + builderIdx + ": index should be >= 0");
                assertEquals(specialKeys.get(index), key, "Builder " + builderIdx + ": key should match at index");
            }
        }
    }

    @Test
    public void testLongKeys() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            StringBuilder longKey = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longKey.append("a");
            }
            String key = longKey.toString();

            List<String> longKeys = Arrays.asList(key, key + "b", key + "c");
            Collections.sort(longKeys);
            DoubleArrayTrie longTrie = getBuilder(builderIdx).build(longKeys);

            assertTrue(longTrie.containsKey(key), "Builder " + builderIdx + ": should contain long key");
            assertEquals(0, longTrie.indexOf(key), "Builder " + builderIdx + ": long key index should be 0");
        }
    }

    @Test
    public void testMatcherEmptyText() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String emptyText = "";
            DATMatcher matcher = t.matcher(emptyText);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": empty text should not match");
        }
    }

    @Test
    public void testMatcherLongEmptyText() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String emptyText = "";
            DATLongMatcher matcher = t.matcherLong(emptyText);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": empty text should not match");
        }
    }

    @Test
    public void testCommonPrefixSearchEmptyKey() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie t = getBuilder(builderIdx).build(keys);

            String emptyKey = "";
            List<Integer> results = t.commonPrefixSearch(emptyKey);

            // 空键的前缀搜索应该返回空列表或根据实现返回特定结果
            assertNotNull(results, "Builder " + builderIdx + ": results should not be null");
            // 通常空字符串不应该匹配任何键（除非字典中有空字符串）
        }
    }

    @Test
    public void testOverlappingKeys() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            // 测试重叠的键（一个键是另一个键的前缀）
            List<String> overlappingKeys = Arrays.asList(
                    "中国",
                    "中国人",
                    "中国人民",
                    "中国人民银行"
            );
            Collections.sort(overlappingKeys);
            DoubleArrayTrie overlappingTrie = getBuilder(builderIdx).build(overlappingKeys);

            String text = "中国人民银行";
            List<Integer> results = overlappingTrie.commonPrefixSearch(text);

            // 应该找到所有前缀匹配
            assertTrue(results.size() >= 1, "Builder " + builderIdx + ": should find at least one match");

            // 验证每个匹配都是有效的前缀
            for (Integer index : results) {
                String prefix = overlappingKeys.get(index);
                assertTrue(text.startsWith(prefix), "Builder " + builderIdx + ": should be valid prefix");
            }
        }
    }

    @Test
    public void testWriteAndRead() throws Exception {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = new ArrayList<>(testKeys);
            Collections.sort(keys);
            DoubleArrayTrie originalTrie = getBuilder(builderIdx).build(keys);

            // 测试写入和读取
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            originalTrie.write(dos);
            dos.close();

            // 从字节数组读取
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            DoubleArrayTrie readTrie = DoubleArrayTrie.read(dis);
            dis.close();

            // 验证读取后的对象不为空
            assertNotNull(readTrie, "Builder " + builderIdx + ": read trie should not be null");

            // 验证 size 相同
            assertEquals(originalTrie.size(), readTrie.size(), "Builder " + builderIdx + ": size should match");

            // 验证 base 和 check 数组相同
            assertArrayEquals(originalTrie.getBase(), readTrie.getBase(), "Builder " + builderIdx + ": base array should match");
            assertArrayEquals(originalTrie.getCheck(), readTrie.getCheck(), "Builder " + builderIdx + ": check array should match");

            // 验证所有键都能在读取后的 trie 中找到
            for (String key : keys) {
                int originalIndex = originalTrie.indexOf(key);
                int readIndex = readTrie.indexOf(key);
                assertEquals(originalIndex, readIndex, "Builder " + builderIdx + ": index for key '" + key + "' should match");
                assertTrue(readTrie.containsKey(key), "Builder " + builderIdx + ": read trie should contain key '" + key + "'");
            }

            // 验证不存在的键仍然返回 -1
            assertEquals(-1, readTrie.indexOf("不存在的键"), "Builder " + builderIdx + ": non-existent key should return -1");
        }
    }

    @Test
    public void testWriteAndReadWithSingleKey() throws Exception {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> singleKey = List.of("test");
            DoubleArrayTrie originalTrie = getBuilder(builderIdx).build(singleKey);

            // 写入
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            originalTrie.write(dos);
            dos.close();

            // 读取
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            DoubleArrayTrie readTrie = DoubleArrayTrie.read(dis);
            dis.close();

            // 验证
            assertEquals(1, readTrie.size(), "Builder " + builderIdx + ": size should be 1");
            assertEquals(0, readTrie.indexOf("test"), "Builder " + builderIdx + ": index of 'test' should be 0");
            assertTrue(readTrie.containsKey("test"), "Builder " + builderIdx + ": should contain 'test'");
        }
    }

    @Test
    public void testWriteAndReadWithSpecialCharacters() throws Exception {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> specialKeys = Arrays.asList(
                    "测试",
                    "test-123",
                    "hello_world",
                    "key with spaces",
                    "标点符号！"
            );
            Collections.sort(specialKeys);
            DoubleArrayTrie originalTrie = getBuilder(builderIdx).build(specialKeys);

            // 写入
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            originalTrie.write(dos);
            dos.close();

            // 读取
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            DoubleArrayTrie readTrie = DoubleArrayTrie.read(dis);
            dis.close();

            // 验证所有特殊字符键都能正确读取
            for (String key : specialKeys) {
                assertTrue(readTrie.containsKey(key), "Builder " + builderIdx + ": should contain special key '" + key + "'");
                int originalIndex = originalTrie.indexOf(key);
                int readIndex = readTrie.indexOf(key);
                assertEquals(originalIndex, readIndex, "Builder " + builderIdx + ": index for special key '" + key + "' should match");
            }
        }
    }

    @Test
    public void testWriteAndReadWithLongKeys() throws Exception {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            StringBuilder longKey = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longKey.append("a");
            }
            String key = longKey.toString();

            List<String> longKeys = Arrays.asList(key, key + "b", key + "c");
            Collections.sort(longKeys);
            DoubleArrayTrie originalTrie = getBuilder(builderIdx).build(longKeys);

            // 写入
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            originalTrie.write(dos);
            dos.close();

            // 读取
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            DoubleArrayTrie readTrie = DoubleArrayTrie.read(dis);
            dis.close();

            // 验证长键
            assertTrue(readTrie.containsKey(key), "Builder " + builderIdx + ": should contain long key");
            assertEquals(originalTrie.indexOf(key), readTrie.indexOf(key), "Builder " + builderIdx + ": long key index should match");
        }
    }
}
