package dev.znlp.segment.reader;

import org.junit.jupiter.api.Test;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemStopWordDictTest {

    @Test
    void loadsBundledDictOnceAndDropsEditBuffer() {
        SystemStopWordDict first = new SystemStopWordDict(new ZnlpSystemConfig());
        SystemStopWordDict second = new SystemStopWordDict(new ZnlpSystemConfig());

        assertSame(first.loaded(), second.loaded());
        assertFalse(first.loaded().hasPendingEdits());
        assertTrue(first.contains("的"));
        assertTrue(first.contains("一"));
        assertFalse(first.contains("自然语言"));
    }

    @Test
    void mergeFileAddsAndRemovesWords() throws Exception {
        Path root = Files.createTempDirectory("stopwords");
        try {
            Path dir = root.resolve("znlp/stopword-dict");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("merge_stopwords.txt"), "+自定义停用\n-一\n", StandardCharsets.UTF_8);
            ZnlpSystemConfig config = ZnlpSystemConfig.builder().dataDir(root.toString()).build();

            SystemStopWordDict dict = new SystemStopWordDict(config);

            assertTrue(dict.contains("自定义停用"));
            assertTrue(dict.contains("的"));
            assertFalse(dict.contains("一"));
            assertFalse(dict.loaded().hasPendingEdits());
        } finally {
            try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // 临时目录清不掉不影响断言
                    }
                });
            }
        }
    }

    @Test
    void defaultFilterUsesBundledDict() {
        WordSegmenter segmenter = (text, consumer) -> {
            consumer.accept(new Token("苹果", null, 0));
            consumer.accept(new Token("的", null, 2));
            consumer.accept(new Token("香蕉", null, 3));
        };

        List<Token> tokens = new StopwordFilter(new DefaultWordSegmentReader(segmenter))
                .segment("x")
                .toList();

        assertEquals(List.of("苹果", "香蕉"), tokens.stream().map(token -> token.word).toList());
    }

    @Test
    void rejectsNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> new SystemStopWordDict(null));
    }
}
