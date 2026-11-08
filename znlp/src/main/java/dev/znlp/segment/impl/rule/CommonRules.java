package dev.znlp.segment.impl.rule;

import dev.znlp.segment.Nature;

/**
 * 常用预制分词规则
 */
public enum CommonRules {

    /**
     * 网址 URL
     */
    URL("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", Nature.nx, 100),

    /**
     * 电子邮箱
     */
    EMAIL("[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?", Nature.nx, 100),

    /**
     * 中国大陆手机号 (简单校验：1开头，11位数字)
     */
    MOBILE("1[3-9]\\d{9}", Nature.nx, 100),

    /**
     * 中国大陆身份证号 (18位，末位可能是X)
     */
    ID_CARD("\\d{17}[\\dXx]|\\d{15}", Nature.nx, 100),

    /**
     * IPv4 地址
     */
    IP_ADDR("((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}", Nature.nx, 100);

    private final String regex;
    private final Nature nature;
    private final int priority;

    CommonRules(String regex, Nature nature, int priority) {
        this.regex = regex;
        this.nature = nature;
        this.priority = priority;
    }

    public String getRegex() {
        return regex;
    }

    public Nature getNature() {
        return nature;
    }

    public int getPriority() {
        return priority;
    }
    
    /**
     * 转换为 Rule 对象
     */
    public Rule toRule() {
        return new Rule(regex, nature, priority);
    }
}
