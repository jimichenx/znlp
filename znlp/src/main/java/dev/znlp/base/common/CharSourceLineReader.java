package dev.znlp.base.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;

/**
 * 字符源行读取器，用于逐行读取 {@link BufferedReader} 的内容。
 * <p>
 * 该类实现了 {@link AbstractIterator}，可以以迭代器的方式逐行读取文本内容。
 * 实现了 {@link AutoCloseable} 接口，支持 try-with-resources 语法。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * try (CharSourceLineReader reader = new CharSourceLineReader(bufferedReader)) {
 *     for (String line : reader) {
 *         // 处理每一行
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public class CharSourceLineReader extends AbstractIterator<String> implements AutoCloseable, Iterable<String> {

    @Override
    public Iterator<String> iterator() {
        return this;
    }

    /**
     * 底层 BufferedReader 实例
     */
    private final BufferedReader reader;

    /**
     * 构造一个字符源行读取器。
     *
     * @param reader 用于读取的 BufferedReader 实例，不能为 null
     */
    public CharSourceLineReader(BufferedReader reader) {
        if (reader == null) {
            throw new NullPointerException("reader cannot be null");
        }
        this.reader = reader;
    }

    /**
     * 计算下一个元素（下一行）。
     * <p>
     * 如果还有下一行，调用 {@link #setNext(Object)} 设置它；
     * 如果没有更多行，调用 {@link #done()} 标记迭代完成。
     * </p>
     */
    @Override
    protected void computeNext() {
        try {
            String line = reader.readLine();
            if (line == null) {
                done();
            } else {
                setNext(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read line from BufferedReader", e);
        }
    }

    /**
     * 关闭底层 BufferedReader。
     * <p>
     * 此方法会关闭关联的 BufferedReader，释放相关资源。
     * </p>
     *
     * @throws RuntimeException 如果关闭时发生 IO 错误
     */
    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close BufferedReader", e);
        }
    }
}
