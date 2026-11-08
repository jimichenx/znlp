package dev.znlp.base.common;

/**
 * 字符工具类，提供全角/半角转换、标点符号判断等字符处理功能。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>全角字符与半角字符的相互转换</li>
 *   <li>标点符号的判断（包括ASCII、全角、中文扩展标点）</li>
 *   <li>字符类型判断（ASCII、数字等）</li>
 *   <li>字符显示工具（Unicode转义）</li>
 * </ul>
 * </p>
 *
 * <p>
 * 字符范围说明：
 * <ul>
 *   <li>全角字符范围：U+FF00 - U+FF5E（半角对应：U+0020 - U+007E）</li>
 *   <li>全角空格：U+3000（12288），半角空格：U+0020（32）</li>
 *   <li>字符差值：全角字符 = 半角字符 + 65248</li>
 * </ul>
 * </p>
 *
 * @author jimichan
 */
public class Characters {

    // ==================== 字符转换常量 ====================

    /**
     * 全角字符与半角字符的差值（65248）
     */
    private static final int FULLWIDTH_HALFWIDTH_OFFSET = 'Ａ' - 'A';

    /**
     * 全角空格字符：U+3000
     */
    private static final char FULLWIDTH_SPACE = (char) 12288;

    /**
     * 半角空格字符：U+0020
     */
    private static final char HALFWIDTH_SPACE = ' ';

    /**
     * 全角字符范围起始：U+FF00
     */
    private static final char FULLWIDTH_START = '\uFF00';

    /**
     * 全角字符范围结束：U+FF5E
     */
    private static final char FULLWIDTH_END = '\uFF5E';

    /**
     * 半角字符范围起始：U+0020
     */
    private static final char HALFWIDTH_START = '\u0020';

    /**
     * 半角字符范围结束：U+007E
     */
    private static final char HALFWIDTH_END = '\u007E';

    // ==================== 全角/半角转换方法 ====================

    /**
     * 将字符串中的全角字符转换为半角字符。
     * <p>
     * 转换规则：
     * <ul>
     *   <li>全角空格（U+3000）→ 半角空格（U+0020）</li>
     *   <li>全角字符（U+FF01-U+FF5E）→ 对应的半角字符（U+0021-U+007E）</li>
     * </ul>
     * </p>
     *
     * <p>
     * 性能优化：采用延迟复制策略，只有发现需要转换的字符时才创建新字符串。
     * </p>
     *
     * @param input 输入字符串
     * @return 转换后的字符串；如果输入为null、空字符串或不包含全角字符，则返回原字符串
     */
    public static String fullWidth2halfWidth(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 延迟复制优化：先查找是否有需要转换的字符
        int firstConversionIndex = findFirstFullWidthChar(input);

        if (firstConversionIndex == -1) {
            // 没有需要转换的字符，返回原字符串
            return input;
        }

        // 发现需要转换的字符，创建字符数组进行转换
        char[] chars = input.toCharArray();

        // 从第一个需要转换的位置开始处理
        for (int i = firstConversionIndex; i < chars.length; i++) {
            chars[i] = fullWidthToHalfWidth(chars[i]);
        }

        // 如果转换后字符串内容没有变化，返回原字符串引用（幂等性）
        String result = new String(chars);
        if (input.equals(result)) {
            return input;
        }

        return result;
    }

    /**
     * 将字符数组中的全角字符原地转换为半角字符。
     * <p>
     * 注意：此方法会直接修改传入的字符数组。
     * </p>
     *
     * @param chars 要转换的字符数组（会被原地修改）
     * @throws NullPointerException 如果chars为null
     */
    public static void fullWidth2halfWidth(char[] chars) {
        if (chars == null) {
            throw new NullPointerException("字符数组不能为null");
        }

        for (int i = 0; i < chars.length; i++) {
            chars[i] = fullWidthToHalfWidth(chars[i]);
        }
    }


