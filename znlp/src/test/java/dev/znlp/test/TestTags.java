package dev.znlp.test;

/**
 * JUnit 5 测试标签常量。默认 {@code ./gradlew test} 会排除这些标签。
 */
public final class TestTags {

    public static final String BENCHMARK = "benchmark";
    public static final String TRAIN = "train";
    public static final String PERFORMANCE = "performance";

    /**
     * 依赖本机 data/ 大语料或加载全量词典/模型的测试，内存占用大，
     * 且 CI 环境没有 data/ 目录，默认与 CI 均不运行。
     */
    public static final String HEAVY = "heavy";

    private TestTags() {
    }
}
