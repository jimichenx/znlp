package dev.znlp.duckling.grammar.temperature;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 温度：数字加摄氏度。 */
public class TemperatureGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        rules.add(RuleBuilder.rule("Temperature {num} 摄氏度")
                .filter(NumberNodeValue.class, n -> true)
                .pattern("摄氏度|度|℃")
                .production((text, nodes) -> {
                    NumberNodeValue num = (NumberNodeValue) nodes.get(0).getValue();
                    return new TemperatureDimension((float) num.getDouble());
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check(25.5f), "二十五点五摄氏度", "25.5度", "25.5℃"));
        return examples;
    }

    private Predicate<Dimension> check(float value) {
        return nodeValue -> {
            if (nodeValue instanceof TemperatureDimension) {
                // Use epsilon for float comparison
                return Math.abs(((TemperatureDimension) nodeValue).getValue() - value) < 0.001f;
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
