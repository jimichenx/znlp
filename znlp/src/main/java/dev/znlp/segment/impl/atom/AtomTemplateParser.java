package dev.znlp.segment.impl.atom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 原子模板解析器
 * <p>
 * 解析模板表达式，支持以下格式：
 * <ul>
 *   <li>{Z[1,2]} - 重复次数列表</li>
 *   <li>{Z[1-2]} - 重复次数范围</li>
 *   <li>{(日|月)} - 可选项</li>
 * </ul>
 *
 * @author jimichan
 */
public class AtomTemplateParser {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(\\{(.+?)\\})|(.+?)");

    /**
     * 解析模板表达式，返回所有可能的展开结果
     *
     * @param template 模板字符串
     * @return 所有可能的展开字符串列表
     */
    public static List<String> parseTemplate(String template) {
        List<List<String>> list = new ArrayList<>();

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        while (matcher.find()) {
            String part = matcher.group();
            if (part.startsWith("{") && part.endsWith("}")) {
                part = part.substring(1, part.length() - 1);
            }

            if (part.startsWith("(") && part.endsWith(")")) {
                // 处理可选项：(日|月)
                String[] options = part.substring(1, part.length() - 1).split("\\|");
                List<String> optionList = new ArrayList<>();
                Collections.addAll(optionList, options);
                list.add(optionList);
            } else if (part.contains("[") || part.contains("|")) {
                // 处理重复模式：Z[1,2] 或 Z|N[1-2]
                List<String> st = new ArrayList<>();
                int bracketIndex = part.indexOf("[");
                String prefix = part.substring(0, bracketIndex);
                String[] elements = prefix.split("\\|");

                int rangeStart = part.indexOf("[") + 1;
                int rangeEnd = part.lastIndexOf("]");
                String range = part.substring(rangeStart, rangeEnd);

                if (range.contains(",")) {
                    // 处理逗号分隔的次数：[1,2,3]
                    String[] counts = range.split(",");
                    for (String count : counts) {
                        int n = Integer.parseInt(count.trim());
                        for (String elem : elements) {
                            st.add(repeatString(elem, n));
                        }
                    }
                } else if (range.contains("-")) {
                    // 处理范围：[1-3]
                    String[] bounds = range.split("-");
                    int start = Integer.parseInt(bounds[0].trim());
                    int end = Integer.parseInt(bounds[1].trim());
                    for (int n = start; n <= end; n++) {
                        for (String elem : elements) {
                            st.add(repeatString(elem, n));
                        }
                    }
                } else {
                    // 处理单个次数：[2]
                    int n = Integer.parseInt(range.trim());
                    for (String elem : elements) {
                        st.add(repeatString(elem, n));
                    }
                }
                list.add(st);
            } else {
                // 普通字符串
                List<String> singleList = new ArrayList<>();
                singleList.add(part);
                list.add(singleList);
            }
        }

        // 计算笛卡尔积
        return cartesianProduct(list);
    }

    /**
     * 重复字符串
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 计算笛卡尔积
     */
    private static List<String> cartesianProduct(List<List<String>> lists) {
        if (lists.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        cartesianProductHelper(lists, 0, "", result);
        return result;
    }

    /**
     * 递归辅助方法计算笛卡尔积
     */
    private static void cartesianProductHelper(List<List<String>> lists, int index,
                                               String current, List<String> result) {
        if (index == lists.size()) {
            result.add(current);
            return;
        }

        for (String item : lists.get(index)) {
            cartesianProductHelper(lists, index + 1, current + item, result);
        }
    }
}

