package dev.znlp.fasttext;

import dev.znlp.base.blas.DenseVector;
import dev.znlp.base.blas.Matrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.loss.Loss;

import java.util.List;
import java.util.Random;

/**
 * FastText模型核心类，负责模型的前向传播、预测和参数更新。
 * 包含输入矩阵、输出矩阵和损失函数，实现了FastText的核心算法逻辑。
 *
 * @param wi                输入矩阵，存储词向量和n-gram向量
 * @param wo                输出矩阵，存储标签或词的输出向量
 * @param loss              损失函数实例
 * @param normalizeGradient 是否归一化梯度
 */
public record Model(Matrix wi, Matrix wo, Loss loss, boolean normalizeGradient) {

    /**
     * 表示无限制预测数量的常量
     */
    public static final int kUnlimitedPredictions = -1;

    /**
     * 表示所有标签作为目标的常量
     */
    public static final int kAllLabelsAsTarget = -1;

    /**
     * 构造Model实例
     *
     * @param wi                输入矩阵
     * @param wo                输出矩阵
     * @param loss              损失函数
     * @param normalizeGradient 是否归一化梯度
     */
    public Model {
    }

    /**
     * 计算隐藏层向量
     *
     * @param input 输入词ID列表
     * @param state 模型状态
     */
    private void computeHidden(IntArrayList input, State state) {
        Vector hidden = state.hidden;
        hidden.zero();
        if (input.size() == 0) {
            return;
        }

        for (int i = 0; i < input.size(); i++) {
            wi.addRowToVector(hidden, input.get(i), 1.0);
        }

        hidden.timesAssign(1.0f / input.size());
    }

    /**
     * 预测输入的标签
     *
     * @param input     输入词ID列表
     * @param k         返回前k个预测结果
     * @param threshold 预测概率阈值，低于此值的结果将被过滤
     * @param heap      存储预测结果的堆
     * @param state     模型状态
     */
    public void predict(IntArrayList input, int k, float threshold, List<ScoreIdPair> heap, State state) {
        int kk = (k == kUnlimitedPredictions) ? wo.getRow() : k;
        if (kk <= 0) {
            throw new RuntimeException("k needs to be 1 or higher");
        }

        if (input.size() == 0) {
            return;
        }

        computeHidden(input, state);

        loss.predict(kk, threshold, heap, state);
    }

    /**
     * 更新模型参数
     *
     * @param input       输入词ID列表
     * @param targets     目标标签ID列表
     * @param targetIndex 目标索引
     * @param lr          学习率
     * @param state       模型状态
     */
    public void update(IntArrayList input, IntArrayList targets, int targetIndex, float lr, State state) {
        if (input.size() == 0) {
            return;
        }

        computeHidden(input, state);

        Vector grad = state.grad;
        grad.zero();

        float lossValue = loss.forward(targets, targetIndex, state, lr, true);

        state.incrementNExamples(lossValue);

        if (normalizeGradient) {
            grad.timesAssign(1.0f / input.size());
        }

        for (int i = 0; i < input.size(); i++) {
            wi.addVectorToRow(grad, input.get(i), 1.0f);
        }
    }

    /**
     * 模型状态类，用于保存模型计算过程中的中间状态。
     * 包含隐藏层向量、输出向量、梯度向量和随机数生成器。
     */
    public static class State {
        /**
         * 累计损失值
         */
        private float lossValue = 0.0f;

        /**
         * 处理的样本数量
         */
        private int nexamples = 0;

        /**
         * 隐藏层向量
         */
        public final Vector hidden;

        /**
         * 输出层向量
         */
        public final Vector output;

        /**
         * 梯度向量
         */
        public final Vector grad;

        /**
         * 随机数生成器
         */
        public final Random rng;

        /**
         * 构造State实例
         *
         * @param hiddenSize 隐藏层大小
         * @param outputSize 输出层大小
         * @param seed       随机数种子
         */
        public State(int hiddenSize, int outputSize, int seed) {
            this.hidden = new DenseVector(hiddenSize);
            this.output = new DenseVector(outputSize);
            this.grad = new DenseVector(hiddenSize);
            this.rng = new Random(seed);
        }

        /**
         * 获取平均损失值
         *
         * @return 平均损失值
         */
        public float getLoss() {
            return nexamples == 0 ? 0.0f : lossValue / nexamples;
        }

        /**
         * 增加处理的样本数量并累计损失值
         *
         * @param loss 当前样本的损失值
         */
        public void incrementNExamples(float loss) {
            lossValue += loss;
            nexamples++;
        }
    }
}
