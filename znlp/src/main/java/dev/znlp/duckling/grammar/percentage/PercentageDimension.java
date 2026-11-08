package dev.znlp.duckling.grammar.percentage;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

public class PercentageDimension extends Dimension {
    private final float value;

    public PercentageDimension(float value) {
        this.value = value;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Percentage;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Percentage(" + value + ")";
    }
}
