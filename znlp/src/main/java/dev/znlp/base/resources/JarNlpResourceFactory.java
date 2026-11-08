package dev.znlp.base.resources;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 从 JAR 文件加载资源的工厂实现。
 * <p>
 * 该类在指定的基础目录下查找 JAR 文件，并从这些 JAR 文件中加载资源。
 * JAR 文件按照版本号排序，后加载的版本会覆盖先加载的版本。
 * </p>
 * <p>
 * 版本号格式：{@code x.y.z.jar}，例如 {@code 1.0.9.jar}、{@code 1.0.10.jar}。
 * 版本号会被规范化（补零）以确保正确的排序，例如 {@code 1.0.9} 会排在 {@code 1.0.10} 之前。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * File baseDir = new File("/path/to/jars");
 * NlpResourceFactory factory = new JarNlpResourceFactory(baseDir);
 * NlpResource resource = factory.load("dict/abc.dict");
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public class JarNlpResourceFactory implements NlpResourceFactory {

    /**
     * 日志记录器
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JarNlpResourceFactory.class);

    /**
     * 版本号匹配模式，用于匹配 x.y.z.jar 格式的文件名
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.jar$");

    /**
     * 基础目录
     */
    private final File baseDir;

    /**
     * JAR 文件索引缓存，键为资源名称，值为包含该资源的 JAR 文件
     */
    private volatile Map<String, File> indexCache;

    /**
     * 构造一个 JAR 资源工厂。
     *
     * @param baseDir JAR 文件所在的基础目录，不能为 null
     * @throws NullPointerException 如果 baseDir 为 null
     */
    public JarNlpResourceFactory(File baseDir) {
        if (baseDir == null) {
            throw new NullPointerException("baseDir cannot be null");
        }
        this.baseDir = baseDir;
    }

    /**
     * 从 JAR 文件加载资源。
     * <p>
     * 该方法会扫描基础目录下的所有 JAR 文件，查找指定的资源。
     * 如果多个 JAR 文件中包含同名资源，会返回版本号最大的 JAR 文件中的资源。
     * </p>
     *
     * @param resourceName 资源名称，格式为 {@code dict/abc.dict} 等路径形式
     * @param charset      字符集，用于读取资源内容
     * @return 如果资源存在则返回 {@link ZippedNlpResource} 实例，否则返回 {@code null}
     */
    @Override
    public NlpResource load(String resourceName, Charset charset) {
        // 检查基础目录是否存在且为目录
        if (!baseDir.exists() || !baseDir.isFile()) {
            return null;
        }

        // 构建索引（如果尚未构建）
        Map<String, File> index = buildIndex();

        // 查找包含该资源的 JAR 文件
        File jarFile = index.get(resourceName);
        if (jarFile == null) {
            return null;
        }

        try {
            // 验证资源确实存在于 JAR 文件中
            try (ZipFile zipFile = new ZipFile(jarFile)) {
                ZipEntry entry = zipFile.getEntry(resourceName);
                if (entry == null) {
                    return null;
                }
            }

            return new ZippedNlpResource(jarFile, charset, resourceName);
        } catch (IOException e) {
            logger.error("Failed to load resource: " + resourceName, e);
            return null;
        }
    }

    /**
     * 构建 JAR 文件索引。
     * <p>
     * 该方法会扫描基础目录下的所有 JAR 文件，提取其中的资源名称。
     * JAR 文件按照版本号排序，后加载的版本会覆盖先加载的版本。
     * </p>
     * <p>
     * 该方法使用延迟初始化，第一次调用时构建索引，后续调用返回缓存的索引。
     * </p>
     *
     * @return 资源名称到 JAR 文件的映射
     */
    private Map<String, File> buildIndex() {
        Map<String, File> index = indexCache;
        if (index != null) {
            return index;
        }

        synchronized (this) {
            index = indexCache;
            if (index != null) {
                return index;
            }

            index = doBuildIndex();
            indexCache = index;
            return index;
        }
    }

    /**
     * 执行索引构建。
     *
     * @return 资源名称到 JAR 文件的映射
     */
    private Map<String, File> doBuildIndex() {
        Map<String, File> index = new HashMap<>();

        // 获取所有 JAR 文件
        File[] jarFiles = baseDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (jarFiles == null) {
            return index;
        }

        // 按照版本号排序（后加载的版本会覆盖先加载的版本）
        Arrays.sort(jarFiles, (f1, f2) -> {
            String name1 = normalizeVersion(f1.getName());
            String name2 = normalizeVersion(f2.getName());
            return name1.compareTo(name2);
        });

        // 遍历所有 JAR 文件，提取资源
        for (File jarFile : jarFiles) {
            // 跳过隐藏文件（某些服务器上传文件时会产生 ._ 临时文件）
            if (jarFile.getName().startsWith(".")) {
                continue;
            }

            try (ZipFile zipFile = new ZipFile(jarFile)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        // 后加载的版本会覆盖先加载的版本
                        index.put(name, jarFile);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to read JAR file: " + jarFile.getAbsolutePath(), e);
            }
        }

        return index;
    }

    /**
     * 规范化版本号字符串，用于排序。
     * <p>
     * 将版本号中的每个数字部分补零到 3 位，例如：
     * <ul>
     *   <li>{@code 1.0.9.jar} -> {@code 001.000.009.jar}</li>
     *   <li>{@code 1.0.10.jar} -> {@code 001.000.010.jar}</li>
     * </ul>
     * 这样可以确保 {@code 1.0.9} 排在 {@code 1.0.10} 之前。
     * </p>
     *
     * @param fileName 文件名
     * @return 规范化后的文件名
     */
    private String normalizeVersion(String fileName) {
        java.util.regex.Matcher matcher = VERSION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            String v1 = padStart(matcher.group(1), 3, '0');
            String v2 = padStart(matcher.group(2), 3, '0');
            String v3 = padStart(matcher.group(3), 3, '0');
            matcher.appendReplacement(sb, v1 + "." + v2 + "." + v3 + ".jar");
            matcher.appendTail(sb);
            return sb.toString();
        }
        return fileName;
    }

    /**
     * 在字符串左侧补零（或指定字符）到指定长度。
     *
     * @param str       原始字符串
     * @param minLength 最小长度
     * @param padChar   填充字符
     * @return 补零后的字符串
     */
    private String padStart(String str, int minLength, char padChar) {
        if (str.length() >= minLength) {
            return str;
        }
        StringBuilder sb = new StringBuilder(minLength);
        for (int i = str.length(); i < minLength; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 基于 ZIP/JAR 文件的 NLP 资源实现。
     * <p>
     * 该类封装了一个 ZIP/JAR 文件中的条目，提供从该条目读取资源内容的功能。
     * </p>
     */
    static class ZippedNlpResource implements NlpResource {

        /**
         * ZIP/JAR 文件
         */
        private final File file;

        /**
         * 字符集（保留用于未来扩展）
         */
        private final Charset charset;

        /**
         * 资源在 ZIP 文件中的条目名称
         */
        private final String resourceName;

        /**
         * 构造一个基于 ZIP 条目的资源。
         *
         * @param file         ZIP/JAR 文件，不能为 null
         * @param charset      字符集，用于读取资源内容
         * @param resourceName 资源在 ZIP 文件中的条目名称，不能为 null
         */
        ZippedNlpResource(File file, Charset charset, String resourceName) {
            this.file = file;
            this.charset = charset;
            this.resourceName = resourceName;
        }

        /**
         * 计算资源的哈希值。
         * <p>
         * 使用 ZIP 条目的 CRC 值作为哈希值，这比读取整个文件内容计算 MD5 要快得多。
         * </p>
         *
         * @return 资源的 CRC 值（作为字符串）
         */
        @Override
        public String hash() {
            try (ZipFile zipFile = new ZipFile(file)) {
                ZipEntry entry = zipFile.getEntry(resourceName);
                if (entry != null) {
                    return String.valueOf(entry.getCrc());
                }
            } catch (Exception e) {
                logger.error("Failed to calculate hash for resource: " + resourceName, e);
            }
            return "-1";
        }

        /**
         * 获取资源的输入流。
         * <p>
         * 返回的输入流是缓冲的，以提高读取性能。
         * 当输入流关闭时，关联的 ZipFile 也会被关闭。
         * 调用者负责关闭返回的输入流。
         * </p>
         *
         * @return 资源的缓冲输入流
         * @throws IOException 如果无法打开 ZIP 文件或读取条目时发生错误
         */
        @Override
        public InputStream inputStream() throws IOException {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = zipFile.getEntry(resourceName);
            if (entry == null) {
                zipFile.close();
                throw new FileNotFoundException("Resource not found in JAR: " + resourceName);
            }

            // 创建一个包装输入流，关闭时会同时关闭 ZipFile
            return new BufferedInputStream(zipFile.getInputStream(entry), 16 * 1024) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        zipFile.close();
                    }
                }
            };
        }

        /**
         * 返回资源的字符串表示。
         *
         * @return 格式为 {@code file@resourceName} 的字符串
         */
        @Override
        public String toString() {
            return file + "@" + resourceName;
        }
    }
}
