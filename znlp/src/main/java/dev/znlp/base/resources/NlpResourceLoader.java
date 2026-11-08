package dev.znlp.base.resources;

import dev.znlp.base.common.DictResources;
import dev.znlp.base.common.DownloadUtils;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NLP资源加载器，负责根据指定配置加载NLP相关的资源文件。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>多源资源查找：优先从本地文件系统 → Jar包 → Classpath中查找资源</li>
 *   <li>自动下载：资源未找到且允许自动下载时，从远程仓库自动下载所需资源</li>
 *   <li>资源缓存：避免重复加载，提升性能</li>
 *   <li>并发安全：使用并发容器和原子变量保证线程安全</li>
 *   <li>资源验证：加载前验证资源的完整性和可读性</li>
 *   <li>性能监控：记录加载统计信息，包括总数、失败数和平均耗时</li>
 * </ul>
 *
 * <h2>支持的资源源</h2>
 * <ol>
 *   <li><b>本地文件系统</b>：从config中指定的dataDir目录查找资源文件</li>
 *   <li><b>本地Jar包</b>：从dataDir目录下的jar文件内查找资源</li>
 *   <li><b>Classpath</b>：从classpath中查找资源</li>
 * </ol>
 *
 * <h2>使用示例</h2>
 * <pre>
 * ZnlpSystemConfig config = new ZnlpSystemConfigBuilder().build();
 * NlpResourceLoader loader = new NlpResourceLoader(config);
 *
 * // 直接加载，不存在时抛异常
 * NlpResource resource = loader.loadResource("dict/abc.dict");
 *
 * // 尝试加载，不存在时返回null
 * NlpResource optionalResource = loader.tryLoadResource("dict/abc.dict");
 *
 * // 获取统计信息
 * LoadStatistics stats = loader.getStatistics();
 * System.out.println(stats.getTotalLoads());
 * </pre>
 *
 * <h2>异常处理</h2>
 * <ul>
 *   <li>{@link ResourceNotFoundException} - 资源不存在且无法下载时抛出</li>
 *   <li>{@link ResourceDownloadException} - 自动下载失败时抛出</li>
 *   <li>{@link ResourceValidationException} - 资源验证失败时抛出</li>
 * </ul>
 *
 * @author jimichan@gmail.com
 * @date 2025/08/08
 * @see NlpResource
 * @see ZnlpSystemConfig
 */
