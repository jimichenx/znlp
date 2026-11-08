package dev.znlp.segment.reader;

import dev.znlp.segment.Token;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * 能控制posInc的Iterator过滤器
 *
 * @author jimichan
 */
public class FilterWordItemIterator implements Iterator<Token> {

    private final Iterator<Token> source;
    private final Predicate<Token> predicate;
    private Token nextElement;

    public FilterWordItemIterator(Iterator<Token> source, Predicate<Token> predicate) {
        this.source = source;
        this.predicate = predicate;
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
        int pos = 1;
        while (source.hasNext()) {
            Token next = source.next();
            if (predicate.test(next)) {
                next.setPosInc(pos);
                return next;
            }
            pos++;
        }
        return null;
    }
}