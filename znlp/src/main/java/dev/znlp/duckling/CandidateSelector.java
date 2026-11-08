package dev.znlp.duckling;

import dev.znlp.duckling.ml.MlModel;
import dev.znlp.duckling.support.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在重叠跨度上挑选最终输出。
 * <p>
 * 丢弃 {@link NodeValue#isLatent()} 节点；其余按跨度从长到短、起点从左到右，
 * 同类再用 Naive Bayes，最后看 {@link DimensionKind#getPriority()}。
 */
public class CandidateSelector {

    private final MlModel model;

    public CandidateSelector() {
        this.model = MlModel.load();
    }

    public List<ParserMatch> select(List<DkNode<NodeValue>> nodes) {
        List<ParserMatch> matches = nodes.stream()
                .filter(n -> isCandidate(n.getValue()))
                .map(ParserMatch::new)
                .sorted(createComparator())
                .collect(Collectors.toList());

        List<ParserMatch> result = new ArrayList<>();
        
        for (ParserMatch match : matches) {
            boolean isDisjoint = true;
            for (ParserMatch existing : result) {
                if (!existing.getPosition().isDisjoint(match.getPosition())) {
                    isDisjoint = false;
                    break;
                }
            }
            if (isDisjoint) {
                result.add(match);
            }
        }
        
        // Sort by start position for output
        result.sort(Comparator.comparingInt(m -> m.getPosition().start()));
        return result;
    }

    private boolean isCandidate(NodeValue v) {
        if (v instanceof Dimension || v instanceof FinalNodeValue) {
            return !v.isLatent();
        }
        return false;
    }

    private DimensionKind getKind(NodeValue v) {
        if (v instanceof Dimension) return ((Dimension) v).getKind();
        if (v instanceof FinalNodeValue) return ((FinalNodeValue) v).getKind();
        return null;
    }

    private int getKindPriority(NodeValue v) {
        DimensionKind k = getKind(v);
        return k != null ? k.getPriority() : -1;
    }

    private Comparator<ParserMatch> createComparator() {
        return Comparator.comparingInt(ParserMatch::getLength).reversed() // Longest match first
                .thenComparingInt(m -> m.getPosition().start()) // Then earliest start
                .thenComparing((m1, m2) -> {
                    DimensionKind k1 = getKind(m1.getValue());
                    DimensionKind k2 = getKind(m2.getValue());
                    
                    if (k1 != null && k1 == k2) {
                             double p1 = model.trueProbalog(m1.getNode());
                             double p2 = model.trueProbalog(m2.getNode());
                             return Double.compare(p2, p1); // Higher prob first
                    }
                    return 0;
                })
                .thenComparing(Comparator.comparingInt((ParserMatch m) -> getKindPriority(m.getValue())).reversed()); // Then Higher Priority
    }
}
