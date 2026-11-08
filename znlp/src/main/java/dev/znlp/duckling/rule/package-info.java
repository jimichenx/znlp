/**
 * 规则 DSL 与图解析引擎。
 * <p>
 * {@link dev.znlp.duckling.rule.RuleBuilder} 把词典、正则、类型过滤拼成 {@link dev.znlp.duckling.rule.Rule}；
 * {@link dev.znlp.duckling.rule.DefaultRuleSet} 对输入做最多 30 轮相邻组合。
 * 规则 id 由 {@code RuleBuilder} 进程内递增分配，不是规则列表下标。
 */
package dev.znlp.duckling.rule;
