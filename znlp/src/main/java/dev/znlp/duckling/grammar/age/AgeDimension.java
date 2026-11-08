package dev.znlp.duckling.grammar.age;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

public class AgeDimension extends Dimension {
    private final float value;

    public AgeDimension(float value) {
        this.value = value;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Age;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Age(" + value + "岁)";
    }
}
