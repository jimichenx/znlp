package dev.znlp.duckling.support;

/**
 * 图节点上的值。{@link #isLatent()} 为 true 时不会进入最终输出。
 */
public interface NodeValue {
    default boolean isLatent() {
        return false;
    }
}
