package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.Model;

/**
 * 一对一分类损失函数实现，适用于多标签分类任务。
 * 将多分类问题转化为多个二分类问题，对每个类别训练一个独立的分类器，
 * 适合标签之间有重叠的场景。
 */
public class OneVsAllLoss extends BinaryLogisticLoss {

    /**
     * 构造OneVsAllLoss实例
     *
     * @param wo 输出矩阵
     */
    public OneVsAllLoss(Matrix wo) {
        super(wo);
    }

    /**
     * 前向传播，对每个类别计算二元逻辑回归损失
     *
     * @param targets 目标标签ID列表
     * @param targetIndex 目标索引（在此损失函数中未使用）
     * @param state 模型状态
     * @param lr 学习率
     * @param backprop 是否进行反向传播
     * @return 总损失值
     */
    @Override
    public float forward(IntArrayList targets, int targetIndex, Model.State state, float lr, boolean backprop) {
        float loss = 0f;
        int osz = state.output.length();
        for (int i = 0; i < osz; i++) {
            boolean isMatch = targets.indexOf(i) != -1;
            loss += binaryLogistic(i, state, isMatch, lr, backprop);
        }
        return loss;
    }
}
