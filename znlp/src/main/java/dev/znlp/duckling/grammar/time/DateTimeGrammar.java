package dev.znlp.duckling.grammar.time;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.grammar.time.date.rules.DateRules;
import dev.znlp.duckling.grammar.time.date.rules.GrainRules;
import dev.znlp.duckling.grammar.time.date.rules.HolidayRules;
import dev.znlp.duckling.grammar.time.time.TimeRules;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** 日期、节日、时刻规则的入口（Grain / Date / Holiday / Time）。 */
public class DateTimeGrammar implements DucklingGrammar {
    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        rules.addAll(GrainRules.rules());
        rules.addAll(DateRules.rules());
        rules.addAll(HolidayRules.rules());
        rules.addAll(TimeRules.rules());
        return rules;
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();
        
        // Date Examples
        examples.addAll(createExamples(checkDate("1992-01-01"), "1992年"));
        examples.addAll(createExamples(checkDate("1998-01-01"), "一九九八年"));
        examples.addAll(createExamples(checkDate("2019-01-01"), "今年"));
        examples.addAll(createExamples(checkDate("2018-01-01"), "去年", "2018年"));
        examples.addAll(createExamples(checkDate("2021-01-01"), "后年", "2021年"));
        examples.addAll(createExamples(checkDate("2018-02-01"), "2018年2月"));
        examples.addAll(createExamples(checkDate("2018-08-01"), "去年八月"));
        examples.addAll(createExamples(checkDate("2020-03-08"), "3月8号", "明年三月八号"));
        examples.addAll(createExamples(checkDate("2019-12-08"), "12月8号", "今年十二月八日"));
        examples.addAll(createExamples(checkDate("2021-05-09"), "2021年5月9号", "后年5月九日"));
        examples.addAll(createExamples(checkDate("2019-11-11"), "今天", "今日"));
        examples.addAll(createExamples(checkDate("2019-11-10"), "昨天", "昨日"));
        examples.addAll(createExamples(checkDate("2019-10-01"), "上个月"));
        examples.addAll(createExamples(checkDate("2021-02-12"), "2021年春节"));
        examples.addAll(createExamples(checkDate("2020-01-25"), "春节"));
        examples.addAll(createExamples(checkDate("2018-02-16"), "去年春节"));
        examples.addAll(createExamples(checkDate("2020-10-01"), "国庆节"));
        examples.addAll(createExamples(checkDate("2019-12-25"), "圣诞节"));
        examples.addAll(createExamples(checkDate("2019-11-11"), "星期一", "周一", "礼拜一", "禮拜一", "週一"));
        examples.addAll(createExamples(checkDate("2019-11-12"), "星期二", "周二", "礼拜二", "禮拜二", "週二"));
        examples.addAll(createExamples(checkDate("2019-11-13"), "本周三", "这周三"));
        examples.addAll(createExamples(checkDate("2019-11-17"), "周日", "星期天"));
        examples.addAll(createExamples(checkDate("2019-11-24"), "下周日", "下个星期天"));

        // Time Examples
        examples.addAll(createExamples(checkTime("2019-11-11T09:30:40"), "上午9点30分40秒"));

        return examples;
    }

    private Predicate<Dimension> checkDate(String day) {
        LocalDate ld = LocalDate.parse(day);
        return nodeValue -> {
            if (nodeValue instanceof DateTimeDimension dt) {
                return dt.getYear() == ld.getYear() &&
                       dt.getMonth() == ld.getMonthValue() &&
                       dt.getDay() == ld.getDayOfMonth();
            }
            return false;
        };
    }

    private Predicate<Dimension> checkTime(String dateTime) {
        LocalDateTime ldt = LocalDateTime.parse(dateTime);
        return nodeValue -> {
            if (nodeValue instanceof DateTimeDimension dt) {
                return dt.toDateTime().equals(ldt);
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
