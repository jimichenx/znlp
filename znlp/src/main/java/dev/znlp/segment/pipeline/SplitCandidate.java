package dev.znlp.segment.pipeline;

import org.jetbrains.annotations.NotNull;
import dev.znlp.segment.graph.Wordnet;

/**
 * 把已有的 {@link WordSplitAlgorithm} 当成候选源。冲突策略由算法自己在 {@code fill} 里遵守。
 */
public final class SplitCandidate implements CandidateSource {

    private final WordSplitAlgorithm algorithm;
    private final SpanConflict conflict;

    public SplitCandidate(@NotNull WordSplitAlgorithm algorithm, @NotNull SpanConflict conflict) {
        this.algorithm = algorithm;
        this.conflict = conflict;
    }

    @Override
    public @NotNull SpanConflict conflict() {
        return conflict;
    }

    @Override
    public void fill(@NotNull Wordnet wordnet) {
        algorithm.fill(wordnet);
    }
}