    /**
     * 将字符串中的半角字符转换为全角字符。
     * <p>
     * 转换规则：
     * <ul>
     *   <li>半角空格（U+0020）→ 全角空格（U+3000）</li>
     *   <li>半角字符（U+0021-U+007E）→ 对应的全角字符（U+FF01-U+FF5E）</li>
     * </ul>
     * </p>
     *
     * <p>
     * 性能优化：采用延迟复制策略，只有发现需要转换的字符时才创建新字符串。
     * </p>
     *
     * @param input 输入字符串
     * @return 转换后的字符串；如果输入为null、空字符串或不包含半角字符，则返回原字符串
     */
    public static String halfWidth2fullWidth(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 延迟复制优化：先查找是否有需要转换的字符
        int firstConversionIndex = findFirstHalfWidthChar(input);

        if (firstConversionIndex == -1) {
            // 没有需要转换的字符，返回原字符串
            return input;
        }

        // 发现需要转换的字符，创建字符数组进行转换
        char[] chars = input.toCharArray();

        // 从第一个需要转换的位置开始处理
        for (int i = firstConversionIndex; i < chars.length; i++) {
            chars[i] = halfWidthToFullWidth(chars[i]);
        }

        // 如果转换后字符串内容没有变化，返回原字符串引用（幂等性）
        String result = new String(chars);
        if (input.equals(result)) {
            return input;
        }

        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 查找字符串中第一个全角字符的位置。
     *
     * @param str 输入字符串
     * @return 第一个全角字符的索引，如果没有找到则返回-1
     */
    private static int findFirstFullWidthChar(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == FULLWIDTH_SPACE || (FULLWIDTH_START <= ch && ch <= FULLWIDTH_END)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找字符串中第一个半角字符的位置。
     *
     * @param str 输入字符串
     * @return 第一个半角字符的索引，如果没有找到则返回-1
     */
    private static int findFirstHalfWidthChar(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == HALFWIDTH_SPACE || (HALFWIDTH_START <= ch && ch <= HALFWIDTH_END)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 将单个全角字符转换为半角字符。
     *
     * @param ch 输入字符
     * @return 如果是全角字符则返回对应的半角字符，否则返回原字符
     */
    private static char fullWidthToHalfWidth(char ch) {
        if (ch == FULLWIDTH_SPACE) {
            return HALFWIDTH_SPACE;
        }
        if (FULLWIDTH_START < ch && ch <= FULLWIDTH_END) {
            return (char) (ch - FULLWIDTH_HALFWIDTH_OFFSET);
        }
        return ch;
    }

    /**
     * 将单个半角字符转换为全角字符。
     *
     * @param ch 输入字符
     * @return 如果是半角字符则返回对应的全角字符，否则返回原字符
     */
    private static char halfWidthToFullWidth(char ch) {
        if (ch == HALFWIDTH_SPACE) {
            return FULLWIDTH_SPACE;
        }
        // 注意：原代码使用 '\u0022' < ch，这里保持一致
        // '\u0022' 是双引号 "，所以转换范围是 U+0023-U+007E
        if ('\u0022' < ch && ch <= HALFWIDTH_END) {
            return (char) (ch + FULLWIDTH_HALFWIDTH_OFFSET);
        }
        return ch;
    }

    // ==================== 标点符号判断 ====================

    /**
     * 标点符号位图，用于快速判断字符是否为标点符号。
     * 位图索引对应字符的Unicode码点，值为true表示是标点符号。
     */
    private static final boolean[] PUNCTUATION_BITMAP = new boolean[0xFFFF + 1];

    // 静态初始化标点符号位图
    static {
        // ASCII范围（0-127）的标点符号
        for (char c = 0; c <= 47; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        for (char c = 58; c <= 64; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        for (char c = 91; c <= 96; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        for (char c = 123; c <= 127; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }

        // 不间断空格 U+00A0 (160)
        PUNCTUATION_BITMAP[160] = true;

        // 拉丁-1补充区标点：U+00A1-U+00BF
        for (char c = '\u00A1'; c <= '\u00BF'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }

        // 通用标点符号：U+2010-U+2027
        for (char c = '\u2010'; c <= '\u2027'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        // 通用标点符号：U+2030-U+205E
        for (char c = '\u2030'; c <= '\u205E'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        // 补充标点符号：U+2E00-U+2E30
        for (char c = '\u2E00'; c <= '\u2E30'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        // CJK符号和标点：U+3000-U+303F
        for (char c = '\u3000'; c <= '\u303F'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }

        // 竖排标点：U+FE10-U+FE1F
        for (char c = '\uFE10'; c <= '\uFE1F'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }

        // 全角和特殊标点范围：U+FF00-U+FFEF
        // 全角标点：U+FF00-U+FF0F
        for (char c = '\uFF00'; c <= '\uFF0F'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        // 全角标点：U+FF1A-U+FF20
        for (char c = '\uFF1A'; c <= '\uFF20'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        // 全角标点：U+FF3B-U+FF40
        for (char c = '\uFF3B'; c <= '\uFF40'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
        // 全角标点：U+FF5B-U+FFEF
        for (char c = '\uFF5B'; c <= '\uFFEF'; c++) {
            PUNCTUATION_BITMAP[c] = true;
        }
    }

    /**
     * 判断字符是否为标点符号。
     * <p>
     * 包括以下类型的标点符号：
     * <ul>
     *   <li>ASCII标点符号（U+0000-U+007F范围内的非字母数字字符）</li>
     *   <li>拉丁-1补充标点（U+00A1-U+00BF）</li>
     *   <li>通用标点符号（U+2010-U+2027, U+2030-U+205E）</li>
     *   <li>补充标点符号（U+2E00-U+2E30）</li>
     *   <li>CJK符号和标点（U+3000-U+303F）</li>
     *   <li>全角ASCII标点（U+FF00-U+FFEF范围内的标点）</li>
     *   <li>竖排标点（U+FE10-U+FE1F）</li>
     * </ul>
     * </p>
     *
     * @param c 待判断的字符
     * @return 如果是标点符号返回true，否则返回false
     */
    public static boolean isPunctuation(char c) {
        return PUNCTUATION_BITMAP[c];
    }

    // ==================== 字符类型判断 ====================

    /**
     * 判断字符是否为ASCII字符（U+0000-U+007F）。
     *
     * @param c 待判断的字符
     * @return 如果是ASCII字符返回true，否则返回false
     */
    public static boolean isASCII(char c) {
        return c <= 127;
    }

    // ==================== 字符显示工具 ====================

    /**
     * 十六进制字符表
     */
    private static final String HEX_DIGITS = "0123456789ABCDEF";

    /**
     * 将字符转换为Unicode转义序列形式（格式：反斜杠u后跟4位十六进制数）。
     * <p>
     * 例如：字符 'A' 会被转换为 "\u0041"
     * </p>
     *
     * @param c 待转换的字符
     * @return Unicode转义序列字符串
     */
    public static String showCharacter(char c) {
        char[] result = {'\\', 'u', '\0', '\0', '\0', '\0'};

        // 将字符的Unicode值转换为4位十六进制
        for (int i = 0; i < 4; i++) {
            result[5 - i] = HEX_DIGITS.charAt(c & 0xF);
            c >>= 4;
        }

        return String.copyValueOf(result);
    }

    /**
     * 获取字符表示的数字值（十进制）。
     * <p>
     * 支持：
     * <ul>
     *   <li>ASCII数字字符：'0'-'9'</li>
     *   <li>全角数字字符：'０'-'９'</li>
     *   <li>其他Unicode数字字符</li>
     * </ul>
     * </p>
     *
     * @param ch 待转换的字符
     * @return 字符表示的数字值（0-9），如果不是有效数字字符则返回-1
     */
    public static int digit(char ch) {
        return Character.digit(ch, 10);
    }
}
