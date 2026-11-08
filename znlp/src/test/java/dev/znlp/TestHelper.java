package dev.znlp;

import dev.znlp.base.common.ModuleLoader;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.File;

/**
 * 测试环境配置工具类
 * <p>
 * 提供便利方法用于在单元测试中快速构建 ZnlpSystemConfig 配置对象，
 * 避免重复编写相同的配置代码。自动检测项目根目录，找到正确的 data/dat 和 data/cache 目录。
 */
public class TestHelper {

    /**
     * 自动检测项目根目录
     * <p>
     * 从当前工作目录向上查找，找到包含 data/dat 目录的项目根目录。
     * 支持从项目根目录或 znlp 子目录运行。
     *
     * @return 项目根目录的绝对路径
     * @throws IllegalStateException 如果无法找到项目根目录
     */
    private static String detectProjectRoot() {
        File currentDir = new File(System.getProperty("user.dir"));

        // 最多向上查找 3 级目录
        for (int i = 0; i < 3; i++) {
            File datDir = new File(currentDir, "data/dat");
            File cacheDir = new File(currentDir, "data/cache");

            // 如果找到 data/dat 和 data/cache 目录，则当前目录就是项目根目录
            if (datDir.exists() && cacheDir.exists()) {
                return currentDir.getAbsolutePath();
            }

            // 向上级目录查找
            currentDir = currentDir.getParentFile();
            if (currentDir == null) {
                break;
            }
        }

        throw new IllegalStateException(
                "无法找到项目根目录，请确保从项目根目录或 znlp 子目录运行测试"
        );
    }

    /**
     * 创建测试环境默认配置
     * <p>
     * 自动检测项目根目录，使用相对于项目根目录的 data/dat 和 data/cache 路径。
     * 支持从项目根目录或 znlp 子目录运行。
     *
     * @return 配置好的 ZnlpSystemConfig 对象
     */
    public static ZnlpSystemConfig createTestConfig() {
        String projectRoot = detectProjectRoot();
        return ZnlpSystemConfig.builder()
                .dataDir(new File(projectRoot, "data/dat").getAbsolutePath())
                .cacheDir(new File(projectRoot, "data/cache").getAbsolutePath())
                .build();
    }

    /**
     * 创建自定义数据目录的测试配置
     *
     * @param dataDir 数据目录路径（绝对路径或相对于项目根目录的路径）
     * @return 配置好的 ZnlpSystemConfig 对象
     */
    private static ZnlpSystemConfig createTestConfig(String dataDir) {
        String projectRoot = detectProjectRoot();
        File dataDirFile = new File(dataDir);
        // 如果不是绝对路径，则相对于项目根目录
        if (!dataDirFile.isAbsolute()) {
            dataDirFile = new File(projectRoot, dataDir);
        }

        return ZnlpSystemConfig.builder()
                .dataDir(dataDirFile.getAbsolutePath())
                .cacheDir(new File(projectRoot, "data/cache").getAbsolutePath())
                .build();
    }

    /**
     * 创建自定义数据和缓存目录的测试配置
     *
     * @param dataDir  数据目录路径（绝对路径或相对于项目根目录的路径）
     * @param cacheDir 缓存目录路径（绝对路径或相对于项目根目录的路径）
     * @return 配置好的 ZnlpSystemConfig 对象
     */
    private static ZnlpSystemConfig createTestConfig(String dataDir, String cacheDir) {
        String projectRoot = detectProjectRoot();

        File dataDirFile = new File(dataDir);
        if (!dataDirFile.isAbsolute()) {
            dataDirFile = new File(projectRoot, dataDir);
        }

        File cacheDirFile = new File(cacheDir);
        if (!cacheDirFile.isAbsolute()) {
            cacheDirFile = new File(projectRoot, cacheDir);
        }

        return ZnlpSystemConfig.builder()
                .dataDir(dataDirFile.getAbsolutePath())
                .cacheDir(cacheDirFile.getAbsolutePath())
                .build();
    }

    /**
     * 创建完整自定义的测试配置
     *
     * @param dataDir              数据目录路径（绝对路径或相对于项目根目录的路径）
     * @param cacheDir             缓存目录路径（绝对路径或相对于项目根目录的路径）
     * @param autoDownloadResource 是否自动下载资源
     * @return 配置好的 ZnlpSystemConfig 对象
     */
    private static ZnlpSystemConfig createTestConfig(String dataDir, String cacheDir, Boolean autoDownloadResource) {
        String projectRoot = detectProjectRoot();

        File dataDirFile = new File(dataDir);
        if (!dataDirFile.isAbsolute()) {
            dataDirFile = new File(projectRoot, dataDir);
        }

        File cacheDirFile = new File(cacheDir);
        if (!cacheDirFile.isAbsolute()) {
            cacheDirFile = new File(projectRoot, cacheDir);
        }

        return ZnlpSystemConfig.builder()
                .dataDir(dataDirFile.getAbsolutePath())
                .cacheDir(cacheDirFile.getAbsolutePath())
                .autoDownloadResource(autoDownloadResource)
                .build();
    }

    /**
     * 创建测试环境的 ServiceRegistry
     * <p>
     * 自动注册配置对象并加载所有模块，提供完整的测试环境。
     * 所有测试类可以复用此方法获取已初始化的 ServiceRegistry。
     *
     * @return 已初始化的 ServiceRegistry 对象
     */
    public static ServiceRegistry createTestServiceRegistry() {
        return createTestServiceRegistry(createTestConfig());
    }

    /**
     * 使用指定配置创建测试环境的 ServiceRegistry
     * <p>
     * 自动注册配置对象并加载所有模块，提供完整的测试环境。
     *
     * @param config 测试配置对象
     * @return 已初始化的 ServiceRegistry 对象
     */
    public static ServiceRegistry createTestServiceRegistry(ZnlpSystemConfig config) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.registerSingleton(ZnlpSystemConfig.class, config);
        ModuleLoader.loadModules(registry, config);
        return registry;
    }
}
