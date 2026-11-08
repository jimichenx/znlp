package dev.znlp.base.bintrie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BinTrieTree 单元测试
 * <p>
 * 该测试类测试BinTrieTree的各种功能，包括插入、查询、删除、匹配等操作。
 */
public class BinTrieTreeTest {

    private BinTrieTree<String> tree;

    @BeforeEach
    public void setUp() {
        tree = BinTrieTreeBuilder._default.build();
    }

    @Test
    public void testPutAndGet() {
        tree.put("中国", "CN");
        tree.put("美国", "US");
        tree.put("日本", "JP");

        assertEquals("CN", tree.get("中国"));
        assertEquals("US", tree.get("美国"));
        assertEquals("JP", tree.get("日本"));
        assertNull(tree.get("英国"));
    }

    @Test
    public void testPutAndGetWithCharSequence() {
        tree.put("hello", "en");
        tree.put("world", "en");

        assertEquals("en", tree.get("hello"));
        assertEquals("en", tree.get("world"));
    }

    @Test
    public void testPutAndGetWithCharArray() {
        tree.put("test", "value1");
        tree.put("testing", "value2");

        assertEquals("value1", tree.get("test".toCharArray()));
        assertEquals("value2", tree.get("testing".toCharArray()));
    }

    @Test
    public void testPutAndGetWithOffset() {
        tree.put("hello", "en");
        tree.put("world", "en");

        char[] chars = "hello world".toCharArray();
        assertEquals("en", tree.get(chars, 0, 5));
        assertEquals("en", tree.get(chars, 6, 11));
    }

