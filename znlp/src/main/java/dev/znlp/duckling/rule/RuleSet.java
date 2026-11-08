package dev.znlp.duckling.rule;

import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

import java.util.List;

/**
 * 规则图的一次执行。
 * <p>
 * {@link #ITERATIONS_MAX} 限制组合轮数，{@link #MAX_STASH_SIZE} 限制节点总数，防止病态输入把图撑爆。
 */
public interface RuleSet {
    int ITERATIONS_MAX = 30;
    int MAX_STASH_SIZE = 1024;

    List<DkNode<NodeValue>> execute(String text);
}
