package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.Model;

import java.util.Random;

/**
 * 负采样损失函数实现，适用于词向量训练。
 * 通过采样负样本来替代计算所有类别的概率，显著提高训练效率，
 * 特别是在大规模词汇表场景下表现出色。
 */
public class NegativeSamplingLoss extends BinaryLogisticLoss {

    /**
     * 负采样表的大小
     */
    private static final int NEGATIVE_TABLE_SIZE = 10000000;
    
    /**
     * 每个正样本对应的负样本数量
     */
    private final int neg;
    
    /**
     * 负采样表，根据词频分布构建，用于高效采样负样本
     */
    private final IntArrayList negatives = new IntArrayList();

    /**
     * 构造NegativeSamplingLoss实例
     *
     * @param wo 输出矩阵
     * @param neg 每个正样本对应的负样本数量
     * @param targetCounts 目标类别或词的频率数组
     */
    public NegativeSamplingLoss(Matrix wo, int neg, long[] targetCounts) {
        super(wo);
        this.neg = neg;

        int nonzero = 0;
        for (long count : targetCounts) {
            if (count > 0) {
                nonzero++;
            }
        }
        if (targetCounts.length < 2 || nonzero < 2) {
            throw new IllegalArgumentException("Negative sampling requires at least 2 targets with positive count");
        }

        double z = 0.0;
        for (long count : targetCounts) {
            z += Math.pow(count, 0.5);
        }
        if (z == 0.0) {
            throw new IllegalArgumentException("Negative sampling table is empty");
        }

        for (int i = 0; i < targetCounts.length; i++) {
            double c = Math.pow(targetCounts[i], 0.5);
            int count = (int) (c * NEGATIVE_TABLE_SIZE / z);
            for (int j = 0; j < count; j++) {
                negatives.add(i);
            }
        }
        if (negatives.isEmpty()) {
            throw new IllegalArgumentException("Negative sampling table is empty");
        }
    }

    /**
     * 前向传播，计算正样本损失和负样本损失
     *
     * @param targets 目标标签ID列表
     * @param targetIndex 目标索引
     * @param state 模型状态
     * @param lr 学习率
     * @param backprop 是否进行反向传播
     * @return 总损失值
     */
    @Override
    public float forward(IntArrayList targets, int targetIndex, Model.State state, float lr, boolean backprop) {
        int target = targets.get(targetIndex);
        float loss = binaryLogistic(target, state, true, lr, backprop);

        for (int n = 0; n < neg; n++) {
            int negativeTarget = getNegative(target, state.rng);
            loss += binaryLogistic(negativeTarget, state, false, lr, backprop);
        }
        return loss;
    }

    /**
     * 从负采样表中获取一个负样本，确保不是目标样本
     *
     * @param target 目标样本索引
     * @param rng 随机数生成器
     * @return 负样本索引
     */
    private int getNegative(int target, Random rng) {
        int negative;
        do {
            negative = negatives.get(rng.nextInt(negatives.size()));
        } while (target == negative);
        return negative;
    }
}
