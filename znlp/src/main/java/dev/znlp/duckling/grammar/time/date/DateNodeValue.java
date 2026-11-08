package dev.znlp.duckling.grammar.time.date;

import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.grammar.time.time.TimeNodeValue;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;
import dev.znlp.duckling.support.FinalNodeValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日期图节点。落地时先解析星期（「下周日」），再叠加 {@code dayOffset}。
 */
public class DateNodeValue extends FinalNodeValue {
    private final Grain grain;
    private final Integer year;
    private final Integer yearOffset;
    private final Integer month;
    private final Integer monthOffset;
    private final Integer day;
    private final Integer dayOffset;
    private final Integer weekday;
    private final TimeNodeValue time;
    private final Map<Integer, DateValue> computed;

    public DateNodeValue(Grain grain, Integer year, Integer yearOffset, Integer month, Integer monthOffset, Integer day, Integer dayOffset, Integer weekday, TimeNodeValue time, Map<Integer, DateValue> computed) {
        super(DimensionKind.Time);
        this.grain = grain;
        this.year = year;
        this.yearOffset = yearOffset;
        this.month = month;
        this.monthOffset = monthOffset;
        this.day = day;
        this.dayOffset = dayOffset;
        this.weekday = weekday;
        this.time = time;
        this.computed = computed;
    }

    public DateNodeValue(Grain grain, Integer year, Integer yearOffset, Integer month, Integer monthOffset, Integer day, Integer dayOffset, Integer weekday, Map<Integer, DateValue> computed) {
        this(grain, year, yearOffset, month, monthOffset, day, dayOffset, weekday, null, computed);
    }

    public DateNodeValue(Grain grain, Integer year, Integer yearOffset, Integer month, Integer monthOffset, Integer day, Integer dayOffset, Map<Integer, DateValue> computed) {
        this(grain, year, yearOffset, month, monthOffset, day, dayOffset, null, computed);
    }


    // Constructor for Holidays (Grain, computed map)
    public DateNodeValue(Grain grain, Map<Integer, DateValue> computed) {
        this(grain, null, null, null, null, null, null, computed);
    }

    // Constructor for Month/Day (Grain, month, day)
    public DateNodeValue(Grain grain, int month, int day) {
        this(grain, null, null, month, null, day, null, null);
    }

    // Builder/Copy-like constructor or setters would be useful, but for now full constructor.
    // I'll add getters.

    public Grain getGrain() {
        return grain;
    }

    public boolean isYearGrain() { return Grain.Year == grain; }
    public boolean isMonthGrain() { return Grain.Month == grain; }
    public boolean isDayGrain() { return Grain.Day == grain; }

    private boolean hasYear() { return year != null || yearOffset != null; }
    private boolean hasMonth() { return month != null || monthOffset != null; }

    public boolean justMissYearForDay() {
        return this.grain == Grain.Day && (computed != null || hasMonth()) && !hasYear();
    }

    public boolean justHasDay() {
        return this.grain == Grain.Day && !((hasYear() && hasMonth()) || hasMonth() || computed != null);
    }

