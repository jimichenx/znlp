package dev.znlp.pinyin;

import dev.znlp.SystemModule;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.pinyin.spliter.PinyinSplitPerceptron;

import java.io.IOException;

/**
 * 拼音模块
 * <p>
 * 负责注册拼音相关服务
 *
 * @author jimichan
 */
public final class PinyinModule implements SystemModule {

    private static final String PY_SPLIT_MODEL_PATH = "znlp/pinyin/pinyin-split.model";

    @Override
    public String getName() {
        return "pinyin";
    }


    @Override
    public void register(ServiceRegistry services, ZnlpSystemConfig config) {

        services.registerFactory(PinyinDict.class, PinyinDictFactory.class, true);

        services.registerFactory(PinyinSplitPerceptron.class, PinyinSplitPerceptronFactory.class,true);

        services.registerSingletonClass(PinyinService.class);
    }

    public static final class PinyinSplitPerceptronFactory implements ServiceFactory<PinyinSplitPerceptron> {
        private final NlpResourceLoader resourceLoader;

        public PinyinSplitPerceptronFactory(NlpResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public PinyinSplitPerceptron create() {
            try {
                return PinyinSplitPerceptron.load(resourceLoader, PY_SPLIT_MODEL_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
