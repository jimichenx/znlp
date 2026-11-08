package dev.znlp.duckling.grammar.number;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

/**
 * 对外输出的数字。大数请用 {@link #getValue()}，{@link #getInt()} 会截断到 32 位。
 */
public class NumberDimension extends Dimension {
    private final double value;

    public NumberDimension(double value) {
        this.value = value;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Number;
    }

    public double getValue() {
        return value;
    }

    public boolean isInt() {
        return value % 1 == 0;
    }

    public int getInt() {
        return (int) value;
    }

    @Override
    public String toString() {
        return "Number(" + value + ")";
    }
}
