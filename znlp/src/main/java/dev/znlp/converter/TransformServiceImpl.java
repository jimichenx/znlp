package dev.znlp.converter;

/**
 * 繁简体转换服务实现类
 * <p>
 * 该实现类封装了对TransformDictionary的访问，提供了简洁的API用于执行
 * 简体到繁体和繁体到简体的转换。
 * </p>
 * <p>
 * 该服务使用两个独立的词典实例：
 * <ul>
 *   <li>简体转繁体词典</li>
 *   <li>繁体转简体词典</li>
 * </ul>
 * 每个词典都基于Aho-Corasick自动机实现，确保转换过程的高效性。
 * </p>
 *
 * @author jimichan
 */
public class TransformServiceImpl implements TransformService {

    private final TransformDictionary s2tDictionary;
    private final TransformDictionary t2sDictionary;

    /**
     * 构造函数，注入所需的词典实例
     *
     * @param innerDict 内置词典
     */
    public TransformServiceImpl(TransformInnerDict innerDict) {
        this.s2tDictionary = innerDict.getS2tDictionary();
        this.t2sDictionary = innerDict.getT2sDictionary();
    }

    @Override
    public String s2t(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return s2tDictionary.transform(text);
    }

    @Override
    public String t2s(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return t2sDictionary.transform(text);
    }

    @Override
    public String s2t(char[] charArray) {
        if (charArray == null || charArray.length == 0) {
            return "";
        }
        return s2tDictionary.transform(charArray);
    }

    @Override
    public String t2s(char[] charArray) {
        if (charArray == null || charArray.length == 0) {
            return "";
        }
        return t2sDictionary.transform(charArray);
    }
}
