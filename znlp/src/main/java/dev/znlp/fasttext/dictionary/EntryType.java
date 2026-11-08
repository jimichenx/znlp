package dev.znlp.fasttext.dictionary;

/**
 * 词典条目类型枚举，定义了FastText词典中条目的两种类型：词和标签。
 * 用于区分词典中的普通词和分类标签，是词典管理的重要组成部分。
 */
public enum EntryType {
    /**
     * 普通词类型，表示词典中的常规词汇
     */
    word(0),
    
    /**
     * 标签类型，表示词典中的分类标签
     */
    label(1);

    /**
     * 条目类型对应的整数值
     */
    public final int value;

    /**
     * 构造EntryType枚举值
     *
     * @param value 条目类型对应的整数值
     */
    EntryType(int value) {
        this.value = value;
    }

    /**
     * 根据整数值获取对应的EntryType枚举值
     *
     * @param value 条目类型对应的整数值
     * @return 对应的EntryType枚举值
     * @throws IllegalArgumentException 当输入值不存在对应的枚举值时抛出
     */
    public static EntryType fromValue(int value) {
        if (value == 0) return word;
        if (value == 1) return label;
        throw new IllegalArgumentException("Unknown EntryType value: " + value);
    }
}
