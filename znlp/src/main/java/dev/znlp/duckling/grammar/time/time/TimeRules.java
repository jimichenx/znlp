package dev.znlp.duckling.grammar.time.time;

import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.grammar.time.AllInnerGrain;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.grammar.time.date.TimeGrainNodeValue;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;

/** 时刻：几点几分、上午/下午，以及与日期节点的拼接。 */
public class TimeRules {
    public static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        
        // Hour: 8点
        rules.add(RuleBuilder.rule("hour")
            .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 0 && n.getInt() <= 24)
            .dict("点", "时") // Use dict explicitly
            .production((text, nodes) -> {
                int val = (int) ((NumberNodeValue) nodes.get(0).getValue()).getInt();
                return new TimeNodeValue(Grain.Hour, val, 0, 0);
            })
            .build());
            
        // Hour Minute: 8点30(分)
        rules.add(RuleBuilder.rule("hour minute")
            .filter(TimeNodeValue.class, t -> t.getGrain() == Grain.Hour)
            .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 0 && n.getInt() < 60)
            .production((text, nodes) -> {
                TimeNodeValue t = (TimeNodeValue) nodes.get(0).getValue();
                int m = (int) ((NumberNodeValue) nodes.get(1).getValue()).getInt();
                return new TimeNodeValue(Grain.Minute, t.getHour(), m, 0, t.isAM(), t.isPM());
            })
            .build());
            
        // Hour Minute (explicit grain): 8点30分
        rules.add(RuleBuilder.rule("hour minute (explicit)")
            .filter(TimeNodeValue.class, t -> t.getGrain() == Grain.Hour)
            .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 0 && n.getInt() < 60)
            .filter(TimeGrainNodeValue.class, g -> g.grain() == AllInnerGrain.Minute)
            .production((text, nodes) -> {
                TimeNodeValue t = (TimeNodeValue) nodes.get(0).getValue();
                int m = (int) ((NumberNodeValue) nodes.get(1).getValue()).getInt();
                return new TimeNodeValue(Grain.Minute, t.getHour(), m, 0, t.isAM(), t.isPM());
            })
            .build());
            
        // Minute Second
        rules.add(RuleBuilder.rule("minute second")
            .filter(TimeNodeValue.class, t -> t.getGrain() == Grain.Minute)
            .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 0 && n.getInt() < 60)
            .filter(TimeGrainNodeValue.class, g -> g.grain() == AllInnerGrain.Second)
            .production((text, nodes) -> {
                TimeNodeValue t = (TimeNodeValue) nodes.get(0).getValue();
                int s = (int) ((NumberNodeValue) nodes.get(1).getValue()).getInt();
                return new TimeNodeValue(Grain.Second, t.getHour(), t.getMinute(), s, t.isAM(), t.isPM());
            })
            .build());
            
        // AM/PM
        rules.add(RuleBuilder.rule("AM time")
            .dict("上午", "早上", "凌晨")
            .filter(TimeNodeValue.class, t -> true)
            .production((text, nodes) -> {
                TimeNodeValue t = (TimeNodeValue) nodes.get(1).getValue();
                return new TimeNodeValue(t.getGrain(), t.getHour(), t.getMinute(), t.getSecond(), true, false);
            })
            .build());

        rules.add(RuleBuilder.rule("PM time")
            .dict("下午", "晚上", "中午")
            .filter(TimeNodeValue.class, t -> true)
            .production((text, nodes) -> {
                TimeNodeValue t = (TimeNodeValue) nodes.get(1).getValue();
                return new TimeNodeValue(t.getGrain(), t.getHour(), t.getMinute(), t.getSecond(), false, true);
            })
            .build());

        return rules;
    }
}
