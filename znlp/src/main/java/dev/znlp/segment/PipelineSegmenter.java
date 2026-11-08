package dev.znlp.segment;

import dev.znlp.segment.recipe.SegmentRecipe;
import org.jetbrains.annotations.NotNull;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BestPathAlgorithm;
import dev.znlp.segment.pipeline.CandidateSource;
import dev.znlp.segment.pipeline.CharNormalize;
import dev.znlp.segment.pipeline.SpanConstraint;
import dev.znlp.segment.pipeline.TokenAnnotator;
import dev.znlp.segment.pipeline.WordTermCollector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 按固定阶段执行的分词器：规范化、分句、铺候选、选路、强制成词、标注、输出。
 * <p>
 * 无状态，可在多线程中复用。由 {@link SegmentRecipe} 构建。
 */
public final class PipelineSegmenter implements WordSegmenter {

    private final CharNormalize[] normalizers;
    private final CandidateSource[] sources;
    private final BestPathAlgorithm bestPath;
    private final SpanConstraint[] constraints;
    private final TokenAnnotator[] annotators;
    private final WordTermCollector collector;

    private static final ThreadLocal<Wordnet> GRAPHS = ThreadLocal.withInitial(() -> new Wordnet(new char[0], 0, 0));

    public PipelineSegmenter(@NotNull List<CharNormalize> normalizers,
                             @NotNull List<CandidateSource> sources,
                             @NotNull BestPathAlgorithm bestPath,
                             @NotNull List<SpanConstraint> constraints,
                             @NotNull List<TokenAnnotator> annotators,
                             @NotNull WordTermCollector collector) {
        this.normalizers = normalizers.toArray(CharNormalize[]::new);
        this.sources = sources.toArray(CandidateSource[]::new);
        this.bestPath = bestPath;
        this.constraints = constraints.toArray(SpanConstraint[]::new);
        this.annotators = annotators.toArray(TokenAnnotator[]::new);
        this.collector = collector;
    }

    @Override
    public void segment(char[] text, @NotNull Consumer<Token> consumer) {
        if (text == null) {
            throw new NullPointerException("Text cannot be null");
        }
        if (text.length == 0) {
            return;
        }

        char[] working = text;
        if (normalizers.length > 0) {
            working = Arrays.copyOf(text, text.length);
            for (CharNormalize normalizer : normalizers) {
                normalizer.normal(working);
            }
        }

        int start = 0;
        for (int i = 0; i < working.length; i++) {
            boolean last = i == working.length - 1;
            if (!last && !isSentenceBreak(working[i])) {
                continue;
            }
            int end = i + 1;
            segmentWindow(working, start, end, consumer);
            start = end;
        }
    }

    private void segmentWindow(char[] text, int start, int end, Consumer<Token> consumer) {
        Consumer<Token> out = start == 0 ? consumer : token -> {
            shift(token, start);
            consumer.accept(token);
        };

        Wordnet wordnet = GRAPHS.get();
        wordnet.prepare(text, start, end - start);
        for (CandidateSource source : sources) {
            source.fill(wordnet);
        }
        wordnet.fillNill();

        Wordpath path = bestPath.select(wordnet);
        for (SpanConstraint constraint : constraints) {
            constraint.apply(path);
        }
        for (TokenAnnotator annotator : annotators) {
            annotator.annotate(path);
        }
        collector.collect(null, wordnet, path, out);
    }

    private static void shift(Token token, int base) {
        token.setOffset(token.getOffset() + base);
        List<Token> subwords = token.getSubword();
        if (subwords == null) {
            return;
        }
        for (Token subword : subwords) {
            shift(subword, base);
        }
    }

    /**
     * 句号、问号、叹号、分号和换行。逗号留在句内，小数点不切。
     * 断句符算进当前句，这样它仍是一个词。
     */
    static boolean isSentenceBreak(char ch) {
        return ch == '\n' || ch == '\r'
                || ch == '。' || ch == '！' || ch == '？' || ch == '；'
                || ch == '!' || ch == '?' || ch == ';';
    }
}
