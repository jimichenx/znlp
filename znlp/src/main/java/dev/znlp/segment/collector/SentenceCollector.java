package dev.znlp.segment.collector;

import dev.znlp.base.common.StringUtils;
import dev.znlp.segment.Token;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.WordTermCollector;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * WordTermCollector的默认实现，从各种数据结构中收集和生成词序列
 *
 * @author jimichan
 */
public class SentenceCollector implements WordTermCollector {

    private final List<SubwordComputer> subwordComputer;
    private final List<SubwordInfoSetup> setupList;

    public SentenceCollector() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public SentenceCollector(List<SubwordComputer> subwordComputer, List<SubwordInfoSetup> setupList) {
        this.subwordComputer = subwordComputer;
        this.setupList = setupList;
    }

    @Override
    public void collect(char[] txtChars, Wordnet wordnet, Wordpath wordPath, Consumer<Token> consumer) {

        Iterator<Vertex> vertexIterator = wordPath.iteratorVertex();

        for (SubwordInfoSetup setup : setupList) {
            setup.fill(wordnet, wordPath);
        }

        while (vertexIterator.hasNext()) {
            Vertex vertex = vertexIterator.next();

            String word = vertex.realWord();

            Token term = new Token(word, vertex.nature, vertex.offset());

            if (StringUtils.isWhiteSpace(term.word)) {
                continue;
            }

            // 如果运行成功，后面的就不运行了
            for (SubwordComputer computer : subwordComputer) {
                if (computer.run(term, wordnet, wordPath)) {
                    break;
                }
            }

            consumer.accept(term);
        }
    }
}

