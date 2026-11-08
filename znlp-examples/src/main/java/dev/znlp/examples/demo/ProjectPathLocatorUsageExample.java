package dev.znlp.examples.demo;

import dev.znlp.base.common.ProjectPathLocator;

import java.io.File;

/**
 * ProjectPathLocator 使用示例
 * <p>
 * 展示如何使用 ProjectPathLocator 的各种方法来管理项目路径
 *
 * @author jimichan
 */
public class ProjectPathLocatorUsageExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("ProjectPathLocator 使用示例");
        System.out.println("=".repeat(60));

        // 示例 1: 定位外部数据目录
        example1_LocateExternalData();

        // 示例 2: 查找 Gradle 项目根目录
        example2_FindGradleRoot();

        // 示例 3: 自动创建工作目录
        example3_EnsureDirectory();

        // 示例 4: 获取项目路径（不创建）
        example4_GetProjectPath();

        // 示例 5: 实际应用场景
        example5_RealWorldUsage();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("所有示例执行完成！");
        System.out.println("=".repeat(60));
    }

    /**
     * 示例 1: 定位外部数据目录
     */
    private static void example1_LocateExternalData() {
        System.out.println("\n【示例 1】定位外部数据目录");
        System.out.println("-".repeat(60));

        // 定位训练数据
        File nerTrainDir = ProjectPathLocator.locate("znlp-data/data_train/ner/train");
        System.out.println("NER 训练数据目录: " + nerTrainDir.getAbsolutePath());

        // 定位语料数据
        File pkuCorpus = ProjectPathLocator.locate("znlp-data/data_corpus/pku");
        System.out.println("PKU 语料目录: " + pkuCorpus.getAbsolutePath());

        // 安全检查路径是否存在
        if (ProjectPathLocator.exists("znlp-data/data_train/ner")) {
            System.out.println("✓ NER 数据目录存在");
        }
    }

    /**
     * 示例 2: 查找 Gradle 项目根目录
     */
    private static void example2_FindGradleRoot() {
        System.out.println("\n【示例 2】查找 Gradle 项目根目录");
        System.out.println("-".repeat(60));

        // 方式 1: 通过 gradle wrapper 查找（推荐）
        File gradleRoot = ProjectPathLocator.findGradleProjectRoot();
        System.out.println("Gradle 项目根目录: " + gradleRoot.getAbsolutePath());

        // 方式 2: 通过标识文件查找
        File projectRoot = ProjectPathLocator.locateProjectRoot();
        System.out.println("项目根目录（标识文件）: " + projectRoot.getAbsolutePath());

        // 验证是否包含 gradle wrapper
        File wrapperJar = new File(gradleRoot, "gradle/wrapper/gradle-wrapper.jar");
        System.out.println("包含 gradle wrapper: " + wrapperJar.exists());
    }

    /**
     * 示例 3: 自动创建工作目录
     */
    private static void example3_EnsureDirectory() {
        System.out.println("\n【示例 3】自动创建工作目录");
        System.out.println("-".repeat(60));

        // 创建工作目录（如果不存在会自动创建）
        File workDir = ProjectPathLocator.ensureProjectPath("data.work/example");
        System.out.println("工作目录: " + workDir.getAbsolutePath());
        System.out.println("目录存在: " + workDir.exists());

        // 创建嵌套目录
        File nestedDir = ProjectPathLocator.ensureProjectPath("build/test-output/nested/deep");
        System.out.println("嵌套目录: " + nestedDir.getAbsolutePath());
        System.out.println("目录存在: " + nestedDir.exists());

        // 再次调用相同路径不会报错
        File sameDirAgain = ProjectPathLocator.ensureProjectPath("data.work/example");
        System.out.println("重复调用结果相同: " + sameDirAgain.equals(workDir));
    }

    /**
     * 示例 4: 获取项目路径（不创建）
     */
    private static void example4_GetProjectPath() {
        System.out.println("\n【示例 4】获取项目路径（不创建）");
        System.out.println("-".repeat(60));

        // 仅获取路径，不创建目录
        File configPath = ProjectPathLocator.getProjectPath("config/application.properties");
        System.out.println("配置文件路径: " + configPath.getAbsolutePath());
        System.out.println("文件存在: " + configPath.exists());

        // 获取数据目录路径
        File dataPath = ProjectPathLocator.getProjectPath("data/models");
        System.out.println("数据目录路径: " + dataPath.getAbsolutePath());
        System.out.println("目录存在: " + dataPath.exists());
    }

    /**
     * 示例 5: 实际应用场景
     */
    private static void example5_RealWorldUsage() {
        System.out.println("\n【示例 5】实际应用场景");
        System.out.println("-".repeat(60));

        try {
            // 场景 1: 模型训练 - 定位训练数据 + 创建工作目录
            System.out.println("\n场景 1: 模型训练");
            File trainData = ProjectPathLocator.locate("znlp-data/data_train/ner/train");
            File workingDir = ProjectPathLocator.ensureProjectPath("data.work/ner-training");
            System.out.println("  训练数据: " + trainData.getAbsolutePath());
            System.out.println("  工作目录: " + workingDir.getAbsolutePath());

            // 场景 2: 模型保存 - 确保输出目录存在
            System.out.println("\n场景 2: 模型保存");
            File modelDir = ProjectPathLocator.ensureProjectPath("znlp-resources/src/main/resources/znlp/ner");
            System.out.println("  模型目录: " + modelDir.getAbsolutePath());
            System.out.println("  目录已创建: " + modelDir.exists());

            // 场景 3: 缓存管理 - 创建缓存目录
            System.out.println("\n场景 3: 缓存管理");
            File cacheDir = ProjectPathLocator.ensureProjectPath("data/cache/feature-cache");
            System.out.println("  缓存目录: " + cacheDir.getAbsolutePath());

            // 场景 4: 日志输出 - 创建日志目录
            System.out.println("\n场景 4: 日志输出");
            File logDir = ProjectPathLocator.ensureProjectPath("logs/training");
            System.out.println("  日志目录: " + logDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示错误处理
     */
    @SuppressWarnings("unused")
    private static void exampleErrorHandling() {
        System.out.println("\n【错误处理示例】");
        System.out.println("-".repeat(60));

        // 使用 tryLocate 处理可能不存在的路径
        File maybeExists = ProjectPathLocator.tryLocate("znlp-data/maybe-not-exists");
        if (maybeExists != null) {
            System.out.println("找到路径: " + maybeExists);
        } else {
            System.out.println("路径不存在，使用默认配置");
        }

        // 使用 exists 预先检查
        if (ProjectPathLocator.exists("znlp-data/data_train/ner")) {
            File nerDir = ProjectPathLocator.locate("znlp-data/data_train/ner");
            System.out.println("使用 NER 训练数据: " + nerDir);
        } else {
            System.out.println("NER 训练数据不存在，跳过训练");
        }
    }
}
