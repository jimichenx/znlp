package dev.znlp.fasttext.utils;

import java.nio.charset.StandardCharsets;

/**
 * 哈希工具类，提供与 Facebook FastText 一致的 FNV-1a 实现。
 * 按 UTF-8 字节计算，并将每个字节按有符号 {@code int8_t} 参与异或，
 * 以便与官方 {@code .bin} 模型对齐。
 */
public class HashUtils {

    /**
     * 对字符串的 UTF-8 字节应用 FNV-1a。
     *
     * @param str 要哈希的字符串
     * @return 32 位哈希值（Java int，与 C++ uint32 同比特）
     */
    public static int fnv1aHash(String str) {
        return fnv1aHash(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 对整个字节数组应用 FNV-1a。
     *
     * @param bytes 要哈希的字节
     * @return 32 位哈希值
     */
    public static int fnv1aHash(byte[] bytes) {
        return fnv1aHash(bytes, 0, bytes.length);
    }

    /**
     * 对字节数组切片应用 FNV-1a。
     * 与 C++ {@code h ^ uint32_t(int8_t(str[i]))} 一致：Java {@code byte} 异或时符号扩展。
     *
     * @param bytes 字节数组
     * @param off   起始偏移
     * @param len   长度
     * @return 32 位哈希值
     */
    public static int fnv1aHash(byte[] bytes, int off, int len) {
        int h = 0x811c9dc5;
        int end = off + len;
        for (int i = off; i < end; i++) {
            h ^= bytes[i];
            h *= 16777619;
        }
        return h;
    }
}
