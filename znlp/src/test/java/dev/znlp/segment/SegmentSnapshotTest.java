package dev.znlp.segment;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.impl.correction.MemCorrectionDictionary;
import dev.znlp.segment.impl.rule.Rule;
import dev.znlp.segment.impl.rule.RuleManager;
import dev.znlp.segment.impl.userdict.MemUserDictionary;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 冻结优化前的切分。词、词性、offset 必须与资源文件一致。
 */
class SegmentSnapshotTest {

    static final String SHORT = "南京市长江大桥";
    static final String TYPICAL = "自然语言处理技术是人工智能的重要领域";
    static final String LONG = """
            上海市浦东新区近年来持续推进科技创新和产业发展，人工智能、集成电路与生物医药成为重点方向。
            研究人员认为，中文分词质量会直接影响搜索、推荐和问答系统的召回效果。
            例如「乒乓球拍卖完了」「美国会通过这项法案」这类歧义句，词典路径与感知机路径往往给出不同切分。
            工程上还要同时考虑吞吐量：线上接口通常要求毫秒级延迟，批量离线任务则更看重每秒处理字数。
            """.replace("\n", "");
    static final String BROKEN = "你好。世界\n今天见。";
    static final String MIXED = "请发邮件到test@example.com，电话13800138000。";

    private static final String RESOURCE = "/dev/znlp/segment/segment-snapshot.txt";

    @Test
    void snapshotMatchesLockedOutput() throws IOException {
        assertEquals(readResource(), render());
    }

    @Test
    void sharedSegmenterAgreesAcrossThreads() throws Exception {
        List<Case> cases = cases();
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            List<Future<String>> futures = new ArrayList<>();
            for (int round = 0; round < 8; round++) {
                for (int i = 0; i < cases.size(); i++) {
                    int index = i;
                    futures.add(pool.submit(() -> format(cases.get(index).segmenter, cases.get(index).text)));
                }
            }
            for (int i = 0; i < futures.size(); i++) {
                Case item = cases.get(i % cases.size());
                assertEquals(format(item.segmenter, item.text), futures.get(i).get());
            }
        }
    }

    static String render() {
        StringBuilder out = new StringBuilder();
        for (Case item : cases()) {
            out.append("== ").append(item.name).append('\n');
            out.append(format(item.segmenter, item.text)).append('\n');
        }
        return out.toString();
    }

    private static String format(WordSegmenter segmenter, String text) {
        StringBuilder line = new StringBuilder();
        List<Token> tokens = segmenter.segment(text);
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                line.append(' ');
            }
            Token token = tokens.get(i);
            line.append(escape(token.word))
                    .append('/')
                    .append(token.getNatureString())
                    .append('@')
                    .append(token.getOffset());
            if (token.hasSubword()) {
                line.append('{');
                List<Token> subwords = token.getSubword();
                for (int s = 0; s < subwords.size(); s++) {
                    if (s > 0) {
                        line.append(' ');
                    }
                    Token sub = subwords.get(s);
                    line.append(escape(sub.word)).append('/').append(sub.getOffset());
                }
                line.append('}');
            }
        }
        return line.toString();
    }

    private static String escape(String word) {
        return word.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String readResource() throws IOException {
        try (InputStream in = SegmentSnapshotTest.class.getResourceAsStream(RESOURCE)) {
            assertNotNull(in, "missing " + RESOURCE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<Case> cases() {
        MemUserDictionary dict = new MemUserDictionary();
        dict.addWord("奥利奥饼干");
        RuleManager rules = new RuleManager();
        rules.addRule(new Rule("订单\\d+", Nature.n, 100));
        MemCorrectionDictionary corrections = new MemCorrectionDictionary();
        corrections.addRule("乒乓球拍/卖/完了");

        List<Case> cases = new ArrayList<>();
        cases.add(new Case("standard/short", SegmentRecipe.standard().build(), SHORT));
        cases.add(new Case("standard/typical", SegmentRecipe.standard().build(), TYPICAL));
        cases.add(new Case("standard/long", SegmentRecipe.standard().build(), LONG));
        cases.add(new Case("standard/broken", SegmentRecipe.standard().build(), BROKEN));
        cases.add(new Case("standard/mixed", SegmentRecipe.standard().build(), MIXED));
        cases.add(new Case("dictionary/short", SegmentRecipe.dictionary().build(), SHORT));
        cases.add(new Case("dictionary/long", SegmentRecipe.dictionary().build(), LONG));
        cases.add(new Case("perceptron/short", SegmentRecipe.perceptron().build(), SHORT));
        cases.add(new Case("perceptron/typical", SegmentRecipe.perceptron().build(), TYPICAL));
        cases.add(new Case("perceptron/long", SegmentRecipe.perceptron().build(), LONG));
        cases.add(new Case("user/oreo", SegmentRecipe.standard().withUserDictionary(dict).build(), "奥利奥饼干很好吃。再来一盒。"));
        cases.add(new Case("rule/order", SegmentRecipe.dictionary().withRules(rules).build(), "请处理订单123456和特殊词。"));
        cases.add(new Case("ner/beida", SegmentRecipe.standard().withNer().build(), "北京大学在北京海淀区中关村"));
        cases.add(new Case("correction/pingpong", SegmentRecipe.dictionary().withCorrections(corrections).build(), "乒乓球拍卖完了"));
        cases.add(new Case("index/short", SegmentRecipe.standard().indexTerms().build(), SHORT));
        cases.add(new Case("index/typical", SegmentRecipe.standard().indexTerms().build(), TYPICAL));
        return cases;
    }

    private record Case(String name, WordSegmenter segmenter, String text) {
    }

}
