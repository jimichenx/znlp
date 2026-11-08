package dev.znlp.base.common;

/**
 * 资源坐标为 {@code dev.znlp:znlp-resources}，全部词典与模型都在这一个纯资源 jar 内，
 * 运行时路径统一带 {@code znlp/} 前缀。
 * <p>
 * 该常量供资源缺失时的提示信息与自动下载使用。版本需与 Gradle 的 {@code version} 保持一致。
 */
public final class DictResources {

    public static final String GROUP = "dev.znlp";
    public static final String ARTIFACT_ID = "znlp-resources";
    public static final String VERSION = "0.1.0";

    /**
     * 资源 jar 内的根目录。
     */
    public static final String PREFIX = "znlp/";

    private DictResources() {
    }

    /**
     * 判断资源是否由 {@code znlp-resources} 提供。
     * <p>
     * 运行时请求的路径形如 {@code znlp/core-dict/CoreDict.txt}，与 jar 内布局一致，
     * 因此按前缀判定即可，不再维护逐条资源的白名单。
     *
     * @param resourcePath 运行时资源路径
     * @return true 表示可以在资源 jar 中找到
     */
    public static boolean provides(String resourcePath) {
        return resourcePath != null && resourcePath.startsWith(PREFIX);
    }

    /**
     * Maven / Gradle 坐标，用于错误信息。
     */
    public static String coordinate() {
        return GROUP + ":" + ARTIFACT_ID + ":" + VERSION;
    }
}
