package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.Model;
import dev.znlp.fasttext.ScoreIdPair;

import java.util.List;

/**
 * 损失函数抽象基类，定义了FastText模型损失函数的基本接口和通用方法。
 * 所有具体损失函数（如Softmax、Negative Sampling等）都继承自此类。
 */
public abstract class Loss {

    /**
     * 输出矩阵，存储标签或词的输出向量
     */
    protected final Matrix wo;

    /**
     * 构造Loss实例
     *
     * @param wo 输出矩阵
     */
    public Loss(Matrix wo) {
        this.wo = wo;
    }

    /**
     * 预测前k个最佳结果
     *
     * @param k 返回结果的数量
     * @param threshold 预测分数阈值
     * @param heap 存储结果的列表
     * @param state 模型状态
     */
    public void predict(int k, float threshold, List<ScoreIdPair> heap, Model.State state) {
        computeOutput(state);
        findKBest(k, threshold, heap, state.output);
    }

    /**
     * 查找输出向量中的前 k 个最佳结果（有界最小堆，O(n log k)）。
     */
    protected void findKBest(int k, float threshold, List<ScoreIdPair> heap, Vector output) {
        TopKScoreHeap top = new TopKScoreHeap(k);
        int n = output.length();
        for (int i = 0; i < n; i++) {
            float val = output.get(i);
            if (val < threshold) {
                continue;
            }
            top.offer(stdLog(val), i);
        }
        top.drainDescending(heap);
    }

    /**
     * 计算输出向量
     *
     * @param state 模型状态
     */
    public abstract void computeOutput(Model.State state);

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
    public abstract float forward(IntArrayList targets, int targetIndex, Model.State state, float lr, boolean backprop);

    // Static helpers
    /**
     * Sigmoid查找表大小
     */
    public static final int SIGMOID_TABLE_SIZE = 512;
    
    /**
     * Sigmoid函数的最大输入值
     */
    public static final int MAX_SIGMOID = 8;
    
    /**
     * Log查找表大小
     */
    public static final int LOG_TABLE_SIZE = 512;

    /**
     * Sigmoid查找表
     */
    private static final float[] tSigmoid = new float[SIGMOID_TABLE_SIZE + 1];
    
    /**
     * Log查找表
     */
    private static final float[] tLog = new float[LOG_TABLE_SIZE + 1];

    static {
        for (int i = 0; i < SIGMOID_TABLE_SIZE + 1; i++) {
            float x = (float) (i * 2 * MAX_SIGMOID) / SIGMOID_TABLE_SIZE - MAX_SIGMOID;
            tSigmoid[i] = (float) (1.0f / (1.0f + Math.exp(-x)));
        }

        for (int i = 0; i < LOG_TABLE_SIZE + 1; i++) {
            float x = (i + 1e-5f) / LOG_TABLE_SIZE;
            tLog[i] = (float) Math.log(x);
        }
    }

    /**
     * 使用查找表计算Log值
     *
     * @param x 输入值
     * @return Log值
     */
    public static float log(float x) {
        if (x > 1.0f) {
            return 0.0f;
        }
        int i = (int) (x * LOG_TABLE_SIZE);
        return tLog[i];
    }

    /**
     * 标准Log函数，添加小epsilon避免除以零
     *
     * @param d 输入值
     * @return Log值
     */
    public static float stdLog(float d) {
        return (float) Math.log(d + 1e-5);
    }

    /**
     * 使用查找表计算Sigmoid值
     *
     * @param x 输入值
     * @return Sigmoid值
     */
    public static float sigmoid(float x) {
        if (x < -MAX_SIGMOID) {
            return 0.0f;
        } else if (x > MAX_SIGMOID) {
            return 1.0f;
        } else {
            int i = (int) ((x + MAX_SIGMOID) * SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2);
            return tSigmoid[i];
        }
    }
}
