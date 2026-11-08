package dev.znlp.segment.impl.cws;

import dev.znlp.base.resources.NlpResourceLoader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;
import dev.znlp.test.TestTags;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
class CwsPluginTest {

    @Test
    void cwsOnlyPipelineSegmentsText() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        WordSegmenter segmenter = SegmentRecipe.perceptron(registry).build();

        List<String> words = segmenter.segmentToStrings("自然语言处理技术");
        assertFalse(words.isEmpty());
        assertEquals(String.join("", words), "自然语言处理技术");
    }

    @Test
    void bundledModelLoadsAndSplits() throws Exception {
        CwsPerceptron cws = CwsPerceptron.load(
                TestHelper.createTestServiceRegistry().getService(NlpResourceLoader.class));
        List<String> words = cws.split("南京市长江大桥");
        assertFalse(words.isEmpty());
        assertEquals("南京市长江大桥", String.join("", words));
    }
}
