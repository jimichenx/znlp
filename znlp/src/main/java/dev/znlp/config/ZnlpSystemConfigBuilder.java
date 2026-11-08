package dev.znlp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的链式配置构建器类
 * 提供链式API来创建和配置 ZnlpSystemConfig 实例
 */
public class ZnlpSystemConfigBuilder {

    private final ZnlpSystemConfig config;

    private ZnlpSystemConfigBuilder() {
        this.config = new ZnlpSystemConfig();
    }

    /**
     * 创建新的配置构建器实例
     */
    public static ZnlpSystemConfigBuilder create() {
        return new ZnlpSystemConfigBuilder();
    }

    /**
     * 设置数据目录配置
     */
    public ZnlpSystemConfigBuilder dataDir(String dataDir) {
        config.setDataDir(dataDir);
        return this;
    }

    /**
     * 设置多个数据目录配置
     */
    public ZnlpSystemConfigBuilder dataDirs(List<String> dataDirs) {
        List<String> trimmedDirs = new ArrayList<>();
        if (dataDirs != null) {
            for (String dir : dataDirs) {
                if (dir != null && !dir.trim().isEmpty()) {
                    trimmedDirs.add(dir.trim());
                }
            }
        }
        config.setDataDirs(trimmedDirs);
        return this;
    }

    /**
     * 设置多个数据目录配置（逗号分隔的字符串）
     */
    public ZnlpSystemConfigBuilder dataDirs(String dataDirs) {
        List<String> dirs = new ArrayList<>();
        if (dataDirs != null && !dataDirs.trim().isEmpty()) {
            String[] split = dataDirs.split(",");
            for (String dir : split) {
                String trimmed = dir.trim();
                if (!trimmed.isEmpty()) {
                    dirs.add(trimmed);
                }
            }
        }
        config.setDataDirs(dirs);
        return this;
    }

    /**
     * 设置缓存目录配置
     */
    public ZnlpSystemConfigBuilder cacheDir(String cacheDir) {
        config.setCacheDir(cacheDir);
        return this;
    }

    /**
     * 设置是否自动下载资源的配置
     */
    public ZnlpSystemConfigBuilder autoDownloadResource(boolean autoDownloadResource) {
        config.setAutoDownloadResource(autoDownloadResource);
        return this;
    }

    /**
     * 设置被禁用的模块列表。
     * <p>
     * 值为逗号分隔的模块名称列表，例如："module1,module2,module3"。
     * 这些模块在系统初始化时不会被加载。
     *
     * @param disabledModules 被禁用的模块列表，可以为 null 或空字符串
     * @return 当前构建器实例，用于链式调用
     */
    public ZnlpSystemConfigBuilder disabledModules(String disabledModules) {
        config.setDisabledModules(disabledModules);
        return this;
    }

    /**
     * 构建配置对象
     */
    public ZnlpSystemConfig build() {
        return config;
    }

    /**
     * 构建配置对象并应用系统默认配置（用于继承未设置的属性）
     */
    public ZnlpSystemConfig buildWithDefaults() {
        ZnlpSystemConfig defaultConfig = SystemDefaultConfig.getInstance();

        // 如果没有设置某些属性，继承默认配置
        if (config.getDataDir() == null) {
            config.setDataDir(defaultConfig.getDataDir());
        }
        if (config.getCacheDir() == null) {
            config.setCacheDir(defaultConfig.getCacheDir());
        }
        if (config.getAutoDownloadResource() == null) {
            config.setAutoDownloadResource(defaultConfig.getAutoDownloadResource());
        }
        if (config.getDisabledModules() == null) {
            config.setDisabledModules(defaultConfig.getDisabledModules());
        }

        return config;
    }
}