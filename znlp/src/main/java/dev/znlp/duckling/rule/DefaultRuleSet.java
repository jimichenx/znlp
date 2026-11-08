package dev.znlp.duckling.rule;

import dev.znlp.base.dat.DATMatcher;
import dev.znlp.base.dat.DoubleArrayTrie;
import dev.znlp.base.dat.DoubleArrayTrieBuilder2;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.duckling.pattern.*;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;
import dev.znlp.duckling.support.TextPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 默认图解析实现：DAT 铺词典、正则铺其余叶子，再按脏规则迭代组合。
 * <p>
 * 构造时为词典启用 DAT 失败指针；Filter 按值类型索引。
 * {@link Rule} id 不是列表下标，查找必须走 {@code rulesById}。
 */
public class DefaultRuleSet implements RuleSet {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRuleSet.class);

    private final List<Rule> ruleList;
    private final Rule[] rulesById;
    private final List<TextDucklingPattern> textPatterns;
    private final Map<Class<?>, FilterDucklingPattern<?>[]> filtersByClass;
    private final FilterDucklingPattern<?>[] untypedFilters;
    private final List<Rule>[] rulesByPatternId;
    private final DoubleArrayTrie dat;
    private final int patternSize;
    private final int maxRuleId;

    @SuppressWarnings("unchecked")
    public DefaultRuleSet(List<Rule> ruleList) {
        this.ruleList = ruleList;

        Set<DucklingPattern<?>> allPatternsSet = new HashSet<>();
        int maxPid = 0;
        int maxRid = 0;
        for (Rule rule : ruleList) {
            maxRid = Math.max(maxRid, rule.getId());
            for (DucklingPattern<?> p : rule.getPatterns()) {
                allPatternsSet.add(p);
                maxPid = Math.max(maxPid, p.getId());
            }
        }
        this.patternSize = maxPid + 1;
        this.maxRuleId = maxRid;

        this.rulesById = new Rule[maxRid + 1];
        for (Rule rule : ruleList) {
            this.rulesById[rule.getId()] = rule;
        }

        this.textPatterns = new ArrayList<>();
        Map<Class<?>, List<FilterDucklingPattern<?>>> filterIndex = new HashMap<>();
        List<FilterDucklingPattern<?>> untyped = new ArrayList<>();
        for (DucklingPattern<?> p : allPatternsSet) {
            if (p instanceof TextDucklingPattern) {
                textPatterns.add((TextDucklingPattern) p);
            } else if (p instanceof FilterPattern<?> fp) {
                filterIndex.computeIfAbsent(fp.getTargetClass(), k -> new ArrayList<>()).add(fp);
            } else if (p instanceof FilterDucklingPattern<?> fp) {
                untyped.add(fp);
            }
        }

        Map<Class<?>, FilterDucklingPattern<?>[]> byClass = new HashMap<>();
        for (Map.Entry<Class<?>, List<FilterDucklingPattern<?>>> e : filterIndex.entrySet()) {
            byClass.put(e.getKey(), e.getValue().toArray(new FilterDucklingPattern<?>[0]));
        }
        this.filtersByClass = byClass;
        this.untypedFilters = untyped.toArray(new FilterDucklingPattern<?>[0]);

        this.rulesByPatternId = (List<Rule>[]) new List<?>[patternSize];
        for (int i = 0; i < patternSize; i++) {
            rulesByPatternId[i] = new ArrayList<>();
        }
        for (Rule rule : ruleList) {
            for (DucklingPattern<?> p : rule.getPatterns()) {
                int id = p.getId();
                if (id >= 0 && id < patternSize) {
                    rulesByPatternId[id].add(rule);
                }
            }
        }

        TreeSet<String> words = new TreeSet<>();
        for (TextDucklingPattern p : textPatterns) {
            if (p instanceof DictPattern) {
                words.addAll(((DictPattern) p).getWordBag());
            }
        }
        if (words.isEmpty()) {
            this.dat = null;
        } else {
            List<String> keys = new ArrayList<>(words);
            this.dat = new DoubleArrayTrieBuilder2().build(keys);
            this.dat.enableFailure(keys);
        }
    }

    @Override
    public List<DkNode<NodeValue>> execute(String text) {
        return new Reaction(text).execute();
    }

    private class Reaction {
        private final String text;
        private final PatternNodes[] pattern2Node;
        private final boolean[] ruleChangeFlag;
        private final int[] dirtyRules;
        private int dirtyCount;
        private final Set<RepeatKey> repeatSet;

        Reaction(String text) {
            this.text = text;
            this.pattern2Node = new PatternNodes[patternSize];
            for (int i = 0; i < patternSize; i++) {
                pattern2Node[i] = new PatternNodes();
            }
            this.ruleChangeFlag = new boolean[maxRuleId + 1];
            this.dirtyRules = new int[ruleList.size()];
            this.repeatSet = new HashSet<>();
        }

        List<DkNode<NodeValue>> execute() {
            Map<String, List<DkNode<TextToken>>> dictMatchResult = exeDictItem(text);

            List<DkNode<NodeValue>> allNodes = new ArrayList<>();

            for (TextDucklingPattern pattern : textPatterns) {
                List<DkNode<TextToken>> matches = pattern.predicate(text, dictMatchResult);
                if (matches.isEmpty()) {
                    continue;
                }
                int pid = pattern.getId();
                if (pid < pattern2Node.length) {
                    for (DkNode<TextToken> node : matches) {
                        pattern2Node[pid].addNode(node);
                    }
                }
                markRules(pid);
            }

            for (int loop = 0; loop < RuleSet.ITERATIONS_MAX; loop++) {
                List<DkNode<NodeValue>> newNodeList = new ArrayList<>();
                int snapshot = dirtyCount;
                for (int d = 0; d < snapshot; d++) {
                    Rule rule = rulesById[dirtyRules[d]];
                    if (rule == null) {
                        continue;
                    }
                    List<List<DkNode<?>>> matches = matchRule(rule);
                    for (List<DkNode<?>> match : matches) {
                        RepeatKey key = RepeatKey.of(rule.getId(), match);
                        if (!repeatSet.add(key)) {
                            continue;
                        }

                        NodeValue newValue;
                        try {
                            newValue = rule.production(text, match);
                        } catch (Exception e) {
                            logger.error("Error in rule " + rule.getName(), e);
                            continue;
                        }

                        if (newValue != null) {
                            int start = match.get(0).getRange().start() + rule.getLeftPad();
                            int end = match.get(match.size() - 1).getRange().end() - rule.getRightPad();
                            newNodeList.add(new DkNode<>(
                                    rule.getName(),
                                    new TextPosition(start, end),
                                    match,
                                    newValue
                            ));
                        }
                    }
                }

                if (newNodeList.isEmpty() || allNodes.size() > RuleSet.MAX_STASH_SIZE) {
                    break;
                }

                clearDirty();

                for (DkNode<NodeValue> node : newNodeList) {
                    allNodes.add(node);
                    applyFilters(node);
                }

                if (allNodes.size() > RuleSet.MAX_STASH_SIZE) {
                    break;
                }
            }

            return allNodes;
        }

        private void applyFilters(DkNode<NodeValue> node) {
            NodeValue value = node.getValue();
            if (value != null) {
                Class<?> c = value.getClass();
                while (c != null && c != Object.class) {
                    FilterDucklingPattern<?>[] filters = filtersByClass.get(c);
                    if (filters != null) {
                        for (FilterDucklingPattern<?> fp : filters) {
                            acceptFilter(node, fp);
                        }
                    }
                    c = c.getSuperclass();
                }
            }
            for (FilterDucklingPattern<?> fp : untypedFilters) {
                acceptFilter(node, fp);
            }
        }

        private void acceptFilter(DkNode<NodeValue> node, FilterDucklingPattern<?> fp) {
            if (!fp.predicate(node)) {
                return;
            }
            int pid = fp.getId();
            if (pid < pattern2Node.length) {
                pattern2Node[pid].addNode(node);
            }
            markRules(pid);
        }

        private void markRules(int patternId) {
            if (patternId < 0 || patternId >= rulesByPatternId.length) {
                return;
            }
            for (Rule rule : rulesByPatternId[patternId]) {
                int id = rule.getId();
                if (id >= 0 && id < ruleChangeFlag.length && !ruleChangeFlag[id]) {
                    ruleChangeFlag[id] = true;
                    dirtyRules[dirtyCount++] = id;
                }
            }
        }

        private void clearDirty() {
            for (int i = 0; i < dirtyCount; i++) {
                ruleChangeFlag[dirtyRules[i]] = false;
            }
            dirtyCount = 0;
        }

        private Map<String, List<DkNode<TextToken>>> exeDictItem(String text) {
            Map<String, List<DkNode<TextToken>>> result = new HashMap<>();
            if (dat == null) {
                return result;
            }
            DATMatcher matcher = dat.matcher(text);
            while (matcher.next()) {
                String word = text.substring(matcher.getBegin(), matcher.getBegin() + matcher.getLength());
                TextPosition range = new TextPosition(matcher.getBegin(), matcher.getBegin() + matcher.getLength() - 1);
                result.computeIfAbsent(word, k -> new ArrayList<>()).add(
                        new DkNode<>(
                                "Dict[" + word + "]",
                                range,
                                List.of(),
                                new TextToken(word),
                                true
                        )
                );
            }
            return result;
        }

        private List<List<DkNode<?>>> matchRule(Rule rule) {
            List<DucklingPattern<?>> patterns = rule.getPatterns();
            List<List<DkNode<?>>> list = new ArrayList<>(patterns.size());
            for (DucklingPattern<?> p : patterns) {
                int pid = p.getId();
                if (pid >= pattern2Node.length) {
                    return List.of();
                }
                PatternNodes nodes = pattern2Node[pid];
                if (nodes.isEmpty()) {
                    return List.of();
                }
                list.add(nodes.toList());
            }
            return Rule.matches(list, text);
        }
    }

    private static class PatternNodes {
        private final List<DkNode<?>> list = new ArrayList<>();

        void addNode(DkNode<?> node) {
            list.add(node);
        }

        List<DkNode<?>> toList() {
            return list;
        }

        boolean isEmpty() {
            return list.isEmpty();
        }
    }

    /**
     * 规则 + 最多 3 段 span，避免字符串拼接做去重 key。
     */
    private record RepeatKey(int ruleId, int s0, int e0, int s1, int e1, int s2, int e2) {
        static RepeatKey of(int ruleId, List<DkNode<?>> match) {
            int s0 = Integer.MIN_VALUE, e0 = Integer.MIN_VALUE;
            int s1 = Integer.MIN_VALUE, e1 = Integer.MIN_VALUE;
            int s2 = Integer.MIN_VALUE, e2 = Integer.MIN_VALUE;
            int n = match.size();
            if (n > 0) {
                TextPosition p = match.get(0).getRange();
                s0 = p.start();
                e0 = p.end();
            }
            if (n > 1) {
                TextPosition p = match.get(1).getRange();
                s1 = p.start();
                e1 = p.end();
            }
            if (n > 2) {
                TextPosition p = match.get(2).getRange();
                s2 = p.start();
                e2 = p.end();
            }
            return new RepeatKey(ruleId, s0, e0, s1, e1, s2, e2);
        }
    }
}
