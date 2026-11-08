package dev.znlp.segment.impl.personname;

/**
 * 人名识别结果
 */
public record PersonName(int offset, int length) {

    @Override
    public String toString() {
        return "PersonName{offset=" + offset + ", length=" + length + '}';
    }
}
