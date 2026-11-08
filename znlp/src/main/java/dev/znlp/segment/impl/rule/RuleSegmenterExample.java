package dev.znlp.segment.impl.rule;

import dev.znlp.segment.Nature;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;

/**
 * 规则分词使用示例。规则作为候选源叠在词典配方上，靠高词频进入 Viterbi。
 */
public class RuleSegmenterExample {

    public static void main(String[] args) {
        RuleManager ruleManager = new RuleManager();
        ruleManager.addRule(new Rule("订单号\\d+", Nature.n, 100));
        ruleManager.addRule(new Rule("测试", Nature.v, 10));

        WordSegmenter segmenter = SegmentRecipe.dictionary()
                .withRules(ruleManager)
                .build();

        String text = "这是一个测试文本，包含订单号123456。";
        System.out.println("原始文本：" + text);
        System.out.println("分词结果：");
        segmenter.segment(text).forEach(term -> System.out.println(term.word + " / " + term.getNature()));
    }
}
