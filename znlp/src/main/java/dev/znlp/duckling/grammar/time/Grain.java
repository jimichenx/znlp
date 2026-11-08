package dev.znlp.duckling.grammar.time;

/** 时间粒度。{@link #justStep(Grain)} 表示两个粒度是否刚好差一级（如日与小时）。 */
public enum Grain {
    Year, Month, Day, Hour, Minute, Second;

    public boolean justStep(Grain g) {
        if (this == Second) return false;
        return (this.ordinal() - g.ordinal()) == -1;
    }
}
