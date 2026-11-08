package dev.znlp.converter;

/**
 * 繁简体转换服务接口
 * <p>
 * 该接口提供了简洁易用的繁简体转换功能，支持：
 * <ul>
 *   <li>简体中文到繁体中文的转换 (s2t)</li>
 *   <li>繁体中文到简体中文的转换 (t2s)</li>
 * </ul>
 * </p>
 * <p>
 * 该服务基于Aho-Corasick自动机实现，能够高效地处理文本中的繁简体转换，
 * 支持长词优先匹配策略，确保转换结果的正确性。
 * </p>
 *
 * @author jimichan
 */
public interface TransformService {

    /**
     * 将简体中文转换为繁体中文
     *
     * @param text 待转换的简体中文文本
     * @return 转换后的繁体中文文本
     */
    String s2t(String text);

    /**
     * 将繁体中文转换为简体中文
     *
     * @param text 待转换的繁体中文文本
     * @return 转换后的简体中文文本
     */
    String t2s(String text);

    /**
     * 将简体中文转换为繁体中文（字符数组版本）
     *
     * @param charArray 待转换的简体中文字符数组
     * @return 转换后的繁体中文文本
     */
    String s2t(char[] charArray);

    /**
     * 将繁体中文转换为简体中文（字符数组版本）
     *
     * @param charArray 待转换的繁体中文字符数组
     * @return 转换后的简体中文文本
     */
    String t2s(char[] charArray);
}
