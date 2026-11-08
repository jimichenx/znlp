package dev.znlp.segment.impl.rule;

import dev.znlp.segment.Nature;

import java.util.regex.Pattern;

/**
 * 分词规则定义
 */
public class Rule {

    private final Pattern pattern;
    private final Nature nature;
    private final int priority;

    /**
     * @param regex    正则表达式
     * @param nature   词性，可以为null
     * @param priority 优先级（用于后续扩展冲突处理，当前主要体现为词频权重）
     */
    public Rule(String regex, Nature nature, int priority) {
        this.pattern = Pattern.compile(regex);
        this.nature = nature;
        this.priority = priority;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Nature getNature() {
        return nature;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "pattern=" + pattern +
                ", nature=" + nature +
                ", priority=" + priority +
                '}';
    }
}
