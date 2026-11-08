package dev.znlp.converter;

import org.junit.jupiter.api.Test;
import dev.znlp.base.dat.DoubleArrayTrieMap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 词典实现走最长匹配器。这里用「收集全部命中，再按起点留最长词」的另一条路径对照，
 * 两条路径的替换结果应当相同。
 */
class LongestMatchParityTest {

    @Test
    void craftedCasesAgree() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("长江", "長江");
        map.put("长江大桥", "長江大橋");
        map.put("南京市", "南京市");
        map.put("市长", "市長");
        map.put("软件", "軟件");
        map.put("开发", "開發");
        map.put("he", "HE");
        map.put("she", "SHE");
        map.put("中", "中");
        map.put("中国", "中國");
        map.put("中国人", "中國人");

        String[] texts = {
                "南京市长江大桥",
                "南京市长",
                "x软件y",
                "软件和开发",
                "软件软件",
                "xxsheyy",
                "she",
                "中国人爱中国",
                "软件" + '\uFFFF' + "开发",
                ""
        };
        assertAgree(map, texts);
    }

    @Test
    void bundledSimplifiedToTraditionalAgrees() throws Exception {
        TreeMap<String, String> map = loadDict("znlp/ts-dict/s2t.txt");
        assertFalse(map.isEmpty());
        List<String> keys = new ArrayList<>(map.keySet());
        List<String> texts = new ArrayList<>();
        texts.add("软件开发南京市长江大桥");
        texts.add("发");
        for (int i = 0; i < keys.size(); i += 97) {
            String key = keys.get(i);
            texts.add(key);
            if (i + 97 < keys.size()) {
                texts.add("x" + key + "，" + keys.get(i + 97) + '\uFFFF');
            }
            if (i + 200 < keys.size()) {
                texts.add(key + keys.get(i + 200) + key);
            }
        }
        assertAgree(map, texts.toArray(String[]::new));
    }

    private static void assertAgree(TreeMap<String, String> map, String[] texts) {
        TransformDictionaryImpl dict = new TransformDictionaryImpl(map);
        DoubleArrayTrieMap<String> words = new DoubleArrayTrieMap<>(map);
        for (String text : texts) {
            assertEquals(wordNet(words, text), dict.transform(text), text);
        }
    }

    /**
     * 与替换前的挑选规则相同：同一起点只保留更长的键，然后从左向右跳过这段字数。
     */
    private static String wordNet(DoubleArrayTrieMap<String> words, String text) {
        char[] chars = text.toCharArray();
        String[] chosen = new String[chars.length];
        int[] lengths = new int[chars.length];
        DoubleArrayTrieMap<String>.MapMatcher matcher = words.match(chars);
        while (matcher.next()) {
            int begin = matcher.getBegin();
            int length = matcher.getLength();
            if (length > lengths[begin]) {
                chosen[begin] = matcher.getValue();
                lengths[begin] = length;
            }
        }
        StringBuilder sb = new StringBuilder(chars.length);
        for (int offset = 0; offset < chars.length; ) {
            if (chosen[offset] == null) {
                sb.append(chars[offset]);
                offset++;
            } else {
                sb.append(chosen[offset]);
                offset += lengths[offset];
            }
        }
        return sb.toString();
    }

    private static TreeMap<String, String> loadDict(String resource) throws Exception {
        TreeMap<String, String> map = new TreeMap<>();
        InputStream in = LongestMatchParityTest.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("missing " + resource);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int split = line.indexOf('=');
                if (split <= 0 || split == line.length() - 1) {
                    continue;
                }
                map.put(line.substring(0, split), line.substring(split + 1));
            }
        }
        return map;
    }
}
