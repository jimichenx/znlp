package dev.znlp.segment.impl.atom;

/**
 * 模板类型枚举
 *
 * @author jimichan
 */
public enum TemplateType {
    /**
     * 时间
     */
    TIME(0),
    /**
     * 数字
     */
    NUMBER(1),
    /**
     * 数量词
     */
    MQ(2),
    /**
     * 单词
     */
    WORD(3),
    /**
     * 连接符
     */
    CONNECT(4);

    private final int value;

    TemplateType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

