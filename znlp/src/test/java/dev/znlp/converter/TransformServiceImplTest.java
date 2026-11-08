package dev.znlp.converter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.Znlp;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.test.TestTags;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class TransformServiceImplTest {

    @Test
    @Tag(TestTags.HEAVY)
    void bundledServiceMatchesFacade() {
        TransformService service = createService();
        assertEquals(service.s2t("发"), Znlp.s2t("发"));
        assertEquals(service.t2s("發"), Znlp.t2s("發"));
    }

    @Test
    void longestMatchAndRoundTrip() {
        TreeMap<String, String> s2t = new TreeMap<>();
        s2t.put("软件", "軟件");
        s2t.put("开发", "開發");
        TransformDictionaryImpl dict = new TransformDictionaryImpl(s2t);

        assertEquals("軟件開發", dict.transform("软件开发"));

        TreeMap<String, String> t2s = new TreeMap<>();
        t2s.put("軟件", "软件");
        t2s.put("開發", "开发");
        TransformDictionaryImpl reverse = new TransformDictionaryImpl(t2s);
        assertEquals("软件开发", reverse.transform("軟件開發"));
    }

    @Test
    @Tag(TestTags.HEAVY)
    void emptyAndNullInputs() {
        TransformService service = createService();
        assertEquals("", service.s2t(""));
        assertNull(service.s2t((String) null));
        assertEquals("", service.t2s(new char[0]));
        assertEquals("", service.t2s((char[]) null));
    }

    private static TransformService createService() {
        ZnlpSystemConfig config = TestHelper.createTestConfig();
        NlpResourceLoader loader = new NlpResourceLoader(config);
        return new TransformServiceFactory(config, loader).create();
    }
}
