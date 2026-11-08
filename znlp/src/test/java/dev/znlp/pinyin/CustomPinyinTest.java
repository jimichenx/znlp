package dev.znlp.pinyin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.test.TestTags;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestTags.HEAVY)
class CustomPinyinTest {

    @Test
    void customMappingOverridesDefault() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        PinyinService service = registry.getService(PinyinService.class);

        CustomPinyin custom = service.getCustomPinyin();
        custom.put("重庆", "chong2,qing4");
        service.rebuild();

        assertEquals("chong qing", service.convert("重庆").asString());
    }

    @Test
    void removeCustomMappingRestoresDefault() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        PinyinService service = registry.getService(PinyinService.class);
        CustomPinyin custom = service.getCustomPinyin();
        String before = service.convert("测试").asString();

        custom.put("测试", "ce4,shi4");
        service.rebuild();
        assertEquals("ce shi", service.convert("测试").asString());

        custom.remove("测试");
        service.rebuild();
        assertEquals(before, service.convert("测试").asString());
    }
}
