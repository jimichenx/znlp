package dev.znlp.segment.impl.cws;

import dev.znlp.segment.Nature;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.impl.dict.CoreDictionary;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.WordSplitAlgorithm;

/**
 * 感知机分词算法：用 CWS 模型切词，把结果填进词网。
 * <p>
 * 词典中已有的词会带上词频；未登录词标为 {@link Nature#newWord}。
 */
public class CwsSplitAlgorithm extends BaseSegmentComponent implements WordSplitAlgorithm {

    /**
     * 词图上下文里的感知机切分，int 数组成对存放 offset、length。线性路径只认这份边界。
     */
    public static final String SPANS = "cws-spans";

    private final CwsPerceptron perceptron;
    private final CoreDictionary coreDictionary;

    public CwsSplitAlgorithm(CwsPerceptron perceptron, CoreDictionary coreDictionary) {
        super(LEVEL1);
        this.perceptron = perceptron;
        this.coreDictionary = coreDictionary;
    }

    @Override
    public void fill(Wordnet wordnet) {
        char[] text = wordnet.getCharArray();
        int origin = wordnet.origin();
        int window = wordnet.length();
        int[] decode = perceptron.decode(text, origin, window);
        int[] spans = new int[Math.max(window, 1) * 2];
        int spanCount = 0;

        int p = 0;
        for (int i = 0; i < decode.length; i++) {
            int x = decode[i];
            if (x == CwsPerceptron.S || x == CwsPerceptron.E) {
                spanCount = putSpan(spans, spanCount, wordnet, text, p, i - p + 1);
                p = i + 1;
            }
        }
        if (p < window) {
            spanCount = putSpan(spans, spanCount, wordnet, text, p, window - p);
        }
        wordnet.set(SPANS, spanCount == spans.length ? spans : java.util.Arrays.copyOf(spans, spanCount));
    }

    private int putSpan(int[] spans, int spanCount, Wordnet wordnet, char[] text, int offset, int length) {
        spans[spanCount++] = offset;
        spans[spanCount++] = length;
        combine(wordnet, text, offset, length);
        return spanCount;
    }

    private void combine(Wordnet wordnet, char[] text, int offset, int length) {
        Vertex vertex = wordnet.put(offset, length);
        int wordId = coreDictionary.wordId(text, wordnet.origin() + offset, length);
        if (wordId >= 0) {
            vertex.wordID = wordId;
            vertex.freq = coreDictionary.wordFreq(wordId);
        } else {
            vertex.nature = Nature.newWord;
            vertex.freq = 1;
        }
    }
}
