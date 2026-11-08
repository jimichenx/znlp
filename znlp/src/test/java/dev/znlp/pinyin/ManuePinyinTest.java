package dev.znlp.pinyin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.pinyin.enums.Pinyin;
import dev.znlp.test.TestTags;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PinyinService 手动测试改造为单元测试
 * 测试拼音转换功能
 *
 * @author jimichan
 */
@Tag(TestTags.HEAVY)
public class ManuePinyinTest {

    private static ServiceRegistry registry;
    private static PinyinService py;

    @BeforeAll
    public static void setUp() {
        registry = TestHelper.createTestServiceRegistry();
        py = registry.getService(PinyinService.class);
    }

    /**
     * 测试复旦大学的拼音转换
     * 预期结果: [fu4, dan4, da4, xue2]
     */
    @Test
    public void testConvertFudanUniversity() {
        PinyinResult result = py.convert("复旦大学");

        assertNotNull(result, "拼音转换结果不应为空");

        List<String> pinyinList = result.getPinyinList().stream().map(Pinyin::toString)
                .toList();
        assertNotNull(pinyinList, "拼音列表不应为空");
        assertEquals(4, pinyinList.size(), "复旦大学应该有4个拼音");

        // 验证具体拼音值
        assertEquals("fu4", pinyinList.get(0), "复的拼音应为fu4");
        assertEquals("dan4", pinyinList.get(1), "旦的拼音应为dan4");
        assertEquals("da4", pinyinList.get(2), "大的拼音应为da4");
        assertEquals("xue2", pinyinList.get(3), "学的拼音应为xue2");
    }
}
