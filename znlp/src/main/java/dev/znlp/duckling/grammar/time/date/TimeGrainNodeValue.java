package dev.znlp.duckling.grammar.time.date;

import dev.znlp.duckling.grammar.time.AllInnerGrain;
import dev.znlp.duckling.support.NodeValue;

/** 粒度词在图上的值，供时长和日期规则过滤。 */
public record TimeGrainNodeValue(AllInnerGrain grain, String text) implements NodeValue {
}
