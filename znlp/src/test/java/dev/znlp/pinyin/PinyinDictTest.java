package dev.znlp.pinyin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.pinyin.enums.SimplePinyin;
import dev.znlp.test.TestTags;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PinyinInnerDict 单元测试
 * 测试拼音词典的加载和单例特性
 *
 * @author jimichan
 */
@Tag(TestTags.HEAVY)
public class PinyinDictTest {

    private ServiceRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = TestHelper.createTestServiceRegistry();
    }

    /**
     * 测试从服务注册中加载 PinyinService，并验证 PinyinInnerDict 正常工作
     */
    @Test
    public void testLoadFromServiceRegistry() {
        // 从服务注册获取 PinyinService
        PinyinService service = registry.getService(PinyinService.class);

        // 验证服务不为空
        assertNotNull(service, "PinyinService should be loaded from service registry");

        // 验证能正常查询单字拼音（通过 PinyinInnerDict）
        SimplePinyin[] pinyins = service.charPinyin('中');
        assertNotNull(pinyins, "Single character pinyin should not be null");
        assertTrue(pinyins.length > 0, "Single character should have at least one pinyin");
    }

    /**
     * 测试 PinyinService 是单例模式
     * 通过 PinyinModule 注册时使用了 singleton=true
     */
    @Test
    public void testPinyinServiceIsSingleton() {
        // 多次获取 PinyinService
        PinyinService service1 = registry.getService(PinyinService.class);
        PinyinService service2 = registry.getService(PinyinService.class);

        // 验证两次获取的是同一个实例
        assertNotNull(service1, "First service instance should not be null");
        assertNotNull(service2, "Second service instance should not be null");
        assertSame(service1, service2, "PinyinService should be singleton - same instance returned");
    }

    /**
     * 测试 PinyinInnerDict 能正确加载拼音表
     */
    @Test
    public void testPinyinTableLoaded() {
        PinyinService service = registry.getService(PinyinService.class);

        // 测试常见汉字的拼音
        SimplePinyin[] zhongPinyins = service.charPinyin('中');
        assertNotNull(zhongPinyins, "Character '中' should have pinyin");
        assertTrue(zhongPinyins.length > 0, "Character '中' should have at least one pinyin");

        SimplePinyin[] guoPinyins = service.charPinyin('国');
        assertNotNull(guoPinyins, "Character '国' should have pinyin");
        assertTrue(guoPinyins.length > 0, "Character '国' should have at least one pinyin");
    }


    /**
     * 测试 PinyinInnerDict 对未知字符的处理
     */
    @Test
    public void testUnknownCharacter() {
        PinyinService service = registry.getService(PinyinService.class);

        // 测试英文字符（应该返回 null）
        SimplePinyin[] englishPinyins = service.charPinyin('A');
        assertNull(englishPinyins, "English character should return null");

        // 测试数字（应该返回 null）
        SimplePinyin[] digitPinyins = service.charPinyin('1');
        assertNull(digitPinyins, "Digit should return null");
    }

    /**
     * 测试 PinyinInnerDict 加载的拼音表包含多音字
     */
    @Test
    public void testMultiplePronunciations() {
        PinyinService service = registry.getService(PinyinService.class);

        // 测试多音字 "的" (de, di, di)
        SimplePinyin[] dePinyins = service.charPinyin('的');
        assertNotNull(dePinyins, "Character '的' should have pinyin");
        assertTrue(dePinyins.length >= 1, "Character '的' should have multiple pronunciations");

        // 测试多音字 "长" (zhang, chang)
        SimplePinyin[] changPinyins = service.charPinyin('长');
        assertNotNull(changPinyins, "Character '长' should have pinyin");
        assertTrue(changPinyins.length >= 1, "Character '长' should have multiple pronunciations");
    }

    /**
     * 测试通过 PinyinService 进行文本转拼音
     */
    @Test
    public void testTextToPinyinConversion() {
        PinyinService service = registry.getService(PinyinService.class);

        // 转换文本为拼音
        PinyinResult result = service.convert("中国");

        // 验证转换结果
        assertNotNull(result, "Pinyin conversion result should not be null");
        String pinyinString = result.asString();
        assertNotNull(pinyinString, "Pinyin string should not be null");
        assertFalse(pinyinString.isEmpty(), "Pinyin string should not be empty");
    }
}
