package dev.znlp.duckling.grammar.number;

import dev.znlp.duckling.grammar.DucklingGrammar;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.pattern.TextToken;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleBuilder;
import dev.znlp.duckling.support.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 数字：阿拉伯数字、中文数字、小数、正负与十/百/千/万/亿。
 * <p>
 * 中文数位拼接只向左加一位且跨度不超过 {@link #MAX_DIGIT_SEQ_LEN}。
 * 两个数位串不做「相邻量级相加」，以免「一九九八」被读成 36。
 */
public class NumberGrammar implements DucklingGrammar {

    /** 中文数位拼接最长跨度：覆盖年份「一九九八」，避免「一二三四五…」组合爆炸。 */
    static final int MAX_DIGIT_SEQ_LEN = 4;

    /**
     * 千分位小数 / 千分位整数 / 小数 / 整数，按从特殊到一般排列，只扫一遍文本。
     */
    private static final String NUMERIC_REGEX =
            "(?:\\d+(?:,\\d{3})+\\.\\d+)|(?:\\d{1,3}(?:,\\d{3}){1,5})|(?:\\d*\\.\\d+)|(?:\\d+)";

    private static final Map<String, Integer> CN_NUM_MAP = new HashMap<>();
    private static final List<NumSuffixDef> SUFFIXES = new ArrayList<>();

    static {
        CN_NUM_MAP.put("〇", 0);
        CN_NUM_MAP.put("零", 0);
        CN_NUM_MAP.put("一", 1);
        CN_NUM_MAP.put("壹", 1);
        CN_NUM_MAP.put("二", 2);
        CN_NUM_MAP.put("两", 2);
        CN_NUM_MAP.put("贰", 2);
        CN_NUM_MAP.put("兩", 2);
        CN_NUM_MAP.put("三", 3);
        CN_NUM_MAP.put("叁", 3);
        CN_NUM_MAP.put("四", 4);
        CN_NUM_MAP.put("肆", 4);
        CN_NUM_MAP.put("五", 5);
        CN_NUM_MAP.put("伍", 5);
        CN_NUM_MAP.put("六", 6);
        CN_NUM_MAP.put("陆", 6);
        CN_NUM_MAP.put("七", 7);
        CN_NUM_MAP.put("柒", 7);
        CN_NUM_MAP.put("八", 8);
        CN_NUM_MAP.put("捌", 8);
        CN_NUM_MAP.put("九", 9);
        CN_NUM_MAP.put("玖", 9);
        CN_NUM_MAP.put("十", 10);
        CN_NUM_MAP.put("拾", 10);

        SUFFIXES.add(new NumSuffixDef("K", 3, 1000));
        SUFFIXES.add(new NumSuffixDef("k", 3, 1000));
        SUFFIXES.add(new NumSuffixDef("M", 6, 1000000));
        SUFFIXES.add(new NumSuffixDef("m", 6, 1000000));
        SUFFIXES.add(new NumSuffixDef("G", 9, 1000000000));
        SUFFIXES.add(new NumSuffixDef("g", 9, 1000000000));
        SUFFIXES.add(new NumSuffixDef("十", 1, 10));
        SUFFIXES.add(new NumSuffixDef("拾", 1, 10));
        SUFFIXES.add(new NumSuffixDef("百", 2, 100));
        SUFFIXES.add(new NumSuffixDef("佰", 2, 100));
        SUFFIXES.add(new NumSuffixDef("千", 3, 1000));
        SUFFIXES.add(new NumSuffixDef("仟", 3, 1000));
        SUFFIXES.add(new NumSuffixDef("万", 4, 10000));
        SUFFIXES.add(new NumSuffixDef("亿", 8, 100000000));
    }

    private int numLevel(long value) {
        long num = Math.abs(value);
        if (num < 10) return 1;
        if (num < 100) return 2;
        if (num < 1000) return 3;
        if (num < 10000) return 4;
        if (num < 100000) return 5;
        if (num < 1000000) return 6;
        if (num < 10000000) return 7;
        if (num < 100000000) return 8;
        if (num < 1000000000) return 9;
        if (num < 10000000000L) return 10;
        return (int) Math.log10((double) num) + 1;
    }

    @Override
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        rules.add(RuleBuilder.rule("numeric")
                .pattern(NUMERIC_REGEX)
                .production((text, nodes) -> parseNumericToken(((TextToken) nodes.get(0).getValue()).text()))
                .build());
        
        // Rule: Chinese Digits Single
        rules.add(RuleBuilder.rule("cn_digit")
                .dict(CN_NUM_MAP.keySet().toArray(new String[0]))
                .production((text, nodes) -> {
                    String s = ((TextToken) nodes.get(0).getValue()).text();
                    Integer val = CN_NUM_MAP.get(s);
                    if (val != null) {
                        // Only 0-9 are digit sequences
                        boolean isDigit = val >= 0 && val <= 9;
                        return new NumberNodeValue(val.longValue(), NumberNodeValue.NumberType.Integer, isDigit);
                    }
                    return null;
                })
                .build());

        // 只向左结合（右侧单字）且限制跨度，避免任意二分拼接。
        rules.add(RuleBuilder.rule("digit sequence")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.isDigitSequence())
                .filter(NumberNodeValue.class, n -> n.isInt() && n.isDigitSequence())
                .production((text, nodes) -> {
                    if (nodes.get(1).getRange().getLength() != 1) {
                        return null;
                    }
                    int span = nodes.get(1).getRange().end() - nodes.get(0).getRange().start() + 1;
                    if (span > MAX_DIGIT_SEQ_LEN) {
                        return null;
                    }
                    NumberNodeValue n1 = (NumberNodeValue) nodes.get(0).getValue();
                    NumberNodeValue n2 = (NumberNodeValue) nodes.get(1).getValue();
                    try {
                        String s = String.valueOf(n1.getInt()) + n2.getInt();
                        return new NumberNodeValue(Long.parseLong(s), NumberNodeValue.NumberType.Integer, true);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .build());

        // Rule: {整数} 点 {整数}
        rules.add(RuleBuilder.rule("{整数} 点 {整数}")
                .filter(NumberNodeValue.class, NumberNodeValue::isInt)
                .dict("点")
                .filter(NumberNodeValue.class, NumberNodeValue::isInt)
                .production((text, nodes) -> {
                    NumberNodeValue n1 = (NumberNodeValue) nodes.get(0).getValue();
                    NumberNodeValue n2 = (NumberNodeValue) nodes.get(2).getValue();
                    String s2 = String.valueOf(n2.getInt());
                    // 5.5 -> 5 + 0.5. 0.5 = 5 * 10^-1
                    // But if n2 is 05 (from "零五"), it's 5.
                    // If text is "五点零五", n2 is 5 (from "零五" rule?). 
                    // Wait, "零五" might not be parsed as 5 by simple rules unless "cn inter" rule exists.
                    // But for "五点五", n2 is 5. Length of "五" is 1.
                    // We need length of the original text for n2 to determine power.
                    int len = nodes.get(2).getRange().getLength(); 
                    double val = n1.getInt() + n2.getInt() * Math.pow(10, -len);
                    return new NumberNodeValue(val, NumberNodeValue.NumberType.Double);
                })
                .build());

        // Suffixes
        for (NumSuffixDef suffix : SUFFIXES) {
            rules.add(RuleBuilder.rule("numberSuffix:" + suffix.name())
                    .dict(suffix.name())
                    .production((text, nodes) -> suffix)
                    .build());
        }

        // Compose by multiplication
        rules.add(RuleBuilder.rule("compose by multiplication")
                .filter(NumberNodeValue.class, n -> true)
                .filter(NumSuffixDef.class, s -> true)
                .production((text, nodes) -> {
                    NumberNodeValue n1 = (NumberNodeValue) nodes.get(0).getValue();
                    NumSuffixDef n2 = (NumSuffixDef) nodes.get(1).getValue();
                    return n1.multiply(n2.value());
                })
                .build());

        // integer with consecutive unit modifiers (e.g. 1亿2000万)
        rules.add(RuleBuilder.rule("integer with consecutive unit modifiers")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() > 0)
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() > 0)
                .production((text, nodes) -> {
                    NumberNodeValue n1 = (NumberNodeValue) nodes.get(0).getValue();
                    NumberNodeValue n2 = (NumberNodeValue) nodes.get(1).getValue();
                    // 纯数位串只走 digit sequence，避免「一九九八」被加成 36、「一二三」加成 15。
                    if (n1.isDigitSequence() && n2.isDigitSequence()) {
                        return null;
                    }

                    int level1 = numLevel(n1.getInt());
                    int level2 = numLevel(n2.getInt());
                    
                    if (level1 - level2 == 1) {
                        return n1.add(n2);
                    } else {
                         // 只有当级别差距不超过万级(4)时，才允许跨级别相加
                         // 例如：一万零四千(level=5,level=4)、十万零四千(level=6,level=4)可以
                         // 但是：一亿二千(level=9,level=4)不可以，因为中间可能还有"万"级的数字
                         if (n1.getInt() % 10000 == 0 && level2 == 4 && level1 <= 8) {
                             return n1.add(n2);
                         }
                    }
                    return null;
                })
                .build());

        // integer with nonconsecutive unit modifiers (e.g. 105: 一百零五)
        rules.add(RuleBuilder.rule("integer with nonconsecutive unit modifiers")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() > 0)
                .dict("零", "〇")
                .filter(NumberNodeValue.class, n -> n.isInt() && n.getInt() > 0)
                .production((text, nodes) -> {
                    NumberNodeValue n1 = (NumberNodeValue) nodes.get(0).getValue();
                    NumberNodeValue n2 = (NumberNodeValue) nodes.get(2).getValue();
                    int level1 = numLevel(n1.getInt());
                    int level2 = numLevel(n2.getInt());
                    if (level1 - level2 <= 1) {
                        return null;
                    }
                    return n1.add(n2);
                })
                .build());

        // Negative
        rules.add(RuleBuilder.rule("numbers prefix with -, negative or minus")
                .dict("-", "负", "負")
                .filter(NumberNodeValue.class, n -> n.getDouble() > 0)
                .production((text, nodes) -> {
                    NumberNodeValue n2 = (NumberNodeValue) nodes.get(1).getValue();
                    return n2.multiply(-1);
                })
                .build());
        
        return rules;
    }

    static NumberNodeValue parseNumericToken(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        boolean comma = raw.indexOf(',') >= 0;
        boolean dot = raw.indexOf('.') >= 0;
        String s = comma ? raw.replace(",", "") : raw;
        try {
            if (dot) {
                if (s.startsWith(".")) {
                    s = "0" + s;
                }
                return new NumberNodeValue(Double.parseDouble(s), NumberNodeValue.NumberType.Double);
            }
            return new NumberNodeValue(Long.parseLong(s), NumberNodeValue.NumberType.Integer, !comma);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<Example> examples() {
        List<Example> examples = new ArrayList<>();

        examples.addAll(createExamples(check(0), "〇", "零"));
        examples.addAll(createExamples(check(1), "1", "一", "壹"));
        examples.addAll(createExamples(check(2), "2", "二", "贰"));
        examples.addAll(createExamples(check(10), "10", "十", "拾"));
        examples.addAll(createExamples(check(11), "11", "十一"));
        examples.addAll(createExamples(check(20), "20", "二十", "贰拾"));
        examples.addAll(createExamples(check(60), "60", "六十", "陆拾"));
        examples.addAll(createExamples(check(33), "33", "三十三", "叁拾叁"));
        examples.addAll(createExamples(check(96), "96", "九十六", "玖拾陆"));
        examples.addAll(createExamples(check(203), "203", "二百零三"));
        examples.addAll(createExamples(check(534), "534", "五百三十四"));
        examples.addAll(createExamples(check(34567), "34567", "34,567", "三万四千五百六十七"));
        examples.addAll(createExamples(check(10040), "10040", "10,040", "一万零四十", "壹万零肆拾"));
        
        examples.addAll(createExamples(numCheck(1.1), "1.1", "1.10", "01.10"));
        examples.addAll(createExamples(numCheck(0.77), "0.77", ".77"));
        examples.addAll(createExamples(check(34507), "34507", "34,507", "三万四千五百零七"));
        
        examples.addAll(createExamples(check(100000), 
                "100000", "100,000", "100K", "100k", "十万", "拾万"));
        
        examples.addAll(createExamples(check(3000000), "3M", "3000000", "3,000,000", "三百万"));
        examples.addAll(createExamples(check(1040000), "1.04M", "1,040,000", "1040000", "一百零四万"));
        
        examples.addAll(createExamples(check(1200000), 
                "1,200,000", "1200000", "1.2M", "一百二十万"));
        
        examples.addAll(createExamples(check(123456789), "一亿二千三百四十五万六千七百八十九"));
        examples.addAll(createExamples(numCheck(0.618), "零点六一八")); // Note: Requires "cn inter" rule for "六一八" if "点" logic doesn't handle digits
        examples.addAll(createExamples(numCheck(88.123), "八十八点一二三"));
        
        examples.addAll(createExamples(check(-1200000),
                "- 1,200,000", "-1200000", "负1,200,000", "负 1,200,000", 
                "負 1,200,000", "负1200000", "负 1200000", "-1.2M", 
                "-1200K", "负一百二十万"));

        return examples;
    }

    private Predicate<Dimension> check(int value) {
        return nodeValue -> {
            if (nodeValue instanceof NumberDimension) {
                return ((NumberDimension) nodeValue).getInt() == value;
            }
            return false;
        };
    }

    private Predicate<Dimension> numCheck(double value) {
        return nodeValue -> {
            if (nodeValue instanceof NumberDimension) {
                return Math.abs(((NumberDimension) nodeValue).getValue() - value) < 0.0001;
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

