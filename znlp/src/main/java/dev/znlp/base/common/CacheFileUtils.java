package dev.znlp.base.common;

import dev.znlp.config.ZnlpSystemConfig;

import java.io.File;

/**
 * 缓存文件工具类，提供缓存文件路径的获取功能。
 * <p>
 * 该类提供了根据配置和缓存名称获取缓存文件路径的静态方法。
 * 缓存目录的优先级为：cacheDir > dataDir > 系统临时目录
 * </p>
 *
 * @author jimichan
 */
public final class CacheFileUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private CacheFileUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 根据配置和缓存名称获取缓存文件。
     * <p>
     * 缓存目录的确定规则：
     * <ol>
     *   <li>如果配置了 cacheDir，使用 cacheDir</li>
     *   <li>否则，如果配置了 dataDir，使用 dataDir</li>
     *   <li>否则，使用系统临时目录（java.io.tmpdir）</li>
     * </ol>
     * </p>
     * <p>
     * 如果缓存目录不存在，会自动创建。
     * </p>
     *
     * @param config    系统配置
     * @param cacheName 缓存文件名（不包含路径）
     * @return 缓存文件对象
     */
    public static File getCacheFile(ZnlpSystemConfig config, String cacheName) {
        String cacheDir = config.getCacheDir();
        if (cacheDir == null || cacheDir.isEmpty()) {
            // 如果没有配置缓存目录，使用 dataDir
            cacheDir = config.getDataDir();
        }
        if (cacheDir == null || cacheDir.isEmpty()) {
            // 如果 dataDir 也没有，使用系统临时目录
            cacheDir = System.getProperty("java.io.tmpdir");
        }

        File cacheDirFile = new File(cacheDir);
        if (!cacheDirFile.exists()) {
            cacheDirFile.mkdirs();
        }

        return new File(cacheDirFile, cacheName);
    }
}

