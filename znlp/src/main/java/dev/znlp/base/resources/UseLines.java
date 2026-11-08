package dev.znlp.base.resources;

import dev.znlp.base.common.CharSourceLineReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 行处理工具类，提供便捷的方法来处理输入流中的每一行。
 * <p>
 * 该类提供了多种方法来逐行处理输入流：
 * <ul>
 *   <li>{@link #forEachLine(InputStream, boolean, boolean, Consumer)} - 遍历每一行</li>
 *   <li>{@link #useLines(InputStream, boolean, boolean, Consumer)} - 使用行（与 forEachLine 功能相同）</li>
 *   <li>{@link #lineReader(InputStream, Charset)} - 获取行读取器</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * try (InputStream is = resource.inputStream()) {
 *     UseLines.forEachLine(is, true, true, line -> {
 *         // 处理每一行
 *         System.out.println(line);
 *     });
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public final class UseLines {

    /**
     * 私有构造函数，防止实例化
     */
    private UseLines() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 遍历输入流中的每一行，并对每一行执行指定的操作。
     * <p>
     * 该方法会自动关闭输入流。如果需要在处理完成后继续使用输入流，请使用 {@link #lineReader(InputStream, Charset)}。
     * </p>
     *
     * @param inputStream 输入流，不能为 null
     * @param trim        是否去除每行的首尾空白字符
     * @param skipBlank   是否跳过空行
     * @param consumer    对每一行执行的操作，不能为 null
     * @throws NullPointerException 如果 inputStream 或 consumer 为 null
     * @throws RuntimeException     如果读取输入流时发生错误
     */
    public static void forEachLine(
            InputStream inputStream,
            boolean trim,
            boolean skipBlank,
            Consumer<String> consumer) {
        if (inputStream == null) {
            throw new NullPointerException("inputStream cannot be null");
        }
        if (consumer == null) {
            throw new NullPointerException("consumer cannot be null");
        }

        useLines(inputStream, StandardCharsets.UTF_8, trim, skipBlank, consumer);
    }

    /**
     * 遍历输入流中的每一行，并对每一行执行指定的操作。
     * <p>
     * 使用默认设置：去除首尾空白字符，跳过空行，使用 UTF-8 字符集。
     * </p>
     *
     * @param inputStream 输入流，不能为 null
     * @param consumer    对每一行执行的操作，不能为 null
     * @throws NullPointerException 如果 inputStream 或 consumer 为 null
     * @throws RuntimeException     如果读取输入流时发生错误
     */
    public static void forEachLine(InputStream inputStream, Consumer<String> consumer) {
        forEachLine(inputStream, true, true, consumer);
    }

    /**
     * 使用输入流中的每一行，并对每一行执行指定的操作。
     * <p>
     * 该方法与 {@link #forEachLine(InputStream, boolean, boolean, Consumer)} 功能相同，
     * 提供此方法是为了保持与 Kotlin 版本的 API 兼容性。
     * </p>
     *
     * @param inputStream 输入流，不能为 null
     * @param trim        是否去除每行的首尾空白字符
     * @param skipBlank   是否跳过空行
     * @param consumer    对每一行执行的操作，不能为 null
     * @throws NullPointerException 如果 inputStream 或 consumer 为 null
     * @throws RuntimeException     如果读取输入流时发生错误
     */
    public static void useLines(
            InputStream inputStream,
            boolean trim,
            boolean skipBlank,
            Consumer<String> consumer) {
        forEachLine(inputStream, trim, skipBlank, consumer);
    }

    /**
     * 使用输入流中的每一行，并对每一行执行指定的操作。
     * <p>
     * 使用默认设置：去除首尾空白字符，跳过空行，使用 UTF-8 字符集。
     * </p>
     *
     * @param inputStream 输入流，不能为 null
     * @param consumer    对每一行执行的操作，不能为 null
     * @throws NullPointerException 如果 inputStream 或 consumer 为 null
     * @throws RuntimeException     如果读取输入流时发生错误
     */
    public static void useLines(InputStream inputStream, Consumer<String> consumer) {
        useLines(inputStream, true, true, consumer);
    }

    /**
     * 获取一个行读取器，用于逐行读取输入流。
     * <p>
     * 返回的 {@link CharSourceLineReader} 实现了 {@link Iterable} 接口，
     * 可以使用增强 for 循环或流式 API 来处理每一行。
     * </p>
     * <p>
     * 使用示例：
     * <pre>{@code
     * try (CharSourceLineReader reader = UseLines.lineReader(inputStream)) {
     *     for (String line : reader) {
     *         // 处理每一行
     *     }
     * }
     * }</pre>
     * </p>
     *
     * @param inputStream 输入流，不能为 null
     * @param charset     字符集，用于解码输入流
     * @return 行读取器实例
     * @throws NullPointerException 如果 inputStream 或 charset 为 null
     */
    public static CharSourceLineReader lineReader(InputStream inputStream, Charset charset) {
        if (inputStream == null) {
            throw new NullPointerException("inputStream cannot be null");
        }
        if (charset == null) {
            throw new NullPointerException("charset cannot be null");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        return new CharSourceLineReader(reader);
    }

    /**
     * 获取一个行读取器，使用 UTF-8 字符集。
     *
     * @param inputStream 输入流，不能为 null
     * @return 行读取器实例
     * @throws NullPointerException 如果 inputStream 为 null
     */
    public static CharSourceLineReader lineReader(InputStream inputStream) {
        return lineReader(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * 内部方法：使用指定的字符集处理输入流中的每一行。
     *
     * @param inputStream 输入流
     * @param charset     字符集
     * @param trim        是否去除每行的首尾空白字符
     * @param skipBlank   是否跳过空行
     * @param consumer    对每一行执行的操作
     */
    private static void useLines(
            InputStream inputStream,
            Charset charset,
            boolean trim,
            boolean skipBlank,
            Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (trim) {
                    line = line.trim();
                }
                if (skipBlank && line.isEmpty()) {
                    continue;
                }
                consumer.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines from input stream", e);
        }
    }
}
