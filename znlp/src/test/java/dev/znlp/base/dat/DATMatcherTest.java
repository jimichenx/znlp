package dev.znlp.base.dat;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DATMatcher 单元测试
 * <p>
 * 该测试类专门测试 DATMatcher 的功能，确保匹配器能够正确找到所有匹配项。
 * 构建器只有 {@link DoubleArrayTrieBuilder2}。
 */
public class DATMatcherTest {

    private DoubleArrayTrieBuilder getBuilder(int index) {
        return new DoubleArrayTrieBuilder2();
    }

    /**
     * 测试使用 String 创建 matcher 并查找所有匹配
     */
    @Test
    public void testMatcherWithString() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名", "五谷", "五谷丰登");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动一举成名五谷丰登";
            DATMatcher matcher = trie.matcher(text);

            List<MatchResult> results = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = text.substring(begin, begin + length);
                results.add(new MatchResult(begin, length, index, matched));
            }

            // 验证找到的匹配数量
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": 应该找到至少一个匹配");

            // 验证每个匹配都是有效的
            for (MatchResult result : results) {
                assertTrue(keys.contains(result.matched), "Builder " + builderIdx + ": 匹配的字符串应该在keys中: " + result.matched);
                assertTrue(result.begin >= 0, "Builder " + builderIdx + ": begin应该 >= 0");
                assertTrue(result.length > 0, "Builder " + builderIdx + ": length应该 > 0");
                assertTrue(result.index >= 0, "Builder " + builderIdx + ": index应该 >= 0");
                assertTrue(result.index < keys.size(), "Builder " + builderIdx + ": index应该 < keys.size()");
                assertEquals(keys.get(result.index), result.matched, "Builder " + builderIdx + ": 匹配的字符串应该与keys中的对应");
            }
        }
    }

    /**
     * 测试使用 char[] 创建 matcher 并查找所有匹配
     */
    @Test
    public void testMatcherWithCharArray() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名", "五谷", "五谷丰登");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            char[] text = "一举一动一举成名五谷丰登".toCharArray();
            DATMatcher matcher = trie.matcher(text);

            List<MatchResult> results = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = new String(text, begin, length);
                results.add(new MatchResult(begin, length, index, matched));
            }

            // 验证找到的匹配数量
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": 应该找到至少一个匹配");

            // 验证每个匹配都是有效的
            for (MatchResult result : results) {
                assertTrue(keys.contains(result.matched),
                        "Builder " + builderIdx + ": 匹配的字符串应该在keys中: " + result.matched);
                assertEquals(keys.get(result.index), result.matched,
                        "Builder " + builderIdx + ": 匹配的字符串应该与keys中的对应");
            }
        }
    }

    /**
     * 测试使用 offset 参数创建 matcher
     */
    @Test
    public void testMatcherWithOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "前缀一举一动一举成名后缀";
            int offset = 2; // 从"一举"开始
            DATMatcher matcher = trie.matcher(text, offset);

            List<MatchResult> results = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = text.substring(begin, begin + length);
                results.add(new MatchResult(begin, length, index, matched));
            }

            // 验证找到的匹配
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": 应该找到至少一个匹配");

            // 验证所有匹配的起始位置都 >= offset
            for (MatchResult result : results) {
                assertTrue(result.begin >= offset, "Builder " + builderIdx + ": begin应该 >= offset");
            }
        }
    }

    /**
     * 测试使用 char[] 和 offset 创建 matcher
     */
    @Test
    public void testMatcherWithCharArrayAndOffset() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            char[] text = "前缀一举一动一举成名后缀".toCharArray();
            int offset = 2;
            DATMatcher matcher = trie.matcher(text, offset);

            List<MatchResult> results = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = new String(text, begin, length);
                results.add(new MatchResult(begin, length, index, matched));
            }

            // 验证找到的匹配
            assertTrue(results.size() > 0, "Builder " + builderIdx + ": 应该找到至少一个匹配");

            // 验证所有匹配的起始位置都 >= offset
            for (MatchResult result : results) {
                assertTrue(result.begin >= offset, "Builder " + builderIdx + ": begin应该 >= offset");
            }
        }
    }

    /**
     * 测试重叠匹配（一个匹配是另一个匹配的前缀）
     */
    @Test
    public void testOverlappingMatches() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("中国", "中国人", "中国人民", "中国人民银行");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "中国人民银行";
            DATMatcher matcher = trie.matcher(text);

            Set<String> foundMatches = new HashSet<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                String matched = text.substring(begin, begin + length);
                foundMatches.add(matched);
            }

            // 应该找到所有重叠的匹配
            assertTrue(foundMatches.size() >= 2, "Builder " + builderIdx + ": 应该找到多个重叠匹配");
            assertTrue(foundMatches.contains("中国"), "Builder " + builderIdx + ": 应该包含'中国'");
            assertTrue(foundMatches.contains("中国人"), "Builder " + builderIdx + ": 应该包含'中国人'");
        }
    }

    /**
     * 测试空文本
     */
    @Test
    public void testEmptyText() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String emptyText = "";
            DATMatcher matcher = trie.matcher(emptyText);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": 空文本不应该找到匹配");
        }
    }

    /**
     * 测试空文本（使用 char[]）
     */
    @Test
    public void testEmptyCharArray() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            char[] emptyText = new char[0];
            DATMatcher matcher = trie.matcher(emptyText);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": 空字符数组不应该找到匹配");
        }
    }

    /**
     * 测试无匹配的文本
     */
    @Test
    public void testNoMatches() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "五谷");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "没有任何匹配的文本";
            DATMatcher matcher = trie.matcher(text);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": 不应该找到匹配");
        }
    }

    /**
     * 测试单个字符匹配
     */
    @Test
    public void testSingleCharMatches() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("a", "b", "c", "ab", "abc");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "abc";
            DATMatcher matcher = trie.matcher(text);

            Set<String> foundMatches = new HashSet<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                String matched = text.substring(begin, begin + length);
                foundMatches.add(matched);
            }

            // 应该找到 "a", "ab", "abc", "b", "c" 等（取决于实现）
            assertTrue(foundMatches.size() > 0, "Builder " + builderIdx + ": 应该找到至少一个匹配");
            assertTrue(foundMatches.contains("a"), "Builder " + builderIdx + ": 应该包含'a'");
            assertTrue(foundMatches.contains("ab"), "Builder " + builderIdx + ": 应该包含'ab'");
            assertTrue(foundMatches.contains("abc"), "Builder " + builderIdx + ": 应该包含'abc'");
        }
    }

    /**
     * 测试多次调用 next() 后返回 false
     */
    @Test
    public void testNextReturnsFalseAfterExhaustion() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动";
            DATMatcher matcher = trie.matcher(text);

            // 第一次调用应该返回 true
            boolean firstCall = matcher.next();
            assertTrue(firstCall, "Builder " + builderIdx + ": 第一次调用next()应该返回true");

            // 继续调用直到返回 false
            int count = 1;
            while (matcher.next()) {
                count++;
            }

            // 再次调用应该仍然返回 false
            boolean afterExhaustion = matcher.next();
            assertFalse(afterExhaustion, "Builder " + builderIdx + ": 耗尽后再次调用next()应该返回false");
        }
    }

    /**
     * 测试匹配的索引正确性
     */
    @Test
    public void testMatchIndexCorrectness() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名", "五谷", "五谷丰登");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动五谷丰登";
            DATMatcher matcher = trie.matcher(text);

            Map<String, Integer> expectedIndices = new HashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                expectedIndices.put(keys.get(i), i);
            }

            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = text.substring(begin, begin + length);

                Integer expectedIndex = expectedIndices.get(matched);
                assertNotNull(expectedIndex, "Builder " + builderIdx + ": 匹配的字符串应该有对应的索引: " + matched);
                assertEquals(expectedIndex.intValue(), index,
                        "Builder " + builderIdx + ": 索引应该正确: " + matched);
            }
        }
    }

    /**
     * 测试匹配的 begin 和 length 正确性
     */
    @Test
    public void testMatchBeginAndLength() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动一举成名";
            DATMatcher matcher = trie.matcher(text);

            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                String matched = text.substring(begin, begin + length);

                // 验证 begin 和 length 的组合能正确提取匹配的字符串
                assertTrue(begin >= 0, "Builder " + builderIdx + ": begin应该 >= 0");
                assertTrue(length > 0, "Builder " + builderIdx + ": length应该 > 0");
                assertTrue(begin + length <= text.length(),
                        "Builder " + builderIdx + ": begin + length应该 <= text.length()");
                assertTrue(keys.contains(matched), "Builder " + builderIdx + ": 匹配的字符串应该在keys中");
            }
        }
    }

    /**
     * 测试从文本中间开始匹配（offset > 0）
     */
    @Test
    public void testMatchingFromMiddle() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动", "一举成名");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "前缀一举一动一举成名后缀";
            // 从位置3开始（跳过"前缀"）
            int offset = 2;
            DATMatcher matcher = trie.matcher(text, offset);

            List<MatchResult> results = new ArrayList<>();
            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = text.substring(begin, begin + length);
                results.add(new MatchResult(begin, length, index, matched));
            }

            // 验证所有匹配都从offset之后开始
            for (MatchResult result : results) {
                assertTrue(result.begin >= offset,
                        "Builder " + builderIdx + ": 所有匹配应该从offset之后开始");
            }
        }
    }

    /**
     * 测试边界情况：offset 等于文本长度
     */
    @Test
    public void testOffsetAtTextEnd() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动";
            int offset = text.length();
            DATMatcher matcher = trie.matcher(text, offset);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": offset在文本末尾时不应该找到匹配");
        }
    }

    /**
     * 测试边界情况：offset 大于文本长度
     */
    @Test
    public void testOffsetBeyondTextLength() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList("一举", "一举一动");
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动";
            int offset = text.length() + 10;
            DATMatcher matcher = trie.matcher(text, offset);

            int count = 0;
            while (matcher.next()) {
                count++;
            }

            assertEquals(0, count, "Builder " + builderIdx + ": offset超出文本长度时不应该找到匹配");
        }
    }

    /**
     * 测试复杂场景：多个键、重叠匹配、不同长度
     */
    @Test
    public void testComplexScenario() {
        for (int builderIdx = 0; builderIdx < 1; builderIdx++) {
            List<String> keys = Arrays.asList(
                    "一", "一举", "一举一动", "一举成名", "一举成名天下知",
                    "五", "五谷", "五谷丰登",
                    "万", "万万"
            );
            Collections.sort(keys);
            DoubleArrayTrie trie = getBuilder(builderIdx).build(keys);

            String text = "一举一动一举成名天下知五谷丰登万万";
            DATMatcher matcher = trie.matcher(text);

            Set<String> foundMatches = new HashSet<>();
            Map<String, List<Integer>> matchPositions = new HashMap<>();

            while (matcher.next()) {
                int begin = matcher.getBegin();
                int length = matcher.getLength();
                int index = matcher.getIndex();
                String matched = text.substring(begin, begin + length);

                foundMatches.add(matched);
                matchPositions.computeIfAbsent(matched, k -> new ArrayList<>()).add(begin);

                // 验证索引正确
                assertEquals(keys.indexOf(matched), index, "Builder " + builderIdx + ": 索引应该正确");
            }

            // 验证找到了一些匹配
            assertTrue(foundMatches.size() > 0, "Builder " + builderIdx + ": 应该找到多个匹配");

            // 验证某些关键匹配被找到
            assertTrue(foundMatches.contains("一举"), "Builder " + builderIdx + ": 应该找到'一举'");
            assertTrue(foundMatches.contains("一举一动"), "Builder " + builderIdx + ": 应该找到'一举一动'");
        }
    }

    /**
     * 失败指针路径与逐字重启路径命中集合一致，且能扫到后缀词。
     */
    @Test
    public void failurePointer_matchesNaiveRestartSet() {
        List<String> keys = Arrays.asList("he", "she", "his", "hers", "一举", "一举一动", "举");
        Collections.sort(keys);

        DoubleArrayTrie naive = new DoubleArrayTrieBuilder2().build(keys);
        DoubleArrayTrie ac = new DoubleArrayTrieBuilder2().build(keys);
        ac.enableFailure(keys);
        assertTrue(ac.hasFailure());

        String[] texts = {"she", "hers", "hishe", "一举一动", "xxsheyy", ""};
        for (String text : texts) {
            assertEquals(collect(naive, text), collect(ac, text), "text=" + text);
        }
    }

    /**
     * 数组不再预留 65536 槽之后，最长匹配碰到表外字符应结束这一支，而不是抛越界。
     */
    @Test
    public void longMatcherSurvivesCharacterPastArray() {
        List<String> keys = Arrays.asList("ab", "abc", "bc");
        DoubleArrayTrie trie = getBuilder(0).build(keys);
        assertTrue(trie.baseSize() < 65536);

        String text = "ab" + '\uFFFF' + "bc";
        DATLongMatcher matcher = trie.matcherLong(text);
        List<String> found = new ArrayList<>();
        while (matcher.next()) {
            found.add(text.substring(matcher.getBegin(), matcher.getBegin() + matcher.getLength()));
        }
        assertEquals(List.of("ab", "bc"), found);
    }

    private static Set<String> collect(DoubleArrayTrie trie, String text) {
        Set<String> found = new HashSet<>();
        DATMatcher matcher = trie.matcher(text);
        while (matcher.next()) {
            found.add(matcher.getBegin() + ":" + text.substring(matcher.getBegin(), matcher.getBegin() + matcher.getLength()));
        }
        return found;
    }

    /**
         * 辅助类：存储匹配结果
         */
        private record MatchResult(int begin, int length, int index, String matched) {

        @Override
            public String toString() {
                return String.format("MatchResult{begin=%d, length=%d, index=%d, matched='%s'}",
                        begin, length, index, matched);
            }
        }
}
