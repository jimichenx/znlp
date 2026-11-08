package dev.znlp.segment.impl.correction;

import dev.znlp.base.dat.DoubleArrayTrieMap;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.SpanConstraint;
import dev.znlp.segment.pipeline.StagePriority;
import dev.znlp.segment.pipeline.WordpathProcessor;

/**
 * 自定分词纠错。
 * 放在最后一步
 * 纠错的逻辑仅限：
 * 1.一个词拆为多个词
 * 2.多词组合和拆分 AB CDE FG => ABC DE FG
 * <p>
 * 一定不能把已经成词的边界破坏掉
 * Created by jimichan on 2017/7/3.
 *
 * @author jimichan
 */
public class CorrectionWordpathProcessor implements WordpathProcessor, SpanConstraint {

    private static final int LEVEL3 = 300;  // LEVEL3 的order值

    private final CorrectionDictionary dictionary;

    // PipelineComponent 属性
    private final String name = "CorrectionWordpathProcessor";
    private boolean enabled = true;
    private int order = LEVEL3;

    public CorrectionWordpathProcessor(CorrectionDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public Wordpath process(Wordpath wordPath) {

        DoubleArrayTrieMap<CorrectionWord> dat = dictionary.getTrie();

        if (dat == null) {
            return wordPath;
        }

        Wordnet wordnet = wordPath.getWordnet();
        int origin = wordnet.origin();
        DoubleArrayTrieMap<CorrectionWord>.MapMatcher datSearch = dat.match(wordnet.getCharArray(), origin, wordnet.limit());

        while (datSearch.next()) {
            int offset = datSearch.getBegin() - origin;
            int matchLength = datSearch.getLength();

            if (!wordPath.canForce(offset, matchLength, priority())) {
                continue;
            }

            CorrectionWord aw = datSearch.getValue();

            int cursor = offset;
            for (int len : aw.words()) {
                wordPath.combine(cursor, len);
                cursor += len;
            }
            wordPath.markForced(offset, matchLength, priority());
        }

        return wordPath;
    }

    @Override
    public int priority() {
        return StagePriority.CORRECTION;
    }

    @Override
    public void apply(Wordpath path) {
        process(path);
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
}
