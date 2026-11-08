package dev.znlp.highlight;

import org.junit.jupiter.api.Test;
import dev.znlp.ZnlpSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SmartHighlighter 测试
 */
public class SmartHighlighterTest {

    private SmartHighlighter getHighlighter() {
        return ZnlpSystem.getInstance().getService(SmartHighlighter.class);
    }

    @Test
    public void testChineseMatch() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig();

        String text = "理财菜单";
        String query = "理财";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<em>理财</em>菜单", result);

        text = "这里有理财";
        result = highlighter.highlight(text, query, config);
        assertEquals("这里有<em>理财</em>", result);
    }

    @Test
    public void testDiscontinuousChineseMatch() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig();

        String text = "理A财";
        String query = "理财";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<em>理</em>A<em>财</em>", result);
    }

    @Test
    public void testIntersectionMatch() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig();

        String text = "理财菜单";
        String query = "理财的秘密";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<em>理财</em>菜单", result);
    }

    @Test
    public void testPinyinMatch() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig();

        String text = "理财产品";
        String query = "licai";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<em>理财</em>产品", result);
    }

    @Test
    public void testDiscontinuousPinyinMatch() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig();

        String text = "理A财";
        String query = "licai";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<em>理</em>A<em>财</em>", result);
    }

    @Test
    public void testMixedMatch() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig();

        String text = "Python的教程";
        String query = "Python教程";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<em>Python</em>的<em>教程</em>", result);
    }

    @Test
    public void testConfigOverride() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig().withEnablePinyin(false);

        String text = "理财";
        String query = "licai";
        String result = highlighter.highlight(text, query, config);
        assertEquals("理财", result);
    }
    
    @Test
    public void testCustomTag() {
        SmartHighlighter highlighter = getHighlighter();
        SmartHighlightConfig config = new SmartHighlightConfig().withHighlightTag("b");

        String text = "理财";
        String query = "理财";
        String result = highlighter.highlight(text, query, config);
        assertEquals("<b>理财</b>", result);
    }
}
