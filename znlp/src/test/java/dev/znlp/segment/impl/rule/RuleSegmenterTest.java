package dev.znlp.segment.impl.rule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;

public class RuleSegmenterTest {

    @Test
    public void testRuleSegmenter() {
        RuleManager ruleManager = new RuleManager();
        ruleManager.addRule(new Rule("订单\\d+", Nature.n, 100));
        ruleManager.addRule(new Rule("特殊词", Nature.Ag, 100));

        WordSegmenter segmenter = SegmentRecipe.dictionary().withRules(ruleManager).build();

        String text = "请处理订单123456和特殊词";
        List<Token> tokens = segmenter.segment(text);

        boolean foundOrder = false;
        boolean foundSpecial = false;

        for (Token token : tokens) {
            System.out.println(token.word + "/" + token.getNature());
            if ("订单123456".equals(token.word) && Nature.n == token.getNature()) {
                foundOrder = true;
            }
            if ("特殊词".equals(token.word) && Nature.Ag == token.getNature()) {
                foundSpecial = true;
            }
        }

        Assertions.assertTrue(foundOrder, "Should contain 订单123456/n");
        Assertions.assertTrue(foundSpecial, "Should contain 特殊词/Ag");
    }
}
