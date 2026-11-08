package dev.znlp.examples.demo;

import dev.znlp.config.ConfigLoader;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 演示从 classpath 的 test-empty.properties 加载全为空的配置项。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run -PmainClass=demo.examples.dev.znlp.EmptyPropertyDemo}
 */
public class EmptyPropertyDemo {

    public static void main(String[] args) {
        System.out.println("=== 测试空属性值处理 ===");

        ZnlpSystemConfig config = new ZnlpSystemConfig();
        ConfigLoader.load(config, "test-empty.properties");

        System.out.println("加载后的配置: " + config);

        System.out.println("\n=== 属性值验证 ===");
        System.out.println("dataDir: " + config.getDataDir());
        System.out.println("cacheDir: " + config.getCacheDir());
        System.out.println("autoDownloadResource: " + config.getAutoDownloadResource());

        boolean allNull = (config.getDataDir() == null) &&
                (config.getCacheDir() == null) &&
                (config.getAutoDownloadResource() == null);

        System.out.println(allNull
                ? "\n所有空属性值都被正确设置为 null"
                : "\n空属性值处理不正确");

        System.out.println("\n=== 演示完成 ===");
    }
}
