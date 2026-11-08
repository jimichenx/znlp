package dev.znlp.duckling.grammar.distance;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.pattern.TextToken;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 距离：数字加公里/米等单位。 */
public class DistanceGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        rules.add(RuleBuilder.rule("distance {num} 单位(公里|米)")
                .filter(NumberNodeValue.class, n -> true)
                .pattern("公里|米|km|里|m")
                .production((text, nodes) -> {
                    NumberNodeValue numNode = (NumberNodeValue) nodes.get(0).getValue();
                    String unitTxt = ((TextToken) nodes.get(1).getValue()).text();
                    float num = (float) numNode.getDouble();

                    DistanceDimension.DistanceUnit unit;
                    switch (unitTxt.toLowerCase()) {
                        case "公里":
                        case "km":
                            unit = DistanceDimension.DistanceUnit.KM;
                            break;
                        case "里":
                            num *= 0.5f;
                            unit = DistanceDimension.DistanceUnit.KM;
                            break;
                        case "米":
                        case "m":
                            unit = DistanceDimension.DistanceUnit.M;
                            break;
                        default:
                            return null;
                    }
                    return new DistanceDimension(num, unit);
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check(125f), "125公里", "125km", "250里", "125000米", "一百二十五公里"));
        return examples;
    }

    private Predicate<Dimension> check(float value) {
        return nodeValue -> {
            if (nodeValue instanceof DistanceDimension) {
                float km = ((DistanceDimension) nodeValue).toKm();
                return km == value;
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
