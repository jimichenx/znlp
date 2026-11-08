package dev.znlp.segment.reader;

import dev.znlp.base.common.Characters;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmentReader;

import java.util.function.Predicate;

/**
 * 过滤标点符号
 *
 * @author jimichan
 */
public class PunctuationFilter extends BaseFilterWordSegmentReader implements Predicate<Token> {


    public PunctuationFilter() {
        super(null);
    }

    public PunctuationFilter(WordSegmentReader source) {
        super(source);
    }

    /**
     * 整词每个字符都是标点时返回 false，表示排除。
     */
    @Override
    public boolean test(Token term) {
        int wordLen = term.word.length();
        if (wordLen == 0) {
            return false;
        }
        for (int i = 0; i < wordLen; i++) {
            if (!Characters.isPunctuation(term.word.charAt(i))) {
                return true;
            }
        }
        return false;
    }

}
