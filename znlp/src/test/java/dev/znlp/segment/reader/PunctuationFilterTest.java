package dev.znlp.segment.reader;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmentReader;
import dev.znlp.segment.WordSegmenter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunctuationFilterTest {

    private final PunctuationFilter filter = new PunctuationFilter();

    @Test
    void keepsWordUnlessEveryCharacterIsPunctuation() {
        assertFalse(filter.test(new Token("")));
        assertFalse(filter.test(new Token("。")));
        assertFalse(filter.test(new Token("！！")));
        assertFalse(filter.test(new Token("！！！！")));
        assertTrue(filter.test(new Token("你好")));
        assertTrue(filter.test(new Token("！！！A")));
        assertTrue(filter.test(new Token("A！！！")));
    }

    @Test
    void pipelineDropsPunctuationAndAdvancesPosInc() {
        WordSegmenter segmenter = (text, consumer) -> {
            consumer.accept(new Token("你好", null, 0));
            consumer.accept(new Token("！！！A", null, 2));
            consumer.accept(new Token("。。。", null, 6));
            consumer.accept(new Token("世界", null, 9));
        };
        WordSegmentReader reader = new PunctuationFilter(new DefaultWordSegmentReader(segmenter));

        List<Token> tokens = reader.segment("ignored").toList();

        assertEquals(List.of("你好", "！！！A", "世界"), tokens.stream().map(token -> token.word).toList());
        assertEquals(1, tokens.get(0).getPosInc());
        assertEquals(1, tokens.get(1).getPosInc());
        assertEquals(2, tokens.get(2).getPosInc());
    }
}
