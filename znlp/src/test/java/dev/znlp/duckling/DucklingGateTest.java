package dev.znlp.duckling;

import org.junit.jupiter.api.Test;
import dev.znlp.duckling.grammar.Grammars;
import dev.znlp.duckling.grammar.duration.DurationDimension;
import dev.znlp.duckling.grammar.money.MoneyDimension;
import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.NodeValue;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Duckling 正确性与性能门禁：example 全量、词典改写后的单位，以及解析耗时上限。
 */
public class DucklingGateTest {

    private static final LocalDateTime REF = LocalDateTime.of(2019, 11, 11, 0, 0);
    private static final DucklingParser PARSER = DucklingParser.getInstance();

    @Test
    public void allGrammarExamples_stillMatch() {
        DucklingParser parser = new DucklingParser(new DefaultRuleSet(Grammars.rules()));
        List<Example> examples = Grammars.examples();
        assertTrue(examples.size() >= 140, "example 数量异常下降: " + examples.size());

        StringBuilder failMsg = new StringBuilder();
        int failed = 0;
        for (Example example : examples) {
            List<Dimension> result = parser.parse(example.text(), REF);
            boolean match = false;
            for (Dimension d : result) {
                if (example.predicate().test(d)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                failed++;
                failMsg.append("Failed: '").append(example.text()).append("' -> ").append(result).append('\n');
            }
        }
        if (failed > 0) {
            fail(failed + " examples failed\n" + failMsg);
        }
    }

    @Test
    public void dictConvertedUnits_stillParse() {
        assertInstanceOf(MoneyDimension.class, PARSER.parse("5元5角").get(0));
        assertEquals(5.5f, ((MoneyDimension) PARSER.parse("5元5角").get(0)).getValue(), 0.001f);

        assertFalse(PARSER.parse("5公里").isEmpty());
        assertFalse(PARSER.parse("25摄氏度").isEmpty());
        assertFalse(PARSER.parse("5%").isEmpty());
        assertFalse(PARSER.parse("百分之五点五").isEmpty());

        DurationDimension hour = assertInstanceOf(DurationDimension.class, PARSER.parse("一小时").get(0));
        assertEquals(Grain.Hour, hour.getGrain());

        DurationDimension minutes = assertInstanceOf(DurationDimension.class, PARSER.parse("5分钟").get(0));
        assertEquals(5.0f, minutes.getValue(), 0.001f);

        List<Dimension> festival = PARSER.parse("中秋节", REF);
        assertInstanceOf(DateTimeDimension.class, festival.get(0));
    }

    @Test
    public void typicalSentence_nodeCountStaysSmall() {
        List<DkNode<NodeValue>> nodes = PARSER.generateAll("明天下午3点见面，电话13800138000");
        assertTrue(nodes.size() < 40, "节点膨胀: " + nodes.size());
    }

    @Test
    public void parseLatency_withinBudget() {
        warmup();

        assertAvgBelow("短句", "明天下午3点见面，电话13800138000", 5.0);
        assertAvgBelow("混合实体", "我们准备明天下午三点半开会，预算五千元，路程大约5公里，大约需要1小时30分", 8.0);
        assertAvgBelow("长填充", "a".repeat(200) + "明天下午3点" + "b".repeat(200), 15.0);
        assertAvgBelow("中文数字串", "一二三四五六七八九十", 10.0);
    }

    @Test
    public void chineseDigitRun_nodeCountStaysBounded() {
        List<DkNode<NodeValue>> nodes = PARSER.generateAll("一二三四五六七八九十");
        assertTrue(nodes.size() < 80, "中文数字串节点膨胀: " + nodes.size());
    }

    private static void warmup() {
        for (int i = 0; i < 80; i++) {
            PARSER.parse("明天下午3点见面，电话13800138000", REF);
        }
    }

    private static void assertAvgBelow(String label, String text, double budgetMs) {
        int n = 200;
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            PARSER.parse(text, REF);
        }
        double avgMs = (System.nanoTime() - t0) / (n * 1_000_000.0);
        assertTrue(avgMs < budgetMs,
                label + " 平均 " + String.format("%.3f", avgMs) + "ms，超过门禁 " + budgetMs + "ms");
    }
}
