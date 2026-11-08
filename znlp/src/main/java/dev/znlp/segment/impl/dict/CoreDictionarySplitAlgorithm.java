package dev.znlp.segment.impl.dict;


import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.WordSplitAlgorithm;

/**
 * 基于核心词典的基础切词器
 *
 * @author jimichan
 */
public class CoreDictionarySplitAlgorithm extends BaseSegmentComponent implements WordSplitAlgorithm {

    private final CoreDictionary coreDictionary;

    public CoreDictionarySplitAlgorithm(CoreDictionary coreDictionary) {
        super(LEVEL1);
        this.coreDictionary = coreDictionary;
    }

    @Override
    public void fill(Wordnet wordnet) {
        char[] text = wordnet.getCharArray();
        int origin = wordnet.origin();

        StringIntDATMap.DATMapMatcherInt searcher = coreDictionary.match(text, origin, wordnet.limit());

        while (searcher.next()) {
            int offset = searcher.getBegin() - origin;
            int length = searcher.getLength();
            int wordId = searcher.getIndex();

            Vertex v = wordnet.acquire(length, wordId, searcher.getValue());

            wordnet.put(offset, v);
        }
    }

}