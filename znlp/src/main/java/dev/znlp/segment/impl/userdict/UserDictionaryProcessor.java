package dev.znlp.segment.impl.userdict;

import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.SpanConstraint;
import dev.znlp.segment.pipeline.StagePriority;
import dev.znlp.segment.pipeline.WordpathProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义词典的合并处理器.
 * <p>
 * 小词合并为大词
 * 但是不去解决  AAA BBB CCC 有一个自定义词汇 ABBBC 这个时候不能去拆分，变更原有路径
 * 只能解决 A BB C 然后有自定义词 ABBC 那可以把他们联合起来
 *
 * @author jimichan
 */
public class UserDictionaryProcessor implements WordpathProcessor, SpanConstraint {

    private static final int LEVEL2 = 200;  // LEVEL2 的order值

    private List<UserDictionary> dictionaries;

    // PipelineComponent 属性
    private final String name = "CustomDictionaryProcessor";
    private boolean enabled = true;
    private int order = LEVEL2;

    public UserDictionaryProcessor(List<UserDictionary> dictionaries) {
        this.dictionaries = dictionaries != null ? dictionaries : new ArrayList<>();
    }

    @Override
    public Wordpath process(Wordpath wordPath) {
        if (dictionaries == null || dictionaries.isEmpty()) {
            return wordPath;
        }

        Wordnet wordnet = wordPath.getWordnet();
        char[] text = wordnet.getCharArray();
        int origin = wordnet.origin();

        // 遍历所有自定义词典
        for (UserDictionary dictionary : dictionaries) {
            if (dictionary == null) {
                continue;
            }

            StringIntDATMap dat = dictionary.getTrie();
            if (dat == null) {
                continue;
            }

            StringIntDATMap.DATMapMatcherInt datSearch = dat.match(text, origin, wordnet.limit());

            while (datSearch.next()) {
                int offset = datSearch.getBegin() - origin;
                int length = datSearch.getLength();

                if (wordPath.canForce(offset, length, priority())) {
                    wordPath.force(offset, length, priority());
                }
            }
        }

        return wordPath;
    }

    @Override
    public int priority() {
        return StagePriority.USER_DICTIONARY;
    }

    @Override
    public void apply(Wordpath path) {
        process(path);
    }

    public List<UserDictionary> getDictionaries() {
        return dictionaries;
    }

    public UserDictionaryProcessor setDictionaries(List<UserDictionary> dictionaries) {
        this.dictionaries = dictionaries;
        return this;
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
