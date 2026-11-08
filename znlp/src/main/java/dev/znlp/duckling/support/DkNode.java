package dev.znlp.duckling.support;

import dev.znlp.duckling.pattern.TextToken;
import dev.znlp.duckling.ml.FeatureInput;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图节点：一条规则在某一跨度上的取值，以及用于消歧的特征。
 *
 * @param <T> 节点值类型，叶子多为 {@link TextToken}
 */
public class DkNode<T extends NodeValue> {
    private final String ruleSym;
    private final TextPosition range;
    private final List<DkNode<?>> children;
    private final T value;
    private final boolean leaf;
    
    private Double probalog;
    private final FeatureInput featureInput;

    public DkNode(String ruleSym, TextPosition range, List<DkNode<?>> children, T value) {
        this(ruleSym, range, children, value, false);
    }

    public DkNode(String ruleSym, TextPosition range, List<DkNode<?>> children, T value, boolean leaf) {
        this.ruleSym = ruleSym;
        this.range = range;
        this.children = children;
        this.value = value;
        this.leaf = leaf;
        
        // Calculate FeatureInput
        List<String> features = new ArrayList<>();
        if (children != null) {
            features.addAll(children.stream().map(DkNode::getRuleSym).collect(Collectors.toList()));
        }
        this.featureInput = new FeatureInput(ruleSym, features);
    }

    public Double getProbalog() {
        return probalog;
    }

    public void setProbalog(Double probalog) {
        this.probalog = probalog;
    }

    public FeatureInput getFeatureInput() {
        return featureInput;
    }

    public String getRuleSym() {
        return ruleSym;
    }

    public TextPosition getRange() {
        return range;
    }

    public List<DkNode<?>> getChildren() {
        return children;
    }

    public T getValue() {
        return value;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public int height() {
        if (children == null || children.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (DkNode<?> child : children) {
            max = Math.max(max, child.height());
        }
        return max + 1;
    }

    public int numNodes() {
        if (children == null || children.isEmpty()) {
            return 1;
        }
        int sum = 1;
        for (DkNode<?> child : children) {
            sum += child.numNodes();
        }
        return sum;
    }
}
