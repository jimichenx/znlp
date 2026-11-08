package dev.znlp.base.resources;

import java.net.URL;
import java.nio.charset.Charset;

/**
 * 从类路径（Classpath）加载资源的工厂实现。
 * <p>
 * 该类使用指定的 {@link ClassLoader} 从类路径中查找和加载资源。
 * 资源名称可以以 "/" 开头，该类会自动处理。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
 * NlpResourceFactory factory = new ClasspathNlpResourceFactory(classLoader);
 * NlpResource resource = factory.load("dict/abc.dict");
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public class ClasspathNlpResourceFactory implements NlpResourceFactory {

    /**
     * 用于加载资源的类加载器
     */
    private final ClassLoader classLoader;

    /**
     * 构造一个类路径资源工厂。
     *
     * @param classLoader 用于加载资源的类加载器，不能为 null
     * @throws NullPointerException 如果 classLoader 为 null
     */
    public ClasspathNlpResourceFactory(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader cannot be null");
        }
        this.classLoader = classLoader;
    }

    /**
     * 从类路径加载资源。
     * <p>
     * 如果资源名称以 "/" 开头，会自动去除该前缀。
     * </p>
     *
     * @param resourceName 资源名称，格式为 {@code dict/abc.dict} 等路径形式
     * @param charset      字符集（在此实现中未使用，保留用于接口兼容性）
     * @return 如果资源存在则返回 {@link URLNlpResource} 实例，否则返回 {@code null}
     */
    @Override
    public NlpResource load(String resourceName, Charset charset) {
        // 去除开头的 "/"（如果存在）
        String path = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;

        URL resource = classLoader.getResource(path);

        if (resource != null) {
            return new URLNlpResource(resource, charset);
        }

        return null;
    }
}
