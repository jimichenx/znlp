package dev.znlp.pinyin;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;

public final class PinyinDictFactory implements ServiceFactory<PinyinDict>{
    private final NlpResourceLoader resourceLoader;

    public PinyinDictFactory(NlpResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public PinyinDict create() {
        return PinyinDict.create(resourceLoader);
    }
}
