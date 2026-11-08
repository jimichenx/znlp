package dev.znlp.duckling.pattern;

import dev.znlp.duckling.support.NodeValue;
import dev.znlp.duckling.support.TextPosition;

import java.util.Collections;
import java.util.List;

/**
 * 原文切片。{@code groups} 对应正则捕获组跨度，词典命中时为空。
 */
public record TextToken(String text, List<TextPosition> groups) implements NodeValue {
    public TextToken(String text) {
        this(text, Collections.emptyList());
    }

}
