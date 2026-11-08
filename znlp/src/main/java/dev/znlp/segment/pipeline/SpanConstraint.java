package dev.znlp.segment.pipeline;

import dev.znlp.segment.graph.Wordpath;

/**
 * 在已选路径上强制成词。
 * <p>
 * 优先级数字大的先执行，并保护自己的跨度不被后来的约束切开。
 * 约束可以盖过选路结果，但不能从别的词中间切开。
 */
public interface SpanConstraint {

    int priority();

    void apply(Wordpath path);
}
