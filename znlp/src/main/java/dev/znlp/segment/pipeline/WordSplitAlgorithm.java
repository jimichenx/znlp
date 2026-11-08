package dev.znlp.segment.pipeline;

import org.jetbrains.annotations.NotNull;
import dev.znlp.segment.graph.Wordnet;

/**
 * 分词算法。
 * 分词逻辑基本上是面向字符的处理程序。
 * 分词算法的作用是对文本分析后，产生一种或多种分词路径，结果保存在Wordnet数据结构里面。
 * <p>
 * 1. 基于词典
 * 3. 基于字分割
 * 2. 基于规则
 * <p>
 * 在一个具体的分词器中，有可能综合同时使用多个分词算法。
 *
 * @author jimichan
 */
public interface WordSplitAlgorithm extends PipelineComponent {

    /**
     * 填充Wordnet实例
     *
     * @param wordnet
     */
    void fill(@NotNull Wordnet wordnet);

}