package dev.znlp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置功能测试类
 */
public class ZnlpSystemConfigTest {

    @BeforeEach
    void setUp() {
        // 清除之前可能设置的JVM参数
        System.clearProperty("znlp.data-dir");
        System.clearProperty("znlp.cache-dir");
        System.clearProperty("znlp.auto-download-resource");
        SystemDefaultConfig.reset();
    }

    @Test
    void testDefaultConfig() {
        System.out.println("测试默认配置初始化...");

        ZnlpSystemConfig config = SystemDefaultConfig.getInstance();

        // 验证默认配置值
        assertEquals("data", config.getDataDir());
        assertEquals("data/cache", config.getCacheDir());
        assertTrue(config.getAutoDownloadResource());

        System.out.println("默认配置验证通过: " + config);
    }

    @Test
    void testJvmParametersOverride() {
        System.out.println("测试JVM参数覆盖配置...");

        // 设置JVM参数
        System.setProperty("znlp.data-dir", "/custom/data/path");
        System.setProperty("znlp.cache-dir", "/custom/cache/path");
        System.setProperty("znlp.auto-download-resource", "false");

        SystemDefaultConfig.reset();

        ZnlpSystemConfig config = SystemDefaultConfig.getInstance();

        // 验证JVM参数是否覆盖了配置文件
        assertEquals("/custom/data/path", config.getDataDir());
        assertEquals("/custom/cache/path", config.getCacheDir());
        assertFalse(config.getAutoDownloadResource());

        System.out.println("JVM参数覆盖验证通过: " + config);
    }

    @Test
    void testConfigBuilder() {
        System.out.println("测试链式Builder类...");

        ZnlpSystemConfig config = ZnlpSystemConfigBuilder.create()
                .dataDir("/builder/data")
                .cacheDir("/builder/cache")
                .autoDownloadResource(false)
                .build();

        // 验证Builder设置的属性值
        assertEquals("/builder/data", config.getDataDir());
        assertEquals("/builder/cache", config.getCacheDir());
        assertFalse(config.getAutoDownloadResource());

        System.out.println("链式Builder验证通过: " + config);
    }

    @Test
    void testBuilderWithDefaults() {
        System.out.println("测试继承默认配置的Builder...");

        ZnlpSystemConfig config = ZnlpSystemConfigBuilder.create()
                .dataDir("/custom/data")
                .buildWithDefaults();

        // 验证设置的属性和继承的默认属性
        assertEquals("/custom/data", config.getDataDir());
        assertEquals("data/cache", config.getCacheDir()); // 继承默认配置
        assertTrue(config.getAutoDownloadResource()); // 继承默认配置

        System.out.println("继承默认配置的Builder验证通过: " + config);
    }

    @Test
    void testConfigImmutability() {
        System.out.println("测试配置对象的访问控制...");

        ZnlpSystemConfig config = SystemDefaultConfig.getInstance();

        // 验证配置对象有get方法
        assertNotNull(config.getDataDir());
        assertNotNull(config.getCacheDir());
        assertNotNull(config.getAutoDownloadResource());

        // 验证没有公开的set方法
        java.lang.reflect.Method[] methods = ZnlpSystemConfig.class.getMethods();

        for (java.lang.reflect.Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                fail("配置对象不应该有公开的set方法: " + method.getName());
            }
        }

        System.out.println("配置对象访问控制验证通过");
    }

    @Test
    void testConfigLoader() {
        System.out.println("测试配置加载工具类...");

        ZnlpSystemConfig config = new ZnlpSystemConfig();
        ConfigLoader.load(config);

        // 验证配置加载
        assertEquals("data", config.getDataDir());
        assertEquals("data/cache", config.getCacheDir());
        assertTrue(config.getAutoDownloadResource());

        System.out.println("配置加载工具类验证通过: " + config);
    }

    @Test
    void testSystemDefaultConfigSingleton() {
        System.out.println("测试系统默认配置的单例性...");

        ZnlpSystemConfig instance1 = SystemDefaultConfig.getInstance();
        ZnlpSystemConfig instance2 = SystemDefaultConfig.getInstance();

        assertSame(instance1, instance2);

        System.out.println("系统默认配置单例性验证通过");
    }

    @Test
    void testEmptyPropertyValue() {
        System.out.println("测试空属性值处理...");

        ZnlpSystemConfig config = new ZnlpSystemConfig();
        ConfigLoader.load(config, "test-empty.properties");

        // 验证空属性值是否被正确设置为null
        assertNull(config.getDataDir());
        assertNull(config.getCacheDir());
        assertNull(config.getAutoDownloadResource());

        System.out.println("空属性值处理验证通过: " + config);
    }
}