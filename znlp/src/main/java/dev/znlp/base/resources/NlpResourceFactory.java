package dev.znlp.base.resources;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * NLP 资源工厂接口，用于从不同来源加载资源文件。
 * <p>
 * 资源文件的来源可以是：
 * <ul>
 *   <li>文件系统（通过 {@link FileNlpResourceFactory}）</li>
 *   <li>类路径（通过 {@link ClasspathNlpResourceFactory}）</li>
 *   <li>JAR 文件（通过 {@link JarNlpResourceFactory}）</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * NlpResourceFactory factory = new ClasspathNlpResourceFactory(ClassLoader.getSystemClassLoader());
 * NlpResource resource = factory.load("dict/abc.dict");
 * if (resource != null) {
 *     // 使用资源
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public interface NlpResourceFactory {

    /**
     * 加载资源。
     * <p>
     * 资源名称格式通常为路径形式，例如：{@code dict/abc.dict}
     * </p>
     *
     * @param resourceName 资源名称，格式为 {@code dict/abc.dict} 等路径形式
     * @param charset      字符集，用于读取资源内容
     * @return 如果资源存在则返回 {@link NlpResource} 实例，否则返回 {@code null}
     */
    NlpResource load(String resourceName, Charset charset);

    /**
     * 加载资源，使用 UTF-8 字符集。
     * <p>
     * 这是 {@link #load(String, Charset)} 的便捷方法，默认使用 UTF-8 字符集。
     * </p>
     *
     * @param resourceName 资源名称，格式为 {@code dict/abc.dict} 等路径形式
     * @return 如果资源存在则返回 {@link NlpResource} 实例，否则返回 {@code null}
     */
    default NlpResource load(String resourceName) {
        return load(resourceName, StandardCharsets.UTF_8);
    }
}
