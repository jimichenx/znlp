package dev.znlp.duckling.grammar.distance;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

public class DistanceDimension extends Dimension {
    private final float value;
    private final DistanceUnit unit;

    public DistanceDimension(float value, DistanceUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Distance;
    }

    public float toKm() {
        switch (unit) {
            case KM: return value;
            case M: return value / 1000;
            default: return value;
        }
    }

    public float getValue() {
        return value;
    }

    public DistanceUnit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "Distance(" + value + " " + unit + ")";
    }

    public enum DistanceUnit {
        KM, M
    }
}
