package dev.znlp.duckling.grammar.number;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;
import dev.znlp.duckling.support.FinalNodeValue;

import java.time.LocalDateTime;

/**
 * 图上的数字节点。{@code isDigitSequence} 表示由 0–9 数位拼接而成（不含「十」=10）。
 */
public class NumberNodeValue extends FinalNodeValue {
    public enum NumberType {
        Integer, Double
    }

    private final Number number;
    private final NumberType type;
    private final boolean isDigitSequence;

    public NumberNodeValue(Number number) {
        this(number, number instanceof Double || number instanceof Float ? NumberType.Double : NumberType.Integer);
    }

    public NumberNodeValue(Number number, NumberType type) {
        this(number, type, false);
    }

    public NumberNodeValue(Number number, NumberType type, boolean isDigitSequence) {
        super(DimensionKind.Number);
        this.number = number;
        this.type = type;
        this.isDigitSequence = isDigitSequence;
    }

    public boolean isDigitSequence() {
        return isDigitSequence;
    }

    public boolean isInt() {
        return type == NumberType.Integer;
    }

    public long getInt() {
        return number.longValue();
    }

    public double getDouble() {
        return number.doubleValue();
    }

    public NumberNodeValue multiply(long v) {
        if (type == NumberType.Integer) {
            return new NumberNodeValue(number.longValue() * v, NumberType.Integer);
        } else {
            return new NumberNodeValue(number.doubleValue() * v, NumberType.Double);
        }
    }
    
    public NumberNodeValue multiply(double v) {
        return new NumberNodeValue(number.doubleValue() * v, NumberType.Double);
    }

    public NumberNodeValue add(long v) {
        if (type == NumberType.Integer) {
            return new NumberNodeValue(number.longValue() + v, NumberType.Integer);
        } else {
            return new NumberNodeValue(number.doubleValue() + v, NumberType.Double);
        }
    }

    public NumberNodeValue add(double v) {
        return new NumberNodeValue(number.doubleValue() + v, NumberType.Double);
    }

    public NumberNodeValue add(NumberNodeValue v) {
        if (type == NumberType.Integer) {
            if (v.isInt()) {
                return new NumberNodeValue(number.longValue() + v.getInt(), NumberType.Integer);
            } else {
                return new NumberNodeValue(number.doubleValue() + v.getDouble(), NumberType.Double);
            }
        } else {
            return new NumberNodeValue(number.doubleValue() + v.getDouble(), NumberType.Double);
        }
    }

    @Override
    public Dimension output(LocalDateTime clock) {
        return new NumberDimension(number.doubleValue());
    }
    
    public Number getNumber() {
        return number;
    }
}
