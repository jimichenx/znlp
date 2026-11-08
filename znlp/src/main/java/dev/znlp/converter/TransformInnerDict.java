package dev.znlp.converter;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;

/**
 * 繁简体转换内置词典
 * <p>
 * 持有简体转繁体(s2t)和繁体转简体(t2s)的转换词典实例。
 * </p>
 *
 * @author jimichan
 */
public class TransformInnerDict {

    private final TransformDictionary s2tDictionary;
    private final TransformDictionary t2sDictionary;

    TransformInnerDict(TransformDictionary s2tDictionary, TransformDictionary t2sDictionary) {
        this.s2tDictionary = s2tDictionary;
        this.t2sDictionary = t2sDictionary;
    }

    /**
     * 创建繁简体转换内置词典实例
     *
     * @param config 系统配置
     * @param loader 资源加载器
     * @return 转换词典实例
     */
    public static TransformInnerDict create(ZnlpSystemConfig config, NlpResourceLoader loader) {
        TransformInnerDictLoader dictLoader = new TransformInnerDictLoader(config, loader);
        return dictLoader.load();
    }

    public TransformDictionary getS2tDictionary() {
        return s2tDictionary;
    }

    public TransformDictionary getT2sDictionary() {
        return t2sDictionary;
    }
}
