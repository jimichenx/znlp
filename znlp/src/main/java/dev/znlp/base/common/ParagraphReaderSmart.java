package dev.znlp.base.common;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * 按长度上限把文档切成段。
 * <p>
 * 剩余内容不超过 {@code maxLength} 时整段返回。超过上限时，只在
 * {@code [expectSize - 1, maxLength)} 里选断点，优先级从高到低是换行、句末、句中、空白，
 * 同级取最早的一个，断点字符算进本段。{@code \r\n} 会一起吃掉。
 * 窗口里没有断点就在 {@code maxLength} 处切开，并避免拆开 UTF-16 代理对。
 * </p>
 * <p>
 * {@code maxLength = expectSize + max(256, expectSize / 2)}。字符串直接按下标切片；
 * {@link Reader} 批量读入一块缓冲区，未消费的尾部留给下一段。
 * </p>
 *
 * @author jimichan
 */
public class ParagraphReaderSmart implements ParagraphReader {

    /**
     * 默认期望长度。
     */
    public static final int DEFAULT_EXPECT_SIZE = 1024;

    /**
     * 默认期望长度对应的段落上限。
     */
    public static int defaultMaxLength() {
        return maxLength(DEFAULT_EXPECT_SIZE);
    }

    private static final int MIN_PADDING = 256;

    private static final int PRI_WHITESPACE = 1;
    private static final int PRI_CLAUSE = 2;
    private static final int PRI_SENTENCE = 3;
    private static final int PRI_NEWLINE = 4;

    private final String text;
    private final Reader reader;
    private final int expectSize;
    private final int maxLength;

    private int pos;
    private char[] buf;
    private int len;
    private boolean eof;

    /**
     * 用默认期望长度读取字符串。
     *
     * @param text 全文，不能为 null
     */
    public ParagraphReaderSmart(String text) {
        this(text, DEFAULT_EXPECT_SIZE);
    }

    /**
     * 用指定期望长度读取字符串。
     *
     * @param text       全文，不能为 null
     * @param expectSize 期望长度，必须为正
     */
    public ParagraphReaderSmart(String text, int expectSize) {
        this.expectSize = checkExpectSize(expectSize);
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        this.text = text;
        this.reader = null;
        this.maxLength = maxLength(expectSize);
    }

    /**
     * 用默认期望长度读取字符流。
     *
     * @param reader 字符流，不能为 null
     */
    public ParagraphReaderSmart(Reader reader) {
        this(reader, DEFAULT_EXPECT_SIZE);
    }

    /**
     * 用指定期望长度读取字符流。
     *
     * @param reader     字符流，不能为 null
     * @param expectSize 期望长度，必须为正
     */
    public ParagraphReaderSmart(Reader reader, int expectSize) {
        this.expectSize = checkExpectSize(expectSize);
        if (reader == null) {
            throw new IllegalArgumentException("reader cannot be null");
        }
        this.text = null;
        this.reader = reader;
        this.maxLength = maxLength(expectSize);
        // 多留一个字符，用来识别贴在上限上的 \r\n 和代理对
        this.buf = new char[this.maxLength + 1];
    }

    /**
     * 读取下一段。文档结束后返回 null。
     */
    @Override
    public String next() throws IOException {
        if (text != null) {
            return nextString();
        }
        return nextReader();
    }

    private String nextString() {
        int available = text.length() - pos;
        if (available <= 0) {
            return null;
        }
        int cut = consume(text, pos, available, true);
        String paragraph = text.substring(pos, pos + cut);
        pos += cut;
        return paragraph;
    }

    private String nextReader() throws IOException {
        fillTo(maxLength + 1);
        if (len == 0) {
            return null;
        }
        boolean endOfInput = len <= maxLength;
        int cut = consume(CharBuffer.wrap(buf, 0, len), 0, len, endOfInput);
        String paragraph = new String(buf, 0, cut);
        int tail = len - cut;
        if (tail > 0) {
            System.arraycopy(buf, cut, buf, 0, tail);
        }
        len = tail;
        return paragraph;
    }

    private void fillTo(int target) throws IOException {
        while (len < target && !eof) {
            int n = reader.read(buf, len, target - len);
            if (n < 0) {
                eof = true;
                return;
            }
            if (n == 0) {
                throw new IOException("Reader.read returned 0");
            }
            len += n;
        }
    }

    /**
     * 计算本段要消费的字符数。
     *
     * @param endOfInput available 之后没有更多字符
     */
    private int consume(CharSequence seq, int offset, int available, boolean endOfInput) {
        if (endOfInput && available <= maxLength) {
            return available;
        }

        int limit = Math.min(available, maxLength);
        int from = offset + expectSize - 1;
        int to = offset + limit;
        int breakAt = -1;
        int bestPriority = 0;
        for (int i = from; i < to; i++) {
            int priority = breakPriority(seq.charAt(i));
            if (priority > bestPriority) {
                breakAt = i;
                bestPriority = priority;
                if (priority == PRI_NEWLINE) {
                    break;
                }
            }
        }

        if (breakAt < 0) {
            int cut = limit;
            if (cut > 0
                    && cut < available
                    && Character.isHighSurrogate(seq.charAt(offset + cut - 1))
                    && Character.isLowSurrogate(seq.charAt(offset + cut))) {
                cut--;
            }
            return cut;
        }

        int cut = breakAt - offset + 1;
        if (seq.charAt(breakAt) == '\r'
                && cut < available
                && seq.charAt(offset + cut) == '\n') {
            cut++;
        }
        return cut;
    }

    private static int maxLength(int expectSize) {
        return expectSize + Math.max(MIN_PADDING, expectSize / 2);
    }

    private static int checkExpectSize(int expectSize) {
        if (expectSize <= 0) {
            throw new IllegalArgumentException("expectSize must be positive, got: " + expectSize);
        }
        return expectSize;
    }

    private static int breakPriority(char c) {
        switch (c) {
            case '\n':
            case '\r':
                return PRI_NEWLINE;
            case '。':
            case '！':
            case '？':
            case '!':
            case '?':
            case '；':
            case ';':
                return PRI_SENTENCE;
            case '，':
            case '、':
            case ',':
                return PRI_CLAUSE;
            case ' ':
            case '\t':
            case '\u3000':
            case '\u00A0':
                return PRI_WHITESPACE;
            default:
                return 0;
        }
    }
}
