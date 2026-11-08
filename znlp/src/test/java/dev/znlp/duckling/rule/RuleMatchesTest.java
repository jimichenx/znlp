package dev.znlp.duckling.rule;

import org.junit.jupiter.api.Test;
import dev.znlp.duckling.pattern.TextToken;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.TextPosition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuleMatchesTest {

    @Test
    public void twoColumns_matchesNaiveCartesian() {
        String sentence = "ab cd";
        List<DkNode<?>> left = List.of(node(0, 0), node(1, 1), node(3, 3));
        List<DkNode<?>> right = List.of(node(1, 1), node(3, 4), node(0, 0), node(2, 2));

        Set<String> optimized = keys(Rule.matches(List.of(left, right), sentence));
        Set<String> naive = keys(naive(List.of(left, right), sentence));
        assertEquals(naive, optimized);
    }

    @Test
    public void threeColumns_withWhitespaceGap() {
        String sentence = "a b c";
        List<DkNode<?>> a = List.of(node(0, 0));
        List<DkNode<?>> b = List.of(node(2, 2));
        List<DkNode<?>> c = List.of(node(4, 4));

        Set<String> optimized = keys(Rule.matches(List.of(a, b, c), sentence));
        Set<String> naive = keys(naive(List.of(a, b, c), sentence));
        assertEquals(naive, optimized);
        assertEquals(Set.of("0-0|2-2|4-4"), optimized);
    }

    @Test
    public void overlappingSpans_areNotAdjacent() {
        String sentence = "abcdef";
        List<DkNode<?>> left = List.of(node(0, 3));
        List<DkNode<?>> right = List.of(node(2, 5), node(4, 5));

        Set<String> optimized = keys(Rule.matches(List.of(left, right), sentence));
        Set<String> naive = keys(naive(List.of(left, right), sentence));
        assertEquals(naive, optimized);
        assertEquals(Set.of("0-3|4-5"), optimized);
    }

    @Test
    public void emptyColumn_returnsEmpty() {
        assertTrue(Rule.matches(List.of(List.of(node(0, 0)), List.of()), "ab").isEmpty());
    }

    @Test
    public void plainAlternation_becomesDictLiterals() {
        String[] parts = RuleBuilder.plainDictLiterals("块|元|圆|角|毛|分|元整");
        assertEquals(7, parts.length);
        assertEquals("元整", parts[6]);
    }

    @Test
    public void regexMeta_staysRegex() {
        assertNull(RuleBuilder.plainDictLiterals("1\\d{10}"));
        assertNull(RuleBuilder.plainDictLiterals("秒(?:钟|鐘)?"));
        assertNull(RuleBuilder.plainDictLiterals("\\d+"));
    }

    private static Set<String> keys(List<List<DkNode<?>>> matches) {
        Set<String> set = new HashSet<>();
        for (List<DkNode<?>> m : matches) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < m.size(); i++) {
                if (i > 0) sb.append('|');
                TextPosition p = m.get(i).getRange();
                sb.append(p.start()).append('-').append(p.end());
            }
            set.add(sb.toString());
        }
        return set;
    }

    private static List<List<DkNode<?>>> naive(List<List<DkNode<?>>> cols, String sentence) {
        List<List<DkNode<?>>> acc = new ArrayList<>();
        acc.add(new ArrayList<>());
        for (List<DkNode<?>> col : cols) {
            List<List<DkNode<?>>> next = new ArrayList<>();
            for (List<DkNode<?>> prefix : acc) {
                for (DkNode<?> node : col) {
                    if (prefix.isEmpty() || Rule.adjacent2(prefix.get(prefix.size() - 1), node, sentence)) {
                        List<DkNode<?>> row = new ArrayList<>(prefix);
                        row.add(node);
                        next.add(row);
                    }
                }
            }
            acc = next;
        }
        return acc;
    }

    private static DkNode<TextToken> node(int start, int end) {
        return new DkNode<>("t", new TextPosition(start, end), List.of(), new TextToken("x"), true);
    }
}
