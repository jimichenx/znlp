package dev.znlp.segment.collector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link DefaultSubwordRuleDict}：规则在 add 时即生效，无需额外的构建步骤。
 */
class DefaultSubwordRuleDictTest {

    @Test
    void exactRuleAppliesImmediately() {
        DefaultSubwordRuleDict dict = new DefaultSubwordRuleDict();
        dict.add("北京/大学");

        assertArrayEquals(new int[]{2, 2}, dict.match("北京大学"));
        assertNull(dict.match("北京"));
    }

    @Test
    void wildcardMatchesExactlyOneChar() {
        DefaultSubwordRuleDict dict = new DefaultSubwordRuleDict();
        dict.add("中/?国");

        assertArrayEquals(new int[]{1, 2}, dict.match("中华国"));
        assertArrayEquals(new int[]{1, 2}, dict.match("中美国"));
        // 少一个字就不匹配：切分长度的和必须等于词长，否则下游 substring 越界
        assertNull(dict.match("中国"));
    }

    @Test
    void separatorAndStarAreNormalized() {
        DefaultSubwordRuleDict dict = new DefaultSubwordRuleDict();
        dict.addAll(List.of("北京\\大学", "北京/*大学"));

        // 反斜杠等价于斜杠
        assertArrayEquals(new int[]{2, 2}, dict.match("北京大学"));
        // * 等价于 ?，占一个长度的通配位
        assertArrayEquals(new int[]{2, 3}, dict.match("北京A大学"));
    }

    @Test
    void wildcardAtBothEndsIsRejected() {
        DefaultSubwordRuleDict dict = new DefaultSubwordRuleDict();

        assertThrows(IllegalArgumentException.class, () -> dict.add("?国?"));
    }

    @Test
    void clearRemovesAllRules() {
        DefaultSubwordRuleDict dict = new DefaultSubwordRuleDict();
        dict.add("北京/大学");
        dict.add("中/?国");

        dict.clear();

        assertNull(dict.match("北京大学"));
        assertNull(dict.match("中华国"));
    }

    @Test
    void latterRuleOverridesSameWordForm() {
        DefaultSubwordRuleDict dict = new DefaultSubwordRuleDict();
        dict.add("北京/大学");
        dict.add("北京大学");

        // 同词面后加入的规则生效：只输出一段，表示整体不切分
        assertArrayEquals(new int[]{4}, dict.match("北京大学"));
    }
}
