package dev.znlp.segment.impl.correction;

import org.junit.jupiter.api.Test;
import dev.znlp.converter.TransformDictionaryImpl;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CorrectionWordpathProcessorTest {

    @Test
    void dictionaryBuildsAndMatchesLongestPhrase() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("乒乓球拍", "乒乓球拍");
        TransformDictionaryImpl dict = new TransformDictionaryImpl(map);
        assertEquals("乒乓球拍", dict.transform("乒乓球拍"));
    }

    @Test
    void customDictionaryCanSplitMergedToken() {
        MemCorrectionDictionary dictionary = new MemCorrectionDictionary();
        dictionary.addRule("乒乓球拍/卖/完了");
        assertNotNull(dictionary.getTrie().get("乒乓球拍卖完了"));

        CorrectionWordpathProcessor processor = new CorrectionWordpathProcessor(dictionary);

        char[] text = "乒乓球拍卖完了".toCharArray();
        Wordnet wordnet = new Wordnet(text);
        wordnet.put(0, text.length);

        Wordpath path = new Wordpath(wordnet);
        path.combine(0, text.length);
        assertEquals(1, path.wordCount());

        Wordpath corrected = processor.process(path);
        assertNotNull(corrected);
        assertEquals(3, corrected.wordCount());
    }

    @Test
    void batchRulesPublishTogetherAndReplaceAllDropsOldRules() {
        MemCorrectionDictionary dictionary = new MemCorrectionDictionary();
        dictionary.addRule("甲/乙");
        dictionary.addRules(List.of("丙/丁", "戊/己"));

        assertNotNull(dictionary.getTrie().get("甲乙"));
        assertNotNull(dictionary.getTrie().get("丙丁"));
        assertNotNull(dictionary.getTrie().get("戊己"));

        dictionary.replaceAll(Stream.of("庚/辛"));

        assertNull(dictionary.getTrie().get("甲乙"));
        assertNotNull(dictionary.getTrie().get("庚辛"));

        dictionary.clear();
        assertNull(dictionary.getTrie());
    }

    @Test
    void failedReplaceAllKeepsPublishedRules() {
        MemCorrectionDictionary dictionary = new MemCorrectionDictionary();
        dictionary.addRule("甲/乙");
        var published = dictionary.getTrie();

        assertThrows(NullPointerException.class, () -> dictionary.replaceAll(Stream.of("庚/辛", null)));

        assertSame(published, dictionary.getTrie());
        assertNotNull(dictionary.getTrie().get("甲乙"));
    }
}
