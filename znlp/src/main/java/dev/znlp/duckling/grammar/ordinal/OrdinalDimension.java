package dev.znlp.duckling.grammar.ordinal;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

public class OrdinalDimension extends Dimension {
    private final int value;

    public OrdinalDimension(int value) {
        this.value = value;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Ordinal;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "序号: 第" + value;
    }
}
