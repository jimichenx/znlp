package dev.znlp.duckling.rule;

import dev.znlp.duckling.pattern.DictPattern;
import dev.znlp.duckling.pattern.DucklingPattern;
import dev.znlp.duckling.pattern.RegexPattern;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 规则构造器。{@code pattern(String)} 在纯字面或 {@code a|b|c} 时走词典，否则编译正则。
 * <p>
 * rule / pattern 的 id 在进程内单调递增，多次 {@code new NumberGrammar().rules()} 会得到不同 id，
 * 这是正常的；不要假设 id 等于规则在列表中的位置。
 */
public class RuleBuilder {
    private final String name;
    private final List<DucklingPattern<?>> patterns = new ArrayList<>();
    private RuleProduction production;
    private static int ruleIdCounter = 0;
    private static int patternIdCounter = 0;

    public RuleBuilder(String name) {
        this.name = name;
    }

    public static RuleBuilder rule(String name) {
        return new RuleBuilder(name);
    }

    public RuleBuilder pattern(String regex) {
        String[] literals = plainDictLiterals(regex);
        if (literals != null) {
            return dict(literals);
        }
        patterns.add(new RegexPattern(patternIdCounter++, Pattern.compile(regex)));
        return this;
    }

    /**
     * 不含正则元字符的字面量或 {@code a|b|c} 可走 DAT 词典，避免每条正则扫整句。
     */
    static String[] plainDictLiterals(String regex) {
        if (regex == null || regex.isEmpty()) {
            return null;
        }
        for (int i = 0; i < regex.length(); i++) {
            switch (regex.charAt(i)) {
                case '.', '*', '+', '?', '^', '$', '{', '}', '[', ']', '(', ')', '\\':
                    return null;
                default:
                    break;
            }
        }
        String[] parts = regex.split("\\|", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                return null;
            }
        }
        return parts;
    }
    
    public RuleBuilder pattern(Pattern regex) {
        patterns.add(new RegexPattern(patternIdCounter++, regex));
        return this;
    }

    public RuleBuilder dict(String... words) {
        Set<String> set = new HashSet<>(Arrays.asList(words));
        patterns.add(new DictPattern(patternIdCounter++, set));
        return this;
    }
    
    public <T extends NodeValue> RuleBuilder filter(Class<T> clazz, Predicate<T> predicate) {
        patterns.add(new FilterPattern<>(patternIdCounter++, clazz, predicate));
        return this;
    }
    
    public RuleBuilder pattern(DucklingPattern<?> pattern) {
        patterns.add(pattern);
        return this;
    }

    public RuleBuilder production(RuleProduction production) {
        this.production = production;
        return this;
    }

    public Rule build() {
        return new Rule(ruleIdCounter++, name) {
            @Override
            public List<DucklingPattern<?>> getPatterns() {
                return patterns;
            }

            @Override
            public NodeValue production(String text, List<DkNode<?>> matchedNodes) {
                return production.produce(text, matchedNodes);
            }
        };
    }

    public interface RuleProduction {
        NodeValue produce(String text, List<DkNode<?>> nodes);
    }
}
