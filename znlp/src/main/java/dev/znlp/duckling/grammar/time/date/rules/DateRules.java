package dev.znlp.duckling.grammar.time.date.rules;

import dev.znlp.duckling.grammar.number.NumberNodeValue;
import dev.znlp.duckling.grammar.time.AllInnerGrain;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.grammar.time.date.DateNodeValue;
import dev.znlp.duckling.grammar.time.date.TimeGrainNodeValue;
import dev.znlp.duckling.grammar.time.time.TimeNodeValue;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;

import java.util.ArrayList;
import java.util.List;

/** 绝对/相对日期：年月日、今天昨天、星期、以及与时刻的组合。 */
public class DateRules {

    public static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        // Year: "2019" (latent)
        rules.add(RuleBuilder.rule("year (latent)")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 1900 && n.getInt() <= 2100)
                .production((text, nodes) -> {
                    int val = (int) ((NumberNodeValue) nodes.get(0).getValue()).getInt();
                    return new DateNodeValue(Grain.Year, val, null, null, null, null, null, null).makeLatent();
                })
                .build());

        // Year: "2019年"
        rules.add(RuleBuilder.rule("year")
                .filter(NumberNodeValue.class, n -> n.isInt())
                .filter(TimeGrainNodeValue.class, g -> g.grain() == AllInnerGrain.Year)
                .production((text, nodes) -> {
                    int val = (int) ((NumberNodeValue) nodes.get(0).getValue()).getInt();
                    return new DateNodeValue(Grain.Year, val, null, null, null, null, null, null);
                })
                .build());

        // Relative Year
        rules.add(RuleBuilder.rule("this year")
                .dict("今年", "本年")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Year, null, 0, null, null, null, null, null);
                })
                .build());

        rules.add(RuleBuilder.rule("last year")
                .dict("去年", "上年")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Year, null, -1, null, null, null, null, null);
                })
                .build());

        rules.add(RuleBuilder.rule("next year")
                .dict("明年", "下年")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Year, null, 1, null, null, null, null, null);
                })
                .build());

        rules.add(RuleBuilder.rule("year before last")
                .dict("前年")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Year, null, -2, null, null, null, null, null);
                })
                .build());

        rules.add(RuleBuilder.rule("year after next")
                .dict("后年")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Year, null, 2, null, null, null, null, null);
                })
                .build());

        // Intersect Year + Date
        rules.add(RuleBuilder.rule("year date")
                .filter(DateNodeValue.class, d -> d.isYearGrain())
                .filter(DateNodeValue.class, d -> d.justMissYearForDay())
                .production((text, nodes) -> {
                    DateNodeValue y = (DateNodeValue) nodes.get(0).getValue();
                    DateNodeValue d = (DateNodeValue) nodes.get(1).getValue();
                    return new DateNodeValue(d.getGrain(),
                            y.getYear(), y.getYearOffset(),
                            d.getMonth(), d.getMonthOffset(),
                            d.getDay(), d.getDayOffset(),
                            d.getComputed());
                })
                .build());

        // Month: "5月"
        rules.add(RuleBuilder.rule("month")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 1 && n.getInt() <= 12)
                .filter(TimeGrainNodeValue.class, g -> g.grain() == AllInnerGrain.Month)
                .production((text, nodes) -> {
                    int val = (int) ((NumberNodeValue) nodes.get(0).getValue()).getInt();
                    return new DateNodeValue(Grain.Month, null, null, val, null, null, null, null);
                })
                .build());

        // Day: "1号", "1日"
        rules.add(RuleBuilder.rule("day")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() >= 1 && n.getInt() <= 31)
                .filter(TimeGrainNodeValue.class, g -> g.grain() == AllInnerGrain.Day)
                .production((text, nodes) -> {
                    int val = (int) ((NumberNodeValue) nodes.get(0).getValue()).getInt();
                    return new DateNodeValue(Grain.Day, null, null, null, null, val, null, null);
                })
                .build());

        // Year Month
        rules.add(RuleBuilder.rule("year month")
                .filter(DateNodeValue.class, d -> d.isYearGrain())
                .filter(DateNodeValue.class, d -> d.isMonthGrain())
                .production((text, nodes) -> {
                    DateNodeValue y = (DateNodeValue) nodes.get(0).getValue();
                    DateNodeValue m = (DateNodeValue) nodes.get(1).getValue();
                    return new DateNodeValue(Grain.Month,
                            y.getYear(), y.getYearOffset(),
                            m.getMonth(), m.getMonthOffset(),
                            null, null, null);
                })
                .build());

        // Month Day
        rules.add(RuleBuilder.rule("month day")
                .filter(DateNodeValue.class, d -> d.isMonthGrain())
                .filter(DateNodeValue.class, d -> d.isDayGrain())
                .production((text, nodes) -> {
                    DateNodeValue m = (DateNodeValue) nodes.get(0).getValue();
                    DateNodeValue d = (DateNodeValue) nodes.get(1).getValue();
                    return new DateNodeValue(Grain.Day,
                            m.getYear(), m.getYearOffset(),
                            m.getMonth(), m.getMonthOffset(),
                            d.getDay(), d.getDayOffset(), null);
                })
                .build());
                
        // Relative Month
        rules.add(RuleBuilder.rule("last month")
                .dict("上个月", "上月")
                .production((text, nodes) -> new DateNodeValue(Grain.Month, null, null, null, -1, null, null, null))
                .build());

        rules.add(RuleBuilder.rule("this month")
                .dict("这个月", "本月")
                .production((text, nodes) -> new DateNodeValue(Grain.Month, null, null, null, 0, null, null, null))
                .build());

        rules.add(RuleBuilder.rule("next month")
                .dict("下个月", "下月")
                .production((text, nodes) -> new DateNodeValue(Grain.Month, null, null, null, 1, null, null, null))
                .build());

        // Weekdays
        rules.add(RuleBuilder.rule("weekday-mon")
                .dict("星期一", "周一", "礼拜一", "禮拜一", "週一")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 1, null))
                .build());
        
        rules.add(RuleBuilder.rule("weekday-tue")
                .dict("星期二", "周二", "礼拜二", "禮拜二", "週二")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 2, null))
                .build());
        
        rules.add(RuleBuilder.rule("weekday-wed")
                .dict("星期三", "周三", "礼拜三", "禮拜三", "週三")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 3, null))
                .build());
                
        rules.add(RuleBuilder.rule("weekday-thu")
                .dict("星期四", "周四", "礼拜四", "禮拜四", "週四")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 4, null))
                .build());
                
        rules.add(RuleBuilder.rule("weekday-fri")
                .dict("星期五", "周五", "礼拜五", "禮拜五", "週五")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 5, null))
                .build());
                
        rules.add(RuleBuilder.rule("weekday-sat")
                .dict("星期六", "周六", "礼拜六", "禮拜六", "週六")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 6, null))
                .build());
                
        rules.add(RuleBuilder.rule("weekday-sun")
                .dict("星期日", "周日", "礼拜日", "禮拜日", "週日", "星期天")
                .production((text, nodes) -> new DateNodeValue(Grain.Day, null, null, null, null, null, null, 7, null))
                .build());
        
        // Relative Week
        rules.add(RuleBuilder.rule("next weekday")
                .dict("下", "下个")
                .filter(DateNodeValue.class, d -> d.getWeekday() != null)
                .production((text, nodes) -> {
                    DateNodeValue d = (DateNodeValue) nodes.get(1).getValue();
                    int offset = d.getDayOffset() != null ? d.getDayOffset() : 0;
                    return new DateNodeValue(Grain.Day, null, null, null, null, null, offset + 7, d.getWeekday(), null, null);
                })
                .build());
        
        // "本周三"
        rules.add(RuleBuilder.rule("this weekday")
                .dict("本", "这", "这个")
                .filter(DateNodeValue.class, d -> d.getWeekday() != null)
                .production((text, nodes) -> {
                    DateNodeValue d = (DateNodeValue) nodes.get(1).getValue();
                    return d; // Same as weekday (this week)
                })
                .build());


        // Relative Day: "今天"
        rules.add(RuleBuilder.rule("today")
                .dict("今天", "今日", "当天")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Day, null, null, null, null, null, 0, null);
                })
                .build());
        
        // Relative Day: "明天"
        rules.add(RuleBuilder.rule("tomorrow")
                .dict("明天", "明日", "次日")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Day, null, null, null, null, null, 1, null);
                })
                .build());
        
        // Relative Day: "昨天"
        rules.add(RuleBuilder.rule("yesterday")
                .dict("昨天", "昨日")
                .production((text, nodes) -> {
                    return new DateNodeValue(Grain.Day, null, null, null, null, null, -1, null);
                })
                .build());

        // Date Time
        rules.add(RuleBuilder.rule("date time")
                .filter(DateNodeValue.class, d -> d.isDayGrain())
                .filter(TimeNodeValue.class, t -> true)
                .production((text, nodes) -> {
                    DateNodeValue d = (DateNodeValue) nodes.get(0).getValue();
                    TimeNodeValue t = (TimeNodeValue) nodes.get(1).getValue();
                    return new DateNodeValue(t.getGrain(),
                            d.getYear(), d.getYearOffset(),
                            d.getMonth(), d.getMonthOffset(),
                            d.getDay(), d.getDayOffset(),
                            d.getWeekday(),
                            t,
                            d.getComputed());
                })
                .build());

        return rules;
    }
}
