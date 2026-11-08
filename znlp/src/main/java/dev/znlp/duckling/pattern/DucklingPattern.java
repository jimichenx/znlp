package dev.znlp.duckling.pattern;

import dev.znlp.duckling.rule.RuleBuilder;
import dev.znlp.duckling.support.NodeValue;

import java.util.Objects;

/**
 * 规则左侧匹配子。id 由 {@link RuleBuilder} 分配，用于图上的节点桶。
 */
public abstract class DucklingPattern<T extends NodeValue> {
    protected final int id;

    public DucklingPattern(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DucklingPattern<?> that = (DucklingPattern<?>) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
