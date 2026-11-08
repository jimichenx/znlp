package dev.znlp.base.blas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DenseArrayMatrixTest {

    @Test
    void dotRowComputesRowVectorProduct() {
        DenseArrayMatrix matrix = new DenseArrayMatrix(2, 3);
        matrix.set(0, 0, 1f);
        matrix.set(0, 1, 2f);
        matrix.set(0, 2, 3f);
        matrix.set(1, 0, 4f);
        matrix.set(1, 1, 5f);
        matrix.set(1, 2, 6f);

        DenseVector vec = new DenseVector(new float[]{1f, 1f, 1f});
        assertEquals(6f, matrix.dotRow(vec, 0), 1e-5);
        assertEquals(15f, matrix.dotRow(vec, 1), 1e-5);
    }

    @Test
    void addVectorToRowUpdatesValues() {
        DenseArrayMatrix matrix = new DenseArrayMatrix(1, 3);
        matrix.set(0, 0, 1f);
        matrix.set(0, 1, 2f);
        matrix.set(0, 2, 3f);

        DenseVector vec = new DenseVector(new float[]{1f, 2f, 3f});
        matrix.addVectorToRow(vec, 0, 1f);

        assertEquals(2f, matrix.get(0, 0), 1e-5);
        assertEquals(4f, matrix.get(0, 1), 1e-5);
        assertEquals(6f, matrix.get(0, 2), 1e-5);
    }
}
