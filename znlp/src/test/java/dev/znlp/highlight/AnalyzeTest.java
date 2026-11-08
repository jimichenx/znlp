package dev.znlp.highlight;

import org.junit.jupiter.api.Test;
import dev.znlp.Znlp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnalyzeTest {

    @Test
    public void testAnalyzeBasic() {
        String text = "理财产品";
        String query = "理财";
        List<Fragment> fragments = Znlp.highlightFragments(text, query);
        
        assertEquals(2, fragments.size());
        assertEquals("理财", fragments.get(0).text());
        assertTrue(fragments.get(0).highlighted());
        assertEquals("产品", fragments.get(1).text());
        assertFalse(fragments.get(1).highlighted());
    }

    @Test
    public void testAnalyzePinyin() {
        String text = "理财产品";
        String query = "licai";
        List<Fragment> fragments = Znlp.highlightFragments(text, query);
        
        assertEquals(2, fragments.size());
        assertEquals("理财", fragments.get(0).text());
        assertTrue(fragments.get(0).highlighted());
        assertEquals("产品", fragments.get(1).text());
        assertFalse(fragments.get(1).highlighted());
    }

    @Test
    public void testAnalyzeDiscontinuous() {
        String text = "理A财";
        String query = "理财";
        List<Fragment> fragments = Znlp.highlightFragments(text, query);
        
        assertEquals(3, fragments.size());
        assertEquals("理", fragments.get(0).text());
        assertTrue(fragments.get(0).highlighted());
        assertEquals("A", fragments.get(1).text());
        assertFalse(fragments.get(1).highlighted());
        assertEquals("财", fragments.get(2).text());
        assertTrue(fragments.get(2).highlighted());
    }
}
