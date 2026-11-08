package dev.znlp.base.blas;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Float Array Vector
 */
public class DenseVector implements Vector, Serializable {

    private static final long serialVersionUID = 112L;

    private final float[] data;
    private final int offset;
    private final int length;

    public DenseVector(float[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public DenseVector(float[] data) {
        this(data, 0, data.length);
    }

    public DenseVector(int size) {
        this(new float[size], 0, size);
    }

    public DenseVector(int size, float initValue) {
        this(initializedArray(size, initValue), 0, size);
    }

    private static float[] initializedArray(int size, float initValue) {
        float[] data = new float[size];
        if (initValue != 0) {
            Arrays.fill(data, initValue);
        }
        return data;
    }

    public float[] getData() {
        return data;
    }

    @Override
    public Vector subVector(int offset, int size) {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] += this.get(i + offset);
        }
        return new DenseVector(result, 0, size);
    }

    @Override
    public Vector plusTo(Vector v) {
        if (length != v.length()) throw new IllegalArgumentException("Length mismatch");
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = this.get(i) + v.get(i);
        }
        return new DenseVector(result, 0, length);
    }

    @Override
    public Vector minusTo(Vector v) {
        if (length != v.length()) throw new IllegalArgumentException("Length mismatch");
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = this.get(i) - v.get(i);
        }
        return new DenseVector(result, 0, length);
    }

    @Override
    public float get(int index) {
        return data[index + offset];
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public float prod(Vector vector) {
        float result = 0f;
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            result += data[i] * vector.get(j++);
        }
        return result;
    }

    @Override
    public float norm2() {
        double norm = 0.0f;
        for (int i = offset; i < offset + length; i++) {
            float x = data[i];
            norm += x * x;
        }
        if (Double.isNaN(norm)) {
            throw new RuntimeException("NaN norm");
        }
        return (float) Math.sqrt(norm);
    }

    @Override
    public float norm2Pow() {
        float sum = 0.0f;
        for (int i = offset; i < offset + length; i++) {
            float x = data[i];
            sum += x * x;
        }
        return sum;
    }

    @Override
    public void check() {
        for (int i = offset; i < offset + length; i++) {
            float f = data[i];
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                throw new RuntimeException("Vector contains NaN or Infinite");
            }
        }
    }

    @Override
    public Vector copy() {
        float[] dest = new float[length];
        System.arraycopy(this.data, offset, dest, 0, length);
        return new DenseVector(dest, 0, length);
    }

    @Override
    public void access(VectorConsumer consumer) {
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            consumer.accept(j++, data[i]);
        }
    }

    @Override
    public void fill(float value) {
        for (int i = offset; i < offset + length; i++) {
            data[i] = value;
        }
    }

    @Override
    public void fill(VectorFunction function) {
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            data[i] = function.apply(j++);
        }
    }

    @Override
    public void set(int index, float value) {
        data[index + offset] = value;
    }

    @Override
    public void plusAssign(Vector vector) {
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            data[i] += vector.get(j++);
        }
    }

    @Override
    public void plusAssign(float scale, Vector vector) {
        if (scale == 1.0f) {
            plusAssign(vector);
        } else {
            int j = 0;
            for (int i = offset; i < offset + length; i++) {
                data[i] += vector.get(j++) * scale;
            }
        }
    }

    @Override
    public void minusAssign(Vector vector) {
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            data[i] -= vector.get(j++);
        }
    }

    @Override
    public void minusAssign(float scale, Vector vector) {
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            data[i] -= vector.get(j++) * scale;
        }
    }

    @Override
    public void timesAssign(float scale) {
        for (int i = offset; i < offset + length; i++) {
            data[i] *= scale;
        }
    }

    @Override
    public void divAssign(float scale) {
        for (int i = offset; i < offset + length; i++) {
            data[i] /= scale;
        }
    }

    @Override
    public void putAll(float[] array) {
        if (length != array.length) throw new IllegalArgumentException("Length mismatch");
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            data[i] = array[j++];
        }
    }

    @Override
    public void zero() {
        fill(0);
    }

    @Override
    public void copyFrom(Vector vector) {
        if (this.length() != vector.length()) throw new IllegalArgumentException("Length mismatch");
        int j = offset;
        for (int i = 0; i < vector.length(); i++) {
            data[j++] = vector.get(i);
        }
    }

    @Override
    public String toString() {
        if (length() == 0) return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        int iMax = length() - 1;
        int end = offset + iMax;
        int i = offset;
        while (true) {
            b.append(data[i]);
            if (i == end) return b.append(']').toString();
            b.append(", ");
            i++;
        }
    }
}
