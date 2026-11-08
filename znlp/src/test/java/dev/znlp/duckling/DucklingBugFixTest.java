package dev.znlp.duckling;

import org.junit.jupiter.api.Test;
import dev.znlp.duckling.grammar.duration.DurationDimension;
import dev.znlp.duckling.grammar.money.MoneyDimension;
import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Duckling 审查缺陷 B05 / B06 / B25 / B26 / B27。
 */
public class DucklingBugFixTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2019, 11, 11, 0, 0);

    private final DucklingParser parser = DucklingParser.getInstance();

    @Test
    public void durationOneHour_matchesNumGainRule() {
        List<Dimension> result = parser.parse("一小时");
        assertFalse(result.isEmpty(), "一小时应解析为时长");
        DurationDimension d = assertInstanceOf(DurationDimension.class, result.get(0));
        assertEquals(Grain.Hour, d.getGrain());
        assertEquals(1.0f, d.getValue(), 0.001f);
    }

    @Test
    public void durationHourAndMinute_combinesToMinutes() {
        List<Dimension> result = parser.parse("1小时30分");
        assertFalse(result.isEmpty(), "1小时30分应解析为时长");
        DurationDimension d = assertInstanceOf(DurationDimension.class, result.get(0));
        assertEquals(Grain.Minute, d.getGrain());
        assertEquals(90.0f, d.getValue(), 0.001f);
    }

    @Test
    public void nextSunday_isNextWeekNotPlusSevenDays() {
        List<Dimension> result = parser.parse("下周日", MONDAY);
        DateTimeDimension dt = assertInstanceOf(DateTimeDimension.class, result.get(0));
        assertEquals(2019, dt.getYear());
        assertEquals(11, dt.getMonth());
        assertEquals(24, dt.getDay());
    }

    @Test
    public void nextSundayAlias_isNextWeek() {
        List<Dimension> result = parser.parse("下个星期天", MONDAY);
        DateTimeDimension dt = assertInstanceOf(DateTimeDimension.class, result.get(0));
        assertEquals(2019, dt.getYear());
        assertEquals(11, dt.getMonth());
        assertEquals(24, dt.getDay());
    }

    @Test
    public void fiveFen_isMoneyNotLatentDuration() {
        List<Dimension> result = parser.parse("5分");
        assertFalse(result.isEmpty());
        MoneyDimension money = assertInstanceOf(MoneyDimension.class, result.get(0),
                "「分」单独出现时应优先金额，不应选出 latent 时长");
        assertEquals(0.05f, money.getValue(), 0.0001f);
    }

    @Test
    public void fiveMinutes_isDuration() {
        List<Dimension> result = parser.parse("5分钟");
        DurationDimension d = assertInstanceOf(DurationDimension.class, result.get(0));
        assertEquals(Grain.Minute, d.getGrain());
        assertEquals(5.0f, d.getValue(), 0.001f);
    }

    @Test
    public void twentyFourOClock_normalizesToNextDayMidnight() {
        List<Dimension> result = parser.parse("24点", MONDAY);
        DateTimeDimension dt = assertInstanceOf(DateTimeDimension.class, result.get(0));
        LocalDateTime time = assertDoesNotThrow(dt::toDateTime);
        assertEquals(LocalDateTime.of(2019, 11, 12, 0, 0, 0), time);
    }

    @Test
    public void springFestivalOutsideTable_doesNotThrow() {
        assertDoesNotThrow(() -> parser.parse("3000年春节", MONDAY));
        assertDoesNotThrow(() -> parser.parse("2061年春节", MONDAY));
    }
}
