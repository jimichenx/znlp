
package dev.znlp.segment.pipeline;

import dev.znlp.segment.graph.Wordpath;

/**
 *
 * @author jimichan
 */
public interface WordpathProcessor extends PipelineComponent {

    /**
     * 对传入的Wordpath进行处理，然后返回一个旧的或者新的对象
     *
     * @param wordPath
     * @return 一般对传入的wordPath修改，返回对象本身
     */
    Wordpath process(Wordpath wordPath);

}
