package dev.znlp.base.common;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 字符规范化工具类
 * <p>
 * 提供字符标准化转换功能，用于将异体字、全角字符等转换为规范形式。
 * 转换规则从资源文件 {@code char_norm} 中加载，格式为 "源字符=目标字符"。
 * </p>
 * <p>
 * 性能特点：
 * <ul>
 *   <li>使用查找表(lookup table)实现O(1)时间复杂度的字符转换</li>
 *   <li>静态初始化，避免运行时加载开销</li>
 *   <li>字符串转换时采用延迟复制策略，无需转换时直接返回原字符串</li>
 * </ul>
 * </p>
 * <p>
 * 线程安全：所有方法都是线程安全的，查找表在类加载时初始化完成后为只读状态。
 * </p>
 *
 * @author jimichan
 */
public final class CharNormUtils {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CharNormUtils.class);

    /**
     * 字符转换查找表，索引为源字符的Unicode码点，值为目标字符。
     * 值为0表示该字符无需转换。
     * 使用char数组覆盖BMP(Basic Multilingual Plane)范围的字符。
     */
    private static final char[] CONVERSION_TABLE = new char[65536];

    static {
        try {
            loadConversionRules();
        } catch (Exception e) {
            // 记录错误但不影响类加载，未配置的字符将不进行转换
            logger.error("Failed to load character normalization rules", e);
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private CharNormUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 从资源文件加载字符转换规则
     */
    private static void loadConversionRules() throws IOException {
        List<String> lines = readResourceLines(CharNormUtils.class, "char_norm");

        for (String line : lines) {
            // 跳过空行和注释行
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // 解析转换规则：格式为 "源字符=目标字符"
            int equalIndex = line.indexOf('=');
            if (equalIndex != 1 || line.length() != 3) {
                logger.warn("Invalid normalization rule format (expected 'A=B'): {}", line);
                continue;
            }

            char sourceChar = line.charAt(0);
            char targetChar = line.charAt(2);
            CONVERSION_TABLE[sourceChar] = targetChar;
        }
    }

    /**
     * 从类路径读取资源文件的所有行
     *
     * @param clazz        用于定位资源的类
     * @param resourceName 资源文件名
     * @return 文件内容的行列表
     * @throws IOException 如果资源未找到或读取失败
     */
    private static List<String> readResourceLines(Class<?> clazz, String resourceName) throws IOException {
        InputStream is = clazz.getResourceAsStream(resourceName);
        if (is == null) {
            // 尝试从类路径根目录加载
            is = clazz.getClassLoader().getResourceAsStream(resourceName);
        }
        if (is == null) {
            // 资源未找到，返回空列表而不是抛出异常，使类加载更健壮
            return new ArrayList<>();
        }

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * 转换单个字符为其规范形式
     *
     * @param input 待转换的字符
     * @return 转换后的字符，如果无需转换则返回原字符
     */
    public static char convert(char input) {
        char converted = CONVERSION_TABLE[input];
        return converted != 0 ? converted : input;
    }

    /**
     * 原地转换字符数组中的所有字符
     * <p>
     * 注意：此方法会修改传入的数组。
     * </p>
     *
     * @param chars 待转换的字符数组，不能为null
     * @throws NullPointerException 如果chars为null
     */
    public static void convert(char[] chars) {
        if (chars == null) {
            throw new NullPointerException("Input char array cannot be null");
        }

        for (int i = 0; i < chars.length; i++) {
            char converted = CONVERSION_TABLE[chars[i]];
            if (converted != 0) {
                chars[i] = converted;
            }
        }
    }

    /**
     * 转换字符串中的所有字符为其规范形式
     * <p>
     * 性能优化：采用延迟复制策略，只有当字符串中确实存在需要转换的字符时，
     * 才会创建新的字符数组。如果字符串无需转换，直接返回原字符串引用。
     * </p>
     *
     * @param input 待转换的字符串，不能为null
     * @return 转换后的字符串，如果无需转换则返回原字符串
     * @throws NullPointerException 如果input为null
     */
    public static String convert(String input) {
        if (input == null) {
            throw new NullPointerException("Input string cannot be null");
        }

        if (input.isEmpty()) {
            return input;
        }

        final int len = input.length();

        // 第一遍扫描：查找第一个真正需要转换的字符位置（转换后字符和原字符不同）
        int firstConversionIndex = -1;
        for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            char converted = CONVERSION_TABLE[ch];
            if (converted != 0 && converted != ch) {
                firstConversionIndex = i;
                break;
            }
        }

        // 如果没有字符需要转换，直接返回原字符串（避免不必要的对象创建）
        if (firstConversionIndex == -1) {
            return input;
        }

        // 延迟复制：只有在确实需要转换时才创建新数组
        char[] chars = input.toCharArray();

        // 从第一个需要转换的位置开始处理
        for (int i = firstConversionIndex; i < len; i++) {
            char converted = CONVERSION_TABLE[chars[i]];
            if (converted != 0 && converted != chars[i]) {
                chars[i] = converted;
            }
        }

        // 如果转换后字符串内容没有变化，返回原字符串引用（幂等性）
        String result = new String(chars);
        if (input.equals(result)) {
            return input;
        }

        return result;
    }

}
