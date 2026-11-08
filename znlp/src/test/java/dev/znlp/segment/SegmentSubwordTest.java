package dev.znlp.segment;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * indexTerms() 索引子词测试。
 * <p>
 * 主切分不变的前提下，长词（>=3 字）应附带词典中可见的子词组合，
 * 例如 北京大学 => 北京 / 大学。子词 offset 必须落在父词范围内，
 * 且词面与父词对应位置严格一致。
 */
class SegmentSubwordTest {

    private final WordSegmenter segmenter = SegmentRecipe.standard().indexTerms().build();

    @Test
    void longWordsCarrySubwords() {
        List<Token> tokens = segmenter.segment("北京市北京大学和南京市长江大桥");
        assertTrue(tokens.stream().anyMatch(Token::hasSubword),
                "at least one token should carry subwords");
        assertSubwordsConsistent(tokens);
    }

    @Test
    void subwordsAreDictionaryWordsInsideParent() {
        // 北京大学 是常见长词；无论主切分是否合并它，出现的任何长词的子词都必须与父词对齐
        List<Token> tokens = segmenter.segment("北京大学");
        assertSubwordsConsistent(tokens);

        // 长词至少存在子词候选（北京/大学 都在核心词典中）
        boolean hasSubword = tokens.stream()
                .filter(t -> t.length() >= 3)
                .anyMatch(Token::hasSubword);
        if (tokens.stream().anyMatch(t -> t.length() >= 3)) {
            assertTrue(hasSubword, "word >= 3 chars should have subword candidates");
        }
    }

    @Test
    void mainSegmentationUnchangedByIndexTerms() {
        // indexTerms 只附加子词，不能改变主切分
        WordSegmenter plain = SegmentRecipe.standard().build();
        String text = "南京市长江大桥";
        assertEquals(plain.segmentToStrings(text), segmenter.segmentToStrings(text));
    }

    @Test
    void subwordOffsetsStayInsideParent() {
        String text = "人工智能与集成电路是重点方向";
        List<Token> tokens = segmenter.segment(text);
        assertSubwordsConsistent(tokens);
    }

    /**
     * 子词一致性不变量：每个子词的 offset 在 [parent.offset, parent.offset+parent.length) 内，
     * 词面等于父词从 (sub.offset - parent.offset) 开始、长度为 sub.length 的子串。
     */
    private static void assertSubwordsConsistent(List<Token> tokens) {
        for (Token token : tokens) {
            if (!token.hasSubword()) {
                continue;
            }
            for (Token sub : token.getSubword()) {
                assertTrue(sub.offset >= token.offset,
                        "subword before parent: " + sub + " in " + token);
                assertTrue(sub.offset + sub.length() <= token.offset + token.length(),
                        "subword crosses parent boundary: " + sub + " in " + token);
                String expected = token.word.substring(
                        sub.offset - token.offset,
                        sub.offset - token.offset + sub.length());
                assertEquals(expected, sub.word, "subword text mismatch");
            }
        }
    }
}
