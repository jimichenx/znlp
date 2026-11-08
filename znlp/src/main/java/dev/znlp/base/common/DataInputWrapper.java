package dev.znlp.base.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * DataInput 的包装类，提供高效的 int 数组和集合的反序列化功能。
 * <p>
 * 该类采用分页处理策略，通过批量 I/O 操作来优化性能：
 * <ul>
 *   <li>分页处理：将大数组分成固定大小的页（每页 4096 个 int，16KB），降低内存峰值和 GC 压力</li>
 *   <li>批量 I/O：使用批量读写操作，减少系统调用次数</li>
 *   <li>手动字节转换：使用位运算进行 int 和 byte 数组之间的转换，性能优于标准序列化</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * DataInputWrapper in = new DataInputWrapper(dataInput);
 * int[] array = in.readIntArray();
 * int[][] array2D = in.readIntArray2D();
 * ArrayList<Integer> list = in.readArrayList(input -> input.readInt());
 * }</pre>
 *
 * @author jimichan
 */
public record DataInputWrapper(DataInput input) {

    /**
     * 创建 DataInput 包装器。
     *
     * @param input 底层的数据输入流
     */
    public DataInputWrapper {
        if (input == null) {
            throw new NullPointerException("input cannot be null");
        }
    }

    /**
     * 获取底层的 DataInput 对象。
     *
     * @return 底层的 DataInput
     */
    @Override
    public DataInput input() {
        return input;
    }

    /**
     * 从输入流读取 int 数组。
     * <p>
     * 使用分页策略进行读取，在循环中复用缓冲区以减少对象分配。
     * 支持 null 值的识别，如果读取到魔法数字标识，则返回 null。
     *
     * @return 读取的 int 数组，如果读取到 null 标识则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    public int[] readIntArray() throws IOException {
        return DataInOutputUtils.readIntArray(input);
    }

    /**
     * 从输入流读取 float 数组。
     * <p>
     * 使用分页策略进行读取，在循环中复用缓冲区以减少对象分配。
     * 支持 null 值的识别，如果读取到魔法数字标识，则返回 null。
     *
     * @return 读取的 float 数组，如果读取到 null 标识则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    public float[] readFloatArray() throws IOException {
        return DataInOutputUtils.readFloatArray(input);
    }

    /**
     * 从输入流读取二维 int 数组。
     * <p>
     * 先读取行数，然后逐行读取每行的 int 数组。
     *
     * @return 读取的二维 int 数组
     * @throws IOException 如果发生 I/O 错误
     */
    public int[][] readIntArray2D() throws IOException {
        return DataInOutputUtils.readDoubleIntArray(input);
    }

    /**
     * 从输入流读取 ArrayList。
     * <p>
     * 使用泛型和函数式接口，支持读取任意类型的 ArrayList。
     * 先读取列表大小，然后使用提供的 reader 函数逐个读取元素。
     *
     * @param <T>    列表元素的类型
     * @param reader 用于读取单个元素的函数
     * @return 读取的 ArrayList
     * @throws IOException 如果发生 I/O 错误
     */
    public <T> ArrayList<T> readArrayList(Function<DataInput, T> reader) throws IOException {
        return DataInOutputUtils.readArrayList(input, reader);
    }

    /**
     * 从输入流读取字符串列表。
     * <p>
     * 使用高效的 UTF-8 编码方式，先读取列表大小，然后逐个读取字符串。
     * 支持 null 列表和 null 字符串元素。
     * <p>
     * 性能优化：
     * <ul>
     *   <li>直接使用 UTF-8 编码，避免 modified UTF-8 格式开销</li>
     *   <li>批量字节数组读取，减少系统调用</li>
     *   <li>预分配 ArrayList 容量，减少扩容开销</li>
     * </ul>
     *
     * @return 读取的字符串列表，如果读取到 null 标识则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    public ArrayList<String> readStringList() throws IOException {
        return DataInOutputUtils.readStringList(input);
    }

    // 委托 DataInput 的所有方法，保持完整的 API

    /**
     * 读取一个 boolean 值。
     *
     * @return 读取的 boolean 值
     * @throws IOException 如果发生 I/O 错误
     */
    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    /**
     * 读取一个 byte 值。
     *
     * @return 读取的 byte 值
     * @throws IOException 如果发生 I/O 错误
     */
    public byte readByte() throws IOException {
        return input.readByte();
    }

    /**
     * 读取一个无符号 byte 值。
     *
     * @return 读取的无符号 byte 值（0-255）
     * @throws IOException 如果发生 I/O 错误
     */
    public int readUnsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    /**
     * 读取一个 short 值。
     *
     * @return 读取的 short 值
     * @throws IOException 如果发生 I/O 错误
     */
    public short readShort() throws IOException {
        return input.readShort();
    }

    /**
     * 读取一个无符号 short 值。
     *
     * @return 读取的无符号 short 值（0-65535）
     * @throws IOException 如果发生 I/O 错误
     */
    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
    }

    /**
     * 读取一个 char 值。
     *
     * @return 读取的 char 值
     * @throws IOException 如果发生 I/O 错误
     */
    public char readChar() throws IOException {
        return input.readChar();
    }

    /**
     * 读取一个 int 值。
     *
     * @return 读取的 int 值
     * @throws IOException 如果发生 I/O 错误
     */
    public int readInt() throws IOException {
        return input.readInt();
    }

    /**
     * 读取一个 long 值。
     *
     * @return 读取的 long 值
     * @throws IOException 如果发生 I/O 错误
     */
    public long readLong() throws IOException {
        return input.readLong();
    }

    /**
     * 读取一个 float 值。
     *
     * @return 读取的 float 值
     * @throws IOException 如果发生 I/O 错误
     */
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    /**
     * 读取一个 double 值。
     *
     * @return 读取的 double 值
     * @throws IOException 如果发生 I/O 错误
     */
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    /**
     * 读取一个字符串（使用 UTF-8 编码）。
     *
     * @return 读取的字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public String readUTF() throws IOException {
        return input.readUTF();
    }

    /**
     * 读取字节数组，填满整个数组。
     *
     * @param b 要填充的字节数组
     * @throws IOException 如果发生 I/O 错误
     */
    public void readFully(byte[] b) throws IOException {
        input.readFully(b);
    }

    /**
     * 读取字节数组的一部分。
     *
     * @param b   要填充的字节数组
     * @param off 起始偏移量
     * @param len 要读取的字节数
     * @throws IOException 如果发生 I/O 错误
     */
    public void readFully(byte[] b, int off, int len) throws IOException {
        input.readFully(b, off, len);
    }

    /**
     * 跳过指定数量的字节。
     *
     * @param n 要跳过的字节数
     * @return 实际跳过的字节数
     * @throws IOException 如果发生 I/O 错误
     */
    public int skipBytes(int n) throws IOException {
        return input.skipBytes(n);
    }
}
