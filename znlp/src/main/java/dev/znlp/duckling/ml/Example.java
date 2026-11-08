package dev.znlp.duckling.ml;

import dev.znlp.duckling.support.Dimension;

import java.util.function.Predicate;

/**
 * 一条语法样例：文本以及「解析结果是否算对」的谓词。
 */
public record Example(String text, Predicate<Dimension> predicate) {
}
