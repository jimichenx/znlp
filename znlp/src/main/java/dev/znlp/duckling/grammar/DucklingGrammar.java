package dev.znlp.duckling.grammar;

import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.Rule;

import java.util.Collections;
import java.util.List;

/**
 * 一套维度的规则与回归例子。
 */
public interface DucklingGrammar {
    List<Rule> rules();
    
    default List<Example> examples() {
        return Collections.emptyList();
    }
}
