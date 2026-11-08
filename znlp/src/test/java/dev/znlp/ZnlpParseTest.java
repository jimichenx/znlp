package dev.znlp;

import org.junit.jupiter.api.Test;
import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZnlpParseTest {

    private static final LocalDateTime REF = LocalDateTime.of(2019, 11, 11, 0, 0);

    @Test
    void nullTextThrows() {
        assertThrows(IllegalArgumentException.class, () -> Znlp.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Znlp.parse(null, REF));
    }

    @Test
    void nullClockThrows() {
        assertThrows(IllegalArgumentException.class, () -> Znlp.parse("明天", null));
    }

    @Test
    void relativeTimeWithFixedClock() {
        List<Dimension> dims = Znlp.parse("明天", REF);
        assertFalse(dims.isEmpty());
        DateTimeDimension dt = assertInstanceOf(DateTimeDimension.class, dims.get(0));
        assertEquals(LocalDateTime.of(2019, 11, 12, 0, 0, 0), dt.toDateTime());
    }

    @Test
    void noMatchReturnsEmptyList() {
        assertTrue(Znlp.parse("没有结构化信息", REF).isEmpty());
        assertTrue(Znlp.parse("").isEmpty());
    }
}
