package dev.znlp.duckling.pattern;

import dev.znlp.duckling.support.DkNode;

import java.util.List;
import java.util.Map;

/**
 * 直接打在原文上的 pattern（词典或正则），产出 {@link TextToken} 叶子。
 */
public abstract class TextDucklingPattern extends DucklingPattern<TextToken> {

    public TextDucklingPattern(int id) {
        super(id);
    }

    /**
     * @param text      原文
     * @param dictMatch 整句 DAT 命中，按词分组；正则实现可忽略
     */
    public abstract List<DkNode<TextToken>> predicate(String text, Map<String, List<DkNode<TextToken>>> dictMatch);
}
