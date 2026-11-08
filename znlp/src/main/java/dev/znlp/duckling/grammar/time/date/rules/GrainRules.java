package dev.znlp.duckling.grammar.time.date.rules;

import dev.znlp.duckling.grammar.time.AllInnerGrain;
import dev.znlp.duckling.grammar.time.date.TimeGrainNodeValue;
import dev.znlp.duckling.pattern.TextToken;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;

/** 年/月/日/时/分/秒等粒度词，秒与分钟已展开成词典项。 */
public class GrainRules {

    public static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        addDictRule(rules, AllInnerGrain.Second, "秒", "秒钟", "秒鐘");
        addDictRule(rules, AllInnerGrain.Minute, "分", "分钟", "分鐘");
        addRule(rules, "小时|个小时", AllInnerGrain.Hour);
        addRule(rules, "天|日|号", AllInnerGrain.Day);
        addRule(rules, "周|週|礼拜|禮拜|星期", AllInnerGrain.Week);
        addRule(rules, "月", AllInnerGrain.Month);
        addRule(rules, "年", AllInnerGrain.Year);

        return rules;
    }

    private static void addDictRule(List<Rule> rules, AllInnerGrain grain, String... words) {
        rules.add(RuleBuilder.rule("Grain(" + grain.name() + ")")
                .dict(words)
                .production((text, nodes) -> {
                    String txt = ((TextToken) nodes.get(0).getValue()).text();
                    return new TimeGrainNodeValue(grain, txt);
                })
                .build());
    }

    private static void addRule(List<Rule> rules, String pattern, AllInnerGrain grain) {
        if (pattern.contains("|")) {
            // Dictionary or simple regex choice that can be treated as dictionary split
            // The original code uses dictSplit for some and regex for others.
            // If it's a simple list of words, use dict for efficiency, otherwise regex.
            if (pattern.contains("?:")) {
                 rules.add(RuleBuilder.rule("Grain(" + grain.name() + ")")
                    .pattern(pattern)
                    .production((text, nodes) -> {
                        String txt = ((TextToken) nodes.get(0).getValue()).text();
                        return new TimeGrainNodeValue(grain, txt);
                    })
                    .build());
            } else {
                 rules.add(RuleBuilder.rule("Grain(" + grain.name() + ")")
                    .dict(pattern.split("\\|"))
                    .production((text, nodes) -> {
                        String txt = ((TextToken) nodes.get(0).getValue()).text();
                        return new TimeGrainNodeValue(grain, txt);
                    })
                    .build());
            }
        } else {
            rules.add(RuleBuilder.rule("Grain(" + grain.name() + ")")
                    .pattern(pattern)
                    .production((text, nodes) -> {
                        String txt = ((TextToken) nodes.get(0).getValue()).text();
                        return new TimeGrainNodeValue(grain, txt);
                    })
                    .build());
        }
    }
}
