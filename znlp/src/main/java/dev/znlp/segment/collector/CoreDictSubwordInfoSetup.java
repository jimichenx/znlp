package dev.znlp.segment.collector;

import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.impl.dict.CoreDictionary;

/**
 * 基于词典的子词补全.
 * 一般在感知机分词器，需要补全
 */
public class CoreDictSubwordInfoSetup implements SubwordInfoSetup {

    private final CoreDictionary dictionary;

    public CoreDictSubwordInfoSetup(CoreDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void fill(Wordnet wordnet, Wordpath wordPath) {
        char[] text = wordnet.getCharArray();
        int origin = wordnet.origin();
        StringIntDATMap.DATMapMatcherInt searcher = dictionary.match(text, origin, wordnet.limit());

        while (searcher.next()) {
            int offset = searcher.getBegin() - origin;
            int length = searcher.getLength();
            int wordId = searcher.getIndex();
            int value = searcher.getValue();

            Vertex v = wordnet.acquire(length, wordId, value);
            if (!wordnet.row(offset).contains(length)) {
                wordnet.put(offset, v);
            }
        }
    }


}

