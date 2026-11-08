package dev.znlp.segment.impl.dict;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.segment.impl.bigram.BiGramTableDictionary;
import dev.znlp.segment.impl.bigram.BiGramTableDictionaryFactory;
import dev.znlp.test.TestTags;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
public class CoreDictionaryTest {

    @Test
    public void coreWordsAndBigramExist() {
        ZnlpSystemConfig config = TestHelper.createTestConfig();

        NlpResourceLoader loader = new NlpResourceLoader(config);

        CoreDictionaryFactory coreDictionaryFactory = new CoreDictionaryFactory(config, loader);
        CoreDictionary coreDictionary = coreDictionaryFactory.create();

        assertTrue(coreDictionary.contains("陈"));
        assertTrue(coreDictionary.contains("你"));
        assertTrue(coreDictionary.contains("好"));

        BiGramTableDictionaryFactory bi = new BiGramTableDictionaryFactory(config, loader, coreDictionary);
        BiGramTableDictionary bif = bi.create();

        int nihao = bif.bigramFrequency(coreDictionary.wordId("你"), coreDictionary.wordId("好"));
        assertTrue(nihao > 0, "你->好 二元频次应大于 0");
    }
}
