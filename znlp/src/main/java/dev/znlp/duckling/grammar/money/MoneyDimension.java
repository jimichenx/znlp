package dev.znlp.duckling.grammar.money;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

/**
 * 金额（人民币）。grain 表示精度档：元=1、角=2、分=3。
 */
public class MoneyDimension extends Dimension {
    private final float value;
    private final MoneyUnit unit;
    private final int grain;

    public MoneyDimension(float value, MoneyUnit unit) {
        this(value, unit, 1);
    }
    
    public MoneyDimension(float value, int grain) {
        this(value, MoneyUnit.RMB, grain);
    }

    public MoneyDimension(float value, MoneyUnit unit, int grain) {
        this.value = value;
        this.unit = unit;
        this.grain = grain;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.AmountOfMoney;
    }

    public float getValue() {
        return value;
    }

    public MoneyUnit getUnit() {
        return unit;
    }

    public int getGrain() {
        return grain;
    }

    @Override
    public String toString() {
        return "Money(" + value + " " + unit + ")";
    }

    public enum MoneyUnit {
        RMB
    }
}
