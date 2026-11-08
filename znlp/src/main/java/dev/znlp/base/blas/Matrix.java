package dev.znlp.base.blas;

import java.io.File;
import java.io.Serializable;
import java.nio.channels.FileChannel;

/**
 * Float Matrix
 */
public interface Matrix extends Serializable {

    int getRow();
    int getCol();

    /**
     * Dot product of i-th row and vec
     */
    float dotRow(Vector vec, int i);

    /**
     * Add vector to i-th row with scale a
     */
    void addVectorToRow(Vector vector, int row, float a);

    void addRowToVector(Vector target, int i, double a);

    void save(File file);

    void save(FileChannel channel);
}
