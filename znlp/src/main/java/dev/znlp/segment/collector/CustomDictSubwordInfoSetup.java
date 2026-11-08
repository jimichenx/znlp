package dev.znlp.segment.collector;

import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

/**
 * 基于自定义词典的子词补全.
 * 一般在感知机分词器，需要补全
 * <p>
 * 注意：此实现假设传入的trie是StringIntDATMap类型，如果znlp中没有CustomDictionary类，
 * 可能需要根据实际情况调整
 */
public class CustomDictSubwordInfoSetup implements SubwordInfoSetup {

    private final StringIntDATMap trie;

    public CustomDictSubwordInfoSetup(StringIntDATMap trie) {
        this.trie = trie;
    }

    @Override
    public void fill(Wordnet wordnet, Wordpath wordPath) {
        if (trie == null) {
            return;
        }
        char[] text = wordnet.getCharArray();
        int origin = wordnet.origin();
        StringIntDATMap.DATMapMatcherInt searcher = trie.match(text, origin, wordnet.limit());

        while (searcher.next()) {
            int offset = searcher.getBegin() - origin;
            int length = searcher.getLength();

            Vertex v = wordnet.acquire(length, -1, searcher.getValue());
            if (!wordnet.row(offset).contains(length)) {
                wordnet.put(offset, v);
            }
        }
    }


}

