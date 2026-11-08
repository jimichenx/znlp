package dev.znlp.duckling.grammar.number;

import dev.znlp.duckling.support.NodeValue;

/** 量级后缀：十/百/千/万/亿以及 K/M/G。 */
public record NumSuffixDef(String name, int grain, long value) implements NodeValue {
}
