package dev.znlp.segment.pipeline;

import dev.znlp.segment.graph.Wordpath;

/**
 * 在边界稳定之后给词打标签。
 * <p>
 * 默认不改边界。{@link #mayMerge()} 为 true 时允许合并，例如 NER。
 */
public interface TokenAnnotator {

    void annotate(Wordpath path);

    default boolean mayMerge() {
        return false;
    }
}
