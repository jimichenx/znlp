package dev.znlp.base.resources;

import java.io.*;
import java.nio.charset.Charset;

/**
 * 从文件系统加载资源的工厂实现。
 * <p>
 * 该类在指定的基础目录下查找资源文件。资源名称中的 "/" 会被替换为系统文件分隔符。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * File baseDir = new File("/path/to/resources");
 * NlpResourceFactory factory = new FileNlpResourceFactory(baseDir);
 * NlpResource resource = factory.load("dict/abc.dict");
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public class FileNlpResourceFactory implements NlpResourceFactory {

    /**
     * 基础目录
     */
    private final File baseDir;

    /**
     * 构造一个文件系统资源工厂。
     *
     * @param baseDir 资源文件的基础目录，不能为 null
     * @throws NullPointerException 如果 baseDir 为 null
     */
    public FileNlpResourceFactory(File baseDir) {
        if (baseDir == null) {
            throw new NullPointerException("baseDir cannot be null");
        }
        this.baseDir = baseDir;
    }

    /**
     * 从文件系统加载资源。
     * <p>
     * 资源名称中的 "/" 会被替换为系统文件分隔符。
     * 只有当文件存在且可读时才会返回资源实例。
     * </p>
     *
     * @param resourceName 资源名称，格式为 {@code dict/abc.dict} 等路径形式
     * @param charset      字符集，用于读取资源内容
     * @return 如果资源存在且可读则返回 {@link FileNlpResource} 实例，否则返回 {@code null}
     */
    @Override
    public NlpResource load(String resourceName, Charset charset) {
        // 检查基础目录是否存在且为目录
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return null;
        }

        // 将资源名称中的 "/" 替换为系统文件分隔符
        String normalizedPath = resourceName.replace('/', File.separatorChar);
        File file = new File(baseDir, normalizedPath);

        // 检查文件是否存在且可读
        if (file.exists() && file.canRead()) {
            return new FileNlpResource(file, charset);
        }

        return null;
    }

    /**
     * 基于文件的 NLP 资源实现。
     * <p>
     * 该类封装了一个文件，提供从该文件读取资源内容的功能。
     * </p>
     */
    static class FileNlpResource implements NlpResource {

        /**
         * 资源文件
         */
        private final File file;

        /**
         * 字符集（保留用于未来扩展）
         */
        private final Charset charset;

        /**
         * 构造一个基于文件的资源。
         *
         * @param file    资源文件，不能为 null
         * @param charset 字符集，用于读取资源内容
         */
        FileNlpResource(File file, Charset charset) {
            this.file = file;
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
         * @throws IOException 如果无法打开文件或读取时发生错误
         */
        @Override
        public InputStream inputStream() throws IOException {
            return new BufferedInputStream(new FileInputStream(file));
        }

        /**
         * 返回资源的字符串表示。
         *
         * @return 文件的绝对路径
         */
        @Override
        public String toString() {
            return file.getAbsolutePath();
        }
    }
}
