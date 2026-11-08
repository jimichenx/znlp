package dev.znlp.examples;

import dev.znlp.Znlp;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 默认分词（词典 + 二元 + 词性）、NER 与 Duckling 示例。
 * <p>
 * 运行：{@code ./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DefaultSegmenterExample}
 */
public class DefaultSegmenterExample {

    public static void main(String[] args) {
        String[] sentences = args.length > 0 ? args : new String[]{
                "自然语言处理技术是人工智能的重要领域",
                "南京市长江大桥",
                "这是张三的快递",
                "胜利海上油田位于渤海"
        };

        System.out.println("== default (bigram + person name + pos) ==");
        System.out.flush();
        for (String sentence : sentences) {
            print(sentence, Znlp.segment(sentence));
        }

        System.out.println("== standard + ner ==");
        WordSegmenter ner = SegmentRecipe.standard().withNer().build();
        for (String sentence : sentences) {
            print(sentence, ner.segment(sentence));
        }

        System.out.println("== duckling parse ==");
        List<Dimension> dims = Znlp.parse(
                "明天下午3点见面，电话13800138000，费用5元",
                LocalDateTime.of(2026, 8, 26, 12, 0)
        );
        for (Dimension dim : dims) {
            System.out.println("  " + dim.getKind() + " [" + dim.getText() + "] " + dim);
        }
    }

    private static void print(String sentence, Iterable<Token> tokens) {
        System.out.println(sentence);
        StringBuilder line = new StringBuilder("  -> ");
        boolean first = true;
        for (Token token : tokens) {
            if (!first) {
                line.append(" / ");
            }
            line.append(token.word);
            if (token.getNature() != null) {
                line.append("/").append(token.getNature());
            }
            first = false;
        }
        System.out.println(line);
        System.out.println();
    }
}
