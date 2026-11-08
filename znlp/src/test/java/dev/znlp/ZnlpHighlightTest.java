package dev.znlp;

import org.junit.jupiter.api.Test;
import dev.znlp.highlight.SmartHighlightConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZnlpHighlightTest {

    @Test
    void nullArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> Znlp.highlight((String) null, "q"));
        assertThrows(IllegalArgumentException.class, () -> Znlp.highlight("text", null));
        assertThrows(IllegalArgumentException.class,
                () -> Znlp.highlight("text", "q", (SmartHighlightConfig) null));
        assertThrows(IllegalArgumentException.class, () -> Znlp.highlight((List<String>) null, "q"));
    }

    @Test
    void singleHighlightWrapsMatch() {
        String text = "复旦大学位于上海市";
        String highlighted = Znlp.highlight(text, "复旦");
        assertTrue(highlighted.contains("<em>复旦</em>"), highlighted);
    }

    @Test
    void batchHighlightPreservesOrder() {
        List<String> texts = List.of("复旦大学", "上海交通大学");
        List<String> result = Znlp.highlight(texts, "大学");
        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("<em>大学</em>"));
        assertTrue(result.get(1).contains("<em>大学</em>"));
    }

    @Test
    void emptyQueryReturnsOriginalText() {
        String text = "原始文本";
        assertEquals(text, Znlp.highlight(text, ""));
    }
}
