/**
 * Duckling 结构化解析：从文本中抽出数字、金额、时间、电话等维度并归一化。
 * <p>
 * 不进入默认分词流水线。应用侧一般走 {@link dev.znlp.Znlp#parse(String)}；
 * 引擎入口是 {@link dev.znlp.duckling.DucklingParser}。
 * <p>
 * 处理分两步：{@link dev.znlp.duckling.rule.DefaultRuleSet} 自底向上生成全部候选节点，
 * {@link dev.znlp.duckling.CandidateSelector} 在重叠跨度上做消歧。
 * 设计说明见仓库 {@code docs/设计架构/Duckling.md}。
 */
package dev.znlp.duckling;
