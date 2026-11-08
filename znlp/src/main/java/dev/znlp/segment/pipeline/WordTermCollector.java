package dev.znlp.segment.pipeline;

import dev.znlp.segment.Token;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

import java.util.function.Consumer;

public interface WordTermCollector {

    /**
     * 收集分词结果，最终发送到consumer中。
     * 这样外面是流水线还是list保存结果，由外部决定。
     *
     * @param txtChars 词图
     * @param wordnet  词图
     * @param wordPath 最后的WordPath路径
     * @param consumer 接受WordTerm的消费者
     */
    void collect(char[] txtChars, Wordnet wordnet, Wordpath wordPath, Consumer<Token> consumer);

}