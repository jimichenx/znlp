package dev.znlp.base.resources;

import dev.znlp.base.common.EncryptionUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * NLP 资源接口，用于读取模型和字典等资源文件。
 * <p>
 * 该接口定义了从不同来源（文件系统、类路径、JAR 文件等）读取 NLP 资源的标准方式。
 * 资源通常是基于文本的，一般一行一个数据。
 * </p>
 * <p>
 * 该接口可以与外部系统对接，比如数据库、HDFS 等。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * NlpResource resource = factory.load("dict/abc.dict");
 * if (resource != null) {
 *     try (InputStream is = resource.inputStream()) {
 *         // 读取资源内容
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public interface NlpResource {

    /**
     * 获取资源的输入流。
     * <p>
     * 调用者负责关闭返回的输入流。建议使用 try-with-resources 语句。
     * </p>
     *
     * @return 资源的输入流
     * @throws IOException 如果无法打开资源或读取时发生错误
     */
    InputStream inputStream() throws IOException;

    /**
     * 计算资源的哈希值。
     * <p>
     * 默认实现使用 MD5 算法计算整个资源内容的哈希值。
     * 有多种实现方式：
     * <ul>
     *   <li>对文件或数据进行计算</li>
     *   <li>使用同名文件，如 abc.txt 对应一个 abc.txt.hash 文件进行记录</li>
     * </ul>
     * </p>
     * <p>
     * 注意：此方法会读取整个资源内容，对于大文件可能比较耗时。
     * 子类可以重写此方法以提供更高效的实现（例如使用文件的修改时间或 CRC 值）。
     * </p>
     *
     * @return 资源的 MD5 哈希值（十六进制字符串）
     * @throws RuntimeException 如果计算哈希时发生错误
     */
    default String hash() {
        try (InputStream inputStream = inputStream()) {
            return EncryptionUtil.md5(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate resource hash", e);
        }
    }
}
