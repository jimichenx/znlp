package dev.znlp.duckling.grammar.duration;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.grammar.time.date.TimeGrainNodeValue;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 时长：数字加秒/分/小时等粒度，可组合如「1小时30分」。 */
public class DurationGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        // rule("{num} Gain", filter<NumberNodeValue> { it.number.toFloat() > 0 }, any<TimeGrainNodeValue>())
        rules.add(RuleBuilder.rule("{num} Gain")
                .filter(NumberNodeValue.class, n -> n.getDouble() > 0)
                .filter(TimeGrainNodeValue.class, g -> true)
                .production((text, nodes) -> {
                    NumberNodeValue n = (NumberNodeValue) nodes.get(0).getValue();
                    TimeGrainNodeValue grainNode = (TimeGrainNodeValue) nodes.get(1).getValue();
                    float num = (float) n.getDouble();

                    switch (grainNode.grain()) {
                        case Year:
                            DurationDimension dYear = new DurationDimension(num, Grain.Year);
                            if (num >= 1000) dYear.setLatent(true);
                            return dYear;
                        case Month:
                            return new DurationDimension(num, Grain.Month);
                        case Day:
                            return new DurationDimension(num, Grain.Day);
                        case Hour:
                            return new DurationDimension(num, Grain.Hour);
                        case Minute:
                            DurationDimension dMinute = new DurationDimension(num, Grain.Minute);
                            if ("分".equals(grainNode.text())) dMinute.setLatent(true);
                            return dMinute;
                        case Second:
                            return new DurationDimension(num, Grain.Second);
                        case Week:
                            return new DurationDimension(num * 7, Grain.Day);
                        default:
                            return null;
                    }
                })
                .build());

        // rule("Gain 半", any<DurationNodeValue>(), dict("半"))
        rules.add(RuleBuilder.rule("Gain 半")
                .filter(DurationDimension.class, d -> true)
                .dict("半")
                .production((text, nodes) -> {
                    DurationDimension d = (DurationDimension) nodes.get(0).getValue();
                    return d.copy(d.getValue() + 0.5f);
                })
                .build());

        // rule("grain 零 grain", any<DurationNodeValue>(), dict("零"), any<DurationNodeValue>())
        rules.add(RuleBuilder.rule("grain 零 grain")
                .filter(DurationDimension.class, d -> true)
                .dict("零")
                .filter(DurationDimension.class, d -> true)
                .production((text, nodes) -> {
                    DurationDimension d1 = (DurationDimension) nodes.get(0).getValue();
                    DurationDimension d2 = (DurationDimension) nodes.get(2).getValue();

                    if (d1.getGrain().justStep(d2.getGrain())) {
                        float multiplier = 1;
                        switch (d1.getGrain()) {
                            case Year: multiplier = 12; break; // Month
                            case Month: multiplier = 30; break; // Day
                            case Day: multiplier = 24; break; // Hour
                            case Hour: multiplier = 60; break; // Minute
                            case Minute: multiplier = 60; break; // Second
                            default: break;
                        }
                        return new DurationDimension(d1.getValue() * multiplier + d2.getValue(), d2.getGrain());
                    }
                    return null;
                })
                .build());

        // rule("grain grain", any<DurationNodeValue>(), any<DurationNodeValue>())
        rules.add(RuleBuilder.rule("grain grain")
                .filter(DurationDimension.class, d -> true)
                .filter(DurationDimension.class, d -> true)
                .production((text, nodes) -> {
                    DurationDimension d1 = (DurationDimension) nodes.get(0).getValue();
                    DurationDimension d2 = (DurationDimension) nodes.get(1).getValue();

                    if (d1.getGrain().justStep(d2.getGrain())) {
                        float multiplier = 1;
                        switch (d1.getGrain()) {
                            case Year: multiplier = 12; break; // Month
                            case Month: multiplier = 30; break; // Day
                            case Day: multiplier = 24; break; // Hour
                            case Hour: multiplier = 60; break; // Minute
                            case Minute: multiplier = 60; break; // Second
                            default: break;
                        }
                        return new DurationDimension(d1.getValue() * multiplier + d2.getValue(), d2.getGrain());
                    }
                    return null;
                })
                .build());

        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        examples.addAll(createExamples(check(1f, Grain.Hour), "一小时"));
        examples.addAll(createExamples(check(65f, Grain.Minute), "一小时零5分"));
        return examples;
    }

    private Predicate<Dimension> check(float value, Grain grain) {
        return nodeValue -> {
            if (nodeValue instanceof DurationDimension d) {
                return d.getGrain() == grain && Math.abs(d.getValue() - value) < 0.001f;
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
