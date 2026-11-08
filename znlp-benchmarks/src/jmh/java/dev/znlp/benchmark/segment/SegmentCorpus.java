package dev.znlp.benchmark.segment;

/**
 * 分词基准内置语料。不读仓库外文件，句子长度分三档，方便对照优化前后。
 */
final class SegmentCorpus {

    static final String SHORT = "南京市长江大桥";

    static final String TYPICAL = "自然语言处理技术是人工智能的重要领域";

    static final String LONG = """
            上海市浦东新区近年来持续推进科技创新和产业发展，人工智能、集成电路与生物医药成为重点方向。
            研究人员认为，中文分词质量会直接影响搜索、推荐和问答系统的召回效果。
            例如「乒乓球拍卖完了」「美国会通过这项法案」这类歧义句，词典路径与感知机路径往往给出不同切分。
            工程上还要同时考虑吞吐量：线上接口通常要求毫秒级延迟，批量离线任务则更看重每秒处理字数。
            """.replace("\n", "");

    private SegmentCorpus() {
    }

    static String text(String size) {
        return switch (size) {
            case "short" -> SHORT;
            case "typical" -> TYPICAL;
            case "long" -> LONG;
            default -> throw new IllegalArgumentException("unknown size: " + size);
        };
    }
}
