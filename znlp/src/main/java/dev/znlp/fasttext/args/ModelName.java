package dev.znlp.fasttext.args;

/**
 * FastText模型类型枚举，定义了FastText支持的三种模型类型。
 * 不同的模型类型适用于不同的任务：CBOW和skipgram用于词向量训练，
 * 而supervised用于文本分类任务。
 */
public enum ModelName {
    /**
     * 连续词袋模型（Continuous Bag of Words），用于词向量训练。
     * 通过上下文词预测中心词，适合学习词的共现关系。
     */
    cbow(1),

    /**
     * skipgram模型，用于词向量训练。
     * 通过中心词预测上下文词，适合学习词的语义关系，特别是在小数据集上表现较好。
     */
    sg(2),

    /**
     * 有监督模型，用于文本分类任务。
     * 结合了词向量和n-gram特征，能够高效处理大规模文本分类问题。
     */
    sup(3);

    /**
     * 模型类型对应的整数值
     */
    public final int value;

    /**
     * 构造ModelName枚举值
     *
     * @param value 模型类型对应的整数值
     */
    ModelName(int value) {
        this.value = value;
    }

    /**
     * 根据整数值获取对应的ModelName枚举值
     *
     * @param value 模型类型对应的整数值
     * @return 对应的ModelName枚举值
     * @throws IllegalArgumentException 当输入值不存在对应的枚举值时抛出
     */
    public static ModelName fromValue(int value) {
        switch (value) {
            case 1: return cbow;
            case 2: return sg;
            case 3: return sup;
            default: throw new IllegalArgumentException("Unknown ModelName enum value: " + value);
        }
    }
}
