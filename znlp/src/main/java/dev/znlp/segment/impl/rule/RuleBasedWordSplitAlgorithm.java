package dev.znlp.segment.impl.rule;

import org.jetbrains.annotations.NotNull;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.WordSplitAlgorithm;

import dev.znlp.base.common.ZeroCopyString;

import java.util.regex.Matcher;

/**
 * 基于规则的分词算法
 */
public class RuleBasedWordSplitAlgorithm extends BaseSegmentComponent implements WordSplitAlgorithm {

    private final RuleManager ruleManager;

    public RuleBasedWordSplitAlgorithm(RuleManager ruleManager) {
        // 使用 LEVEL3，确保在核心词典（LEVEL1）之后执行，从而可以覆盖或增强核心词典的结果
        super(BaseSegmentComponent.LEVEL3);
        this.ruleManager = ruleManager;
    }

    @Override
    public void fill(@NotNull Wordnet wordnet) {
        ZeroCopyString text = new ZeroCopyString(wordnet.getCharArray(), wordnet.origin(), wordnet.length());

        for (Rule rule : ruleManager.getRules()) {
            Matcher matcher = rule.getPattern().matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                int length = end - start;

                // 规则匹配到的词，给予极高的词频，以确保在路径选择中胜出
                int priorityFreq = 1000000 + rule.getPriority();

                // 检查是否已存在该位置和长度的节点
                Vertex existing = wordnet.getVertex(start, length);

                if (existing != null) {
                    // 如果存在，增强其词频，并可选地更新词性
                    // 注意：这里直接修改了freq，可能会影响后续基于该节点原始freq的计算（如果还有的话）
                    // 但由于我们的目标是强制选中，所以覆盖是可以的。
                    // 也可以选择取 Math.max(existing.freq, priorityFreq)
                    if (existing.freq < priorityFreq) {
                        existing.freq = priorityFreq;
                    }
                    
                    if (rule.getNature() != null) {
                        existing.nature = rule.getNature();
                    }
                } else {
                    // 如果不存在，创建新节点
                    Vertex newVertex = wordnet.put(start, length);
                    newVertex.freq = priorityFreq;
                    // wordID 默认为 -1，表示非核心词典词汇
                    if (rule.getNature() != null) {
                        newVertex.nature = rule.getNature();
                    }
                }
            }
        }
    }
}
