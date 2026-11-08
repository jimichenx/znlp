package dev.znlp.duckling.grammar.money;

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

/** 金额规则：数字加「元/角/分」等单位。 */
public class MoneyGrammar implements DucklingGrammar {

    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        // rule("money {int} unit", any<NumberRule.NumberNodeValue>(), regex("块|元|圆|角|毛|分|元整"))
        rules.add(RuleBuilder.rule("money {int} unit")
                .filter(NumberNodeValue.class, n -> true)
                .pattern("块|元|圆|角|毛|分|元整")
                .production((text, nodes) -> {
                    NumberNodeValue numNode = (NumberNodeValue) nodes.get(0).getValue();
                    String unitTxt = ((TextToken) nodes.get(1).getValue()).text();
                    float num = (float) numNode.getDouble();

                    switch (unitTxt) {
                        case "元":
                        case "圆":
                        case "块":
                        case "元整":
                            return new MoneyDimension(num, 1);
                        case "角":
                        case "毛":
                            if (!numNode.isInt()) return null;
                            return new MoneyDimension(num * 0.1f, 2);
                        case "分":
                            if (!numNode.isInt()) return null;
                            return new MoneyDimension(num * 0.01f, 3);
                        default:
                            return null;
                    }
                })
                .build());

        // rule("money {monney}+{monney}", any<MoneyNodeValue>(), any<MoneyNodeValue>())
        rules.add(RuleBuilder.rule("money {monney}+{monney}")
                .filter(MoneyDimension.class, m -> true)
                .filter(MoneyDimension.class, m -> true)
                .production((text, nodes) -> {
                    MoneyDimension m1 = (MoneyDimension) nodes.get(0).getValue();
                    MoneyDimension m2 = (MoneyDimension) nodes.get(1).getValue();

                    if (m1.getGrain() < m2.getGrain()) {
                        return new MoneyDimension(m1.getValue() + m2.getValue(), m2.getGrain());
                    } else {
                        return null;
                    }
                })
                .build());

        // rule("money 三块8", filter<MoneyNodeValue> { it.grain <=2 }, filter<NumberRule.NumberNodeValue> { it.isInt() && it.int < 10 && it.int>0 })
        rules.add(RuleBuilder.rule("money 三块8")
                .filter(MoneyDimension.class, m -> m.getGrain() <= 2)
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() < 10 && n.getInt() > 0)
                .production((text, nodes) -> {
                    MoneyDimension m1 = (MoneyDimension) nodes.get(0).getValue();
                    NumberNodeValue n2 = (NumberNodeValue) nodes.get(1).getValue();

                    int g = m1.getGrain() + 1;
                    float x = (g == 2) ? 0.1f : 0.01f;
                    float value = m1.getValue() + x * n2.getInt();
                    return new MoneyDimension(value, g);
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check(99.5f), "99元5毛", "九十九块五"));
        examples.addAll(createExamples(check(33.53f), "三十三元五毛3分", "33.53元"));
        examples.addAll(createExamples(check(3.8f), "三块八"));
        examples.addAll(createExamples(check(55.55f), "55块5毛5"));
        return examples;
    }

    private Predicate<Dimension> check(float value) {
        return nodeValue -> {
            if (nodeValue instanceof MoneyDimension) {
                // Use epsilon for float comparison
                return Math.abs(((MoneyDimension) nodeValue).getValue() - value) < 0.001f;
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
