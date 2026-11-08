package dev.znlp.duckling.rule;

import dev.znlp.duckling.pattern.DucklingPattern;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 一条组合规则：有序 pattern 加上 production。
 * 相邻匹配见 {@link #matches(List, String)}。
 */
public abstract class Rule {
    private final int id;
    private final String name;
    private int leftPad = 0;
    private int rightPad = 0;
    private final List<String> examples = new ArrayList<>();

    public Rule(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addExample(String example) {
        this.examples.add(example);
    }

    public List<String> getExamples() {
        return examples;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLeftPad() {
        return leftPad;
    }

    public void setLeftPad(int leftPad) {
        this.leftPad = leftPad;
    }

    public int getRightPad() {
        return rightPad;
    }

    public void setRightPad(int rightPad) {
        this.rightPad = rightPad;
    }

    public abstract List<DucklingPattern<?>> getPatterns();

    public abstract NodeValue production(String text, List<DkNode<?>> matchedNodes);

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return id == rule.id;
    }

    @Override
    public String toString() {
        return "Rule(" + id + "," + name + ")";
    }

    private static final Comparator<DkNode<?>> START_ORDER =
            Comparator.comparingInt(n -> n.getRange().start());

    /**
     * 将各 pattern 命中的节点列组合成相邻序列。
     * 第 2 列起按起点排序后二分，避免全量笛卡尔积。
     */
    public static List<List<DkNode<?>>> matches(List<List<DkNode<?>>> matchNodes, String sentence) {
        int arity = matchNodes.size();
        if (arity == 0) {
            return List.of();
        }
        for (List<DkNode<?>> col : matchNodes) {
            if (col.isEmpty()) {
                return List.of();
            }
        }

        if (arity == 1) {
            List<DkNode<?>> col = matchNodes.get(0);
            List<List<DkNode<?>>> result = new ArrayList<>(col.size());
            for (DkNode<?> x : col) {
                result.add(List.of(x));
            }
            return result;
        }

        DkNode<?>[][] cols = new DkNode<?>[arity][];
        for (int i = 0; i < arity; i++) {
            List<DkNode<?>> col = matchNodes.get(i);
            cols[i] = col.toArray(new DkNode<?>[0]);
            if (i > 0) {
                Arrays.sort(cols[i], START_ORDER);
            }
        }

        List<List<DkNode<?>>> result = new ArrayList<>();
        List<DkNode<?>> path = new ArrayList<>(arity);
        matchFrom(cols, 0, path, sentence, result);
        return result;
    }

    private static void matchFrom(DkNode<?>[][] cols, int depth, List<DkNode<?>> path,
                                 String sentence, List<List<DkNode<?>>> result) {
        if (depth == cols.length) {
            result.add(new ArrayList<>(path));
            return;
        }
        if (depth == 0) {
            for (DkNode<?> node : cols[0]) {
                path.add(node);
                matchFrom(cols, 1, path, sentence, result);
                path.remove(path.size() - 1);
            }
            return;
        }

        DkNode<?> prev = path.get(path.size() - 1);
        DkNode<?>[] col = cols[depth];
        int from = lowerBoundStart(col, prev.getRange().end() + 1);
        for (int i = from; i < col.length; i++) {
            DkNode<?> node = col[i];
            if (adjacent2(prev, node, sentence)) {
                path.add(node);
                matchFrom(cols, depth + 1, path, sentence, result);
                path.remove(path.size() - 1);
            } else if (node.getRange().start() > prev.getRange().end()
                    && !gapIsWhitespace(sentence, prev.getRange().end() + 1, node.getRange().start())) {
                break;
            }
        }
    }

    static int lowerBoundStart(DkNode<?>[] col, int minStart) {
        int lo = 0;
        int hi = col.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (col[mid].getRange().start() < minStart) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public static boolean adjacent2(DkNode<?> a, DkNode<?> b, String sentence) {
        int pa = a.getRange().end();
        int pb = b.getRange().start();
        if (pb <= a.getRange().start() || pa >= pb) {
            return false;
        }
        return pb - pa == 1 || gapIsWhitespace(sentence, pa + 1, pb);
    }

    static boolean gapIsWhitespace(String sentence, int from, int to) {
        for (int i = from; i < to; i++) {
            if (!Character.isWhitespace(sentence.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
