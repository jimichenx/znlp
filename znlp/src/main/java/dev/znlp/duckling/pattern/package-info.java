/**
 * 规则左侧的匹配子：词典、正则、以及按节点值类型过滤。
 * <p>
 * 词典项由 {@link dev.znlp.duckling.rule.DefaultRuleSet} 用 DAT 整句扫描后回填；
 * 不含元字符的 {@code a|b|c} 在 {@link dev.znlp.duckling.rule.RuleBuilder#pattern(String)} 里会改成词典。
 */
package dev.znlp.duckling.pattern;
