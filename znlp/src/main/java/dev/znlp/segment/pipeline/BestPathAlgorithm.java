
package dev.znlp.segment.pipeline;

import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

/**
 *
 * @author jimichan
 */
public interface BestPathAlgorithm {

    /**
     * 从词图网络中选择一条从头到尾的路径
     *
     * @param wordnet 输入词图
     * @return Wordpath
     */
    Wordpath select(Wordnet wordnet);
}
