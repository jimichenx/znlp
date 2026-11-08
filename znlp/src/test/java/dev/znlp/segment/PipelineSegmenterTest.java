package dev.znlp.segment;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.segment.recipe.SegmentRecipe;
import dev.znlp.test.TestTags;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
class PipelineSegmenterTest {

    @Test
    void emptyInputReturnsEmptyList() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        WordSegmenter segmenter = SegmentRecipe.perceptron(registry).build();

        assertTrue(segmenter.segment("").isEmpty());
        assertTrue(segmenter.segmentToStrings("").isEmpty());
    }

    @Test
    void normalSentenceIsNotFullyFiltered() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        WordSegmenter segmenter = SegmentRecipe.perceptron(registry).build();

        var words = segmenter.segmentToStrings("自然语言处理很重要");
        assertFalse(words.isEmpty());
        assertTrue(words.size() >= 2);
    }

    @Test
    void sentenceBreakKeepsOffsetsInOriginalText() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        WordSegmenter segmenter = SegmentRecipe.dictionary(registry).build();

        var tokens = segmenter.segment("你好。世界");
        assertFalse(tokens.isEmpty());
        String rebuilt = tokens.stream().map(token -> token.word).reduce("", String::concat);
        assertEquals("你好。世界", rebuilt);
        assertTrue(tokens.stream().anyMatch(token -> token.getOffset() >= 3));
    }
}
