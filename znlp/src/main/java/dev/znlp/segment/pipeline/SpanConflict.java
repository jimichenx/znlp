package dev.znlp.segment.pipeline;

/**
 * 同一字符跨度上已有候选时，后到的候选源怎么处理。
 */
public enum SpanConflict {

    /**
     * 替换已有顶点。
     */
    REPLACE,

    /**
     * 已有顶点保持不动，包括词性和词频。
     */
    KEEP,

    /**
     * 没有顶点时才写入。已有顶点只在词性为空时补词性，不改 wordID 和词频。
     */
    FILL_IF_ABSENT
}
