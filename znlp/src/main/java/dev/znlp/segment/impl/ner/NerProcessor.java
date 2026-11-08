package dev.znlp.segment.impl.ner;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.segment.Nature;
import dev.znlp.segment.WordAndNature;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.TokenAnnotator;
import dev.znlp.segment.pipeline.WordpathProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 命名实体识别处理器。依赖词性标注结果，合并地名（ns）和机构名（nt）。
 */
public class NerProcessor extends BaseSegmentComponent implements WordpathProcessor, TokenAnnotator {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NerProcessor.class);

    private final NERPerceptron nerPerceptron;

    public NerProcessor(NERPerceptron nerPerceptron) {
        super(LEVEL5);
        this.nerPerceptron = nerPerceptron;
    }

    @Override
    public Wordpath process(Wordpath wordPath) {
        ArrayList<Vertex> vertices = new ArrayList<>();
        wordPath.iteratorVertex().forEachRemaining(vertices::add);
        if (vertices.isEmpty()) {
            return wordPath;
        }

        try {
            List<WordAndNature> sentence = new ArrayList<>(vertices);
            List<String> tags = nerPerceptron.decode(sentence);

            int from = -1;
            int length = 0;
            int n = Math.min(vertices.size(), tags.size());
            for (int i = 0; i < n; i++) {
                String tag = tags.get(i);
                Vertex vertex = vertices.get(i);

                if ("O".equals(tag) || "S".equals(tag)) {
                    from = -1;
                    length = 0;
                } else if (tag.startsWith("B-")) {
                    from = vertex.offset();
                    length = vertex.length;
                } else if (tag.startsWith("M-")) {
                    length += vertex.length;
                } else if (tag.startsWith("E-")) {
                    length += vertex.length;
                    if (from != -1) {
                        Vertex merged = wordPath.combine(from, length);
                        if (merged != null) {
                            if ("E-nt".equals(tag)) {
                                merged.nature = Nature.nt;
                            } else if ("E-ns".equals(tag)) {
                                merged.nature = Nature.ns;
                            }
                        }
                    }
                    from = -1;
                    length = 0;
                }
            }
        } catch (Exception e) {
            logger.error("NER process failed", e);
        }

        return wordPath;
    }

    @Override
    public void annotate(Wordpath path) {
        process(path);
    }

    @Override
    public boolean mayMerge() {
        return true;
    }
}
