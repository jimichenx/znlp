package dev.znlp.converter;

/**
 * 繁简体转换词典接口
 * <p>
 * 该接口提供繁简体转换。实现按词典做最长匹配后替换。
 * </p>
 *
 * @author jimichan
 */
public interface TransformDictionary {

    /**
     * 对输入文本进行繁简体转换
     *
     * @param text 待转换的文本字符串
     * @return 转换后的文本字符串
     */
    String transform(String text);

    /**
     * 对输入字符数组进行繁简体转换
     *
     * @param charArray 待转换的字符数组
     * @return 转换后的文本字符串
     */
    String transform(char[] charArray);
}
