package dev.znlp.duckling;

import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;
import dev.znlp.duckling.support.TextPosition;

/**
 * 消歧阶段选中的一个跨度及其图节点。
 */
public class ParserMatch {
    private final TextPosition position;
    private final DkNode<NodeValue> node;
    private final NodeValue value;

    public ParserMatch(DkNode<NodeValue> node) {
        this.node = node;
        this.position = node.getRange();
        this.value = node.getValue();
    }

    public TextPosition getPosition() {
        return position;
    }

    public DkNode<NodeValue> getNode() {
        return node;
    }

    public NodeValue getValue() {
        return value;
    }

    public int getLength() {
        return position.getLength();
    }
}
