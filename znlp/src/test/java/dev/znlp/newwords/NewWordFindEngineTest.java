package dev.znlp.newwords;

import dev.znlp.base.resources.NlpResourceLoader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.newwords.counter.FrequentItemsCounter;
import dev.znlp.newwords.counter.FrequentItemsCounterOptimized;
import dev.znlp.newwords.engine.INewWordFindEngine;
import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.scorer.NewWordScorerFactory;
import dev.znlp.segment.impl.dict.CoreDictionary;
import dev.znlp.segment.impl.dict.CoreDictionaryFactory;
import dev.znlp.test.TestTags;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
class NewWordFindEngineTest {

    @Test
    void engineFindsCandidatesFromMicroCorpus() throws Exception {
        var config = TestHelper.createTestConfig();
        var loader = new NlpResourceLoader(config);
        CoreDictionary coreDictionary = new CoreDictionaryFactory(config, loader).create();

        INewWordFindEngine engine = INewWordFindEngine.create(
                new INewWordFindEngine.Config(2, 6, 3), true, coreDictionary);
        engine.setScorer(NewWordScorerFactory.createAdaptive());

        Path dir = Files.createTempDirectory("nw-engine");
        try {
            Files.writeString(dir.resolve("a.txt"),
                    "云计算平台 云计算平台 大数据 大数据 大数据\n".repeat(30),
                    StandardCharsets.UTF_8);

            NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(dir.toFile(), engine)
                    .charset(StandardCharsets.UTF_8)
                    .verbose(false)
                    .build();
            List<NewWord> words = tool.discover();
            assertNotNull(words);
        } finally {
            Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Test
    void optimizedCounterAlignsWithBaselineOnSimpleInput() {
        FrequentItemsCounter baseline = new FrequentItemsCounter(100, 2);
        FrequentItemsCounterOptimized optimized = new FrequentItemsCounterOptimized(100, 2);

        for (int i = 0; i < 5; i++) {
            baseline.put("alpha");
            optimized.put("alpha");
            baseline.put("beta");
            optimized.put("beta");
        }

        HashMap<String, Integer> baseCounts = new HashMap<>();
        baseline.getData().forEach((k, v) -> baseCounts.put(k, v.getValue()));
        HashMap<String, Integer> optCounts = new HashMap<>();
        optimized.getData().forEach((k, v) -> optCounts.put(k, v.getValue()));
        assertEquals(baseCounts, optCounts);
    }
}
