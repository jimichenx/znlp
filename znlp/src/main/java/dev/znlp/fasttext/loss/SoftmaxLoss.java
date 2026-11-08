package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.Model;

/**
 * 标准Softmax损失函数实现，适用于小规模分类任务。
 * 计算所有类别的概率分布，并通过交叉熵损失优化模型参数。
 */
public class SoftmaxLoss extends Loss {

    /**
     * 构造SoftmaxLoss实例
     *
     * @param wo 输出矩阵
     */
    public SoftmaxLoss(Matrix wo) {
        super(wo);
    }

    /**
     * 计算输出向量，应用Softmax激活函数将输出转换为概率分布
     *
     * @param state 模型状态
     */
    @Override
    public void computeOutput(Model.State state) {
        Vector output = state.output;
        output.mul(wo, state.hidden);

        float max = output.get(0);
        float z = 0.0f;
        int osz = output.length();

        for (int i = 0; i < osz; i++) {
            max = Math.max(output.get(i), max);
        }

        for (int i = 0; i < osz; i++) {
            float val = (float) Math.exp(output.get(i) - max);
            output.set(i, val);
            z += val;
        }

        for (int i = 0; i < osz; i++) {
            output.set(i, output.get(i) / z);
        }
    }

    /**
     * 前向传播，计算损失值并更新梯度
     *
     * @param targets 目标标签ID列表
     * @param targetIndex 目标索引
     * @param state 模型状态
     * @param lr 学习率
     * @param backprop 是否进行反向传播
     * @return 损失值
     */
    @Override
    public float forward(IntArrayList targets, int targetIndex, Model.State state, float lr, boolean backprop) {
        computeOutput(state);

        int target = targets.get(targetIndex);
        if (backprop) {
            int osz = wo.getRow();
            for (int i = 0; i < osz; i++) {
                float label = (i == target) ? 1.0f : 0.0f;
                float alpha = lr * (label - state.output.get(i));
                state.grad.addRow(wo, i, alpha);
                wo.addVectorToRow(state.hidden, i, alpha);
            }
        }

        return -log(state.output.get(target));
    }
}
