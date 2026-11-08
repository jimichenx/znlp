package dev.znlp.segment.impl.rule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;

public class RuleSegmenterBuilderTest {

    @Test
    public void testBuilderWithCommonRules() {
        RuleManager rules = RuleSegmenterPluginBuilder.create()
                .addCommonRules(CommonRules.URL, CommonRules.MOBILE, CommonRules.EMAIL)
                .addRule("TEST_\\d+", Nature.v, 50)
                .build();

        WordSegmenter segmenter = SegmentRecipe.dictionary().withRules(rules).build();

        String text = "访问https://www.example.com或者联系13800138000，发邮件给test@example.com，处理TEST_001";
        List<Token> tokens = segmenter.segment(text);

        boolean foundUrl = false;
        boolean foundMobile = false;
        boolean foundEmail = false;
        boolean foundCustom = false;

        for (Token token : tokens) {
            System.out.println(token.word + " / " + token.getNature());
            if ("https://www.example.com".equals(token.word) && Nature.nx == token.getNature()) {
                foundUrl = true;
            }
            if ("13800138000".equals(token.word) && Nature.nx == token.getNature()) {
                foundMobile = true;
            }
            if ("test@example.com".equals(token.word) && Nature.nx == token.getNature()) {
                foundEmail = true;
            }
            if ("TEST_001".equals(token.word) && Nature.v == token.getNature()) {
                foundCustom = true;
            }
        }

        Assertions.assertTrue(foundUrl, "Should detect URL");
        Assertions.assertTrue(foundMobile, "Should detect Mobile");
        Assertions.assertTrue(foundEmail, "Should detect Email");
        Assertions.assertTrue(foundCustom, "Should detect Custom Rule");
    }
}
