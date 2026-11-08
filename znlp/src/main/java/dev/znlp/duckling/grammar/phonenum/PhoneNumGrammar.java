package dev.znlp.duckling.grammar.phonenum;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.pattern.TextToken;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 大陆 11 位手机号 {@code 1\\d{10}}。 */
public class PhoneNumGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        rules.add(RuleBuilder.rule("phone {num}")
                .pattern("1\\d{10}")
                .production((text, nodes) -> {
                    String val = ((TextToken) nodes.get(0).getValue()).text();
                    return new PhoneNumDimension(val);
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check("18217117903"), "18217117903"));
        return examples;
    }

    private Predicate<Dimension> check(String value) {
        return nodeValue -> {
            if (nodeValue instanceof PhoneNumDimension) {
                return ((PhoneNumDimension) nodeValue).getValue().equals(value);
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
