package dev.znlp.base.common;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加密工具类，提供 MD5 和 SHA 哈希计算功能。
 * <p>
 * 使用 Java 自带的 {@link MessageDigest} 类实现哈希计算。
 * 支持对字符串和输入流进行哈希计算。
 * </p>
 *
 * @author xiaokui
 */
public final class EncryptionUtil {

    /**
     * 用于将字节转换成 16 进制表示的字符数组
     */
    private static final char[] HEX_DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 私有构造函数，防止实例化
     */
    private EncryptionUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 计算字符串的 MD5 哈希值。
     *
     * @param source 需要计算哈希的字符串
     * @return MD5 哈希值的十六进制字符串表示
     * @throws RuntimeException 如果 MD5 算法不可用
     */
    public static String md5(String source) {
        try {
            return getHash(source, "MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 计算输入流的 MD5 哈希值。
     * <p>
     * 注意：此方法会读取整个输入流，读取后流的位置会到达末尾。
     * 如果需要多次使用同一个输入流，请先进行缓冲或复制。
     * </p>
     *
     * @param source 需要计算哈希的输入流
     * @return MD5 哈希值的十六进制字符串表示
     * @throws RuntimeException 如果读取流时发生错误或 MD5 算法不可用
     */
    public static String md5(InputStream source) {
        BufferedInputStream input = new BufferedInputStream(source);

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[8192]; // 使用更大的缓冲区提高性能
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest(); // 完成哈希计算，产生 128 位的摘要

            return byte2String(digest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate MD5 hash", e);
        }
    }

    /**
     * 计算字符串的 SHA-1 哈希值。
     *
     * @param source 需要计算哈希的字符串
     * @return SHA-1 哈希值的十六进制字符串表示
     * @throws RuntimeException 如果 SHA 算法不可用
     */
    public static String sha(String source) {
        try {
            return getHash(source, "SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA algorithm not available", e);
        }
    }

    /**
     * 计算字符串的哈希值。
     * <p>
     * MD5 与 SHA-1 均是从 MD4 发展而来，它们的结构和强度等特性有很多相似之处。
     * SHA-1 与 MD5 的最大区别在于其摘要比 MD5 摘要长 32 比特（1byte=8bit，相当于长4byte，转换16进制后比MD5多8个字符）。
     * </p>
     * <p>
     * 对于强行攻击：
     * <ul>
     *   <li>MD5 是 2^128 数量级的操作</li>
     *   <li>SHA-1 是 2^160 数量级的操作</li>
     * </ul>
     * </p>
     * <p>
     * 对于相同摘要的两个报文的难度：
     * <ul>
     *   <li>MD5 是 2^64 数量级的操作</li>
     *   <li>SHA-1 是 2^80 数量级的操作</li>
     * </ul>
     * </p>
     * <p>
     * 因而，SHA-1 对强行攻击的强度更大。但由于 SHA-1 的循环步骤比 MD5 多（80:64）
     * 且要处理的缓存大（160 比特:128 比特），SHA-1 的运行速度比 MD5 慢。
     * </p>
     *
     * @param source   需要加密的字符串
     * @param hashType 加密类型（MD5 或 SHA）
     * @return 哈希值的十六进制字符串表示
     * @throws NoSuchAlgorithmException 如果指定的哈希算法不可用
     */
    private static String getHash(String source, String hashType) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(hashType);
        md.update(source.getBytes()); // 通过使用 update 方法处理数据，使指定的 byte 数组更新摘要
        byte[] digest = md.digest(); // 完成哈希计算

        return byte2String(digest);
    }

    /**
     * 将字节数组转换为十六进制字符串。
     * <p>
     * 每个字节用 16 进制表示使用两个字符。
     * </p>
     *
     * @param bytes 字节数组
     * @return 十六进制字符串表示
     */
    private static String byte2String(byte[] bytes) {
        char[] str = new char[bytes.length * 2]; // 每个字节用 16 进制表示的话，使用两个字符
        int k = 0; // 表示转换结果中对应的字符位置

        for (byte b : bytes) {
            str[k++] = HEX_DIGITS[(b >>> 4) & 0xf]; // 取字节中高 4 位的数字转换，>>> 为逻辑右移，将符号位一起右移
            str[k++] = HEX_DIGITS[b & 0xf]; // 取字节中低 4 位的数字转换
        }

        return new String(str);
    }
}
