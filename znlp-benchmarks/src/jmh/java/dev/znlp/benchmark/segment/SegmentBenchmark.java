package dev.znlp.benchmark.segment;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import dev.znlp.Znlp;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.impl.cws.CwsPerceptron;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分词热路径基准。模型加载在 {@code Trial} 完成，测量的是切分本身。
 * <p>
 * 默认：{@code ./gradlew :znlp-benchmarks:jmh}<br>
 * 只跑默认流水线：{@code ./gradlew :znlp-benchmarks:jmh -PjmhArgs='SegmentBenchmark.defaultSegment$'}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class SegmentBenchmark {

    @Param({"short", "typical", "long"})
    public String size;

    private String text;
    private WordSegmenter defaultSegmenter;
    private WordSegmenter bigramOnly;
    private WordSegmenter cwsPipeline;
    private CwsPerceptron cws;

    @Setup(Level.Trial)
    public void setup() {
        text = SegmentCorpus.text(size);
        defaultSegmenter = Znlp.segmenter();
        bigramOnly = SegmentRecipe.dictionary().build();
        cwsPipeline = SegmentRecipe.perceptron().build();
        cws = Znlp.getService(CwsPerceptron.class);

        defaultSegmenter.segment(text);
        bigramOnly.segment(text);
        cwsPipeline.segment(text);
        cws.split(text);
    }

    /** 默认门面：词典 + 二元 + 人名 + POS。 */
    @Benchmark
    public List<Token> defaultSegment() {
        return defaultSegmenter.segment(text);
    }

    /** 仅词典 + 二元 + 原子，不含人名/POS，便于拆开插件成本。 */
    @Benchmark
    public List<Token> bigramOnlySegment() {
        return bigramOnly.segment(text);
    }

    /** CWS 感知机走线性路径，原子只补空洞。 */
    @Benchmark
    public List<Token> cwsPipelineSegment() {
        return cwsPipeline.segment(text);
    }

    /** 直接调用感知机 {@code split}，不经过词图流水线。 */
    @Benchmark
    public List<String> cwsPerceptronSplit() {
        return cws.split(text);
    }

    /** 只收集词面，对比 {@link #defaultSegment()} 的 Token 分配开销。 */
    @Benchmark
    public List<String> defaultSegmentToStrings() {
        return defaultSegmenter.segmentToStrings(text);
    }

    /** 回调消费，避免结果列表分配；用于观察收集器成本。 */
    @Benchmark
    public void defaultSegmentConsumer(Blackhole bh) {
        defaultSegmenter.segment(text, bh::consume);
    }
}
