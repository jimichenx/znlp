package dev.znlp.fasttext.utils;

import dev.znlp.base.blas.DenseArrayMatrix;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * DataInput包装类，提供额外的工具方法和字节序处理能力。
 * 支持大端和小端字节序，并提供了更高效的UTF字符串读取方法。
 */
public class AutoDataInput implements DataInput, Closeable {

    /**
     * 底层的DataInput实例
     */
    private final DataInput dataInput;
    
    /**
     * 字节序，支持大端和小端
     */
    private final ByteOrder order;
    
    /**
     * 用于读取UTF字符串的字节数组缓存
     */
    private byte[] utf = new byte[256];
    
    /**
     * 用于读取长整数的字节数组缓存
     */
    private final byte[] readBuffer = new byte[8];

    /**
     * 需要关闭的底层资源，可为 null
     */
    private final Closeable closeable;

    /**
     * 构造AutoDataInput实例
     *
     * @param dataInput 底层的DataInput实例
     * @param order 字节序
     */
    public AutoDataInput(DataInput dataInput, ByteOrder order) {
        this(dataInput, order, dataInput instanceof Closeable c ? c : null);
    }

    /**
     * 构造AutoDataInput实例
     *
     * @param dataInput 底层的DataInput实例
     * @param order     字节序
     * @param closeable 关闭时释放的资源
     */
    public AutoDataInput(DataInput dataInput, ByteOrder order, Closeable closeable) {
        this.dataInput = dataInput;
        this.order = order;
        this.closeable = closeable;
    }

    /**
     * 构造AutoDataInput实例，默认使用大端字节序
     *
     * @param dataInput 底层的DataInput实例
     */
    public AutoDataInput(DataInput dataInput) {
        this(dataInput, ByteOrder.BIG_ENDIAN);
    }

    /**
     * 从文件创建AutoDataInput实例
     *
     * @param file 要打开的文件
     * @return AutoDataInput实例
     * @throws IOException IO异常
     */
    public static AutoDataInput open(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        return new AutoDataInput(dis, ByteOrder.BIG_ENDIAN, dis);
    }

    /**
     * 以小端字节序打开文件，用于读取 Facebook FastText {@code .bin}。
     */
    public static AutoDataInput openLittleEndian(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        return new AutoDataInput(dis, ByteOrder.LITTLE_ENDIAN, dis);
    }

    @Override
    public void close() throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * 读取 FastText DenseMatrix：两个 int64（行、列）+ 行优先 float 数组。
     */
    public DenseArrayMatrix loadFloatMatrix() throws IOException {
        int rows = Math.toIntExact(readLong());
        int cols = Math.toIntExact(readLong());
        if (rows < 0 || cols < 0) {
            throw new IOException("Invalid matrix size: " + rows + "x" + cols);
        }
        long cells = (long) rows * cols;
        if (cells > Integer.MAX_VALUE) {
            throw new IOException("Matrix too large: " + rows + "x" + cols);
        }
        float[] data = new float[(int) cells];
        int chunk = 1024;
        byte[] buf = new byte[chunk * 4];
        int i = 0;
        while (i < data.length) {
            int n = Math.min(chunk, data.length - i);
            readFully(buf, 0, n * 4);
            ByteBuffer.wrap(buf, 0, n * 4).order(order).asFloatBuffer().get(data, i, n);
            i += n;
        }
        return new DenseArrayMatrix(rows, cols, data);
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        dataInput.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        dataInput.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return dataInput.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return dataInput.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return dataInput.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return dataInput.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return dataInput.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return dataInput.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return dataInput.readChar();
    }

    /**
     * 读取整数，根据指定的字节序处理
     *
     * @return 读取的整数
     * @throws IOException IO异常
     */
    @Override
    public int readInt() throws IOException {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            int ch4 = dataInput.readUnsignedByte();
            int ch3 = dataInput.readUnsignedByte();
            int ch2 = dataInput.readUnsignedByte();
            int ch1 = dataInput.readUnsignedByte();
            // Note: readUnsignedByte returns 0-255, so checking < 0 is not needed unless EOF which throws exception
            return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
        } else {
            return dataInput.readInt();
        }
    }

    /**
     * 读取长整数，根据指定的字节序处理
     *
     * @return 读取的长整数
     * @throws IOException IO异常
     */
    @Override
    public long readLong() throws IOException {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            readFully(readBuffer, 0, 8);
            return ((long) readBuffer[0] & 0xff) +
                   (((long) readBuffer[1] & 0xff) << 8) +
                   (((long) readBuffer[2] & 0xff) << 16) +
                   (((long) readBuffer[3] & 0xff) << 24) +
                   (((long) readBuffer[4] & 0xff) << 32) +
                   (((long) readBuffer[5] & 0xff) << 40) +
                   (((long) readBuffer[6] & 0xff) << 48) +
                   (((long) readBuffer[7] & 0xff) << 56);
        } else {
            return dataInput.readLong();
        }
    }

    /**
     * 读取浮点数，通过readInt()实现
     *
     * @return 读取的浮点数
     * @throws IOException IO异常
     */
    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * 读取双精度浮点数，通过readLong()实现
     *
     * @return 读取的双精度浮点数
     * @throws IOException IO异常
     */
    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readLine() throws IOException {
        return dataInput.readLine();
    }

    /**
     * 读取UTF字符串，以null字节(0)结尾
     *
     * @return 读取的UTF字符串
     * @throws IOException IO异常
     */
    @Override
    public String readUTF() throws IOException {
        int i = 0;
        int len = utf.length;
        byte b = readByte();
        while (b != 0) {
            utf[i++] = b;
            if (i == len) {
                utf = Arrays.copyOf(utf, utf.length * 2);
                len = utf.length;
            }
            b = readByte();
        }

        if (i > 0) {
            return new String(utf, 0, i, StandardCharsets.UTF_8);
        } else {
            return "";
        }
    }
}
