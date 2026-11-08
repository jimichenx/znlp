package dev.znlp.segment.impl.cws;

import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.VertexRow;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BestPathAlgorithm;

/**
 * 按感知机给出的边界成词。原子识别只填感知机没有盖住的空洞，不跟二元语法竞争。
 */
public final class LinearPathAlgorithm implements BestPathAlgorithm {

    @Override
    public Wordpath select(Wordnet wordnet) {
        Wordpath path = new Wordpath(wordnet);
        int[] spans = wordnet.get(CwsSplitAlgorithm.SPANS);
        if (spans != null) {
            for (int i = 0; i + 1 < spans.length; i += 2) {
                path.combine(spans[i], spans[i + 1]);
            }
        }
        int size = wordnet.length();
        for (int i = 0; i < size; i++) {
            VertexRow row = wordnet.row(i);
            if (row == null || row.isEmpty()) {
                continue;
            }
            Vertex best = null;
            int rowSize = row.size();
            for (int a = 0; a < rowSize; a++) {
                Vertex cursor = row.vertexAt(a);
                if (cursor.length > 1 && path.isUnmergedSpan(i, cursor.length)) {
                    best = cursor;
                }
            }
            if (best != null) {
                path.combine(i, best.length);
            }
        }
        return path;
    }
}
