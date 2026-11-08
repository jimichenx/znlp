package dev.znlp.duckling.rule;

import dev.znlp.duckling.pattern.FilterDucklingPattern;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

import java.util.function.Predicate;

/**
 * 按节点值类型过滤。{@link DefaultRuleSet} 用 {@link #getTargetClass()} 做索引。
 */
public class FilterPattern<T extends NodeValue> extends FilterDucklingPattern<T> {

    private final Predicate<T> predicate;
    private final Class<T> clazz;

    public FilterPattern(int id, Class<T> clazz, Predicate<T> predicate) {
        super(id);
        this.clazz = clazz;
        this.predicate = predicate;
    }

    public Class<T> getTargetClass() {
        return clazz;
    }

    @Override
    public boolean predicate(DkNode<?> node) {
        if (clazz.isInstance(node.getValue())) {
            return predicate.test(clazz.cast(node.getValue()));
        }
        return false;
    }
}
