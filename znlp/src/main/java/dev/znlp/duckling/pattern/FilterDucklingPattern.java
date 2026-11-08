package dev.znlp.duckling.pattern;

import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

/**
 * 按已有节点的值做谓词过滤，用于组合规则的非叶子段。
 */
public abstract class FilterDucklingPattern<T extends NodeValue> extends DucklingPattern<T> {

    public FilterDucklingPattern(int id) {
        super(id);
    }

    public abstract boolean predicate(DkNode<?> node);
}
