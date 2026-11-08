package dev.znlp.duckling.grammar.time.time;

import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;
import dev.znlp.duckling.support.FinalNodeValue;

import java.time.LocalDateTime;

/** 时刻图节点（时分秒），可与日期节点组合。 */
public class TimeNodeValue extends FinalNodeValue {
    private final Grain grain;
    private final Integer hour;
    private final Integer minute;
    private final Integer second;
    private final boolean isAM;
    private final boolean isPM;

    public TimeNodeValue(Grain grain, Integer hour, Integer minute, Integer second, boolean isAM, boolean isPM) {
        super(DimensionKind.Time);
        this.grain = grain;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.isAM = isAM;
        this.isPM = isPM;
    }
    
    public TimeNodeValue(Grain grain, Integer hour, Integer minute, Integer second) {
        this(grain, hour, minute, second, false, false);
    }

    public Grain getGrain() {
        return grain;
    }

    public Integer getHour() {
        return hour;
    }

    public Integer getMinute() {
        return minute;
    }

    public Integer getSecond() {
        return second;
    }

    public boolean isAM() {
        return isAM;
    }

    public boolean isPM() {
        return isPM;
    }

    @Override
    public Dimension output(LocalDateTime clock) {
        int h = hour != null ? hour : 0;
        int m = minute != null ? minute : 0;
        int s = second != null ? second : 0;
        
        if (isPM && h < 12) h += 12;
        if (isAM && h == 12) h = 0;

        return new DateTimeDimension(grain, clock.getYear(), clock.getMonthValue(), clock.getDayOfMonth(), h, m, s);
    }
}
