package dev.znlp.duckling.pattern;

import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.support.DkNode;

import java.util.*;

/**
 * 词表 pattern。真正扫描由 {@link DefaultRuleSet} 的 DAT 完成，这里只按词取回命中。
 */
public class DictPattern extends TextDucklingPattern {

    private final Set<String> wordBag;

    public DictPattern(int id, Set<String> wordBag) {
        super(id);
        this.wordBag = new TreeSet<>(wordBag);
    }

    public Set<String> getWordBag() {
        return wordBag;
    }

    @Override
    public List<DkNode<TextToken>> predicate(String text, Map<String, List<DkNode<TextToken>>> dictMatch) {
        List<DkNode<TextToken>> list = new ArrayList<>();
        for (String word : wordBag) {
            List<DkNode<TextToken>> nodes = dictMatch.get(word);
            if (nodes != null) {
                list.addAll(nodes);
            }
        }
        return list;
    }
}
