package dev.znlp.segment.reader;

import dev.znlp.base.common.ParagraphReader;
import dev.znlp.base.common.ParagraphReaderSmart;
import dev.znlp.segment.TokenSequence;
import dev.znlp.segment.WordSegmentReader;
import dev.znlp.segment.WordSegmenter;

import java.io.IOException;
import java.io.Reader;

/**
 * 默认的分词读取器实现。
 *
 * <p>此类负责将 {@link WordSegmenter} 和输入源（String 或 Reader）
 * 组合成 {@link TokenSequence}。</p>
 *
 * @author jimichan
 */
public class DefaultWordSegmentReader implements WordSegmentReader {

    private final WordSegmenter wordSegmenter;

    public DefaultWordSegmentReader(WordSegmenter wordSegmenter) {
        if (wordSegmenter == null) {
            throw new NullPointerException("wordSegmenter cannot be null");
        }
        this.wordSegmenter = wordSegmenter;
    }

    @Override
    public TokenSequence segment(Reader reader) {
        if (reader == null) {
            throw new NullPointerException("reader cannot be null");
        }
        return new TokenSequence(
                new LexerIterator(wordSegmenter, new ParagraphReaderSmart(reader))
        );
    }

    @Override
    public TokenSequence segment(String text) {
        if (text == null) {
            throw new NullPointerException("text cannot be null");
        }
        return new TokenSequence(new LexerIterator(wordSegmenter, readerFor(text)));
    }

    /**
     * 不超过一段上限的字符串直接返回原文，避免再切片拷贝。
     */
    static ParagraphReader readerFor(String text) {
        if (text.length() <= ParagraphReaderSmart.defaultMaxLength()) {
            return new SingleParagraph(text);
        }
        return new ParagraphReaderSmart(text);
    }

    static final class SingleParagraph implements ParagraphReader {
        private String text;

        SingleParagraph(String text) {
            this.text = text;
        }

        @Override
        public String next() throws IOException {
            String result = text;
            text = null;
            return result;
        }
    }
}
