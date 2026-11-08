package dev.znlp.segment.reader;

import org.junit.jupiter.api.Test;
import dev.znlp.base.common.ParagraphReader;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerIteratorTest {

    @Test
    void skipsEmptyAndTokenlessParagraphs() throws IOException {
        ParagraphReader paragraphs = new ListParagraphs("你", "", "。。。", "好");
        LexerIterator iterator = new LexerIterator(perCharSkipping("。。。"), paragraphs);

        List<Token> tokens = new ArrayList<>();
        while (iterator.hasNext()) {
            tokens.add(iterator.next());
        }

        assertEquals(List.of("你", "好"), tokens.stream().map(token -> token.word).toList());
        assertEquals(0, tokens.get(0).getOffset());
        assertEquals(4, tokens.get(1).getOffset());
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    void shiftsSubwordOffsetWithParagraph() throws IOException {
        WordSegmenter segmenter = (text, consumer) -> {
            Token parent = new Token(new String(text), null, 0);
            parent.setSubword(List.of(new Token("子", null, 1)));
            consumer.accept(parent);
        };
        LexerIterator iterator = new LexerIterator(segmenter, new ListParagraphs("甲", "乙丙"));

        assertEquals("甲", iterator.next().word);
        Token second = iterator.next();
        assertEquals(1, second.getOffset());
        assertEquals(2, second.getSubword().get(0).getOffset());
        assertNull(iterator.next());
    }

    @Test
    void emptyDocumentHasNoToken() {
        LexerIterator iterator = new LexerIterator(perCharSkipping(""), new ListParagraphs());
        assertFalse(iterator.hasNext());
    }

    @Test
    void wrapsReaderFailure() {
        ParagraphReader reader = () -> {
            throw new IOException("boom");
        };
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> new LexerIterator(perCharSkipping(""), reader));
        assertTrue(error.getCause() instanceof IOException);
    }

    private static WordSegmenter perCharSkipping(String skipped) {
        return (text, consumer) -> {
            String value = new String(text);
            if (value.equals(skipped)) {
                return;
            }
            for (int i = 0; i < text.length; i++) {
                consumer.accept(new Token(value.substring(i, i + 1), null, i));
            }
        };
    }

    private static final class ListParagraphs implements ParagraphReader {
        private final List<String> paragraphs;
        private int index;

        private ListParagraphs(String... paragraphs) {
            this.paragraphs = List.of(paragraphs);
        }

        @Override
        public String next() {
            if (index >= paragraphs.size()) {
                return null;
            }
            return paragraphs.get(index++);
        }
    }
}
