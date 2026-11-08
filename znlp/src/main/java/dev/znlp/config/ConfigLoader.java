package dev.znlp.config;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 配置加载工具类，负责从配置文件读取配置
 * 使用反射自动化读取和设置属性
 */
public final class ConfigLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConfigLoader.class);

    private static final String CONFIG_FILE_NAME = "znlp.properties";

    private ConfigLoader() {
        // 工具类，不允许实例化
    }

    /**
     * 从配置文件加载配置到目标对象
     */
    public static <T> T load(T target) {
        return load(target, CONFIG_FILE_NAME);
    }

    /**
     * 从指定配置文件加载配置到目标对象（用于测试）
     */
    public static <T> T load(T target, String fileName) {
        Properties properties = new Properties();
        loadProperties(properties, fileName);
        setPropertiesToObject(properties, target);
        return target;
    }

    /**
     * 加载classpath中的配置文件
     */
    private static void loadProperties(Properties properties, String fileName) {
        ClassLoader classLoader = ConfigLoader.class.getClassLoader();
        if (tryLoad(properties, classLoader, fileName)) {
            return;
        }
        logger.warn("配置文件 {} 未在classpath中找到，将使用默认配置", fileName);
    }

    private static boolean tryLoad(Properties properties, ClassLoader classLoader, String fileName) {
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                return false;
            }
            properties.load(inputStream);
            return true;
        } catch (IOException e) {
            logger.warn("加载配置文件 {} 失败: {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * 将properties值设置到目标对象的属性上
     */
    private static void setPropertiesToObject(Properties properties, Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            String propertyName = toKebabCase(field.getName());
            String propertyValue = properties.getProperty(propertyName);

            // 对于 dataDirs 字段，如果 data-dirs 属性不存在，尝试使用 data-dir 属性（保持向后兼容性）
            if (field.getName().equals("dataDirs") && propertyValue == null) {
                propertyValue = properties.getProperty("data-dir");
            }

            if (propertyValue != null) {
                // 处理空字符串属性值，设置为 null
                if (propertyValue.trim().isEmpty()) {
                    propertyValue = null;
                }

                try {
                    Method setter = getSetterMethod(target, field);
                    if (setter != null) {
                        Object convertedValue = convertValueToFieldType(propertyValue, field.getType());
                        setter.invoke(target, convertedValue);
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
        // 如果值为 null，直接返回 null（仅适用于包装类类型）
        if (value == null) {
            return isPrimitiveType(fieldType) ? getDefaultPrimitiveValue(fieldType) : null;
        }

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
     * 检查是否是基本类型
     */
    private static boolean isPrimitiveType(Class<?> fieldType) {
        return fieldType.isPrimitive();
    }

    /**
     * 获取基本类型的默认值
     */
    private static Object getDefaultPrimitiveValue(Class<?> fieldType) {
        if (fieldType == boolean.class) {
            return false;
        } else if (fieldType == char.class) {
            return '\0';
        } else if (fieldType == byte.class) {
            return (byte) 0;
        } else if (fieldType == short.class) {
            return (short) 0;
        } else if (fieldType == int.class) {
            return 0;
        } else if (fieldType == long.class) {
            return 0L;
        } else if (fieldType == float.class) {
            return 0.0f;
        } else if (fieldType == double.class) {
            return 0.0d;
        }
        return null;
    }
}