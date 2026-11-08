package dev.znlp.duckling.grammar.percentage;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 百分比：{@code 5%}、{@code 百分之三点一四}。 */
public class PercentageGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        // rule("percentage {num}%", any<NumberNodeValue>(), regex("%|％"))
        rules.add(RuleBuilder.rule("percentage {num}%")
                .filter(NumberNodeValue.class, n -> true)
                .pattern("%|％")
                .production((text, nodes) -> {
                    NumberNodeValue num = (NumberNodeValue) nodes.get(0).getValue();
                    return new PercentageDimension((float) num.getDouble() / 100);
                })
                .build());

        // rule("percentage 百分之{num}", regex("百分之"), any<NumberNodeValue>())
        rules.add(RuleBuilder.rule("percentage 百分之{num}")
                .pattern("百分之")
                .filter(NumberNodeValue.class, n -> true)
                .production((text, nodes) -> {
                    NumberNodeValue num = (NumberNodeValue) nodes.get(1).getValue();
                    return new PercentageDimension((float) num.getDouble() / 100);
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check(0.05f), "5%", "百分之5", "百分之五"));
        examples.addAll(createExamples(check(0.054f), "5.4%", "百分之5点4", "百分之五点四"));
        return examples;
    }

    private Predicate<Dimension> check(float value) {
        return nodeValue -> {
            if (nodeValue instanceof PercentageDimension) {
                // Use epsilon for float comparison
                return Math.abs(((PercentageDimension) nodeValue).getValue() - value) < 0.0001f;
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
