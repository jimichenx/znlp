package dev.znlp.perceptron;

import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.segment.impl.cws.CwsPerceptron;
import dev.znlp.segment.impl.ner.NERPerceptron;
import dev.znlp.segment.impl.pos.PosPerceptron;
import dev.znlp.pinyin.spliter.PinyinSplitPerceptron;
import dev.znlp.test.TestTags;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PerceptronModelLoadTest {

    @Test
    void readBundledModelsFromClasspath() throws IOException {
        assertNotNull(PerceptronModel.readFromClasspath("znlp/ner/ner.model").getDecoder());
        assertNotNull(PerceptronModel.readFromClasspath("znlp/pos-model/pos.model").getFeatureSet());
        assertNotNull(PerceptronModel.readFromClasspath("znlp/personname/nr-name.model").getDecoder());
        assertNotNull(PerceptronModel.readFromClasspath("znlp/pinyin/pinyin-split.model").getFeatureSet());
    }

    @Test
    @Tag(TestTags.HEAVY)
    void readFromNlpResourceLoader() throws IOException {
        NlpResourceLoader loader = new NlpResourceLoader(TestHelper.createTestConfig());
        assertNotNull(PerceptronModel.readFromNlpResource(loader, "znlp/ner/ner.model"));
    }

    @Test
    void badFileFailsClearly() throws Exception {
        Path bad = Files.createTempFile("bad-model", ".bin");
        try {
            Files.write(bad, "not-a-perceptron-model".getBytes(StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> PerceptronModel.read(bad.toFile()));
        } finally {
            Files.deleteIfExists(bad);
        }
    }

    @Test
    @Tag(TestTags.HEAVY)
    void bundledModelsDecodeNonEmpty() throws Exception {
        assertFalse(CwsPerceptron.load(new NlpResourceLoader(TestHelper.createTestConfig())).split("测试").isEmpty());
        assertFalse(NERPerceptron.loadFromClasspath("znlp/ner/ner.model")
                .decode(java.util.List.of(new Token("北京", Nature.ns)))
                .isEmpty());
        assertFalse(PosPerceptron.loadFromClasspath("znlp/pos-model/pos.model")
                .decode(java.util.List.of("测试")).isEmpty());
        assertFalse(PinyinSplitPerceptron.load("znlp/pinyin/pinyin-split.model").split("zhongwen").isEmpty());
    }
}
