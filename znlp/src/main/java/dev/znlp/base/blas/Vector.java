package dev.znlp.base.blas;

import java.io.Serializable;

/**
 * Float Vector
 */
public interface Vector extends Serializable {

    Vector subVector(int offset, int size);

    Vector plusTo(Vector v);

    Vector minusTo(Vector v);

    default Vector divTo(float value) {
        float[] result = new float[length()];
        for (int i = 0; i < length(); i++) {
            result[i] = this.get(i) / value;
        }
        return new DenseVector(result, 0, length());
    }

    int length();

    float prod(Vector vector);

    float norm2();

    float norm2Pow();

    void check();

    Vector copy();

    void access(VectorConsumer consumer);

    interface VectorConsumer {
        void accept(int index, float value);
    }

    // writer
    void fill(float value);

    void fill(VectorFunction function);

    interface VectorFunction {
        float apply(int index);
    }

    float get(int index);

    // Operator overloads in Kotlin map to these methods
    default float times(Vector vector) {
        return prod(vector);
    }

    void set(int index, float value);

    void plusAssign(Vector vector);

    void plusAssign(float scale, Vector vector);

    void minusAssign(Vector vector);

    void minusAssign(float scale, Vector vector);

    void timesAssign(float scale);

    void divAssign(float scale);

    void putAll(float[] array);

    void zero();

    /**
     * Copy values from another vector
     */
    void copyFrom(Vector vector);

    /**
     * Add row from matrix A to this vector
     */
    default void addRow(Matrix A, int i, double a) {
        if (i < 0) throw new IllegalArgumentException("i < 0");
        if (i >= A.getRow()) throw new IllegalArgumentException("i >= A.row");
        if (length() != A.getCol()) throw new IllegalArgumentException("length() != A.col");
        A.addRowToVector(this, i, a);
    }

    /**
     * Dot product of each row of matrix A with vec, storing result in this vector
     */
    default void mul(Matrix A, Vector vec) {
        if (A.getRow() != length()) throw new IllegalArgumentException("A.row != length()");
        if (A.getCol() != vec.length()) throw new IllegalArgumentException("A.col != vec.length()");
        for (int i = 0; i < length(); i++) {
            this.set(i, A.dotRow(vec, i));
        }
    }
}
