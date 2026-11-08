package dev.znlp.duckling.grammar.temperature;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

public class TemperatureDimension extends Dimension {
    private final float value;

    public TemperatureDimension(float value) {
        this.value = value;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Temperature;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Temperature(" + value + "度)";
    }
}