    @Test
    public void testContainsKey() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        assertTrue(tree.containsKey("中国"));
        assertTrue(tree.containsKey("美国"));
        assertFalse(tree.containsKey("英国"));
        assertFalse(tree.containsKey("中"));
    }

    @Test
    public void testPrefixSearch() {
        tree.put("中国", "CN");
        tree.put("中国人", "CNP");
        tree.put("中国话", "CNL");
        tree.put("美国", "US");

        Set<Map.Entry<String, String>> results = tree.prefixSearch("中");
        assertEquals(3, results.size());

        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, String> entry : results) {
            keys.add(entry.getKey());
        }
        assertTrue(keys.contains("中国"));
        assertTrue(keys.contains("中国人"));
        assertTrue(keys.contains("中国话"));
    }

    @Test
    public void testPrefixSearchEmpty() {
        tree.put("中国", "CN");
        Set<Map.Entry<String, String>> results = tree.prefixSearch("英");
        assertTrue(results.isEmpty());
    }

    @Test
    public void testRemove() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        assertTrue(tree.containsKey("中国"));
        tree.remove("中国");
        assertFalse(tree.containsKey("中国"));
        assertTrue(tree.containsKey("美国"));
    }

    @Test
    public void testRemoveNonExistent() {
        tree.put("中国", "CN");
        tree.remove("英国"); // 不应该抛出异常
        assertTrue(tree.containsKey("中国"));
    }

    @Test
    public void testCaseInsensitive() {
        tree.put("Hello", "value1");
        assertEquals("value1", tree.get("hello"));
        assertEquals("value1", tree.get("HELLO"));
        assertEquals("value1", tree.get("HeLlO"));
    }

    @Test
    public void testUpdateValue() {
        tree.put("中国", "CN");
        assertEquals("CN", tree.get("中国"));

        tree.put("中国", "China");
        assertEquals("China", tree.get("中国"));
    }

    @Test
    public void testNestedWords() {
        tree.put("中", "Z");
        tree.put("中国", "CN");
        tree.put("中国人", "CNP");

        assertEquals("Z", tree.get("中"));
        assertEquals("CN", tree.get("中国"));
        assertEquals("CNP", tree.get("中国人"));
    }

    @Test
    public void testKeysIterator() {
        tree.put("中国", "CN");
        tree.put("美国", "US");
        tree.put("日本", "JP");

        Iterator<String> it = tree.keys();
        Set<String> keys = new HashSet<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }

        assertEquals(3, keys.size());
        assertTrue(keys.contains("中国"));
        assertTrue(keys.contains("美国"));
        assertTrue(keys.contains("日本"));
    }

    @Test
    public void testEntryIterator() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        Set<String> keys = new HashSet<>();
        Set<String> values = new HashSet<>();
        for (Map.Entry<String, String> entry : tree.entry()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }

        assertEquals(2, keys.size());
        assertEquals(2, values.size());
        assertTrue(keys.contains("中国"));
        assertTrue(keys.contains("美国"));
        assertTrue(values.contains("CN"));
        assertTrue(values.contains("US"));
    }

    @Test
    public void testRootChildCount() {
        assertEquals(0, tree.rootChildCount());

        tree.put("中国", "CN");
        assertEquals(1, tree.rootChildCount());

        tree.put("美国", "US");
        assertEquals(2, tree.rootChildCount());

        tree.put("中", "Z");
        assertEquals(2, tree.rootChildCount()); // "中"和"中国"共享首字符
    }

    @Test
    public void testReset() {
        tree.put("中国", "CN");
        tree.put("美国", "US");
        assertEquals(2, tree.rootChildCount());

        tree.reset();
        assertEquals(0, tree.rootChildCount());
        assertFalse(tree.containsKey("中国"));
        assertFalse(tree.containsKey("美国"));
    }

    @Test
    public void testForwardMatcher() {
        tree.put("中", "Z");
        tree.put("中国", "CN");
        tree.put("中国人", "CNP");
        tree.put("美国", "US");

        TrieTreeMatcher<String> matcher = tree.newForwardMatcher("中国人");
        List<String> matches = new ArrayList<>();
        String word;
        while ((word = matcher.next()) != null) {
            matches.add(word);
        }

        // 前向最大匹配应该只返回最长的匹配
        assertTrue(matches.size() >= 1);
        assertTrue(matches.contains("中国人"));
    }

    @Test
    public void testAllMatcher() {
        tree.put("中", "Z");
        tree.put("中国", "CN");
        tree.put("中国人", "CNP");

        TrieTreeMatcher<String> matcher = tree.newAllMatcher("中国人");
        List<String> matches = new ArrayList<>();
        String word;
        while ((word = matcher.next()) != null) {
            matches.add(word);
        }

        // 全匹配应该返回所有匹配
        assertTrue(matches.size() >= 3);
        assertTrue(matches.contains("中"));
        assertTrue(matches.contains("中国"));
        assertTrue(matches.contains("中国人"));
    }

    @Test
    public void testMatcherGetParams() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        TrieTreeMatcher<String> matcher = tree.newForwardMatcher("中国");
        matcher.next();
        assertEquals("CN", matcher.getParams());
    }

    @Test
    public void testMatcherGetOffset() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        TrieTreeMatcher<String> matcher = tree.newForwardMatcher("我是中国人");
        matcher.next();
        int offset = matcher.getOffset();
        assertTrue(offset >= 0);
    }

    @Test
    public void testBuildFromMap() {
        Map<String, String> map = new HashMap<>();
        map.put("中国", "CN");
        map.put("美国", "US");
        map.put("日本", "JP");

        BinTrieTree<String> tree2 = BinTrieTreeBuilder._default.build(map);
        assertEquals("CN", tree2.get("中国"));
        assertEquals("US", tree2.get("美国"));
        assertEquals("JP", tree2.get("日本"));
    }

    @Test
    public void testBuildFromLines() {
        List<String> lines = Arrays.asList(
                "中国 CN Beijing",
                "美国 US Washington",
                "日本 JP Tokyo"
        );

        BinTrieTree<String[]> tree2 = BinTrieTreeBuilder._default.build(" ", lines, arr -> arr);
        String[] china = tree2.get("中国");
        assertNotNull(china);
        assertEquals(2, china.length);
        assertEquals("CN", china[0]);
        assertEquals("Beijing", china[1]);
    }

    @Test
    public void testBuildFromLinesSingleWord() {
        List<String> lines = Arrays.asList(
                "中国",
                "美国 US"
        );

        BinTrieTree<String[]> tree2 = BinTrieTreeBuilder._default.build(" ", lines, arr -> arr);
        String[] china = tree2.get("中国");
        assertNotNull(china);
        assertEquals(0, china.length);
    }

    @Test
    public void testDifferentBuilders() {
        // 测试不同的构建器
        BinTrieTree<String> tree1 = BinTrieTreeBuilder._default.build();
        BinTrieTree<String> tree2 = BinTrieTreeBuilder.bigArray.build();
        BinTrieTree<String> tree3 = BinTrieTreeBuilder.miniArray.build();
        BinTrieTree<String> tree4 = BinTrieTreeBuilder.miniHash.build();

        tree1.put("test", "value1");
        tree2.put("test", "value2");
        tree3.put("test", "value3");
        tree4.put("test", "value4");

        assertEquals("value1", tree1.get("test"));
        assertEquals("value2", tree2.get("test"));
        assertEquals("value3", tree3.get("test"));
        assertEquals("value4", tree4.get("test"));
    }

    @Test
    public void testAccessFullTireNode() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        List<String> accessed = new ArrayList<>();
        tree.accessFullTireNode(node -> {
            if (node.value != null) {
                accessed.add(node.value);
            }
        });

        assertTrue(accessed.size() >= 2);
        assertTrue(accessed.contains("CN"));
        assertTrue(accessed.contains("US"));
    }

    @Test
    public void testKeysWithHolder() {
        tree.put("中国", "CN");
        tree.put("美国", "US");

        BinTrieTree.NodeHolder<String> holder = new BinTrieTree.NodeHolder<>();
        Iterator<String> it = tree.keys(holder);
        Set<String> keys = new HashSet<>();
        while (it.hasNext()) {
            String key = it.next();
            keys.add(key);
            if (key.equals("中国")) {
                assertNotNull(holder.getNode());
                assertEquals("CN", holder.getNode().value);
            }
        }

        assertEquals(2, keys.size());
    }

    @Test
    public void testEmptyTree() {
        assertFalse(tree.containsKey("test"));
        assertNull(tree.get("test"));
        assertEquals(0, tree.rootChildCount());
        assertTrue(tree.prefixSearch("test").isEmpty());
    }

    @Test
    public void testSpecialCharacters() {
        tree.put("test-123", "value1");
        tree.put("test_456", "value2");
        tree.put("test.789", "value3");

        assertEquals("value1", tree.get("test-123"));
        assertEquals("value2", tree.get("test_456"));
        assertEquals("value3", tree.get("test.789"));
    }

    @Test
    public void testLongWords() {
        String longWord = "这是一个非常长的词语用来测试Trie树的性能";
        tree.put(longWord, "long");
        assertEquals("long", tree.get(longWord));
        assertTrue(tree.containsKey(longWord));
    }
}
