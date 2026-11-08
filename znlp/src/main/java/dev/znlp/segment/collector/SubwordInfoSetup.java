package dev.znlp.segment.collector;

import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

/**
 * 感知机、crf等分词，wordnet中没有子词信息。那么通过这个接口在收集结果之前，通过词典新增子词信息。
 *
 * @author jimichan
 */
public interface SubwordInfoSetup {
    void fill(Wordnet wordnet, Wordpath wordPath);
}

