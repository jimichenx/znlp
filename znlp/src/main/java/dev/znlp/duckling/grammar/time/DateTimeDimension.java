package dev.znlp.duckling.grammar.time;

import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日期与时钟。{@code hour == 24} 会归一成次日 00:00。
 */
public class DateTimeDimension extends Dimension {
    private final Grain grain;
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;
    private final int second;

    private boolean future = false;
    private List<DateTimeDimension> futureList;

    public DateTimeDimension(Grain grain, int year, int month, int day) {
        this(grain, year, month, day, 0, 0, 0);
    }

    public DateTimeDimension(Grain grain, int year, int month, int day, int hour, int minute, int second) {
        if (hour == 24) {
            LocalDateTime nextDay = LocalDateTime.of(year, month, day, 0, minute, second).plusDays(1);
            this.grain = grain;
            this.year = nextDay.getYear();
            this.month = nextDay.getMonthValue();
            this.day = nextDay.getDayOfMonth();
            this.hour = 0;
            this.minute = nextDay.getMinute();
            this.second = nextDay.getSecond();
        } else {
            this.grain = grain;
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Time;
    }

    public Grain getGrain() {
        return grain;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public void setFuture(boolean future) {
        this.future = future;
    }

    public boolean isFuture() {
        return future;
    }

    public List<DateTimeDimension> getFutureList() {
        return futureList;
    }

    public void setFutureList(List<DateTimeDimension> futureList) {
        this.futureList = futureList;
        this.future = true;
    }
    
    public DateTimeDimension makeFuture() {
        this.future = true;
        return this;
    }

    public LocalDateTime toDateTime() {
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    @Override
    public String toString() {
        if (future) {
            return String.format("DateTime(time:%s,grain:%s,future:%s)", toDateTime(), grain.name().toLowerCase(), futureList);
        } else {
            return String.format("DateTime(time:%s,grain:%s)", toDateTime(), grain.name().toLowerCase());
        }
    }
}
