package dev.znlp.pinyin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.dat.DoubleArrayTrieMap;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.pinyin.enums.Pinyin;
import dev.znlp.test.TestTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 拼音转换与「全命中再按原规则挑选」对照。规则是：更长的键只有在长过已记下的音节数时才替换，
 * 单字只留第一个音节，游标按音节数前进。
 */
@Tag(TestTags.HEAVY)
class PinyinMatchParityTest {

    @Test
    void systemDictionaryAgreesOnSamplePhrases() {
        PinyinDict dict = PinyinDict.create(new NlpResourceLoader(TestHelper.createTestConfig()));
        Tex2PinyinComputer computer = new Tex2PinyinComputer(dict);
        Map<String, Pinyin[]> table = dict.getPinyinTable();
        DoubleArrayTrieMap<Pinyin[]> words = new DoubleArrayTrieMap<>(new TreeMap<>(table));

        List<String> keys = new ArrayList<>(table.keySet());
        List<String> texts = new ArrayList<>();
        Collections.addAll(texts,
                "复旦大学",
                "一个巴掌拍不响",
                "中国",
                "软件开发",
                "x重庆y",
                "甲乙丙");
        for (int i = 0; i < keys.size(); i += 200) {
            String key = keys.get(i);
            texts.add(key);
            if (i + 200 < keys.size()) {
                texts.add(key + "，" + keys.get(i + 200));
            }
        }
        assertFalse(texts.isEmpty());
        for (String text : texts) {
            assertEquals(wordNet(words, text), tones(computer, text), text);
        }
    }

    @Test
    void customSyllableCountAgrees() {
        PinyinDict dict = PinyinDict.create(new NlpResourceLoader(TestHelper.createTestConfig()));
        Map<String, Pinyin[]> custom = new TreeMap<>();
        custom.put("甲乙", Tex2PinyinComputer.parse("jia1,yi1,bing1,bing3"));
        custom.put("甲乙丙", Tex2PinyinComputer.parse("jia3,yi3,bing1"));
        custom.put("乙丙", Tex2PinyinComputer.parse("yi4,bing1"));
        Tex2PinyinComputer computer = new Tex2PinyinComputer(dict, custom);

        TreeMap<String, Pinyin[]> merged = new TreeMap<>(dict.getPinyinTable());
        merged.putAll(custom);
        DoubleArrayTrieMap<Pinyin[]> words = new DoubleArrayTrieMap<>(merged);

        for (String text : new String[]{"甲乙丙", "甲乙", "x甲乙y", "乙丙丁"}) {
            assertEquals(wordNet(words, text), tones(computer, text), text);
        }
    }

    private static List<String> tones(Tex2PinyinComputer computer, String text) {
        return computer.text2Pinyin(text).getPinyinList().stream().map(Pinyin::toString).toList();
    }

    private static List<String> wordNet(DoubleArrayTrieMap<Pinyin[]> words, String text) {
        char[] chars = text.toCharArray();
        Pinyin[][] chosen = new Pinyin[chars.length][];
        DoubleArrayTrieMap<Pinyin[]>.MapMatcher matcher = words.match(chars);
        while (matcher.next()) {
            int begin = matcher.getBegin();
            int length = matcher.getLength();
            if (chosen[begin] == null || length > chosen[begin].length) {
                Pinyin[] value = matcher.getValue();
                chosen[begin] = length == 1 ? new Pinyin[]{value[0]} : value;
            }
        }
        List<String> tones = new ArrayList<>();
        for (int offset = 0; offset < chars.length; ) {
            if (chosen[offset] == null) {
                tones.add(Pinyin.none5.toString());
                offset++;
                continue;
            }
            for (Pinyin pinyin : chosen[offset]) {
                tones.add(pinyin.toString());
            }
            offset += chosen[offset].length;
        }
        return tones;
    }
}
