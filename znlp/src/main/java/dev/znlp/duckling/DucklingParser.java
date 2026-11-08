package dev.znlp.duckling;

import dev.znlp.duckling.grammar.Grammars;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.rule.RuleSet;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.FinalNodeValue;
import dev.znlp.duckling.support.NodeValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Duckling 解析器：先图解析生成候选，再消歧并按参考时间落地。
 * <p>
 * 全量单例 {@link #getInstance()} 使用 {@link Grammars#rules()}。
 * 只跑部分语法时传入自定义 {@link RuleSet}。
 */
public class DucklingParser {

    private static final DucklingParser INSTANCE = new DucklingParser(new DefaultRuleSet(Grammars.rules()));

    private final RuleSet ruleSet;
    private final CandidateSelector selector;

    public DucklingParser(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.selector = new CandidateSelector();
    }

    /**
     * 返回全局静态单例实例
     * @return DucklingParser 单例
     */
    public static DucklingParser getInstance() {
        return INSTANCE;
    }

    /**
     * 只生成图上的全部节点，不做消歧。用于调试节点膨胀或训练特征。
     */
    public List<DkNode<NodeValue>> generateAll(String text) {
        return ruleSet.execute(text);
    }

    /**
     * 解析并消歧，参考时间取当前时刻。
     */
    public List<Dimension> parse(String text) {
        return parse(text, LocalDateTime.now());
    }

    /**
     * 解析并消歧。{@code clock} 用于「明天」「下周日」等相对时间。
     */
    public List<Dimension> parse(String text, LocalDateTime clock) {
        List<DkNode<NodeValue>> nodes = ruleSet.execute(text);
        List<ParserMatch> matches = selector.select(nodes);
        
        return matches.stream()
                .map(m -> {
                    Dimension dim;
                    NodeValue value = m.getValue();
                    if (value instanceof FinalNodeValue) {
                        dim = ((FinalNodeValue) value).output(clock);
                    } else if (value instanceof Dimension) {
                        dim = (Dimension) value;
                    } else {
                        // Should not happen if selector filters correctly
                        return null; 
                    }
                    
                    if (dim != null) {
                        dim.with(
                            text.substring(m.getPosition().start(), m.getPosition().end() + 1),
                            m.getPosition().start(),
                            m.getPosition().end()
                        );
                    }
                    return dim;
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());
    }
}
