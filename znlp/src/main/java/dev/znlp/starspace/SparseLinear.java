package dev.znlp.starspace;

import dev.znlp.base.blas.DenseMatrix;
import dev.znlp.base.blas.DenseVector;
import dev.znlp.base.blas.Vector;

import java.util.List;

/**
 * 稀疏线性操作类，包装了 DenseMatrix 以提供稀疏线性操作（嵌入查找）
 * <p>
 * 该类用于高效地将稀疏索引映射到密集向量，主要用于嵌入查找和特征聚合。
 * 在 StarSpace 模型中，它被用来处理输入特征的嵌入查找和组合。
 *
 * @param matrix 内部存储的密集矩阵
 */
public record SparseLinear(DenseMatrix matrix) {

    /**
     * 构造稀疏线性操作对象
     *
     * @param matrix 密集矩阵
     */
    public SparseLinear {
    }

    /**
     * 获取矩阵行数
     *
     * @return 矩阵行数
     */
    public int numRows() {
        return matrix.getRow();
    }

    /**
     * 获取矩阵列数
     *
     * @return 矩阵列数
     */
    public int numCols() {
        return matrix.getCol();
    }

    /**
     * 获取内部矩阵
     *
     * @return 内部矩阵
     */
    @Override
    public DenseMatrix matrix() {
        return matrix;
    }

    /**
     * 前向传播，获取指定行的向量
     *
     * @param row 行索引
     * @return 对应行的向量
     */
    public Vector forward(int row) {
        return matrix.get(row);
    }

    /**
     * 前向传播，根据索引列表和权重计算加权和向量
     *
     * @param list 索引-权重对列表
     * @return 加权和向量
     */
    public Vector forward(List<Utils.XPair> list) {
        DenseVector vector = new DenseVector(this.numCols());

        for (Utils.XPair pair : list) {
            int row = pair.first;
            float scale = pair.second;
            // vector += scale * matrix[row]
            vector.plusAssign(scale, matrix.get(row));
        }

        return vector;
    }
}
