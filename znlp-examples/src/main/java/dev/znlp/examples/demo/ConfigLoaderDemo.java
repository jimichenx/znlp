package dev.znlp.examples.demo;

import dev.znlp.config.ConfigLoader;
import dev.znlp.config.ZnlpSystemConfig;

import java.util.Properties;

/**
 * ConfigLoader 功能演示：正常属性值加载与空属性值处理。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run -PmainClass=demo.examples.dev.znlp.ConfigLoaderDemo}
 */
public class ConfigLoaderDemo {

    public static void main(String[] args) {
        System.out.println("=== ConfigLoader 功能演示 ===");

        System.out.println("\n1. 正常属性值加载：");
        testNormalProperties();

        System.out.println("\n2. 空属性值加载：");
        testEmptyProperties();

        System.out.println("\n=== 演示完成 ===");
    }

    private static void testNormalProperties() {
        ZnlpSystemConfig config = new ZnlpSystemConfig();
        ConfigLoader.load(config);

        System.out.println("配置内容: " + config);

        boolean allNotNull = (config.getDataDir() != null) &&
                (config.getCacheDir() != null) &&
                (config.getAutoDownloadResource() != null);

        if (allNotNull) {
            System.out.println("所有属性值都被正确加载");
            System.out.println("   dataDir: " + config.getDataDir());
            System.out.println("   cacheDir: " + config.getCacheDir());
            System.out.println("   autoDownloadResource: " + config.getAutoDownloadResource());
        } else {
            System.out.println("正常属性值加载失败");
        }
    }

    /**
     * 通过反射调用私有的 setPropertiesToObject，验证空字符串被当作 null。
     */
    private static void testEmptyProperties() {
        ZnlpSystemConfig config = new ZnlpSystemConfig();
        Properties properties = new Properties();
        properties.setProperty("data-dir", "");
        properties.setProperty("cache-dir", "");
        properties.setProperty("auto-download-resource", "");

        try {
            java.lang.reflect.Method method = ConfigLoader.class.getDeclaredMethod(
                    "setPropertiesToObject", Properties.class, Object.class);
            method.setAccessible(true);
            method.invoke(null, properties, config);

            System.out.println("配置内容: " + config);

            boolean allNull = (config.getDataDir() == null) &&
                    (config.getCacheDir() == null) &&
                    (config.getAutoDownloadResource() == null);

            if (allNull) {
                System.out.println("所有空属性值都被正确设置为 null");
            } else {
                System.out.println("空属性值处理不正确");
                System.out.println("   dataDir: " + config.getDataDir());
                System.out.println("   cacheDir: " + config.getCacheDir());
                System.out.println("   autoDownloadResource: " + config.getAutoDownloadResource());
            }
        } catch (Exception e) {
            System.err.println("演示过程中出现错误: " + e.getMessage());
        }
    }
}
