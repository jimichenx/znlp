package dev.znlp.segment.impl.ner;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;
import dev.znlp.test.TestTags;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
class NerPluginTest {

    @Test
    void nerRecipeAlsoTagsPartOfSpeech() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        WordSegmenter segmenter = SegmentRecipe.perceptron(registry).withNer().build();

        List<Token> tokens = segmenter.segment("北京大学");
        assertFalse(tokens.isEmpty());
        assertTrue(tokens.stream().anyMatch(token -> token.getNature() != null));
    }

    @Test
    void posRecipeSegmentsWithoutRequiringNer() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        WordSegmenter segmenter = SegmentRecipe.perceptron(registry).withPos().build();
        assertFalse(segmenter.segmentToStrings("北京大学").isEmpty());
    }
}
