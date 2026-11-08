package dev.znlp.duckling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.znlp.duckling.grammar.Grammars;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDateTime;
import java.util.List;

public class ComprehensiveTest {

    @Test
    public void testAllExamples() {
        DucklingParser parser = new DucklingParser(new DefaultRuleSet(Grammars.rules()));
        List<Example> examples = Grammars.examples();
        
        // Reference time inferred from DateTimeGrammar examples (e.g. "Today" -> 2019-11-11)
        LocalDateTime refTime = LocalDateTime.of(2019, 11, 11, 0, 0);

        int passed = 0;
        int failed = 0;
        StringBuilder failMsg = new StringBuilder();

        for (Example example : examples) {
            List<Dimension> result = parser.parse(example.text(), refTime);
            boolean match = false;
            for (Dimension d : result) {
                if (example.predicate().test(d)) {
                    match = true;
                    break;
                }
            }
            
            if (match) {
                passed++;
            } else {
                failed++;
                failMsg.append(String.format("Failed: '%s' -> Parsed: %s\n", example.text(), result));
            }
        }

        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.err.println(failMsg);
            Assertions.fail("Some examples failed to parse correctly. See output for details.");
        }
    }
}
