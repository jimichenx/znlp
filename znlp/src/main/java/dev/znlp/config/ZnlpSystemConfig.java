package dev.znlp.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 系统配置类，存储和定义系统将来用到的配置
 * 该类提供无参构造函数和非public的set方法，由Builder类负责初始化
 */
public class ZnlpSystemConfig {

    private List<String> dataDirs;
    private String cacheDir;
    private Boolean autoDownloadResource;
    private String disabledModules;

    /**
     * 默认无参构造函数
     */
    public ZnlpSystemConfig() {
        this.dataDirs = new ArrayList<>();
    }

    /**
     * 获取数据目录配置
     */
    public List<String> getDataDirs() {
        return Collections.unmodifiableList(dataDirs);
    }

    /**
     * 设置数据目录配置（非public，仅包内可见）
     */
    void setDataDirs(List<String> dataDirs) {
        this.dataDirs = new ArrayList<>(dataDirs);
    }

    /**
     * 设置单个数据目录（保持向后兼容性）
     */
    void setDataDir(String dataDir) {
        this.dataDirs = new ArrayList<>();
        if (dataDir != null && !dataDir.trim().isEmpty()) {
            this.dataDirs.add(dataDir.trim());
        }
    }

    /**
     * 获取单个数据目录（保持向后兼容性）
     */
    public String getDataDir() {
        return dataDirs.isEmpty() ? null : dataDirs.get(0);
    }

    /**
     * 获取缓存目录配置
     */
    public String getCacheDir() {
        return cacheDir;
    }

    /**
     * 设置缓存目录配置（非public，仅包内可见）
     */
    void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * 获取是否自动下载资源的配置
     */
    public Boolean getAutoDownloadResource() {
        return autoDownloadResource;
    }

    /**
     * 设置是否自动下载资源的配置（非public，仅包内可见）
     */
    void setAutoDownloadResource(Boolean autoDownloadResource) {
        this.autoDownloadResource = autoDownloadResource;
    }

    /**
     * 获取被禁用的模块列表。
     * <p>
     * 返回逗号分隔的模块名称列表，例如："module1,module2,module3"。
     * 这些模块在系统初始化时不会被加载。
     *
     * @return 被禁用的模块列表，如果未设置则返回 null
     */
    public String getDisabledModules() {
        return disabledModules;
    }

    /**
     * 设置被禁用的模块列表（非public，仅包内可见）。
     * <p>
     * 值为逗号分隔的模块名称列表，例如："module1,module2,module3"。
     * 这些模块在系统初始化时不会被加载。
     *
     * @param disabledModules 被禁用的模块列表，可以为 null 或空字符串
     */
    void setDisabledModules(String disabledModules) {
        this.disabledModules = disabledModules;
    }

    @Override
    public String toString() {
        return "ZnlpSystemConfig{" +
                "dataDirs=" + dataDirs +
                ", cacheDir='" + cacheDir + '\'' +
                ", autoDownloadResource=" + autoDownloadResource +
                ", disabledModules='" + disabledModules + '\'' +
                '}';
    }

    public static ZnlpSystemConfigBuilder builder() {
        return ZnlpSystemConfigBuilder.create();
    }
}
