package dev.znlp.duckling.grammar.phonenum;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

public class PhoneNumDimension extends Dimension {
    private final String value;

    public PhoneNumDimension(String value) {
        this.value = value;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.PhoneNumber;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "PhoneNum(" + value + ")";
    }
}
