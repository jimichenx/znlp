package dev.znlp.segment.collector;

import dev.znlp.segment.Token;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

/**
 * 子词切分计算器接口
 * <p>
 * 从wordnet中计算出子词的所需要的基本信息，计算结果保存在WordTerm的subword字段里面
 *
 * @author jimichan
 */
public interface SubwordComputer {

    /**
     * [term] 一个待切分的子词
     * [wordnet] 当前
     *
     * @return 如果处理了当前term返回true，没有返回false
     */
    boolean run(Token term, Wordnet wordnet, Wordpath wordPath);

}

