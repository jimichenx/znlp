package dev.znlp.fasttext.train;

import java.util.List;

/**
 * 训练样本行类，用于表示一条训练样本的数据结构。
 * 包含一行文本的分词结果，是训练过程中样本传递的基本单位。
 *
 * @param words 样本的分词结果列表
 */
public record SampleLine(List<String> words) {
    /**
     * 构造SampleLine实例
     *
     * @param words 分词结果列表
     */
    public SampleLine {
    }
}
