package dev.znlp.base.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ParagraphReaderSmart 的行为基准。
 * <p>
 * 上限 {@code maxLength = expectSize + max(256, expectSize / 2)}。
 * 剩余文本不超过上限时整段返回。超过上限时，只在
 * {@code [expectSize - 1, maxLength)} 内选断点，优先级为换行、句末、句中、空白，
 * 同级取最早的一个，断点字符留在本段。没有断点就在上限处切开，且不拆开代理对。
 * </p>
 */
public class ParagraphReaderSmartTest {

    private static final int EXPECT_SIZE = 10;

    /**
     * expectSize=10 时的硬上限：10 + max(256, 5) = 266。
     */
    private static final int MAX_LENGTH = 266;

    @Test
    public void testConstructor_RejectsNullAndNonPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> new ParagraphReaderSmart((String) null));
        assertThrows(IllegalArgumentException.class, () -> new ParagraphReaderSmart((Reader) null));
        assertThrows(IllegalArgumentException.class, () -> new ParagraphReaderSmart("test", 0));
        assertThrows(IllegalArgumentException.class, () -> new ParagraphReaderSmart("test", -1));
        assertThrows(IllegalArgumentException.class, () -> new ParagraphReaderSmart(new StringReader("test"), 0));
    }

    @Timeout(1000)
    @Test
    public void testNext_Empty() throws IOException {
        assertNull(new ParagraphReaderSmart("").next());
        assertNull(new ParagraphReaderSmart(new StringReader("")).next());

        ParagraphReaderSmart reader = new ParagraphReaderSmart("完");
        assertEquals("完", reader.next());
        assertNull(reader.next());
        assertNull(reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_FitsInMaxLength() throws IOException {
        String text = "第一段。第二段。\n第三段，还没结束";
        assertTrue(text.length() <= MAX_LENGTH);

        List<String> paragraphs = readAll(new ParagraphReaderSmart(text, EXPECT_SIZE));
        assertEquals(List.of(text), paragraphs);
    }

    @Timeout(1000)
    @Test
    public void testNext_IgnoresBreakBeforeWindow() throws IOException {
        String text = "AA。 " + "B".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        assertEquals(MAX_LENGTH, first.length());
        assertTrue(first.startsWith("AA。"));
        assertEquals("B".repeat(MAX_LENGTH - 4), first.substring(4));

        String second = reader.next();
        assertEquals("B".repeat(text.length() - MAX_LENGTH), second);
        assertNull(reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_PrefersHigherPriorityBreak() throws IOException {
        // 窗口内依次出现空白、逗号、句号、换行，应断在最早的换行。
        String head = "A".repeat(EXPECT_SIZE - 1);
        String text = head + " " + "B".repeat(4) + "，" + "C".repeat(8) + "。" + "D".repeat(10) + "\n" + "E".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        assertEquals(head + " " + "B".repeat(4) + "，" + "C".repeat(8) + "。" + "D".repeat(10) + "\n", first);
        assertTrue(first.endsWith("\n"));

        assertEquals("E".repeat(MAX_LENGTH), reader.next());
        assertNull(reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_SamePriorityTakesEarliest() throws IOException {
        String text = "A".repeat(EXPECT_SIZE - 1) + "。" + "B".repeat(20) + "。" + "C".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        assertEquals("A".repeat(EXPECT_SIZE - 1) + "。", reader.next());
        String rest = reader.next();
        assertTrue(rest.startsWith("B"));
    }

    @Timeout(1000)
    @Test
    public void testNext_ClauseBeatsWhitespace() throws IOException {
        String text = "A".repeat(EXPECT_SIZE - 1) + " " + "B".repeat(5) + "，" + "C".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        assertEquals("A".repeat(EXPECT_SIZE - 1) + " " + "B".repeat(5) + "，", reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_BreaksOnWhitespace() throws IOException {
        String text = "A".repeat(EXPECT_SIZE - 1) + "\u3000" + "B".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        assertEquals("A".repeat(EXPECT_SIZE - 1) + "\u3000", first);
        assertEquals("B".repeat(MAX_LENGTH), reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_DoesNotBreakOnWeakSymbols() throws IOException {
        String text = "A".repeat(EXPECT_SIZE - 1)
                + "-"
                + "B".repeat(3)
                + "_"
                + "3.14"
                + "《"
                + "C".repeat(4)
                + "，"
                + "D".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        assertTrue(first.endsWith("，"));
        assertTrue(first.contains("-"));
        assertTrue(first.contains("_"));
        assertTrue(first.contains("3.14"));
        assertTrue(first.contains("《"));
        assertTrue(reader.next().startsWith("D"));
    }

    @Timeout(1000)
    @Test
    public void testNext_HardCutWhenNoBreak() throws IOException {
        String text = "A".repeat(MAX_LENGTH) + "。" + "B".repeat(20);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        assertEquals("A".repeat(MAX_LENGTH), reader.next());
        assertEquals("。" + "B".repeat(20), reader.next());
        assertNull(reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_CrLfAcrossHardEdge() throws IOException {
        String text = "A".repeat(MAX_LENGTH - 1) + "\r\n" + "B".repeat(30);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        assertEquals("A".repeat(MAX_LENGTH - 1) + "\r\n", first);
        assertEquals("B".repeat(30), reader.next());
        assertNull(reader.next());
        assertEquals(readAll(new ParagraphReaderSmart(new ChunkReader(text, 3), EXPECT_SIZE)),
                List.of(first, "B".repeat(30)));
    }

    @Timeout(1000)
    @Test
    public void testNext_ConsumesCrLfTogether() throws IOException {
        String text = "A".repeat(EXPECT_SIZE - 1) + "\r\n" + "B".repeat(MAX_LENGTH);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        assertEquals("A".repeat(EXPECT_SIZE - 1) + "\r\n", first);
        String second = reader.next();
        assertTrue(second.startsWith("B"));
        assertEquals(text, first + second);
    }

    @Timeout(1000)
    @Test
    public void testNext_DoesNotSplitSurrogatePair() throws IOException {
        String text = "A".repeat(MAX_LENGTH - 1) + "🚀" + "B".repeat(80);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text, EXPECT_SIZE);

        String first = reader.next();
        String second = reader.next();
        assertEquals("A".repeat(MAX_LENGTH - 1), first);
        assertTrue(second.startsWith("🚀"));
        assertEquals(text, first + second);
        assertNull(reader.next());
    }

    @Timeout(1000)
    @Test
    public void testNext_DefaultSizeHardCut() throws IOException {
        int defaultMax = 1024 + 512;
        String text = "A".repeat(defaultMax + 40);
        ParagraphReaderSmart reader = new ParagraphReaderSmart(text);

        assertEquals(defaultMax, reader.next().length());
        assertEquals(40, reader.next().length());
        assertNull(reader.next());
    }

    @Timeout(2000)
    @Test
    public void testNext_StringAndReaderMatch() throws IOException {
        String text = "A".repeat(9) + "\n"
                + "B".repeat(20) + "。"
                + "C".repeat(300)
                + "尾部，还有一句！"
                + "D".repeat(400);

        List<String> fromString = readAll(new ParagraphReaderSmart(text, EXPECT_SIZE));
        assertEquals(fromString, readAll(new ParagraphReaderSmart(new StringReader(text), EXPECT_SIZE)));
        assertEquals(fromString, readAll(new ParagraphReaderSmart(new ChunkReader(text, 1), EXPECT_SIZE)));
        assertEquals(fromString, readAll(new ParagraphReaderSmart(new ChunkReader(text, 7), EXPECT_SIZE)));
        assertEquals(fromString, readAll(new ParagraphReaderSmart(new ChunkReader(text, 128), EXPECT_SIZE)));

        StringBuilder joined = new StringBuilder();
        for (String paragraph : fromString) {
            assertTrue(paragraph.length() <= MAX_LENGTH + 1);
            joined.append(paragraph);
        }
        assertEquals(text, joined.toString());
        assertTrue(fromString.size() > 1);
    }

    private static List<String> readAll(ParagraphReader reader) throws IOException {
        List<String> paragraphs = new ArrayList<>();
        String paragraph;
        while ((paragraph = reader.next()) != null) {
            paragraphs.add(paragraph);
        }
        return paragraphs;
    }

    /**
     * 每次最多返回固定字符数，用来逼出分段器的尾部缓存。
     */
    private static final class ChunkReader extends Reader {
        private final String text;
        private final int chunkSize;
        private int pos;

        private ChunkReader(String text, int chunkSize) {
            this.text = text;
            this.chunkSize = chunkSize;
        }

        @Override
        public int read(char[] cbuf, int off, int len) {
            if (pos >= text.length()) {
                return -1;
            }
            int n = Math.min(len, Math.min(chunkSize, text.length() - pos));
            text.getChars(pos, pos + n, cbuf, off);
            pos += n;
            return n;
        }

        @Override
        public void close() {
        }
    }
}
