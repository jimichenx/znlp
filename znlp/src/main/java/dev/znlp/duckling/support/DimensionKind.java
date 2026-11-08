package dev.znlp.duckling.support;

/**
 * 维度种类。{@link #priority} 越小越“底层”（数字 0），越大越容易在同类冲突时留下（时间 5）。
 */
public enum DimensionKind {
    Number(0),
    AmountOfMoney(1),
    Ordinal(2),
    Temperature(3),
    Duration(4),
    Time(5),
    Date(6),
    PhoneNumber(7),
    Email(8),
    Url(9),
    Distance(10),
    Age(11),
    Percentage(12);

    private final int priority;

    DimensionKind(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
