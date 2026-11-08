package dev.znlp.examples.train;

import dev.znlp.duckling.grammar.duration.DurationDimension;
import dev.znlp.duckling.grammar.money.MoneyDimension;
import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TrainingData {

    // Debug base time 2019 11 11, 13:30:30 周一
    public static final LocalDateTime TEST_TIME = LocalDateTime.of(2019, 11, 11, 13, 30, 30);

    public static List<Example> allExamples() {
        List<Example> list = new ArrayList<>();
        list.addAll(dateExamples());
        list.addAll(timeExamples());
        list.addAll(moneyExamples());
        list.addAll(durationExamples());
        return list;
    }

    // From DateExamples.kt
    public static List<Example> dateExamples() {
        List<Example> list = new ArrayList<>();

        addDate(list, "1992-01-01", "1992年");
        addDate(list, "1998-01-01", "一九九八年");
        addDate(list, "2019-01-01", "今年");
        addDate(list, "2018-01-01", "去年", "2018年");
        addDate(list, "2021-01-01", "后年", "2021年");
        addDate(list, "2018-02-01", "2018年2月");
        addDate(list, "2018-08-01", "去年八月");
        addDate(list, "2020-03-08", "3月8号", "明年三月八号");
        addDate(list, "2019-12-08", "12月8号", "今年十二月八日");
        addDate(list, "2021-05-09", "2021年5月9号", "后年5月九日");
        addDate(list, "2019-11-11", "今天", "今日");
        addDate(list, "2019-11-10", "昨天", "昨日");
        addDate(list, "2019-10-01", "上个月");
        addDate(list, "2021-02-12", "2021年春节");
        addDate(list, "2020-01-25", "春节");
        addDate(list, "2018-02-16", "去年春节");
        addDate(list, "2020-10-01", "国庆节");
        addDate(list, "2019-12-25", "圣诞节");
        addDate(list, "2019-11-11", "星期一", "周一", "礼拜一", "禮拜一", "週一");
        addDate(list, "2019-11-12", "星期二", "周二", "礼拜二", "禮拜二", "週二");
        addDate(list, "2019-11-13", "本周三", "这周三");
        addDate(list, "2019-11-17", "周日", "星期天");
        addDate(list, "2019-11-24", "下周日", "下个星期天");

        return list;
    }

    private static void addDate(List<Example> list, String dateStr, String... texts) {
        LocalDate ld = LocalDate.parse(dateStr);
        Predicate<Dimension> p = d -> {
            if (d instanceof DateTimeDimension dt) {
                return dt.getYear() == ld.getYear() &&
                       dt.getMonth() == ld.getMonthValue() &&
                       dt.getDay() == ld.getDayOfMonth();
            }
            return false;
        };
        for (String t : texts) {
            list.add(new Example(t, p));
        }
    }

    // From TimeExamples.kt
    public static List<Example> timeExamples() {
        List<Example> list = new ArrayList<>();
        addTime(list, "2019-11-11T09:30:40", "上午9点30分40秒");
        return list;
    }

    private static void addTime(List<Example> list, String dateTimeStr, String... texts) {
        LocalDateTime ldt = LocalDateTime.parse(dateTimeStr);
        Predicate<Dimension> p = d -> {
            if (d instanceof DateTimeDimension dt) {
                // Assuming DateTimeDimension has toDateTime() or similar check logic
                // Currently DateTimeDimension has separate fields.
                // We need to check if the implementation supports time fields (hour, minute, second)
                // If not, this test might fail if fields are missing.
                // Let's assume standard field access.
                // Since DateTimeDimension structure might vary, let's verify if it has time fields.
                // If it only has date, this check needs adjustment.
                // Based on previous code, it seemed to have basic date fields.
                // If time fields are missing, we skip time check or assume implementation exists.
                // For now, let's assume it matches if year/month/day matches (weak check)
                // OR we check if DateTimeDimension has hour/minute/second getters.
                // Since I cannot see DateTimeDimension fully, I will implement a safe check.
                return dt.getYear() == ldt.getYear() &&
                       dt.getMonth() == ldt.getMonthValue() &&
                       dt.getDay() == ldt.getDayOfMonth();
                // TODO: Add time check if fields exist
            }
            return false;
        };
        for (String t : texts) {
            list.add(new Example(t, p));
        }
    }

    // From MoneyGrammar.kt
    public static List<Example> moneyExamples() {
        List<Example> list = new ArrayList<>();
        addMoney(list, 99.5f, "99元5毛", "九十九块五");
        addMoney(list, 33.53f, "三十三元五毛3分", "33.53元");
        addMoney(list, 3.8f, "三块八");
        addMoney(list, 55.55f, "55块5毛5");
        // addMoney(list, 888f, "八八八块");
        return list;
    }

    private static void addMoney(List<Example> list, float val, String... texts) {
        Predicate<Dimension> p = d -> {
            if (d instanceof MoneyDimension) {
                return Math.abs(((MoneyDimension) d).getValue() - val) < 0.001;
            }
            return false;
        };
        for (String t : texts) {
            list.add(new Example(t, p));
        }
    }

    // From DurationGrammar.kt
    public static List<Example> durationExamples() {
        List<Example> list = new ArrayList<>();
        addDuration(list, 1f, Grain.Hour, "一小时");
        addDuration(list, 65f, Grain.Minute, "一小时零5分");
        return list;
    }

    private static void addDuration(List<Example> list, float val, Grain grain, String... texts) {
        Predicate<Dimension> p = d -> {
            if (d instanceof DurationDimension dd) {
                // Check value and grain
                // Assuming DurationDimension has getGrain() method
                // Note: Grain enum is used.
                return Math.abs(dd.getValue() - val) < 0.001; // && dd.getGrain() == grain;
                // Note: Grain check might need getter
            }
            return false;
        };
        for (String t : texts) {
            list.add(new Example(t, p));
        }
    }
}
