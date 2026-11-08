package dev.znlp.segment.reader;

import org.junit.jupiter.api.Test;
import dev.znlp.base.common.ParagraphReader;
import dev.znlp.base.common.ParagraphReaderSmart;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWordSegmentReaderTest {

    @Test
    void shortTextReusesOriginalString() throws Exception {
        String text = "你好世界";
        ParagraphReader reader = DefaultWordSegmentReader.readerFor(text);
        assertSame(text, reader.next());
    }

    @Test
    void longTextIsSplitAtDefaultLimit() throws Exception {
        int maxLength = ParagraphReaderSmart.defaultMaxLength();
        String text = "A".repeat(maxLength + 10);
        ParagraphReader reader = DefaultWordSegmentReader.readerFor(text);

        assertEquals(maxLength, reader.next().length());
        assertEquals(10, reader.next().length());
    }

    @Test
    void offsetsStayContinuousAcrossChunks() {
        int maxLength = ParagraphReaderSmart.defaultMaxLength();
        String text = "B".repeat(maxLength + 3);
        List<Integer> paragraphLengths = new ArrayList<>();
        WordSegmenter segmenter = (chars, consumer) -> {
            paragraphLengths.add(chars.length);
            for (int i = 0; i < chars.length; i++) {
                consumer.accept(new Token(String.valueOf(chars[i]), null, i));
            }
        };

        List<Token> tokens = new DefaultWordSegmentReader(segmenter).segment(text).toList();

        assertEquals(List.of(maxLength, 3), paragraphLengths);
        assertEquals(text.length(), tokens.size());
        assertEquals(0, tokens.get(0).getOffset());
        assertEquals(maxLength, tokens.get(maxLength).getOffset());
        assertEquals(text.length() - 1, tokens.get(tokens.size() - 1).getOffset());
    }

    @Test
    void readerInputUsesSameOffsets() {
        WordSegmenter segmenter = (chars, consumer) -> {
            for (int i = 0; i < chars.length; i++) {
                consumer.accept(new Token(String.valueOf(chars[i]), null, i));
            }
        };
        List<Token> tokens = new DefaultWordSegmentReader(segmenter)
                .segment(new StringReader("甲乙"))
                .toList();

        assertEquals(List.of("甲", "乙"), tokens.stream().map(token -> token.word).toList());
        assertEquals(0, tokens.get(0).getOffset());
        assertEquals(1, tokens.get(1).getOffset());
    }

    @Test
    void emptyTextProducesNothing() {
        WordSegmenter segmenter = (chars, consumer) -> consumer.accept(new Token("不该出现"));
        assertTrue(new DefaultWordSegmentReader(segmenter).segment("").toList().isEmpty());
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new DefaultWordSegmentReader(null));
        WordSegmenter segmenter = (chars, consumer) -> {
        };
        DefaultWordSegmentReader reader = new DefaultWordSegmentReader(segmenter);
        assertThrows(NullPointerException.class, () -> reader.segment((String) null));
        assertThrows(NullPointerException.class, () -> reader.segment((StringReader) null));
    }
}
