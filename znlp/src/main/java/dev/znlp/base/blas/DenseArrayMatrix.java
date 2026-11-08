package dev.znlp.base.blas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * Dense Array Matrix
 */
public class DenseArrayMatrix extends BasicDenseMatrix {

    private static final long serialVersionUID = 1L;

    private final float[] data;
    private final int length;
    private final Random rnd = new Random(0);

    public DenseArrayMatrix(int row, int col, float[] data) {
        super(row, col);
        this.data = data;
        this.length = row * col;
    }

    public DenseArrayMatrix(int rows, int cols) {
        this(rows, cols, new float[rows * cols]);
    }

    public DenseArrayMatrix() {
        this(0, 0);
    }

    @Override
    public void zero() {
        for (int i = 0; i < length; i++) {
            data[i] = 0f;
        }
    }

    @Override
    public void uniform(float number) {
        float a = number;
        float lower = -a;
        for (int i = 0; i < length; i++) {
            data[i] = rnd.nextFloat() * (a - lower) + lower;
        }
    }

    @Override
    public void save(FileChannel channel) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(col * 4);
            FloatBuffer asFloatBuffer = byteBuffer.asFloatBuffer();

            // Write header
            ByteBuffer header = ByteBuffer.allocate(8);
            header.putInt(row);
            header.putInt(col);
            header.flip();
            channel.write(header);

            for (int r = 0; r < row; r++) {
                asFloatBuffer.clear();
                asFloatBuffer.put(data, r * col, col);

                byteBuffer.position(0);
                byteBuffer.limit(col * 4);

                channel.write(byteBuffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
             FileChannel channel = fos.getChannel()) {
            save(channel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 底层行优先浮点数组，长度为 {@code row * col}。
     */
    public float[] getData() {
        return data;
    }

    @Override
    public Vector get(int row) {
        return new DenseVector(data, row * col, col);
    }

    @Override
    public float get(int i, int j) {
        return data[i * col + j];
    }

    @Override
    public void set(int i, int j, float v) {
        data[i * col + j] = v;
    }
}
