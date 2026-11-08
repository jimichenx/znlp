package dev.znlp;

import org.junit.jupiter.api.Test;
import dev.znlp.newwords.entity.NewWord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZnlpNewWordsTest {

    @Test
    void nullCorpusDirThrows() {
        assertThrows(NullPointerException.class, () -> Znlp.discoverNewWords((java.io.File) null));
        assertThrows(NullPointerException.class, () -> Znlp.discoverNewWords((String) null));
    }

    @Test
    void missingDirectoryThrows() {
        Path missing = Path.of("non-existent-corpus-dir-" + System.nanoTime());
        assertThrows(RuntimeException.class, () -> Znlp.discoverNewWords(missing.toFile()));
    }

    @Test
    void tinyCorpusReturnsIterableResult() throws IOException {
        Path dir = Files.createTempDirectory("znlp-nw-test");
        try {
            Files.writeString(dir.resolve("corpus.txt"),
                    "云计算平台 云计算平台 大数据 大数据 大数据\n".repeat(20),
                    StandardCharsets.UTF_8);

            List<NewWord> words = Znlp.discoverNewWords(dir.toFile());
            assertNotNull(words);
            for (NewWord word : words) {
                assertNotNull(word.getWord());
            }
        } finally {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    void emptyDirectoryReturnsEmptyListOrThrows() throws IOException {
        Path dir = Files.createTempDirectory("znlp-nw-empty");
        try {
            try {
                List<NewWord> words = Znlp.discoverNewWords(dir.toFile());
                assertTrue(words.isEmpty());
            } catch (RuntimeException ex) {
                assertNotNull(ex.getMessage());
            }
        } finally {
            Files.deleteIfExists(dir);
        }
    }
}
