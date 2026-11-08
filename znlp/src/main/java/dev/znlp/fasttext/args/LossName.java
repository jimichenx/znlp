package dev.znlp.fasttext.args;

/**
 * FastText模型支持的损失函数类型枚举。
 * 不同的损失函数适用于不同的任务和数据集，
 * 选择合适的损失函数可以提高模型的性能和训练效率。
 */
public enum LossName {
    /**
     * 分层Softmax损失函数，适用于大规模分类任务，
     * 通过构建霍夫曼树来减少计算复杂度。
     */
    hs(1),
    
    /**
     * 负采样损失函数，适用于词向量训练，
     * 通过采样负样本替代计算所有类别的概率。
     */
    ns(2),
    
    /**
     * 标准Softmax损失函数，适用于小规模分类任务，
     * 计算所有类别的概率分布。
     */
    softmax(3),
    
    /**
     * 一对一分类损失函数，将多分类问题转化为多个二分类问题，
     * 适用于标签之间有重叠的场景。
     */
    ova(4);

    /**
     * 损失函数对应的整数值
     */
    public final int value;

    /**
     * 构造LossName枚举值
     *
     * @param value 损失函数对应的整数值
     */
    LossName(int value) {
        this.value = value;
    }

    /**
     * 根据整数值获取对应的LossName枚举值
     *
     * @param value 损失函数对应的整数值
     * @return 对应的LossName枚举值
     * @throws IllegalArgumentException 当输入值不存在对应的枚举值时抛出
     */
    public static LossName fromValue(int value) {
        switch (value) {
            case 1: return hs;
            case 2: return ns;
            case 3: return softmax;
            case 4: return ova;
            default: throw new IllegalArgumentException("Unknown LossName enum value: " + value);
        }
    }
}
