package dev.znlp.base.blas;

public interface DenseMatrix extends Matrix {

    void zero();

    void uniform(float number);

    Vector get(int row);

    float get(int i, int j);

    void set(int i, int j, float v);

    /**
     * Multiply rows from ib to ie by nums
     */
    void multiplyRow(Vector nums, int ib, int ie);

    default void multiplyRow(Vector nums) {
        multiplyRow(nums, 0, -1);
    }

    /**
     * Divide rows from ib to ie by denoms
     */
    void divideRow(Vector denoms, int ib, int ie);

    default void divideRow(Vector denoms) {
        divideRow(denoms, 0, -1);
    }

    float l2NormRow(int i);

    void l2NormRow(Vector norms);
}
