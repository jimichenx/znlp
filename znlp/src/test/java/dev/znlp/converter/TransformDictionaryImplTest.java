package dev.znlp.converter;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定简繁替换的最长匹配：同一起点取最长词，然后从左向右跳过这段长度。
 * 落在已消费词内部的更短词不再参与。
 */
class TransformDictionaryImplTest {

    @Test
    void longestWordAtSameStartWins() {
        TransformDictionaryImpl dict = dict(
                "长江", "長江",
                "长江大桥", "長江大橋");
        assertEquals("南京市長江大橋", dict.transform("南京市长江大桥"));
    }

    @Test
    void laterOverlapIsSkippedAfterLongerPrefix() {
        TransformDictionaryImpl dict = dict(
                "南京市", "南京市",
                "市长", "市長");
        assertEquals("南京市长", dict.transform("南京市长"));
    }

    @Test
    void suffixWordIsUsedWhenItIsTheWholeMatch() {
        TransformDictionaryImpl dict = dict(
                "he", "HE",
                "she", "SHE");
        assertEquals("SHE", dict.transform("she"));
        assertEquals("HE", dict.transform("he"));
        assertEquals("xxSHEyy", dict.transform("xxsheyy"));
    }

    @Test
    void gapsAndEdgesKeepOriginalCharacters() {
        TransformDictionaryImpl dict = dict(
                "软件", "軟件",
                "开发", "開發");
        assertEquals("x軟件y", dict.transform("x软件y"));
        assertEquals("去軟件", dict.transform("去软件"));
        assertEquals("軟件和開發", dict.transform("软件和开发"));
        assertEquals("軟件軟件", dict.transform("软件软件"));
        assertEquals("xyz", dict.transform("xyz"));
        assertEquals("", dict.transform(""));
        assertEquals("軟件" + '\uFFFF', dict.transform("软件" + '\uFFFF'));
    }

    @Test
    void charArrayMatchesString() {
        TransformDictionaryImpl dict = dict("发", "發", "发展", "發展");
        char[] text = "发展发".toCharArray();
        assertEquals("發展發", dict.transform(text));
        assertEquals(dict.transform("发展发"), dict.transform(text));
    }

    @Test
    void singleCharacterDoesNotHideALongerWord() {
        TransformDictionaryImpl dict = dict(
                "中", "中",
                "中国", "中國",
                "中国人", "中國人");
        assertEquals("中國人爱中國", dict.transform("中国人爱中国"));
        assertEquals("中", dict.transform("中"));
    }

    private static TransformDictionaryImpl dict(String... pairs) {
        TreeMap<String, String> map = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return new TransformDictionaryImpl(map);
    }
}
