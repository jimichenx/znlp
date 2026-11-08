package dev.znlp.segment.impl.atom;

import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.service.ServiceFactory;
import dev.znlp.segment.Nature;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.pipeline.SpanConflict;
import dev.znlp.segment.pipeline.WordSplitAlgorithm;


/**
 * 高性能多模式识别分词算法。
 * <p>
 * 基于规则的分词算法，用于识别时间、数字、数量词等特殊词语模式。
 * 通过只扫描文本一遍的方式，识别多种字符串模式，解决了性能难题。
 * </p>
 *
 * @author jimichan
 */
public class AtomSplitAlgorithm implements WordSplitAlgorithm {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AtomSplitAlgorithm.class);

    private final AtomRecognizer recognizer;
    private final SpanConflict conflict;

    private final String name = "AtomSplitAlgorithm";
    private boolean enabled = true;
    private int order = 200;  // LEVEL2 的order值

    private static final DefaultAtomSplitAlgorithmTemplateProvider DEFAULT_TEMPLATE_PROVIDER = new DefaultAtomSplitAlgorithmTemplateProvider();

    public static class AtomSplitAlgorithmFactory implements ServiceFactory<AtomSplitAlgorithm> {
        @Override
        public AtomSplitAlgorithm create() {
            return new AtomSplitAlgorithm();
        }
    }


    /**
     * 使用默认模板构造
     */
    public AtomSplitAlgorithm() {
        this(DEFAULT_TEMPLATE_PROVIDER, SpanConflict.FILL_IF_ABSENT);
    }

    /**
     * 使用指定的模板提供者构造
     *
     * @param templateProvider 模板提供者
     */
    public AtomSplitAlgorithm(AtomSplitAlgorithmTemplateProvider templateProvider) {
        this(templateProvider, SpanConflict.FILL_IF_ABSENT);
    }

    public AtomSplitAlgorithm(AtomSplitAlgorithmTemplateProvider templateProvider, SpanConflict conflict) {
        logger.debug("加载 AtomSplitAlgorithm 模板");
        StringIntDATMap dat = templateProvider.load();
        this.recognizer = new AtomRecognizer(dat);
        this.conflict = conflict == null ? SpanConflict.FILL_IF_ABSENT : conflict;
    }

    /**
     * 使用指定的识别器构造
     * <p>
     * 主要用于测试，允许注入自定义的识别器
     *
     * @param recognizer 识别器
     */
    public AtomSplitAlgorithm(AtomRecognizer recognizer) {
        this(recognizer, SpanConflict.FILL_IF_ABSENT);
    }

    public AtomSplitAlgorithm(AtomRecognizer recognizer, SpanConflict conflict) {
        this.recognizer = recognizer;
        this.conflict = conflict == null ? SpanConflict.FILL_IF_ABSENT : conflict;
    }

    /**
     * 共用识别器，换一种同跨度冲突策略。
     */
    public AtomSplitAlgorithm withConflict(SpanConflict conflict) {
        if (this.conflict == conflict) {
            return this;
        }
        return new AtomSplitAlgorithm(recognizer, conflict);
    }

    @Override
    public void fill(Wordnet wordnet) {
        char[] chars = wordnet.getCharArray();

        recognizer.recognize(chars, wordnet.origin(), wordnet.length(), result -> {
            Nature nature = result.getNature();
            if (nature == null) {
                return;
            }
            int offset = result.getOffset();
            int length = result.getLength();
            Vertex existing = wordnet.getVertex(offset, length);
            if (existing != null && conflict != SpanConflict.REPLACE) {
                if (conflict == SpanConflict.FILL_IF_ABSENT && existing.nature == null) {
                    existing.nature = nature;
                }
                return;
            }
            wordnet.put(offset, length).setAbsWordNatureAndFreq(nature);
        });
    }

    // PipelineComponent 接口实现

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * 原子分割算法模板提供者接口
     */
    public interface AtomSplitAlgorithmTemplateProvider {
        /**
         * 加载模板
         *
         * @return 字符串到类型的映射
         */
        StringIntDATMap load();
    }

    /**
     * 默认模板提供者实现
     */
    public static class DefaultAtomSplitAlgorithmTemplateProvider
            implements AtomSplitAlgorithmTemplateProvider {

        @Override
        public StringIntDATMap load() {
            logger.debug("加载默认 AtomSplitAlgorithm 模板");
            return DefaultTemplate.createDefaultTemplates();
        }
    }
}

