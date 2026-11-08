package dev.znlp.segment.reader;

import dev.znlp.segment.Token;
import dev.znlp.segment.TokenSequence;
import dev.znlp.segment.WordSegmentReader;

import java.io.Reader;
import java.util.Iterator;
import java.util.function.Predicate;

public abstract class BaseFilterWordSegmentReader implements WordSegmentReader, Predicate<Token> {

    private final WordSegmentReader source;

    private boolean enable = true;

    public BaseFilterWordSegmentReader(WordSegmentReader source) {
        this.source = source;
    }

    public WordSegmentReader getSource() {
        return source;
    }

    @Override
    public TokenSequence segment(Reader reader) {
        TokenSequence wts = source.segment(reader);
        if (!enable) {
            return wts;
        }
        Iterator<Token> iterator = wts.iterator();
        Iterator<Token> change = new FilterWordItemIterator(iterator, this);
        return new TokenSequence(change);
    }

    @Override
    public TokenSequence segment(String text) {
        TokenSequence wts = source.segment(text);
        if (!enable) {
            return wts;
        }
        Iterator<Token> iterator = wts.iterator();
        Iterator<Token> change = new FilterWordItemIterator(iterator, this);
        return new TokenSequence(change);
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
