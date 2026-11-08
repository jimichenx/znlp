package dev.znlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZnlpTransformTest {

    @Test
    void nullTextThrows() {
        assertThrows(IllegalArgumentException.class, () -> Znlp.s2t(null));
        assertThrows(IllegalArgumentException.class, () -> Znlp.t2s(null));
    }

    @Test
    void emptyStringReturnsEmpty() {
        assertEquals("", Znlp.s2t(""));
        assertEquals("", Znlp.t2s(""));
    }

    @Test
    void singleCharRoundTrip() {
        assertEquals("發", Znlp.s2t("发"));
        assertEquals("发", Znlp.t2s("發"));
    }

    @Test
    void phraseUsesLongestMatch() {
        String simplified = "软件开发";
        String traditional = Znlp.s2t(simplified);
        assertEquals("軟件開發", traditional);
        assertEquals(simplified, Znlp.t2s(traditional));
    }
}
