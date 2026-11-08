package dev.znlp.segment.impl.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则管理器
 * <p>
 * 管理分词规则集合。
 * 支持动态添加和移除规则。
 */
public class RuleManager {

    private final List<Rule> rules = new ArrayList<>();

    public synchronized void addRule(Rule rule) {
        rules.add(rule);
    }

    public synchronized void removeRule(Rule rule) {
        rules.remove(rule);
    }

    public synchronized void clear() {
        rules.clear();
    }

    public synchronized List<Rule> getRules() {
        return Collections.unmodifiableList(new ArrayList<>(rules));
    }
}
