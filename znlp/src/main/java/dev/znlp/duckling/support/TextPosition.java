package dev.znlp.duckling.support;

/**
 * 半闭区间跨度，{@code end} <strong>包含</strong>最后一个字符的下标。
 */
public record TextPosition(int start, int end) {

    public int getLength() {
        return end - start + 1;
    }

    public boolean isDisjoint(TextPosition other) {
        return this.end < other.start || this.start > other.end;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + "]";
    }
}
