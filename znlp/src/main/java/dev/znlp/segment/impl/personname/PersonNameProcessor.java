package dev.znlp.segment.impl.personname;

import dev.znlp.segment.Nature;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.SpanConstraint;
import dev.znlp.segment.pipeline.StagePriority;
import dev.znlp.segment.pipeline.WordpathProcessor;

import java.util.List;

/**
 * 人名识别处理器。
 * <p>
 * 在词路径处理阶段，对于长度>3的人名，如果没有破坏其他词汇的切分，
 * 那么合并为人名。
 * <p>
 * 修复了这种类型的bug：
 * <ul>
 *   <li>阿里/nr 云/u 仓库/n 地址/n 正确/a - 以前会把"阿里云仓"认为是人名</li>
 *   <li>陈宝 奇怪 别人 - 人名又会被忽略的问题</li>
 * </ul>
 * <p>
 * 处理逻辑：
 * <ul>
 *   <li>从词图上下文中获取已识别的人名列表</li>
 *   <li>对于长度>3的人名，检查是否会破坏现有分词</li>
 *   <li>如果不会破坏，则合并为人名并设置词性</li>
 * </ul>
 *
 * @author jimichan
 */
public class PersonNameProcessor extends BaseSegmentComponent implements WordpathProcessor, SpanConstraint {

    private final PersonNamePerceptron perceptron;

    /**
     * 构造函数
     *
     * @param perceptron 人名识别感知机，通过ServiceLocator注入
     */
    public PersonNameProcessor(PersonNamePerceptron perceptron) {
        super(LEVEL2);
        this.perceptron = perceptron;
    }

    @Override
    public Wordpath process(Wordpath wordPath) {
        Wordnet wordnet = wordPath.getWordnet();

        // 从词图上下文中获取已识别的人名列表
        @SuppressWarnings("unchecked")
        List<PersonName> names = wordnet.get(PersonNameAlgorithm.CONTEXT_KEY);

        // 如果上下文中没有，则重新识别
        if (names == null) {
            names = perceptron.findPersonName(wordnet.getCharArray(), wordnet.origin(), wordnet.length());
        }

        for (PersonName name : names) {
            int length = name.length();
            // 三字及以内在铺候选阶段进词图，靠 Viterbi 竞争，这里不强制。
            if (length <= 3) {
                continue;
            }
            int offset = name.offset();
            if (!wordPath.canForce(offset, length, priority())) {
                continue;
            }
            Vertex merged = wordPath.force(offset, length, priority());
            if (merged != null) {
                merged.nature = Nature.nr;
            }
        }

        return wordPath;
    }

    @Override
    public int priority() {
        return StagePriority.PERSON_NAME;
    }

    @Override
    public void apply(Wordpath path) {
        process(path);
    }
}
