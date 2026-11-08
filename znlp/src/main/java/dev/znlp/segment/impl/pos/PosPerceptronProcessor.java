package dev.znlp.segment.impl.pos;

import dev.znlp.segment.Nature;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.TokenAnnotator;
import dev.znlp.segment.pipeline.WordpathProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 感知机词性标注处理器。在最优路径确定后为每个词写入词性。
 */
public class PosPerceptronProcessor extends BaseSegmentComponent implements WordpathProcessor, TokenAnnotator {

    private final PosPerceptron posPerceptron;

    public PosPerceptronProcessor(PosPerceptron posPerceptron) {
        super(LEVEL4);
        this.posPerceptron = posPerceptron;
    }

    @Override
    public Wordpath process(Wordpath wordPath) {
        ArrayList<Vertex> vertices = new ArrayList<>();
        wordPath.iteratorVertex().forEachRemaining(vertices::add);
        if (vertices.isEmpty()) {
            return wordPath;
        }

        char[] text = wordPath.getWordnet().getCharArray();
        int origin = wordPath.getWordnet().origin();
        List<String> words = new ArrayList<>(vertices.size());
        for (Vertex vertex : vertices) {
            String word = new String(text, origin + vertex.offset(), vertex.length());
            vertex.rememberSurface(word);
            words.add(word);
        }

        List<String> tags = posPerceptron.decode(words);
        int n = Math.min(vertices.size(), tags.size());
        for (int i = 0; i < n; i++) {
            Vertex vertex = vertices.get(i);
            Nature nature = Nature.parse(tags.get(i));

            // 普通词被标成人名且长度大于 3（如「基础设施/nr」）时，降为名词
            if (Nature.nr.equals(nature) && vertex.nature == null && vertex.length > 3) {
                vertex.nature = Nature.n;
                continue;
            }
            // 词典词性优先；人名识别的 nr / 新词可以被词性模型覆盖
            if (vertex.nature == null
                    || vertex.nature == Nature.newWord
                    || vertex.nature == Nature.nr) {
                vertex.nature = nature;
            }
        }

        return wordPath;
    }

    @Override
    public void annotate(Wordpath path) {
        process(path);
    }
}
