package dev.znlp.segment;

import dev.znlp.SystemModule;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.segment.impl.atom.AtomSplitAlgorithm;
import dev.znlp.segment.impl.atom.AtomSplitAlgorithm.AtomSplitAlgorithmFactory;
import dev.znlp.segment.impl.cws.CwsPerceptron;
import dev.znlp.segment.impl.cws.CwsPerceptronFactory;
import dev.znlp.segment.impl.cws.CwsSplitAlgorithm;
import dev.znlp.segment.impl.dict.CoreDictionary;
import dev.znlp.segment.impl.dict.CoreDictionaryFactory;
import dev.znlp.segment.impl.dict.CoreDictionarySplitAlgorithm;
import dev.znlp.segment.impl.bigram.BiGramTableDictionary;
import dev.znlp.segment.impl.bigram.BiGramTableDictionaryFactory;
import dev.znlp.segment.impl.ner.NERPerceptron;
import dev.znlp.segment.impl.ner.NERPerceptronFactory;
import dev.znlp.segment.impl.pos.PosPerceptron;
import dev.znlp.segment.impl.pos.PosPerceptronFactory;
import dev.znlp.segment.impl.userdict.ResourceUserDictionary;
import dev.znlp.segment.impl.personname.PersonNamePerceptron;
import dev.znlp.segment.impl.personname.PersonNamePerceptronFactory;


/**
 * 字典模块，负责注册字典相关的组件。
 * <p>
 * 该模块注册以下服务：
 * <ul>
 *   <li>{@link CoreDictionary} - 核心词典，通过工厂类创建（单例模式）</li>
 * </ul>
 * <p>
 * 该模块依赖于 {@link NlpResourceLoader}，因此优先级设置为 50，
 * 确保在核心模块（ResourceLoader）之后加载。
 *
 * @author jimichan
 * @see SystemModule
 */
public class SegmentModule implements SystemModule {

    @Override
    public String getName() {
        return "segment";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public void register(ServiceRegistry registry, ZnlpSystemConfig config) {
        registry.registerSingletonClass(CoreDictionarySplitAlgorithm.class);
        registry.registerFactory(CoreDictionary.class, CoreDictionaryFactory.class, true);

        registry.registerFactory(BiGramTableDictionary.class, BiGramTableDictionaryFactory.class, true);
        registry.registerFactory(PersonNamePerceptron.class, PersonNamePerceptronFactory.class, true);
        registry.registerFactory(CwsPerceptron.class, CwsPerceptronFactory.class, true);
        registry.registerFactory(PosPerceptron.class, PosPerceptronFactory.class, true);
        registry.registerFactory(NERPerceptron.class, NERPerceptronFactory.class, true);
        registry.registerSingletonClass(CwsSplitAlgorithm.class);
        registry.registerSingletonClass(ResourceUserDictionary.class);

        registry.registerFactory(AtomSplitAlgorithm.class, AtomSplitAlgorithmFactory.class, true);
    }

}