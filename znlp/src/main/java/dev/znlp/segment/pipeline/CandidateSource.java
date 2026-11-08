package dev.znlp.segment.pipeline;

import org.jetbrains.annotations.NotNull;
import dev.znlp.segment.graph.Wordnet;

/**
 * 往词图里铺候选。不能选路，也不能改已经选定的路径。
 */
public interface CandidateSource {

    /**
     * 同跨度冲突策略。铺词实现需要自己遵守；运行器按配方顺序调用 {@link #fill}。
     */
    @NotNull
    SpanConflict conflict();

    void fill(@NotNull Wordnet wordnet);
}
