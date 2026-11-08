package dev.znlp.segment.collector;

import dev.znlp.segment.Token;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.WordTermCollector;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * 默认的分词结果收集器
 * <p>
 * 简单地将 Wordpath 路径中的每个词语（Vertex）转换为 WordTerm 并输出。
 * 如果提供了原始字符数组，则使用原始字符数组；否则使用 Wordnet 中的字符数组。
 */
public class SimpleWordTermCollector implements WordTermCollector {

    @Override
    public void collect(char[] txtChars, Wordnet wordnet, Wordpath wordPath, Consumer<Token> consumer) {
        // 确定使用的字符数组：优先使用原始字符数组，否则使用 Wordnet 中的字符数组
        // 遍历路径中的每个词语（Vertex）
        Iterator<Vertex> iterator = wordPath.iteratorVertex();
        while (iterator.hasNext()) {
            Vertex vertex = iterator.next();

            if (vertex.length() == 0) {
                continue;
            }

            String word = vertex.realWord();

            // 创建 WordTerm 对象
            Token term = new Token(word, vertex.nature);
            term.setOffset(vertex.offset());

            // 输出结果
            consumer.accept(term);
        }
    }
}