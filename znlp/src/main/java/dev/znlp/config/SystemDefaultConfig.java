package dev.znlp.config;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统默认配置类，提供静态实例和JVM启动时的初始化
 * 负责管理系统配置的全局访问和初始化
 */
public final class SystemDefaultConfig {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SystemDefaultConfig.class);

    private static volatile ZnlpSystemConfig instance;

    static {
        // JVM启动时初始化默认配置
        initialize();
    }

    private SystemDefaultConfig() {
        // 工具类，不允许实例化
    }

    /**
     * 初始化配置
     */
    private static synchronized void initialize() {
        if (instance == null) {
            ZnlpSystemConfig config = ConfigLoader.load(new ZnlpSystemConfig());
            overrideWithSystemProperties(config);
            instance = config;
        }
    }

    /**
     * 使用系统属性（JVM参数）覆盖配置
     */
    private static void overrideWithSystemProperties(ZnlpSystemConfig config) {
        for (Field field : ZnlpSystemConfig.class.getDeclaredFields()) {
            String kebab = toKebabCase(field.getName());
            String systemValue = System.getProperty("znlp." + kebab);

            if (field.getName().equals("dataDirs") && systemValue == null) {
                systemValue = System.getProperty("znlp.data-dir");
            }

            if (systemValue != null) {
                try {
                    Method setter = getSetterMethod(config, field);
                    if (setter != null) {
                        Object convertedValue = convertValueToFieldType(systemValue, field.getType());
                        setter.invoke(config, convertedValue);
                    }
                } catch (Exception e) {
                    logger.warn("设置属性 {} 失败: {}", field.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 获取属性的setter方法
     */
    private static Method getSetterMethod(Object target, Field field) {
        String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        try {
            return target.getClass().getDeclaredMethod(setterName, field.getType());
        } catch (NoSuchMethodException e) {
            logger.warn("属性 {} 没有对应的setter方法", field.getName());
            return null;
        }
    }

    /**
     * 蛇形命名转换为短横线命名（camelCase to kebab-case）
     */
    private static String toKebabCase(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (result.length() > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 将字符串值转换为字段类型
     */
    private static Object convertValueToFieldType(String value, Class<?> fieldType) {
        if (fieldType == String.class) {
            return value;
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return Integer.parseInt(value);
        } else if (fieldType == Long.class || fieldType == long.class) {
            return Long.parseLong(value);
        } else if (fieldType == Double.class || fieldType == double.class) {
            return Double.parseDouble(value);
        } else if (fieldType == Float.class || fieldType == float.class) {
            return Float.parseFloat(value);
        } else if (List.class.isAssignableFrom(fieldType)) {
            // 处理 List 类型，将逗号分隔的字符串转换为 List<String>
            List<String> list = new ArrayList<>();
            String[] split = value.split(",");
            for (String item : split) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
            return list;
        }
        return null;
    }

    /**
     * 获取系统默认配置实例
     */
    public static ZnlpSystemConfig getInstance() {
        return instance;
    }

    /**
     * 重置配置（主要用于测试）
     */
    public static synchronized void reset() {
        ZnlpSystemConfig config = ConfigLoader.load(new ZnlpSystemConfig());
        overrideWithSystemProperties(config);
        instance = config;
    }
}