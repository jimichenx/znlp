package dev.znlp.base.common;

import java.io.File;

/**
 * 项目路径定位工具
 * <p>
 * 用于定位项目外部的数据目录，如 znlp-data。
 * 从当前工作目录开始，向上逐级查找指定的目录或文件，最多查找 4 层。
 * <p>
 * 使用场景：
 * <ul>
 *     <li>定位训练数据目录：znlp-data/data_train/ner</li>
 *     <li>定位语料数据目录：znlp-data/data_corpus/pku</li>
 *     <li>定位其他项目外部资源目录</li>
 * </ul>
 *
 * @author jimichan
 */
public class ProjectPathLocator {

    /**
     * 默认最大向上查找层数
     */
    private static final int DEFAULT_MAX_DEPTH = 4;

    /**
     * 定位指定路径
     * <p>
     * 从当前工作目录开始，向上逐级查找指定的相对路径，最多查找 4 层。
     *
     * @param relativePath 相对路径，如 "znlp-data/data_train/ner"
     * @return 找到的目录或文件的绝对路径
     * @throws IllegalStateException 如果找不到指定路径
     */
    public static File locate(String relativePath) {
        return locate(relativePath, DEFAULT_MAX_DEPTH);
    }

    /**
     * 定位指定路径
     * <p>
     * 从当前工作目录开始，向上逐级查找指定的相对路径。
     *
     * @param relativePath 相对路径，如 "znlp-data/data_train/ner"
     * @param maxDepth     最大向上查找层数
     * @return 找到的目录或文件的绝对路径
     * @throws IllegalStateException 如果找不到指定路径
     */
    public static File locate(String relativePath, int maxDepth) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("relativePath 不能为空");
        }
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth 必须大于 0");
        }

        File currentDir = new File(System.getProperty("user.dir"));

        // 从当前目录开始，向上逐级查找
        for (int i = 0; i < maxDepth; i++) {
            File targetPath = new File(currentDir, relativePath);

            // 如果找到目标路径，返回其绝对路径
            if (targetPath.exists()) {
                return targetPath;
            }

            // 向上级目录查找
            currentDir = currentDir.getParentFile();
            if (currentDir == null) {
                break;
            }
        }

        // 如果找不到，抛出异常
        throw new IllegalStateException(
                String.format("无法找到路径: %s (从当前工作目录向上查找了 %d 层)", relativePath, maxDepth)
        );
    }

    /**
     * 尝试定位指定路径，如果找不到返回 null
     * <p>
     * 与 {@link #locate(String)} 不同，此方法在找不到路径时返回 null 而不是抛出异常。
     *
     * @param relativePath 相对路径，如 "znlp-data/data_train/ner"
     * @return 找到的目录或文件的绝对路径，找不到返回 null
     */
    public static File tryLocate(String relativePath) {
        return tryLocate(relativePath, DEFAULT_MAX_DEPTH);
    }

    /**
     * 尝试定位指定路径，如果找不到返回 null
     * <p>
     * 与 {@link #locate(String, int)} 不同，此方法在找不到路径时返回 null 而不是抛出异常。
     *
     * @param relativePath 相对路径，如 "znlp-data/data_train/ner"
     * @param maxDepth     最大向上查找层数
     * @return 找到的目录或文件的绝对路径，找不到返回 null
     */
    public static File tryLocate(String relativePath, int maxDepth) {
        try {
            return locate(relativePath, maxDepth);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 检查指定路径是否存在
     * <p>
     * 从当前工作目录开始，向上逐级查找指定的相对路径，最多查找 4 层。
     *
     * @param relativePath 相对路径，如 "znlp-data/data_train/ner"
     * @return 如果找到返回 true，否则返回 false
     */
    public static boolean exists(String relativePath) {
        return tryLocate(relativePath) != null;
    }

    /**
     * 检查指定路径是否存在
     *
     * @param relativePath 相对路径，如 "znlp-data/data_train/ner"
     * @param maxDepth     最大向上查找层数
     * @return 如果找到返回 true，否则返回 false
     */
    public static boolean exists(String relativePath, int maxDepth) {
        return tryLocate(relativePath, maxDepth) != null;
    }

    /**
     * 定位项目根目录
     * <p>
     * 通过查找包含特定标识文件/目录的父目录来定位项目根目录。
     * 默认查找包含 "settings.gradle.kts" 或 "build.gradle.kts" 的目录。
     *
     * @param markerFiles 标识文件名数组，如 ["settings.gradle.kts", "build.gradle.kts"]
     * @return 项目根目录
     * @throws IllegalStateException 如果找不到项目根目录
     */
    public static File locateProjectRoot(String... markerFiles) {
        if (markerFiles == null || markerFiles.length == 0) {
            markerFiles = new String[]{"settings.gradle.kts", "build.gradle.kts", "pom.xml"};
        }

        File currentDir = new File(System.getProperty("user.dir"));

        // 最多向上查找 DEFAULT_MAX_DEPTH 层
        for (int i = 0; i < DEFAULT_MAX_DEPTH; i++) {
            // 检查是否存在任一标识文件
            for (String markerFile : markerFiles) {
                File marker = new File(currentDir, markerFile);
                if (marker.exists()) {
                    return currentDir;
                }
            }

            // 向上级目录查找
            currentDir = currentDir.getParentFile();
            if (currentDir == null) {
                break;
            }
        }

        throw new IllegalStateException(
                String.format("无法找到项目根目录 (查找标识文件: %s)", String.join(", ", markerFiles))
        );
    }

    /**
     * 尝试定位项目根目录，如果找不到返回 null
     *
     * @param markerFiles 标识文件名数组
     * @return 项目根目录，找不到返回 null
     */
    public static File tryLocateProjectRoot(String... markerFiles) {
        try {
            return locateProjectRoot(markerFiles);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * 查找 Gradle 项目根目录
     * <p>
     * 通过向上递归查找包含 gradle/wrapper/gradle-wrapper.jar 的目录来定位项目根目录。
     * 这种方式比查找 settings.gradle.kts 更可靠，因为 gradle wrapper 是 Gradle 项目的标准配置。
     * <p>
     * 适用场景：
     * <ul>
     *     <li>当前工作目录在子模块中（如 znlp/）</li>
     *     <li>需要准确定位到真正的项目根目录</li>
     *     <li>需要相对于项目根目录创建或访问文件</li>
     * </ul>
     *
     * @return Gradle 项目根目录
     * @throws IllegalStateException 如果找不到 Gradle 项目根目录
     */
    public static File findGradleProjectRoot() {
        File currentDir = new File(System.getProperty("user.dir"));

        // 最多向上查找 DEFAULT_MAX_DEPTH 层
        for (int i = 0; i < DEFAULT_MAX_DEPTH; i++) {
            File gradleWrapperJar = new File(currentDir, "gradle/wrapper/gradle-wrapper.jar");

            // 如果找到 gradle-wrapper.jar，则当前目录就是项目根目录
            if (gradleWrapperJar.exists() && gradleWrapperJar.isFile()) {
                return currentDir;
            }

            // 向上级目录查找
            currentDir = currentDir.getParentFile();
            if (currentDir == null) {
                break;
            }
        }

        throw new IllegalStateException(
                "无法找到 Gradle 项目根目录 (未找到 gradle/wrapper/gradle-wrapper.jar)"
        );
    }

    /**
     * 尝试查找 Gradle 项目根目录，如果找不到返回 null
     *
     * @return Gradle 项目根目录，找不到返回 null
     */
    public static File tryFindGradleProjectRoot() {
        try {
            return findGradleProjectRoot();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * 确保目录存在（基于项目根目录）
     * <p>
     * 将给定路径拼接到项目根目录后，如果目录不存在则自动创建。
     * 项目根目录通过查找 gradle/wrapper/gradle-wrapper.jar 来定位。
     * <p>
     * 使用场景：
     * <ul>
     *     <li>创建工作目录：ensureDir("data.work/ner")</li>
     *     <li>创建缓存目录：ensureDir("data/cache")</li>
     *     <li>创建输出目录：ensureDir("output/models")</li>
     * </ul>
     *
     * @param relativePath 相对于项目根目录的路径，如 "data.work/ner"
     * @return 完整的目录 File 对象（已确保存在）
     * @throws IllegalStateException 如果找不到项目根目录或无法创建目录
     */
    public static File ensureProjectPath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("relativePath 不能为空");
        }

        // 查找项目根目录
        File projectRoot = findGradleProjectRoot();

        // 拼接完整路径
        File targetDir = new File(projectRoot, relativePath);

        // 如果目录不存在，创建它
        if (!targetDir.exists()) {
            boolean created = targetDir.mkdirs();
            if (!created) {
                throw new IllegalStateException(
                        "无法创建目录: " + targetDir.getAbsolutePath()
                );
            }
        }

        // 验证是目录而不是文件
        if (!targetDir.isDirectory()) {
            throw new IllegalStateException(
                    "路径存在但不是目录: " + targetDir.getAbsolutePath()
            );
        }

        return targetDir;
    }

    /**
     * 获取相对于项目根目录的路径
     * <p>
     * 将给定路径拼接到项目根目录后返回，但不创建目录。
     * 如果需要自动创建目录，请使用 {@link #ensureProjectPath(String)}。
     *
     * @param relativePath 相对于项目根目录的路径
     * @return 完整的 File 对象（可能不存在）
     * @throws IllegalStateException 如果找不到项目根目录
     */
    public static File getProjectPath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("relativePath 不能为空");
        }

        File projectRoot = findGradleProjectRoot();
        return new File(projectRoot, relativePath);
    }
}
