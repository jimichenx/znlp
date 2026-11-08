package dev.znlp.segment.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 默认的子词规则词典实现。
 * <p>
 * 规则在 {@link #add} 时立即解析并编入索引，不需要额外的构建步骤。
 * 匹配时先查精确表，再顺序尝试通配规则。
 */
public class DefaultSubwordRuleDict implements SubwordRuleDict {

    private final Map<String, int[]> eqMap = new HashMap<>();
    private final List<Tongpei> matchMap = new ArrayList<>();

    record Tongpei(Pattern regex, int[] result, Character start, Character end) {
    }

    public void addAll(List<String> ruleList) {
        for (String rule : ruleList) {
            add(rule);
        }
    }

    /**
     * 添加一条子词规则，形如 {@code 北京/大学} 或 {@code 中?国}。
     *
     * @param rule 规则文本，{@code *} 与 {@code ?} 等价，均匹配任意单字
     * @throws IllegalArgumentException 通配规则的首尾不能同时为通配符
     */
    public void add(String rule) {
        String normalized = rule.trim().replace("\\", "/").replace("*", "?");

        if (normalized.contains("?")) {
            addWildcard(normalized);
        } else {
            addExact(normalized);
        }
    }

    private void addExact(String rule) {
        String[] sp = rule.split("/");
        StringBuilder keyBuilder = new StringBuilder();
        int[] array = new int[sp.length];
        for (int i = 0; i < sp.length; i++) {
            keyBuilder.append(sp[i]);
            array[i] = sp[i].length();
        }
        eqMap.put(keyBuilder.toString(), array);
    }

    private void addWildcard(String rule) {
        String withoutSplit = rule.replace("/", "");
        // ? 代表任意一个汉字，因此用 "." 而不是 ".?"，保证匹配到的词长与切分长度一致
        Pattern regex = Pattern.compile(withoutSplit.replace("?", "."));
        String[] parts = rule.split("/");
        int[] array = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            array[i] = parts[i].length();
        }

        Character start = withoutSplit.charAt(0);
        if (start == '?') {
            start = null;
        }

        Character end = withoutSplit.charAt(withoutSplit.length() - 1);
        if (end == '?') {
            end = null;
        }

        if (start == null && end == null) {
            throw new IllegalArgumentException(rule + " 不能头和尾都是通配符号");
        }

        matchMap.add(new Tongpei(regex, array, start, end));
    }

    public void clear() {
        eqMap.clear();
        matchMap.clear();
    }

    @Override
    public int[] match(String word) {
        int[] exact = eqMap.get(word);
        if (exact != null) {
            return exact;
        }

        for (Tongpei tongpei : matchMap) {
            if ((tongpei.start != null && word.charAt(0) == tongpei.start)
                    || (tongpei.end != null && word.charAt(word.length() - 1) == tongpei.end)) {
                if (tongpei.regex.matcher(word).matches()) {
                    return tongpei.result;
                }
            }
        }
        return null;
    }

}
