package dev.znlp.duckling.grammar;

import dev.znlp.duckling.grammar.age.AgeGrammar;
import dev.znlp.duckling.grammar.distance.DistanceGrammar;
import dev.znlp.duckling.grammar.duration.DurationGrammar;
import dev.znlp.duckling.grammar.money.MoneyGrammar;
import dev.znlp.duckling.grammar.number.NumberGrammar;
import dev.znlp.duckling.grammar.ordinal.OrdinalGrammar;
import dev.znlp.duckling.grammar.percentage.PercentageGrammar;
import dev.znlp.duckling.grammar.phonenum.PhoneNumGrammar;
import dev.znlp.duckling.grammar.temperature.TemperatureGrammar;
import dev.znlp.duckling.grammar.time.DateTimeGrammar;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;

import java.util.ArrayList;
import java.util.List;


/**
 * 默认解析器使用的语法注册表。新增维度时在静态块里 {@code grammars.add(...)}。
 */
public class Grammars {

    private static final List<DucklingGrammar> grammars = new ArrayList<>();

    static {
        grammars.add(new NumberGrammar());
        grammars.add(new OrdinalGrammar());
        grammars.add(new MoneyGrammar());
        grammars.add(new DateTimeGrammar());
        grammars.add(new PercentageGrammar());
        grammars.add(new DistanceGrammar());
        grammars.add(new TemperatureGrammar());
        grammars.add(new AgeGrammar());
        grammars.add(new PhoneNumGrammar());
        grammars.add(new DurationGrammar());
    }

    public static List<Rule> rules() {
        List<Rule> allRules = new ArrayList<>();
        for (DucklingGrammar grammar : grammars) {
            allRules.addAll(grammar.rules());
        }
        return allRules;
    }

    public static List<Example> examples() {
        List<Example> allExamples = new ArrayList<>();
        for (DucklingGrammar grammar : grammars) {
            allExamples.addAll(grammar.examples());
        }
        return allExamples;
    }
}
