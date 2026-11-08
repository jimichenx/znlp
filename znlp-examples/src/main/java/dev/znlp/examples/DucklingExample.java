package dev.znlp.examples;

import dev.znlp.Znlp;
import dev.znlp.duckling.DucklingParser;
import dev.znlp.duckling.grammar.age.AgeDimension;
import dev.znlp.duckling.grammar.distance.DistanceDimension;
import dev.znlp.duckling.grammar.duration.DurationDimension;
import dev.znlp.duckling.grammar.money.MoneyDimension;
import dev.znlp.duckling.grammar.number.NumberDimension;
import dev.znlp.duckling.grammar.number.NumberGrammar;
import dev.znlp.duckling.grammar.ordinal.OrdinalDimension;
import dev.znlp.duckling.grammar.percentage.PercentageDimension;
import dev.znlp.duckling.grammar.phonenum.PhoneNumDimension;
import dev.znlp.duckling.grammar.temperature.TemperatureDimension;
import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Duckling 结构化解析示例：数字、序数、金额、时间、百分比、距离、温度、年龄、电话、时长。
 * <p>
 * 相对时间依赖参考时钟。本例固定为 2019-11-11 00:00（周一），与回归样例一致。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DucklingExample}
 */
public class DucklingExample {

    /** 回归样例常用参考日：2019-11-11 周一。 */
    private static final LocalDateTime CLOCK = LocalDateTime.of(2019, 11, 11, 0, 0);

    public static void main(String[] args) {
        System.out.println("参考时钟: " + CLOCK);
        System.out.println("入口: Znlp.parse(text, clock) 或 DucklingParser.getInstance()");

        section("数字");
        show("123");
        show("3.14");
        show("34,567");
        show("一万零四十");
        show("一九九八");
        show("负一百二十万");

        section("序数");
        show("第一");
        show("第十三");

        section("金额");
        show("5元");
        show("5元5角");
        show("三块五");

        section("百分比");
        show("5%");
        show("百分之三点一四");

        section("距离 / 温度 / 年龄");
        show("5公里");
        show("25摄氏度");
        show("30岁");

        section("电话");
        show("13800138000");
        show("联系电话13800138000");

        section("时长");
        show("一小时");
        show("5分钟");
        show("1小时30分");

        section("日期（绝对 / 相对 / 星期）");
        show("2020年5月1号");
        show("今天");
        show("明天");
        show("昨天");
        show("下周日");
        show("后年三月八号");

        section("节日");
        show("中秋节");
        show("国庆节");
        show("春节");
        show("2021年春节");

        section("时刻");
        show("下午3点");
        show("上午9点30分40秒");
        show("明天下午3点");

        section("混合句（一次抽出多种维度）");
        show("我们准备明天下午三点半开会，预算五千元，路程大约5公里，大约需要1小时30分，电话13800138000");
        show("明天下午3点见面，电话13800138000，费用5元");

        section("参考时钟不同，「明天」落地日期不同");
        show("明天", CLOCK);
        show("明天", LocalDateTime.of(2026, 8, 26, 12, 0));

        section("只跑数字语法（避免金额/时间抢走同一段）");
        DucklingParser numbers = new DucklingParser(new DefaultRuleSet(new NumberGrammar().rules()));
        showWith("三点一四", numbers);
        showWith("零点六一八", numbers);
        showWith("我有123个苹果，还有四十五个梨，以及一千零一夜", numbers);
        show("一万零四十元");
        showWith("一万零四十元", numbers);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    private static void show(String text) {
        show(text, CLOCK);
    }

    private static void show(String text, LocalDateTime clock) {
        printHits(text, Znlp.parse(text, clock));
    }

    private static void showWith(String text, DucklingParser parser) {
        printHits(text, parser.parse(text, CLOCK));
    }

    private static void printHits(String text, List<Dimension> dims) {
        System.out.println(text);
        if (dims.isEmpty()) {
            System.out.println("  (无命中)");
            return;
        }
        for (Dimension dim : dims) {
            System.out.println("  " + dim.getKind()
                    + " [" + dim.getStart() + "," + dim.getEnd() + "]"
                    + " \"" + dim.getText() + "\""
                    + " -> " + detail(dim));
        }
    }

    private static String detail(Dimension dim) {
        if (dim instanceof NumberDimension n) {
            return String.valueOf(n.getValue());
        }
        if (dim instanceof OrdinalDimension o) {
            return "第" + o.getValue();
        }
        if (dim instanceof MoneyDimension m) {
            return m.getValue() + " " + m.getUnit();
        }
        if (dim instanceof PercentageDimension p) {
            return String.valueOf(p.getValue());
        }
        if (dim instanceof DistanceDimension d) {
            return d.getValue() + " " + d.getUnit();
        }
        if (dim instanceof TemperatureDimension t) {
            return t.getValue() + "℃";
        }
        if (dim instanceof AgeDimension a) {
            return a.getValue() + "岁";
        }
        if (dim instanceof PhoneNumDimension p) {
            return p.getValue();
        }
        if (dim instanceof DurationDimension d) {
            return d.getValue() + " " + d.getGrain();
        }
        if (dim instanceof DateTimeDimension t) {
            return t.toDateTime() + " grain=" + t.getGrain();
        }
        return dim.toString();
    }
}
