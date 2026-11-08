package dev.znlp.pinyin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.TestHelper;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.pinyin.enums.Pinyin;
import dev.znlp.test.TestTags;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 拼音按词典最长词切分。自定义词覆盖同名词条；音节个数和词长不一致时，
 * 游标按音节个数前进，这是现有转换的规则。
 */
@Tag(TestTags.HEAVY)
class PinyinLongestMatchTest {

    @Test
    void longestCustomWordBeatsItsPrefix() {
        PinyinService service = service();
        CustomPinyin custom = service.getCustomPinyin();
        custom.put("甲乙丙", "jia3,yi3,bing1");
        custom.put("甲乙", "jia1,yi1");
        custom.put("乙丙", "yi4,bing1");
        service.rebuild();

        assertEquals(List.of("jia3", "yi3", "bing1"), tones(service, "甲乙丙"));
    }

    @Test
    void shorterWordIsKeptWhenNothingLongerStartsThere() {
        PinyinService service = service();
        CustomPinyin custom = service.getCustomPinyin();
        custom.put("甲乙", "jia1,yi1");
        custom.put("乙丙", "yi4,bing1");
        service.rebuild();

        assertEquals(List.of("jia1", "yi1", "bing3"), tones(service, "甲乙丙"));
    }

    @Test
    void syllableCountNotKeyLengthAdvancesBySyllables() {
        PinyinService service = service();
        CustomPinyin custom = service.getCustomPinyin();
        custom.put("甲乙丙", "jia3,yi3");
        custom.put("乙丙", "yi4,bing1");
        service.rebuild();

        assertEquals(List.of("jia3", "yi3", "bing3"), tones(service, "甲乙丙"));
    }

    @Test
    void shorterWordWithMoreSyllablesBlocksLongerKey() {
        PinyinService service = service();
        CustomPinyin custom = service.getCustomPinyin();
        custom.put("甲乙", "jia1,yi1,bing1,bing3");
        custom.put("甲乙丙", "jia3,yi3,bing1");
        service.rebuild();

        assertEquals(List.of("jia1", "yi1", "bing1", "bing3"), tones(service, "甲乙丙"));
    }

    @Test
    void unmatchedCharactersStayNone() {
        PinyinService service = service();
        CustomPinyin custom = service.getCustomPinyin();
        custom.put("甲乙", "jia1,yi1");
        service.rebuild();

        assertEquals(List.of("none5", "jia1", "yi1", "none5"), tones(service, "x甲乙y"));
    }

    private static PinyinService service() {
        ServiceRegistry registry = TestHelper.createTestServiceRegistry();
        return registry.getService(PinyinService.class);
    }

    private static List<String> tones(PinyinService service, String text) {
        return service.convert(text).getPinyinList().stream().map(Pinyin::toString).toList();
    }
}
