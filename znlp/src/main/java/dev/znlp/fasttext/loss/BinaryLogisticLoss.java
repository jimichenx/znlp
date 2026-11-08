package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.fasttext.Model;

/**
 * 二元逻辑回归损失抽象基类，适用于二分类任务或多标签分类任务。
 * 实现了二元逻辑回归的核心计算逻辑，作为负采样损失和一对一损失的基类。
 */
public abstract class BinaryLogisticLoss extends Loss {

    /**
     * 构造BinaryLogisticLoss实例
     *
     * @param wo 输出矩阵
     */
    public BinaryLogisticLoss(Matrix wo) {
        super(wo);
    }

    /**
     * 计算二元逻辑回归损失并更新梯度
     *
     * @param target 目标索引
     * @param state 模型状态
     * @param labelIsPositive 标签是否为正例
     * @param lr 学习率
     * @param backprop 是否进行反向传播
     * @return 损失值
     */
    public float binaryLogistic(int target, Model.State state, boolean labelIsPositive, float lr, boolean backprop) {
        float score = sigmoid(wo.dotRow(state.hidden, target));
        if (backprop) {
            float alpha = lr * ((labelIsPositive ? 1.0f : 0.0f) - score);
            state.grad.addRow(wo, target, alpha);
            wo.addVectorToRow(state.hidden, target, alpha);
        }

        if (labelIsPositive) {
            return -log(score);
        } else {
            return -log(1.0f - score);
        }
    }

    /**
     * 计算输出向量，应用sigmoid激活函数
     *
     * @param state 模型状态
     */
    @Override
    public void computeOutput(Model.State state) {
        Vector output = state.output;
        output.mul(wo, state.hidden);

        int osz = output.length();
        for (int i = 0; i < osz; i++) {
            output.set(i, sigmoid(output.get(i)));
        }
    }
}
