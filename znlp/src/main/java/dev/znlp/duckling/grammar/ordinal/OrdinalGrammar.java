package dev.znlp.duckling.grammar.ordinal;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;
import dev.znlp.duckling.support.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 序数：「第」+ 数字。 */
public class OrdinalGrammar implements DucklingGrammar {

    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        
        rules.add(RuleBuilder.rule("Ordinal {num}")
                .dict("第")
                .filter(NumberNodeValue.class, n -> n.isInt())
                .production((text, nodes) -> {
                    NumberNodeValue num = (NumberNodeValue) nodes.get(1).getValue();
                    return new OrdinalDimension((int) num.getInt());
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        
        examples.addAll(createExamples(check(1), "第一", "第1"));
        
        return examples;
    }

    private Predicate<Dimension> check(int value) {
        return nodeValue -> {
            if (nodeValue instanceof OrdinalDimension) {
                return ((OrdinalDimension) nodeValue).getValue() == value;
            }
            return false;
        };
    }

    private List<Example> createExamples(Predicate<Dimension> predicate, String... texts) {
        List<Example> list = new ArrayList<>();
        for (String text : texts) {
            list.add(new Example(text, predicate));
        }
        return list;
    }
}
