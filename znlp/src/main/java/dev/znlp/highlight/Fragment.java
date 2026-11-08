package dev.znlp.highlight;

/**
 * 高亮片段
 *
 * @param text        片段文本
 * @param highlighted 是否高亮
 * @author znlp
 */
public record Fragment(String text, boolean highlighted) {
}