    public boolean isOffset() {
        return yearOffset != null || monthOffset != null || dayOffset != null;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getYearOffset() {
        return yearOffset;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getMonthOffset() {
        return monthOffset;
    }

    public Integer getDay() {
        return day;
    }

    public Integer getDayOffset() {
        return dayOffset;
    }
    
    public Integer getWeekday() {
        return weekday;
    }
    
    public TimeNodeValue getTime() {
        return time;
    }

    public Map<Integer, DateValue> getComputed() {

        return computed;
    }

    @Override
    public Dimension output(LocalDateTime clock) {
        DateTimeDimension dim = null;
        if (grain == Grain.Year) {
            dim = outputYear(clock);
        } else if (grain == Grain.Month) {
            dim = outputMonth(clock);
        } else if (grain == Grain.Day || grain == Grain.Hour || grain == Grain.Minute || grain == Grain.Second) {
            dim = (DateTimeDimension) outputDay(clock);
        } else {
            throw new RuntimeException("unknown status: " + grain);
        }

        if (dim != null && time != null) {
            int h = time.getHour() != null ? time.getHour() : 0;
            int m = time.getMinute() != null ? time.getMinute() : 0;
            int s = time.getSecond() != null ? time.getSecond() : 0;

            if (time.isPM() && h < 12) h += 12;
            if (time.isAM() && h == 12) h = 0;

            return new DateTimeDimension(grain, dim.getYear(), dim.getMonth(), dim.getDay(), h, m, s);
        }
        return dim;
    }

    private DateTimeDimension outputYear(LocalDateTime clock) {
        if (year != null) {
            return new DateTimeDimension(Grain.Year, year, 1, 1);
        }
        if (yearOffset != null) {
            LocalDate dt = clock.toLocalDate().plusYears(yearOffset);
            return new DateTimeDimension(Grain.Year, dt.getYear(), 1, 1);
        }
        throw new RuntimeException("not found year info");
    }

    private DateTimeDimension outputMonth(LocalDateTime clock) {
        LocalDate dt = clock.toLocalDate();
        if (monthOffset != null) {
            LocalDate pre = dt.plusMonths(monthOffset);
            return new DateTimeDimension(Grain.Month, pre.getYear(), pre.getMonthValue(), 1);
        }

        Integer monthV = month;
        if (monthV != null) {
            if (hasYear()) {
                int y = outputYear(clock).getYear();
                return new DateTimeDimension(Grain.Month, y, monthV, 1);
            }

            int thisMonth = clock.getMonthValue();
            DateTimeDimension dim;
            if (monthV >= thisMonth) {
                dim = new DateTimeDimension(Grain.Month, dt.getYear(), monthV, 1);
            } else {
                dim = new DateTimeDimension(Grain.Month, dt.getYear() + 1, monthV, 1);
            }
            return dim.makeFuture();
        }
        return null;
    }

    private Dimension outputDay(LocalDateTime clock) {
        LocalDate today = clock.toLocalDate();

        if (weekday != null) {
            LocalDate date = today.with(DayOfWeek.of(weekday));
            if (dayOffset != null) {
                date = date.plusDays(dayOffset);
            }
            return toDimension(date);
        }

        if (dayOffset != null) {
            LocalDate to = today.plusDays(dayOffset);
            return new DateTimeDimension(Grain.Day, to.getYear(), to.getMonthValue(), to.getDayOfMonth());
        }

        if (computed != null) {
            int targetYear;
            boolean feature;
            if (hasYear()) {
                targetYear = outputYear(clock).getYear();
                feature = false;
            } else {
                targetYear = today.getYear();
                feature = true;
            }

            if (!feature) {
                DateValue ymd = computed.get(targetYear);
                if (ymd == null) {
                    return null;
                }
                return new DateTimeDimension(Grain.Day, ymd.year(), ymd.month(), ymd.day());
            } else {
                List<DateValue> list = new ArrayList<>();
                if (computed.containsKey(targetYear)) list.add(computed.get(targetYear));
                if (computed.containsKey(targetYear + 1)) list.add(computed.get(targetYear + 1));
                if (computed.containsKey(targetYear + 2)) list.add(computed.get(targetYear + 2));

                List<LocalDate> f = list.stream()
                        .map(it -> LocalDate.of(it.year(), it.month(), it.day()))
                        .collect(Collectors.toList());
                
                if (f.isEmpty()) return null; // Should not happen if computed is correct

                LocalDate first = f.get(0);

                DateTimeDimension resultDim;
                if (today.isBefore(first) || today.equals(first)) {
                    resultDim = toDimension(first);
                    resultDim.setFutureList(f.stream().map(this::toDimension).collect(Collectors.toList()));
                } else {
                    if (f.size() > 1) {
                        LocalDate second = f.get(1);
                        List<LocalDate> f2 = f.subList(1, f.size());
                        resultDim = toDimension(second);
                        resultDim.setFutureList(f2.stream().map(this::toDimension).collect(Collectors.toList()));
                    } else {
                        // Fallback
                        resultDim = toDimension(first);
                    }
                }
                return resultDim;
            }
        }

        if (day != null) {
            DateTimeDimension mon = outputMonth(clock);
            if (mon == null) {
                return new DateTimeDimension(Grain.Day, today.getYear(), today.getMonthValue(), day);
            } else {
                return new DateTimeDimension(Grain.Day, mon.getYear(), mon.getMonth(), day);
            }
        }

        throw new RuntimeException("not found day info");
    }

    private DateTimeDimension toDimension(LocalDate date) {
        return new DateTimeDimension(Grain.Day, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
