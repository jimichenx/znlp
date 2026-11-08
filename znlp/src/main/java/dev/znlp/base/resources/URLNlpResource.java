package dev.znlp.base.resources;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * 基于 URL 的 NLP 资源实现。
 * <p>
 * 该类封装了一个 URL，提供从该 URL 读取资源内容的功能。
 * 通常用于从类路径或网络位置加载资源。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * URL url = classLoader.getResource("dict/abc.dict");
 * if (url != null) {
 *     NlpResource resource = new URLNlpResource(url, StandardCharsets.UTF_8);
 *     try (InputStream is = resource.inputStream()) {
 *         // 读取资源内容
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public class URLNlpResource implements NlpResource {

    /**
     * 日志记录器
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(URLNlpResource.class);

    /**
     * 资源的 URL
     */
    private final URL url;

    /**
     * 字符集（保留用于未来扩展）
     */
    private final Charset charset;

    /**
     * 构造一个基于 URL 的资源。
     *
     * @param url     资源的 URL，不能为 null
     * @param charset 字符集，用于读取资源内容
     * @throws NullPointerException 如果 url 为 null
     */
    public URLNlpResource(URL url, Charset charset) {
        if (url == null) {
            throw new NullPointerException("url cannot be null");
        }
        this.url = url;
        this.charset = charset;
    }

    /**
     * 获取资源的输入流。
     * <p>
     * 返回的输入流是缓冲的，以提高读取性能。
     * 调用者负责关闭返回的输入流。
     * </p>
     *
     * @return 资源的缓冲输入流
     * @throws IOException 如果无法打开 URL 连接或读取时发生错误
     */
    @Override
    public InputStream inputStream() throws IOException {
        return new BufferedInputStream(url.openStream());
    }

    /**
     * 返回资源的字符串表示。
     *
     * @return URL 的字符串表示
     */
    @Override
    public String toString() {
        return url.toString();
    }
}