public class NlpResourceLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NlpResourceLoader.class);

    // 资源缓存，避免重复加载
    private final Map<String, NlpResource> resourceCache = new ConcurrentHashMap<>();

    // 加载统计信息
    private final AtomicLong loadCount = new AtomicLong(0);
    private final AtomicLong totalLoadTime = new AtomicLong(0);
    private final AtomicLong failedLoadCount = new AtomicLong(0);

    private final List<NlpResourceFactory> resourceFactories;

    private final ZnlpSystemConfig config;

    /**
     * 构造函数
     *
     * @param config 配置对象，包含数据目录、缓存目录及自动下载开关等参数
     */
    public NlpResourceLoader(ZnlpSystemConfig config) {
        this.config = config;
        this.resourceFactories = initializeResourceFactories();
    }

    public File getCacheDir() {
        File dir = new File(config.getCacheDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 初始化资源工厂列表
     */
    private List<NlpResourceFactory> initializeResourceFactories() {
        List<NlpResourceFactory> list = new ArrayList<>();
        if (config.getDataDirs() != null && !config.getDataDirs().isEmpty()) {
            for (String dataDir : config.getDataDirs()) {
                File dataDirFile = new File(dataDir);
                if (dataDirFile.exists()) {
                    list.add(new FileNlpResourceFactory(dataDirFile));
                    list.add(new JarNlpResourceFactory(dataDirFile));
                }
            }
        }
        list.add(new ClasspathNlpResourceFactory(NlpResourceLoader.class.getClassLoader()));
        return list;
    }

    /**
     * 加载资源，使用默认UTF-8字符集。
     * <p>
     * 优先从本地缓存中查找；如果缓存不存在，下仄改又下载。
     * </p>
     *
     * @param resourcePath 资源路径名称，如 "dict/abc.dict"
     * @return 成功加载的资源对象
     * @throws ResourceNotFoundException 当资源不存在且无法下载时抛出
     * @throws IllegalArgumentException  当resourcePath为空或非法路径时抛出
     * @see #tryLoadResource(String)
     * @see #loadResource(String, Charset)
     */
    public NlpResource loadResource(String resourcePath) {
        return loadResource(resourcePath, StandardCharsets.UTF_8);
    }

    /**
     * 使用指定字符集加载资源。
     * <p>
     * 该方法是主要的资源加载入口，执行以下步骤：
     * <ol>
     *   <li>验证资源路径的合法性</li>
     *   <li>从缓存中查找资源（快速路径）</li>
     *   <li>如果缓存不存在，从多个资源工厂中加载</li>
     *   <li>加载失败且允许自动下载时，尝试自动下载资源</li>
     *   <li>验证资源的完整性</li>
     *   <li>缓存资源不资源过期是主加载时间穷根</li>
     * </ol>
     * </p>
     *
     * @param resourcePath 资源路径名称，如 "dict/abc.dict"
     * @param charset      字符集
     * @return 成功加载的资源对象
     * @throws ResourceNotFoundException 当资源不存在且无法下载时抛出
     * @throws IllegalArgumentException  当resourcePath为空或非法路径时抛出
     * @see #tryLoadResource(String, Charset)
     * @see #loadNlpResource(String, Charset)
     */
    public NlpResource loadResource(String resourcePath, Charset charset) {
        validateResourcePath(resourcePath);

        // 构建缓存键：资源路径 + 字符集名称，确保不同字符集编码的资源分别缓存
        String cacheKey = resourcePath + ":" + charset.name();
        NlpResource cached = resourceCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Resource loaded from cache: " + resourcePath);
            return cached;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 第一次尝试：从资源工厂加载
            NlpResource resource = loadNlpResource(resourcePath, charset);

            // 如果加载失败且启用自动下载，则尝试下载资源后重新加载
            if (resource == null && Boolean.TRUE.equals(config.getAutoDownloadResource())) {
                logger.debug("Resource not found, attempting to download: " + resourcePath);
                tryDownload(resourcePath);
                resource = loadNlpResource(resourcePath, charset);
            }

            // 最终检查：资源仍然不存在则抛出异常
            if (resource == null) {
                failedLoadCount.incrementAndGet();
                throw createResourceNotFoundException(resourcePath);
            }

            // 验证资源的完整性和可读性
            validateResource(resource, resourcePath);

            // 将资源存入缓存以供后续快速检索
            resourceCache.put(cacheKey, resource);

            // 更新性能统计数据
            long loadTime = System.currentTimeMillis() - startTime;
            loadCount.incrementAndGet();
            totalLoadTime.addAndGet(loadTime);

            logger.debug("Resource loaded successfully: " + resourcePath + " (" + loadTime + "ms)");
            return resource;

        } catch (Exception e) {
            failedLoadCount.incrementAndGet();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }

    }

    /**
     * 计算资源的哈希值，用于资源完整性的验证。
     * <p>
     * 如果资源不存在，返回null而不抛异常。
     * </p>
     *
     * @param resourceName 资源名称
     * @return 资源的哈希值，资源不存在时返回null
     */
    public String hashResource(String resourceName) {
        NlpResource resource = tryLoadResource(resourceName, StandardCharsets.UTF_8);
        return resource != null ? resource.hash() : null;
    }

    /**
     * 尝试加载资源，安全增强版本。
     * <p>
     * 不存在的资源返回null而不抛出异常，供前缓准预处理可选资源的代码使用。
     * </p>
     *
     * @param resourcePath 资源路径
     * @param charset      字符集
     * @return 如果资源存在，返回NlpResource对象；否则返回null
     * @see #loadResource(String, Charset)
     */
    public NlpResource tryLoadResource(String resourcePath, Charset charset) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        String cacheKey = resourcePath + ":" + charset.name();
        NlpResource cached = resourceCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        NlpResource resource = loadNlpResource(resourcePath, charset);
        if (resource != null) {
            resourceCache.put(cacheKey, resource);
        }
        return resource;
    }

    /**
     * 使用默认UTF-8字符集尝试加载资源。
     * <p>
     * 这是{@link #tryLoadResource(String, Charset)}的便利方法。
     * </p>
     *
     * @param resourcePath 资源路径
     * @return 如果资源存在，返回NlpResource对象；否则返回null
     * @see #tryLoadResource(String, Charset)
     */
    public NlpResource tryLoadResource(String resourcePath) {
        return tryLoadResource(resourcePath, StandardCharsets.UTF_8);
    }

    /**
     * 清除所有缓存的资源对象。
     * <p>
     * 清除后的投洋抬变量（统计数据）并不会重置，仅需清空缓存容器。
     * </p>
     */
    public void clearCache() {
        resourceCache.clear();
        logger.info("Resource cache cleared");
    }

    /**
     * 获取资源加载的统计信息。
     * <p>
     * 返回的统计信息包含：
     * <ul>
     *   <li>成功加载次数</li>
     *   <li>加载失败次数</li>
     *   <li>平均加载耗时（毫秒）</li>
     *   <li>当前缓存中的资源旅路数</li>
     * </ul>
     * </p>
     *
     * @return 流量统计信息对象
     * @see LoadStatistics
     */
    public LoadStatistics getStatistics() {
        long totalLoads = loadCount.get();
        long avgLoadTime = totalLoads > 0 ? totalLoadTime.get() / totalLoads : 0;
        return new LoadStatistics(
                totalLoads,
                failedLoadCount.get(),
                avgLoadTime,
                resourceCache.size()
        );
    }

    /**
     * 检查资源是否存在。
     * <p>
     * 预先检查资源是否可以加载，非上靠精定加载。
     * </p>
     *
     * @param resourcePath 资源路径
     * @return 资源存在时返回true，否则返回false
     */
    public boolean resourceExists(String resourcePath) {
        return tryLoadResource(resourcePath) != null;
    }

    /**
     * 获取所有可用的资源工厂。
     * <p>
     * 返回的是工厂列表的副本，不会影响内部流量。
     * </p>
     *
     * @return 资源工厂列表的一个新的序列
     * @see NlpResourceFactory
     */
    public List<NlpResourceFactory> getResourceFactories() {
        return new ArrayList<>(resourceFactories);
    }

    private synchronized NlpResource loadNlpResource(String resourceName, Charset charset) {
        long startTime = System.currentTimeMillis();

        // 按照工厂列表的优先级顺序尝试从不同源加载资源
        // 优先级：本地文件系统 > 本地Jar包 > Classpath
        for (NlpResourceFactory factory : resourceFactories) {
            NlpResource resource = factory.load(resourceName, charset);
            if (resource != null) {
                String string = resource.toString();
                // 对过长的路径字符串进行截断，避免日志输出过长
                if (string.length() >= 100) {
                    string = "../.." + string.substring(string.length() - 60);
                }
                logger.debug("Resource loaded from factory: " + string);
                return resource;
            }
        }

        // 记录搜索耗时，便于性能分析
        long loadTime = System.currentTimeMillis() - startTime;
        logger.debug("Resource not found: " + resourceName + " (searched in " + resourceFactories.size() + " factories, " + loadTime + "ms)");
        return null;
    }

    /**
     * 尝试从远程仓库下载资源。
     * <p>
     * 根据资源名查找对应的下载信息，构建远程URL，并将资源下载到本地dataDir目录。
     * 如果资源信息未在字典中注册，则记录警告日志但不抛异常。
     * </p>
     *
     * @param resourcePath 资源路径
     * @throws ResourceDownloadException 下载失败时抛出
     */
    private void tryDownload(String resourcePath) {
        // 读取 properties 中的路径不带前缀，而运行时请求带 znlp/ 前缀，故按前缀判定
        if (DictResources.provides(resourcePath)) {
            String group = DictResources.GROUP.replace('.', '/');
            String jarName = DictResources.ARTIFACT_ID;
            String version = DictResources.VERSION;
            // 构建完整的远程下载URL
            String path = "https://repo.huaweicloud.com/repository/maven/" + group + "/" + jarName + "/" + version + "/" + jarName + "-" + version + ".jar";
            // 指定本地保存路径为第一个数据目录
            File to = new File(config.getDataDir(), jarName + "-" + version + ".jar");

            logger.info("Downloading " + jarName + ".jar from " + path + " to " + to.getAbsolutePath());

            try {
                DownloadUtils.download(path, to);
                logger.info("Download completed successfully: " + jarName + ".jar");
            } catch (IOException e) {
                logger.error("Failed to download " + jarName + ".jar from " + path, e);
                throw new ResourceDownloadException("Failed to download resource: " + resourcePath, e);
            }
        } else {
            // 资源未在字典中注册，记录警告但不抛异常
            logger.debug("No download information available for resource: " + resourcePath);
        }
    }

    /**
     * 验证资源路径的合法性。
     * <p>
     * 检查路径不为空，且不包含非法字符（如".."和"/"前缀）。
     * </p>
     *
     * @param resourcePath 资源路径
     * @throws IllegalArgumentException 路径为空或包含非法字符时抛出
     */
    private void validateResourcePath(String resourcePath) {
        // 检查路径不为空或空白符
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }

        // 检查路径中是否存在非法字符，防止目录遍历攻击
        // 不允许包含 ".." 序列或以 "/" 开头的绝对路径
        if (resourcePath.contains("..") || resourcePath.startsWith("/")) {
            throw new IllegalArgumentException("Invalid resource path: " + resourcePath);
        }
    }

    /**
     * 验证资源的完整性和可读性。
     * <p>
     * 通过打开资源的输入流来验证资源是否可读。
     * 验证失败会抛出异常，保证返回的资源都是有效的。
     * </p>
     *
     * @param resource     待验证的资源对象
     * @param resourcePath 资源路径（用于错误信息）
     * @throws ResourceValidationException 验证失败时抛出
     */
    private void validateResource(NlpResource resource, String resourcePath) {
        try {
            // 通过打开并关闭输入流来验证资源的可读性
            // 这只验证资源是否可以正确访问，不读取实际内容
            try (InputStream in = resource.inputStream()) {
                // 验证流打开成功即可，不需要读取内容
                Objects.requireNonNull(in, "inputStream must not be null");
            }
        } catch (Exception e) {
            logger.error("Resource validation failed: " + resourcePath, e);
            throw new ResourceValidationException("Resource validation failed: " + resourcePath, e);
        }
    }

    /**
     * 构建资源不存在异常，包含详细的错误信息和依赖提示。
     * <p>
     * 如果资源在字典中有对应的依赖信息，则生成Maven和Gradle的依赖配置；
     * 否则提示用户检查资源路径是否正确。
     * </p>
     *
     * @param resourcePath 资源路径
     * @return 包含详细信息的异常对象
     */
    private ResourceNotFoundException createResourceNotFoundException(String resourcePath) {
        StringBuilder info = new StringBuilder();
        info.append("\n\n没有找到资源 ").append(resourcePath);

        // 判断资源是否由资源 jar 提供，是则给出依赖坐标
        if (DictResources.provides(resourcePath)) {
            info.append("，需要添加依赖 ").append(DictResources.coordinate()).append("\n\n");
            // 生成Maven依赖配置
            info.append("Maven:\n\n");
            info.append(
                    "<dependency>\n" +
                            "    <groupId>").append(DictResources.GROUP).append("</groupId>\n" +
                    "    <artifactId>").append(DictResources.ARTIFACT_ID).append("</artifactId>\n" +
                    "    <version>").append(DictResources.VERSION).append("</version>\n" +
                    "</dependency>\n\n"
            );
            // 生成Gradle依赖配置
            info.append("或者 Gradle:\n\n");
            info.append("implementation '").append(DictResources.coordinate()).append("'\n");
            info.append("\n");
        } else {
            // 资源未注册，提示用户检查路径正不正确
            info.append("，请检查资源路径是否正确\n");
        }

        return new ResourceNotFoundException(info.toString());
    }

    /**
     * 资源未找到异常
     */
    public static class ResourceNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * 资源下载异常
     */
    public static class ResourceDownloadException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ResourceDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 资源验证异常
     */
    public static class ResourceValidationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ResourceValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
         * 加载统计信息，记录资源加载的性能数据。
         * <p>
         * 包含以下统计指标：
         * <ul>
         *   <li>totalLoads - 成功加载的总数</li>
         *   <li>failedLoads - 加载失败的次数</li>
         *   <li>averageLoadTime - 平均加载耗时（毫秒）</li>
         *   <li>cacheSize - 当前缓存中的资源数</li>
         * </ul>
         * </p>
         */
        public record LoadStatistics(long totalLoads, long failedLoads, long averageLoadTime, int cacheSize) {

        /**
             * 获取总加载次数。
             *
             * @return 成功加载的总次数
             */
            @Override
            public long totalLoads() {
                return totalLoads;
            }

            /**
             * 获取失败加载次数。
             *
             * @return 加载失败的次数
             */
            @Override
            public long failedLoads() {
                return failedLoads;
            }

            /**
             * 获取平均加载耗时。
             *
             * @return 平均加载时间（毫秒），无加载时返回0
             */
            @Override
            public long averageLoadTime() {
                return averageLoadTime;
            }

            /**
             * 获取当前缓存中的资源数。
             *
             * @return 缓存中的资源条目数
             */
            @Override
            public int cacheSize() {
                return cacheSize;
            }

            @Override
            public String toString() {
                return "LoadStatistics{" +
                        "totalLoads=" + totalLoads +
                        ", failedLoads=" + failedLoads +
                        ", averageLoadTime=" + averageLoadTime +
                        ", cacheSize=" + cacheSize +
                        '}';
            }
        }
}
