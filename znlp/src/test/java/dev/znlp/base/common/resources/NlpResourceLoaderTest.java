package dev.znlp.base.common.resources;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.test.TestTags;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
class NlpResourceLoaderTest {

    @Test
    void initializesWithFactories() {
        ZnlpSystemConfig config = TestHelper.createTestConfig();
        NlpResourceLoader loader = new NlpResourceLoader(config);
        assertNotNull(loader);
        assertFalse(loader.getResourceFactories().isEmpty());
    }

    @Test
    void emptyPathThrows() {
        NlpResourceLoader loader = new NlpResourceLoader(TestHelper.createTestConfig());
        assertThrows(IllegalArgumentException.class, () -> loader.loadResource(""));
        assertThrows(IllegalArgumentException.class, () -> loader.loadResource("   "));
    }

    @Test
    void invalidTraversalPathThrows() {
        NlpResourceLoader loader = new NlpResourceLoader(TestHelper.createTestConfig());
        assertThrows(IllegalArgumentException.class, () -> loader.loadResource("../escape"));
    }

    @Test
    void canLoadBundledResource() {
        NlpResourceLoader loader = new NlpResourceLoader(TestHelper.createTestConfig());
        assertNotNull(loader.tryLoadResource("znlp/ner/ner.model"));
    }
}
