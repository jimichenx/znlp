
package dev.znlp.base.common;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

/**
 * 高性能字符读取器，专注于单字符读取操作。
 * <p>
 * 与标准 {@link Reader} 接口不同，此类专门优化了单字符读取的性能。
 * 主要优化点：
 * <ul>
 *   <li>使用 {@link CharBuffer} 进行缓冲读取，减少系统调用</li>
 *   <li>非线程安全设计，避免不必要的同步开销</li>
 *   <li>针对 {@link StringReader} 的性能问题进行了优化（标准 StringReader 每次 read 都会进行同步）</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>注意：</strong>此实现不支持多线程访问，单线程使用可以获得最佳性能。
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>{@code
 * FastCharReader reader = new FastCharReader(new StringReader("Hello World"));
 * int ch;
 * while ((ch = reader.read()) != -1) {
 *     System.out.print((char) ch);
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public class FastCharReader {

    /**
     * 默认缓冲区容量（128字符）
     */
    private static final int DEFAULT_CAPACITY = 128;

    /**
     * 最小缓冲区容量（4字符）
     */
    private static final int MIN_CAPACITY = 4;

    /**
     * 短字符串阈值（小于此长度的字符串会使用更小的缓冲区）
     */
    private static final int SHORT_STRING_THRESHOLD = 256;

    /**
     * 底层字符流
     */
    private Reader reader;

    /**
     * 字符缓冲区
     */
    private CharBuffer buffer;

    /**
     * 缓冲区容量
     */
    private final int capacity;

    /**
     * 当前读取位置偏移量（从0开始）
     */
    private int offset = -1;

    /**
     * 流是否已经结束
     */
    private boolean eof = false;

    /**
     * 使用默认缓冲区容量创建字符读取器。
     *
     * @param reader 底层字符流
     */
    public FastCharReader(Reader reader) {
        this(reader, DEFAULT_CAPACITY);
    }

    /**
     * 从字符串创建字符读取器。
     * <p>
     * 如果字符串长度小于 {@link #SHORT_STRING_THRESHOLD}，会根据字符串长度自动调整缓冲区大小。
     * </p>
     *
     * @param string 输入字符串
     */
    public FastCharReader(String string) {
        this.reader = new StringReader(string);
        if (string.length() < SHORT_STRING_THRESHOLD) {
            this.capacity = Math.max(string.length(), MIN_CAPACITY);
        } else {
            this.capacity = DEFAULT_CAPACITY;
        }
    }

    /**
     * 使用指定的缓冲区容量创建字符读取器。
     *
     * @param reader   底层字符流
     * @param capacity 缓冲区容量（至少为 {@link #MIN_CAPACITY}）
     */
    public FastCharReader(Reader reader, int capacity) {
        this.reader = reader;
        this.capacity = Math.max(capacity, MIN_CAPACITY);
    }

    /**
     * 获取当前读取位置偏移量。
     *
     * @return 当前偏移量（从0开始），如果尚未读取任何字符则返回-1
     */
    public int offset() {
        return offset;
    }

    /**
     * 重置读取器，使用新的字符流。
     * <p>
     * 此方法会清空缓冲区并重置偏移量，允许重用读取器对象。
     * </p>
     *
     * @param reader 新的字符流
     */
    public void reset(Reader reader) {
        this.reader = reader;
        this.buffer = null;
        this.offset = -1;
        this.eof = false;
    }

    /**
     * 读取下一个字符。
     * <p>
     * 使用内部缓冲区进行批量读取，减少系统调用次数，提升性能。
     * </p>
     *
     * @return 读取的字符（0-65535），如果已到达流末尾则返回-1
     * @throws IOException 如果发生I/O错误
     */
    public int read() throws IOException {
        // 如果已经到达流末尾，直接返回-1
        if (eof) {
            return -1;
        }

        // 首次读取或缓冲区已用完，需要填充缓冲区
        if (buffer == null || !buffer.hasRemaining()) {
            if (buffer == null) {
                // 首次读取：初始化缓冲区
                buffer = CharBuffer.allocate(capacity);
            } else {
                // 缓冲区已用完：重置位置
                buffer.clear();
            }

            // 填充缓冲区
            int bytesRead = reader.read(buffer);
            if (bytesRead == -1) {
                eof = true;
                return -1;
            }
            buffer.flip(); // 准备读取
        }

        // 从缓冲区读取字符
        offset++;
        return buffer.get();
    }
}
