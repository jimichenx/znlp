package dev.znlp;

import org.junit.jupiter.api.Test;
import dev.znlp.duckling.grammar.money.MoneyDimension;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZnlpDucklingBoundaryTest {

    private static final LocalDateTime REF = LocalDateTime.of(2019, 11, 11, 0, 0);

    @Test
    void emptyMoneyTextReturnsNoMoneyDimension() {
        List<Dimension> dims = Znlp.parse("", REF);
        assertTrue(dims.isEmpty());
        assertTrue(Znlp.parse("没有金额信息", REF).stream().noneMatch(MoneyDimension.class::isInstance));
    }

    @Test
    void invalidMoneyLikeTextDoesNotCrash() {
        assertDoesNotThrow(() -> Znlp.parse("￥￥￥", REF));
        assertTrue(Znlp.parse("￥￥￥", REF).isEmpty());
    }

    @Test
    void whitespaceOnlyTimeTextReturnsEmpty() {
        assertTrue(Znlp.parse("   ", REF).isEmpty());
    }
}
