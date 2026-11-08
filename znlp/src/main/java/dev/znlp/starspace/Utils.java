package dev.znlp.starspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

/**
 * 工具类，包含文件IO、文本归一化和数据结构等辅助方法
 * <p>
 * 该类提供了 StarSpace 模型所需的各种工具方法，包括：
 * 1. 文件操作方法
 * 2. 文本归一化方法
 * 3. 数据结构（Pair, XPair）
 */
public class Utils {

    /**
     * 读取文件的所有行
     * @param file 文件对象
     * @return 字符串流
     * @throws IOException IO异常
     */
    public static Stream<String> lines(File file) throws IOException {
        return Files.lines(file.toPath());
    }

    /**
     * 读取文件的第一行
     * @param file 文件对象
     * @return 文件的第一行，若文件不存在或为空则返回 null
     */
    public static String firstLine(File file) {
        try (Stream<String> stream = Files.lines(file.toPath())) {
            return stream.findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 检查参数是否合法
     * @param expression 表达式，若为 false 则抛出异常
     * @throws IllegalArgumentException 当表达式为 false 时抛出
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * 文本归一化类，用于处理文本的大小写和数字
     */
    public static class NormalizeText {
        /*
         * 我们将较长的字符串分类到以下桶中：
         *
         * 1. 所有标点和数字。此桶中的内容会将数字扁平化，以防止组合爆炸。
         *    它们可能是特定数字、价格等。
         *
         * 2. 所有字母：大小写扁平化。
         *
         * 3. 混合字母和数字：产品 ID？扁平化大小写并保持数字不变。
         *
         * 大小写归一化是由状态机驱动的。
         */

        /**
         * 归一化文本
         * @param str 原始文本
         * @return 归一化后的文本
         */
        public static String normalize(String str) {

            boolean allNumeric = true;
            boolean containsDigits = false;

            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);

                containsDigits = containsDigits || Character.isDigit(c);

                if (!Character.isAlphabetic(c)) {
                    allNumeric = false;
                    continue;
                }
                if (!Character.isAlphabetic(c)) continue; // 冗余但保留逻辑
                allNumeric = false;
            }

            boolean flattenCase = true;
            boolean flattenNum = allNumeric && containsDigits;
            if (!flattenNum && !flattenCase) return str;

            char[] chars = str.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (flattenNum && Character.isDigit(c)) {
                    chars[i] = '0';
                }
                if (Character.isAlphabetic(c)) {
                    chars[i] = Character.toLowerCase(c);
                }
            }
            return new String(chars);
        }
    }

    /**
     * 通用键值对类
     * @param <F> 第一个元素类型
     * @param <S> 第二个元素类型
     */
    public static class Pair<F, S> {
        /** 第一个元素 */
        public F first;
        /** 第二个元素 */
        public S second;

        /**
         * 构造 Pair 对象
         * @param first 第一个元素
         * @param second 第二个元素
         */
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        /**
         * 获取第一个元素
         * @return 第一个元素
         */
        public F getFirst() { return first; }
        /**
         * 获取第二个元素
         * @return 第二个元素
         */
        public S getSecond() { return second; }

        /**
         * 转换为字符串表示
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }
    
    /**
     * 整型-浮点型键值对类，用于存储 (索引, 权重) 对
     */
    public static class XPair {
        /** 第一个元素（通常是索引） */
        public int first;
        /** 第二个元素（通常是权重） */
        public float second;

        /**
         * 构造 XPair 对象
         * @param first 第一个元素
         * @param second 第二个元素
         */
        public XPair(int first, float second) {
            this.first = first;
            this.second = second;
        }

        /**
         * 创建 XPair 对象
         * @param first 第一个元素
         * @param second 第二个元素
         * @return XPair 对象
         */
        public static XPair createPair(int first, float second) {
            return new XPair(first, second);
        }

        /**
         * 创建 XPair 对象
         * @param first 第一个元素（长整型）
         * @param second 第二个元素
         * @return XPair 对象
         */
        public static XPair createPair(long first, float second) {
            return new XPair((int) first, second);
        }

        /**
         * 检查是否相等
         * @param o 比较对象
         * @return 是否相等
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XPair xPair = (XPair) o;
            return first == xPair.first && Float.compare(xPair.second, second) == 0;
        }

        /**
         * 计算哈希码
         * @return 哈希码
         */
        @Override
        public int hashCode() {
            int result = first;
            result = 31 * result + (second != +0.0f ? Float.floatToIntBits(second) : 0);
            return result;
        }

        /**
         * 转换为字符串表示
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "XPair(" + first + ", " + second + ")";
        }
    }
}
