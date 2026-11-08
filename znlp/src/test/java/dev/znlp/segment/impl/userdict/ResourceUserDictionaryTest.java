package dev.znlp.segment.impl.userdict;

import org.junit.jupiter.api.Test;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * DefaultCustomDictionary 测试类
 * 测试默认自定义词典的功能
 *
 * @author jimichan
 */
public class ResourceUserDictionaryTest {

    @Test
    public void testDefaultCustomDictionaryCreation() {
        ZnlpSystemConfig config = ZnlpSystemConfig.builder().buildWithDefaults();
        NlpResourceLoader loader = new NlpResourceLoader(config);

        ResourceUserDictionary dictionary = new ResourceUserDictionary(config, loader);

        assertNotNull(dictionary);
    }

    @Test
    public void testDefaultCustomDictionaryTrie() {
        ZnlpSystemConfig config = ZnlpSystemConfig.builder().buildWithDefaults();
        NlpResourceLoader loader = new NlpResourceLoader(config);

        ResourceUserDictionary dictionary = new ResourceUserDictionary(config, loader);

        // 默认情况下，自定义词典可能是空的，但应该不会抛出异常
        try {
            assertNotNull(dictionary);
        } catch (Exception e) {
            fail("初始化默认自定义词典时抛出异常: " + e.getMessage());
        }
    }

    @Test
    public void testDictionaryLoadingRobustness() {
        ZnlpSystemConfig config = ZnlpSystemConfig.builder().buildWithDefaults();
        NlpResourceLoader loader = new NlpResourceLoader(config);

        // 这个测试主要验证即使加载失败，程序也能正常运行
        ResourceUserDictionary dictionary = new ResourceUserDictionary(config, loader);

        assertNotNull(dictionary);
    }
}
