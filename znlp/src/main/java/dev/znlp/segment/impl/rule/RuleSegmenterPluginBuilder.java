package dev.znlp.segment.impl.rule;

import dev.znlp.segment.Nature;

/**
 * 规则分词插件构建器
 */
public class RuleSegmenterPluginBuilder {

    public static RuleSegmenterPluginBuilder create() {
        return new RuleSegmenterPluginBuilder();
    }

    private final RuleManager ruleManager;

    public RuleSegmenterPluginBuilder() {
        this.ruleManager = new RuleManager();
    }

    /**
     * 添加自定义规则
     *
     * @param regex    正则表达式
     * @param nature   词性
     * @param priority 优先级
     * @return builder
     */
    public RuleSegmenterPluginBuilder addRule(String regex, Nature nature, int priority) {
        ruleManager.addRule(new Rule(regex, nature, priority));
        return this;
    }

    /**
     * 添加规则对象
     *
     * @param rule 规则对象
     * @return builder
     */
    public RuleSegmenterPluginBuilder addRule(Rule rule) {
        ruleManager.addRule(rule);
        return this;
    }

    /**
     * 添加预制规则
     *
     * @param commonRule 预制规则枚举
     * @return builder
     */
    public RuleSegmenterPluginBuilder addCommonRule(CommonRules commonRule) {
        ruleManager.addRule(commonRule.toRule());
        return this;
    }

    /**
     * 批量添加预制规则
     *
     * @param commonRules 预制规则枚举数组
     * @return builder
     */
    public RuleSegmenterPluginBuilder addCommonRules(CommonRules... commonRules) {
        for (CommonRules rule : commonRules) {
            addCommonRule(rule);
        }
        return this;
    }

    /**
     * 返回规则表，交给 {@code SegmentRecipe.withRules}。
     */
    public RuleManager build() {
        return ruleManager;
    }
}
