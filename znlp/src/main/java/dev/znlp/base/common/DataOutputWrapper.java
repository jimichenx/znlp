package dev.znlp.base.common;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * DataOutput 的包装类，提供高效的 int 数组和集合的序列化功能。
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
 * DataOutputWrapper out = new DataOutputWrapper(dataOutput);
 * out.writeIntArray(array);
 * out.writeIntArray(array2D);
 * out.writeArrayList(list, (item, output) -> output.writeInt(item));
 * }</pre>
 *
 * @author jimichan
 */
public record DataOutputWrapper(DataOutput output) {

    /**
     * 创建 DataOutput 包装器。
     *
     * @param output 底层的数据输出流
     */
    public DataOutputWrapper {
        if (output == null) {
            throw new NullPointerException("output cannot be null");
        }
    }

    /**
     * 获取底层的 DataOutput 对象。
     *
     * @return 底层的 DataOutput
     */
    @Override
    public DataOutput output() {
        return output;
    }

    /**
     * 将 int 数组写入到输出流。
     * <p>
     * 使用分页策略，将数组分成多个页进行写入。
     * 如果数组为 null，会写入特殊的魔法数字标识。
     *
     * @param array 要写入的 int 数组，可以为 null
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeIntArray(int[] array) throws IOException {
        DataInOutputUtils.writeIntArray(array, output);
    }

    /**
     * 将 float 数组写入到输出流。
     * <p>
     * 使用分页策略，将数组分成多个页进行写入。
     * 如果数组为 null，会写入特殊的魔法数字标识。
     *
     * @param array 要写入的 float 数组，可以为 null
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeFloatArray(float[] array) throws IOException {
        DataInOutputUtils.writeFloatArray(array, output);
    }

    /**
     * 将二维 int 数组写入到输出流。
     * <p>
     * 先写入行数，然后逐行写入每行的 int 数组。
     *
     * @param array 要写入的二维 int 数组
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeIntArray(int[][] array) throws IOException {
        DataInOutputUtils.writeIntArray(array, output);
    }

    /**
     * 将 ArrayList 写入到输出流。
     * <p>
     * 使用泛型和函数式接口，支持写入任意类型的 ArrayList。
     * 先写入列表大小，然后使用提供的 consumer 函数逐个写入元素。
     *
     * @param <T>      列表元素的类型
     * @param list     要写入的 ArrayList
     * @param consumer 用于写入单个元素的函数
     * @throws IOException 如果发生 I/O 错误
     */
    public <T> void writeArrayList(ArrayList<T> list, BiConsumer<T, DataOutput> consumer) throws IOException {
        DataInOutputUtils.writeArrayList(list, consumer, output);
    }

    /**
     * 将字符串列表写入到输出流。
     * <p>
     * 使用高效的 UTF-8 编码方式，先写入列表大小，然后逐个写入字符串。
     * 支持 null 列表和 null 字符串元素。
     * <p>
     * 性能优化：
     * <ul>
     *   <li>直接使用 UTF-8 编码，避免 modified UTF-8 格式开销</li>
     *   <li>批量字节数组写入，减少系统调用</li>
     *   <li>预分配 ArrayList 容量，减少扩容开销</li>
     * </ul>
     *
     * @param list 要写入的字符串列表，可以为 null
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeStringList(List<String> list) throws IOException {
        DataInOutputUtils.writeStringList(list, output);
    }

    // 委托 DataOutput 的所有方法，保持完整的 API

    /**
     * 写入一个 boolean 值。
     *
     * @param v 要写入的 boolean 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeBoolean(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    /**
     * 写入一个 byte 值。
     *
     * @param v 要写入的 byte 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeByte(int v) throws IOException {
        output.writeByte(v);
    }

    /**
     * 写入一个 short 值。
     *
     * @param v 要写入的 short 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeShort(int v) throws IOException {
        output.writeShort(v);
    }

    /**
     * 写入一个 char 值。
     *
     * @param v 要写入的 char 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeChar(int v) throws IOException {
        output.writeChar(v);
    }

    /**
     * 写入一个 int 值。
     *
     * @param v 要写入的 int 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    /**
     * 写入一个 long 值。
     *
     * @param v 要写入的 long 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    /**
     * 写入一个 float 值。
     *
     * @param v 要写入的 float 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    /**
     * 写入一个 double 值。
     *
     * @param v 要写入的 double 值
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    /**
     * 写入一个字符串（使用 UTF-8 编码）。
     *
     * @param s 要写入的字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeUTF(String s) throws IOException {
        output.writeUTF(s);
    }

    /**
     * 写入一个字节数组。
     *
     * @param b 要写入的字节数组
     * @throws IOException 如果发生 I/O 错误
     */
    public void write(byte[] b) throws IOException {
        output.write(b);
    }

    /**
     * 写入字节数组的一部分。
     *
     * @param b   要写入的字节数组
     * @param off 起始偏移量
     * @param len 要写入的字节数
     * @throws IOException 如果发生 I/O 错误
     */
    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    /**
     * 写入一个字节。
     *
     * @param b 要写入的字节
     * @throws IOException 如果发生 I/O 错误
     */
    public void write(int b) throws IOException {
        output.write(b);
    }
}
