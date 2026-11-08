package dev.znlp.duckling.grammar.age;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 年龄：整数岁，范围 (0, 130)。 */
public class AgeGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        rules.add(RuleBuilder.rule("Age {num} 岁")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getDouble() > 0 && n.getDouble() < 130)
                .dict("岁")
                .production((text, nodes) -> {
                    NumberNodeValue num = (NumberNodeValue) nodes.get(0).getValue();
                    return new AgeDimension((float) num.getDouble());
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check(30f), "三十岁", "30岁"));
        return examples;
    }

    private Predicate<Dimension> check(float value) {
        return nodeValue -> {
            if (nodeValue instanceof AgeDimension) {
                return ((AgeDimension) nodeValue).getValue() == value;
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
