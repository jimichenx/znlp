package dev.znlp.duckling.ml;

import java.util.List;

/** 分类器输入：id 为规则名，features 为子节点规则名。 */
public record FeatureInput(String classifierId, List<String> features) {
}
