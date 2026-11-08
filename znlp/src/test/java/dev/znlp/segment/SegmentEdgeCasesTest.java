package dev.znlp.segment;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分词器边界输入的鲁棒性不变量测试。
 * <p>
 * 核心契约：对任意输入，token 序列的 offset 严格连续递增，
 * 词面拼接必须还原原文（空白字符允许被 SentenceCollector 过滤）。
 * 每个用例同时验证词典 Viterbi 路径（standard）和感知机线性路径（perceptron）。
 */
class SegmentEdgeCasesTest {

    private final WordSegmenter standard = SegmentRecipe.standard().build();
    private final WordSegmenter perceptron = SegmentRecipe.perceptron().build();

    // 不含空白的输入：要求词面拼接完全还原原文
    @Test
    void purePunctuationReconstructs() {
        assertFullyReconstructs("。。。！？；……——");
    }

    @Test
    void singleCharReconstructs() {
        assertFullyReconstructs("五");
        assertFullyReconstructs("的");
    }

    @Test
    void consecutiveDigitsReconstructs() {
        assertFullyReconstructs("12345678901234567890");
        assertFullyReconstructs("3.14159265358979");
    }

    @Test
    void consecutiveLettersReconstructs() {
        assertFullyReconstructs("abcdefghijklmnopqrstuvwxyz");
        assertFullyReconstructs("znlp");
    }

    @Test
    void mixedChineseAlphaNumericReconstructs() {
        assertFullyReconstructs("第2季度GDP增长7.5个百分点");
        assertFullyReconstructs("自然语言处理NLP技术2026年版");
    }

    @Test
    void fullWidthCharsReconstructs() {
        assertFullyReconstructs("１２３４５ＡＢＣｄｅｆ６７８９０");
    }

    @Test
    void surrogatePairEmojiReconstructs() {
        // emoji 是 BMP 外字符，每个占两个 char（代理对）：4 emoji + 4 汉字 = 12 char
        String text = "😀哈哈😀🎉开心🎉";
        assertEquals(12, text.length());
        assertFullyReconstructs(text);
    }

    @Test
    void rareCharsReconstructs() {
        // CJK 扩展 B 区生僻字（代理对）与常用字混排
        assertFullyReconstructs("𠀀𠀁龙龘da");
    }

    @Test
    void newlinesAreKeptAsTokens() {
        // 快照已锁定换行保留为 token（standard/broken），这里锁 offset 连续性
        String text = "你好。世界\n今天见。";
        List<Token> tokens = standard.segment(text);
        assertOffsetsContinuous(text, tokens);
        assertTrue(String.join("", tokens.stream().map(t -> t.word).toList()).contains("\n"));
    }

    @Test
    void longRepeatedTextStaysStable() {
        String text = "自然语言处理是人工智能的重要领域。".repeat(500);
        assertEquals(8500, text.length());
        assertFullyReconstructs(text);
        // 重复文本的每一段切分应一致（前后各取一段对比）
        List<String> first = standard.segmentToStrings(text.substring(0, 17));
        List<String> last = standard.segmentToStrings(text.substring(text.length() - 17));
        assertEquals(first, last);
    }

    // 含空白的输入：SentenceCollector 过滤空白 token，验证不抛异常、offset 非重叠递增、词面是原文子串
    @Test
    void whitespaceInputKeepsConsistentOffsets() {
        String text = "你好 世界\t自然 语言";
        for (WordSegmenter segmenter : List.of(standard, perceptron)) {
            List<Token> tokens = segmenter.segment(text);
            assertFalse(tokens.isEmpty());
            int prevEnd = -1;
            StringBuilder joined = new StringBuilder();
            for (Token token : tokens) {
                assertTrue(token.offset > prevEnd, "overlapping token: " + token);
                prevEnd = token.offset + token.length() - 1;
                assertTrue(text.regionMatches(token.offset, token.word, 0, token.word.length()),
                        "token not a substring at offset: " + token.word + "@" + token.offset);
                joined.append(token.word);
            }
            assertTrue(text.contains(joined.toString()));
        }
    }

    @Test
    void emptyAndNullInputs() {
        assertTrue(standard.segment("").isEmpty());
        assertTrue(perceptron.segment("").isEmpty());
        // WordSegmenter 接口契约：null 宽容处理返回空列表（Znlp 门面才是抛 IllegalArgumentException 的一层）
        assertTrue(standard.segment(null).isEmpty());
        assertTrue(standard.segmentToStrings(null).isEmpty());
        assertTrue(perceptron.segmentToStrings(null).isEmpty());
    }

    private void assertFullyReconstructs(String text) {
        assertFullyReconstructs(standard, text);
        assertFullyReconstructs(perceptron, text);
    }

    private void assertFullyReconstructs(WordSegmenter segmenter, String text) {
        List<Token> tokens = segmenter.segment(text);
        assertOffsetsContinuous(text, tokens);
        assertEquals(text, joinWords(tokens), "tokens must reconstruct input for segmenter "
                + segmenter.getClass().getSimpleName());
    }

    private static void assertOffsetsContinuous(String text, List<Token> tokens) {
        int offset = 0;
        for (Token token : tokens) {
            assertEquals(offset, token.offset, "offset gap at token " + token);
            offset += token.word.length();
        }
        assertEquals(text.length(), offset, "total length mismatch");
    }

    private static String joinWords(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append(token.word);
        }
        return sb.toString();
    }
}
