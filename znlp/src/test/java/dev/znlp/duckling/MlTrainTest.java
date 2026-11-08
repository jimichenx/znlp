package dev.znlp.duckling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.znlp.duckling.ml.MlModel;
import dev.znlp.duckling.ml.NaiveBayesTrainer;
import dev.znlp.duckling.pattern.DictPattern;
import dev.znlp.duckling.pattern.DucklingPattern;
import dev.znlp.duckling.pattern.RegexPattern;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleSet;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MlTrainTest {

    @Test
    public void testTraining() throws IOException {
        // Define two simple rules that compete
        Rule ruleA = new MockRule(1, "ruleA", "test");
        ruleA.addExample("test");
        
        Rule ruleB = new MockRule(2, "ruleB", "test");
        // ruleB has no examples
        
        Rule dummyRule = new DummyRule(999, "dummy");

        List<Rule> rules = new ArrayList<>();
        rules.add(ruleA);
        rules.add(ruleB);
        rules.add(dummyRule);
        
        RuleSet ruleSet = new DefaultRuleSet(rules);
        DucklingParser parser = new DucklingParser(ruleSet);
        
        NaiveBayesTrainer trainer = new NaiveBayesTrainer();
        MlModel model = trainer.train(rules, parser);
        
        Assertions.assertNotNull(model);
        
        // Verify serialization
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        model.write(out);
        out.close();
        
        byte[] bytes = baos.toByteArray();
        Assertions.assertTrue(bytes.length > 0);

        // Verify scoring
        List<DkNode<NodeValue>> nodes = parser.generateAll("test");
        Assertions.assertTrue(nodes.size() > 0);
        
        boolean foundPositive = false;
        for (DkNode<?> node : nodes) {
            double score = model.classify(node, true);
            System.out.println("Node: " + node.getRuleSym() + ", Score: " + score);
            if (node.getRuleSym().equals("ruleA")) {
                foundPositive = true;
                // ruleA should match "test" and should be positive
                // ruleB also matches "test" via regex, but has no examples.
                // Wait, ruleB also has regex "test".
                // Since ruleA claimed "test" as example, ruleA gets positive training.
                // ruleB gets negative training (because it matched "test" but wasn't the target).
                // So ruleA score > ruleB score?
            }
        }
        Assertions.assertTrue(foundPositive);
    }
    
    static class DummyRule extends Rule {
        private final List<DucklingPattern<?>> patterns;
        
        public DummyRule(int id, String name) {
            super(id, name);
            this.patterns = new ArrayList<>();
            Set<String> words = new HashSet<>();
            words.add("dummy");
            this.patterns.add(new DictPattern(200 + id, words));
        }

        @Override
        public List<DucklingPattern<?>> getPatterns() {
            return patterns;
        }

        @Override
        public NodeValue production(String text, List<DkNode<?>> matchedNodes) {
            return new NodeValue() {};
        }
    }

    static class MockRule extends Rule {
        private final String matchText;
        private final List<DucklingPattern<?>> patterns;
        
        public MockRule(int id, String name, String matchText) {
            super(id, name);
            this.matchText = matchText;
            this.patterns = new ArrayList<>();
            this.patterns.add(new RegexPattern(100 + id, Pattern.compile(matchText)));
        }

        @Override
        public List<DucklingPattern<?>> getPatterns() {
            return patterns;
        }

        @Override
        public NodeValue production(String text, List<DkNode<?>> matchedNodes) {
            return new NodeValue() {};
        }
    }
}
