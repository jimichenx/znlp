package dev.znlp;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZnlpSegmentTest {

    @Test
    void nullTextThrows() {
        assertThrows(IllegalArgumentException.class, () -> Znlp.segment(null));
        assertThrows(IllegalArgumentException.class, () -> Znlp.segmentToStrings(null));
    }

    @Test
    void emptyTextReturnsEmptyList() {
        assertTrue(Znlp.segment("").isEmpty());
        assertTrue(Znlp.segmentToStrings("").isEmpty());
    }

    @Test
    void segmentAndSegmentToStringsAreConsistent() {
        String text = "南京市长江大桥";
        List<Token> tokens = Znlp.segment(text);
        List<String> words = Znlp.segmentToStrings(text);

        assertEquals(tokens.size(), words.size());
        for (int i = 0; i < tokens.size(); i++) {
            assertEquals(tokens.get(i).word, words.get(i));
        }
        assertEquals(List.of("南京市", "长江", "大桥"), words);
    }

    @Test
    void offsetsAreContinuous() {
        String text = "我爱自然语言处理";
        List<Token> tokens = Znlp.segment(text);
        int offset = 0;
        for (Token token : tokens) {
            assertEquals(offset, token.offset);
            offset += token.word.length();
        }
        assertEquals(text.length(), offset);
    }

    @Test
    void defaultSegmenterIncludesPos() {
        List<Token> tokens = Znlp.segment("复旦大学在上海市");
        assertFalse(tokens.isEmpty());
        for (Token token : tokens) {
            assertNotNull(token.getNature());
            assertNotEquals(Nature.x, token.getNature());
        }
    }

    @Test
    void singleCharAndMixedLanguage() {
        List<String> single = Znlp.segmentToStrings("学");
        assertEquals(List.of("学"), single);

        List<String> mixed = Znlp.segmentToStrings("你好znlp");
        assertFalse(mixed.isEmpty());
        assertTrue(String.join("", mixed).contains("你好"));
    }

    @Test
    void segmenterSingletonMatchesFacade() {
        WordSegmenter segmenter = Znlp.segmenter();
        String text = "自然语言处理";
        assertEquals(segmenter.segmentToStrings(text), Znlp.segmentToStrings(text));
    }
}
