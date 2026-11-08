package dev.znlp.duckling.support;

/**
 * 对外输出的一个结构化维度，带原文切片与包含式 {@code [start, end]}。
 */
public abstract class Dimension implements NodeValue {
    protected String text;
    protected int start;
    protected int end;

    public Dimension with(String text, int start, int end) {
        this.text = text;
        this.start = start;
        this.end = end;
        return this;
    }

    public String getText() {
        return text;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public abstract DimensionKind getKind();
}
