package dev.znlp.perceptron.feature;

import dev.znlp.perceptron.utils.FastStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * DSL模板类，用于解析和存储特征提取规则
 * <p>
 * DSL语法示例：
 * <pre>
 * %[0]2;          // 当前字符 + '2'
 * %[-1]1;         // 前1个字符 + '1'
 * %[-1]%[0]5;     // 前1个字符 + 当前字符 + '5'
 * %[-2]/%[-1]%[0]4;  // 前2个字符 + '/' + 前1个字符 + 当前字符 + '4'
 * %[1]3;          // 后1个字符 + '3'
 * %[0]/%[1]6;     // 当前字符 + '/' + 后1个字符 + '6'
 * %[1]/%[2]7;     // 后1个字符 + '/' + 后2个字符 + '7'
 * </pre>
 *
 * @author jimichan
 */
public class FeatureTemplate {

    private final List<FeatureRule> rules;

    /**
     * 解析DSL字符串并创建模板
     *
     * @param dsl DSL字符串，规则用分号分隔
     */
    public FeatureTemplate(String dsl) {
        this.rules = parseDSL(dsl);
    }

    public void features(char[] input, int position, FastStringBuilder buffer, FeatureConsumer consumer) {
        int size = input.length;
        if (position < 0 || position >= size) {
            return;
        }

        for (FeatureTemplate.FeatureRule rule : this.getRules()) {
            buffer.clear();
            if (buildFeature(input, rule, position, buffer)) {
                consumer.accept(buffer.toString());
            }
        }
    }

    public void feature2(CharSequence input, int position, FastStringBuilder buffer, FeatureConsumer consumer) {
        int size = input.length();
        if (position < 0 || position >= size) {
            return;
        }

        for (FeatureTemplate.FeatureRule rule : this.getRules()) {
            buffer.clear();
            if (buildFeature2(input, rule, position, buffer)) {
                consumer.accept(buffer.toString());
            }
        }
    }

    /**
     * 获取所有规则
     */
    List<FeatureRule> getRules() {
        return rules;
    }

    /**
     * 解析DSL字符串
     */
    private List<FeatureRule> parseDSL(String dsl) {
        List<FeatureRule> result = new ArrayList<>();
        if (dsl == null || dsl.trim().isEmpty()) {
            return result;
        }

        // 按分号分割规则
        String[] lines = dsl.split(";");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            FeatureRule rule = parseRule(line);
            if (rule != null) {
                result.add(rule);
            }
        }

        return result;
    }

    /**
     * 解析单个规则
     * 例如: %[0]2 或 %[-1]%[0]5 或 %[-2]/%[-1]%[0]4
     */
    private FeatureRule parseRule(String ruleStr) {
        List<RulePart> parts = new ArrayList<>();
        int i = 0;
        int len = ruleStr.length();

        while (i < len) {
            if (ruleStr.charAt(i) == '%' && i + 1 < len && ruleStr.charAt(i + 1) == '[') {
                // 解析 %[index] 格式
                int start = i + 2;
                int end = start;
                while (end < len && ruleStr.charAt(end) != ']') {
                    end++;
                }
                if (end < len) {
                    String indexStr = ruleStr.substring(start, end);
                    try {
                        int index = Integer.parseInt(indexStr);
                        parts.add(new RulePart(RulePartType.INDEX, index));
                        i = end + 1;
                    } catch (NumberFormatException e) {
                        // 解析失败，跳过
                        i++;
                    }
                } else {
                    i++;
                }
            } else {
                // 解析普通字符
                int start = i;
                while (i < len && !(ruleStr.charAt(i) == '%' && i + 1 < len && ruleStr.charAt(i + 1) == '[')) {
                    i++;
                }
                if (i > start) {
                    String literal = ruleStr.substring(start, i);
                    parts.add(new RulePart(RulePartType.LITERAL, literal));
                }
            }
        }

        if (parts.isEmpty()) {
            return null;
        }

        return new FeatureRule(parts);
    }


    /**
     * 构建特征字符串
     * 如果规则中引用的任何索引超出范围，则返回false且不修改buffer
     *
     * @param rule     规则
     * @param position 当前位置
     * @param buffer   缓冲区（调用前应该已clear）
     * @return 是否成功构建（如果所有索引都有效则返回true）
     */
    private boolean buildFeature(char[] sequence, FeatureTemplate.FeatureRule rule, int position, FastStringBuilder buffer) {
        int size = sequence.length;

        // 先检查所有索引是否有效，避免部分填充buffer
        for (FeatureTemplate.RulePart part : rule.parts) {
            if (part.type == FeatureTemplate.RulePartType.INDEX) {
                int index = part.index;
                int actualPos = position + index;
                if (actualPos < 0 || actualPos >= size) {
                    return false;
                }
            }
        }

        // 所有索引都有效，开始构建
        for (FeatureTemplate.RulePart part : rule.parts) {
            if (part.type == FeatureTemplate.RulePartType.INDEX) {
                int index = part.index;
                int actualPos = position + index;
                buffer.append(sequence[actualPos]);
            } else {
                // LITERAL类型
                for (int i = 0; i < part.literal.length(); i++) {
                    buffer.append(part.literal.charAt(i));
                }
            }
        }
        return true;
    }

    private boolean buildFeature2(CharSequence sequence, FeatureTemplate.FeatureRule rule, int position, FastStringBuilder buffer) {
        int size = sequence.length();

        // 先检查所有索引是否有效，避免部分填充buffer
        for (FeatureTemplate.RulePart part : rule.parts) {
            if (part.type == FeatureTemplate.RulePartType.INDEX) {
                int index = part.index;
                int actualPos = position + index;
                if (actualPos < 0 || actualPos >= size) {
                    return false;
                }
            }
        }

        // 所有索引都有效，开始构建
        for (FeatureTemplate.RulePart part : rule.parts) {
            if (part.type == FeatureTemplate.RulePartType.INDEX) {
                int index = part.index;
                int actualPos = position + index;
                buffer.append(sequence.charAt(actualPos));
            } else {
                // LITERAL类型
                for (int i = 0; i < part.literal.length(); i++) {
                    buffer.append(part.literal.charAt(i));
                }
            }
        }
        return true;
    }

    /**
     * 规则部分类型
     */
    enum RulePartType {
        INDEX,    // 字符索引，如 %[-1], %[0], %[1]
        LITERAL   // 字面量字符串
    }

    /**
     * 规则部分
     */
    static class RulePart {
        final RulePartType type;
        final int index;      // 当type为INDEX时使用
        final String literal; // 当type为LITERAL时使用

        RulePart(RulePartType type, int index) {
            this.type = type;
            this.index = index;
            this.literal = null;
        }

        RulePart(RulePartType type, String literal) {
            this.type = type;
            this.index = 0;
            this.literal = literal;
        }
    }

    /**
         * 特征规则
         */
        record FeatureRule(List<RulePart> parts) {

        /**
             * 获取规则中使用的最大前向索引（负数）
             * 例如：%[-2] 返回 -2
             */
            int getMinIndex() {
                int min = 0;
                for (RulePart part : parts) {
                    if (part.type == RulePartType.INDEX && part.index < min) {
                        min = part.index;
                    }
                }
                return min;
            }

            /**
             * 获取规则中使用的最大后向索引（正数）
             * 例如：%[2] 返回 2
             */
            int getMaxIndex() {
                int max = 0;
                for (RulePart part : parts) {
                    if (part.type == RulePartType.INDEX && part.index > max) {
                        max = part.index;
                    }
                }
                return max;
            }
        }
}