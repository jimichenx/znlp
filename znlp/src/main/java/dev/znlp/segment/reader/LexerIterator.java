package dev.znlp.segment.reader;

import dev.znlp.base.common.ParagraphReader;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * 按段落驱动分词器，并把段落内偏移换算成全文偏移。
 *
 * @author jimichan
 */
public class LexerIterator implements Iterator<Token> {

    private final WordSegmenter wordSegmenter;
    private final ParagraphReader paragraphReader;

    /**
     * 当前段落在全文中的起始偏移。
     */
    private int paragraphStart;

    /**
     * 已经交给分词器的字符数。
     */
    private int consumed;

    private final ArrayDeque<Token> buffer = new ArrayDeque<>();
    private Token nextElement;

    public LexerIterator(WordSegmenter wordSegmenter, ParagraphReader paragraphReader) {
        this.wordSegmenter = wordSegmenter;
        this.paragraphReader = paragraphReader;
        this.nextElement = computeNextElement();
    }

    @Override
    public boolean hasNext() {
        return nextElement != null;
    }

    @Override
    public Token next() {
        Token current = nextElement;
        nextElement = computeNextElement();
        return current;
    }

    private Token computeNextElement() {
        while (buffer.isEmpty()) {
            String paragraph = readParagraph();
            if (paragraph == null) {
                return null;
            }
            paragraphStart = consumed;
            consumed += paragraph.length();
            if (paragraph.isEmpty()) {
                continue;
            }
            wordSegmenter.segment(paragraph.toCharArray(), buffer::add);
        }

        Token term = buffer.removeFirst();
        if (paragraphStart != 0) {
            term.setOffset(term.getOffset() + paragraphStart);
            if (term.hasSubword()) {
                for (Token subword : term.getSubword()) {
                    subword.setOffset(subword.getOffset() + paragraphStart);
                }
            }
        }
        return term;
    }

    private String readParagraph() {
        try {
            return paragraphReader.next();
        } catch (IOException e) {
            throw new RuntimeException("Error reading paragraph", e);
        }
    }
}
