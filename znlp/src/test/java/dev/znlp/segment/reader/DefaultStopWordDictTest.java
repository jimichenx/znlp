package dev.znlp.segment.reader;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultStopWordDictTest {

    @Test
    void rebuildReplacesDatAndDiscardKeepsCurrentResult() {
        DefaultStopWordDict dict = new DefaultStopWordDict();
        dict.add("的");
        dict.addNoStop("了");
        assertFalse(dict.contains("的"));

        dict.rebuild();
        assertTrue(dict.contains("的"));
        assertFalse(dict.contains("了"));
        assertTrue(dict.hasPendingEdits());

        dict.discardEditBuffer();
        assertFalse(dict.hasPendingEdits());
        assertTrue(dict.contains("的"));

        dict.add("新");
        assertFalse(dict.contains("新"));
        dict.rebuild();
        assertTrue(dict.contains("新"));
        assertFalse(dict.contains("的"));
    }

    @Test
    void filterDropsStopwordAndAdvancesPosInc() {
        DefaultStopWordDict dict = new DefaultStopWordDict();
        dict.add("的");
        dict.rebuild();
        WordSegmenter segmenter = (text, consumer) -> {
            consumer.accept(new Token("我", null, 0));
            consumer.accept(new Token("的", null, 1));
            consumer.accept(new Token("书", null, 2));
        };

        List<Token> tokens = new StopwordFilter(new DefaultWordSegmentReader(segmenter), dict)
                .segment("x")
                .toList();

        assertEquals(List.of("我", "书"), tokens.stream().map(token -> token.word).toList());
        assertEquals(2, tokens.get(1).getPosInc());
    }
}
