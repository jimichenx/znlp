package dev.znlp.base.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 数据输入输出工具类，提供高效的 int 数组和集合的序列化/反序列化功能。
 * <p>
 * <b>推荐使用包装类：</b>
 * <p>
 * 为了提供更友好的 API，建议使用 {@link DataOutputWrapper} 和 {@link DataInputWrapper} 包装类：
 * <pre>{@code
 * // 推荐的用法
 * DataOutputWrapper out = new DataOutputWrapper(dataOutput);
 * out.writeIntArray(array);
 *
 * DataInputWrapper in = new DataInputWrapper(dataInput);
 * int[] array = in.readIntArray();
 * }</pre>
 * <p>
 * 该类采用分页处理策略，通过批量 I/O 操作来优化性能：
 * <ul>
 *   <li>分页处理：将大数组分成固定大小的页（每页 4096 个 int，16KB），降低内存峰值和 GC 压力</li>
 *   <li>批量 I/O：使用批量读写操作，减少系统调用次数</li>
 *   <li>手动字节转换：使用位运算进行 int 和 byte 数组之间的转换，性能优于标准序列化</li>
 * </ul>
 * <p>
 * 支持的功能：
 * <ul>
 *   <li>一维 int 数组的读写（支持 null 值）</li>
 *   <li>二维 int 数组的读写</li>
 *   <li>泛型 ArrayList 的读写</li>
 * </ul>
 * <p>
 * <b>注意：</b>本类的静态方法主要用于内部实现，包装类会调用这些方法。
 * 如需直接使用，请确保参数不为 null。
 *
 * @author jimichan
 * @see DataOutputWrapper
 * @see DataInputWrapper
 */
public class DataInOutputUtils {

    /**
     * null 值的魔法数字标识符 - 第一部分
     */
    public static final int ic1 = 1123992342;
    /**
     * null 值的魔法数字标识符 - 第二部分
     */
    public static final int ic2 = 832718121;
    /**
     * null 值的魔法数字标识符 - 第三部分
     */
    public static final int ic3 = 957462342;

    /**
     * null 值的魔法数组，用于标识 null 数组
     */
    private static final int[] nullMagic = new int[]{ic1, ic2, ic3};

    /**
     * 每页的 int 数量（4096 个 int = 16KB）
     */
    private static final int perPage = 1024 * 4;
    /**
     * 每页的字节大小
     */
    private static final int pageSize = perPage * 4;

    /**
     * 字符串列表分页的字节大小阈值（64KB）
     */
    private static final int STRING_PAGE_SIZE = 64 * 1024;
    /**
     * 字符串列表读取时的缓冲区大小（64KB）
     */
    private static final int STRING_BUFFER_SIZE = 64 * 1024;

    /**
     * 计算分页数量（向上取整）。
     *
     * @param total       总数量
     * @param itemPerPage 每页的数量
     * @return 分页数量
     */
    private static int pages(int total, int itemPerPage) {
        return (total + itemPerPage - 1) / itemPerPage;
    }

    /**
     * 将 int 数组写入到 DataOutput。
     * <p>
     * 使用分页策略，将数组分成多个页进行写入。
     * 如果数组为 null，会写入特殊的魔法数字标识。
     *
     * @param array  要写入的 int 数组，可以为 null
     * @param output 数据输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void writeIntArray(int[] array, DataOutput output) throws IOException {
        if (array == null) {
            array = nullMagic;
        }

        // 每个 int 对应 4 个字节
        int totalByte = array.length * 4;
        output.writeInt(totalByte);

        // 使用分页策略
        int pages = pages(array.length, perPage);

        for (int page = 0; page < pages; page++) {
            int from = page * perPage;
            int to = Math.min(from + perPage, array.length);
            int count = to - from;
            byte[] buffer = new byte[count * 4];

            // 将 int 数组转换为 byte 数组
            int bufferIndex = 0;
            for (int i = from; i < to; i++) {
                int v = array[i];
                buffer[bufferIndex++] = (byte) (v >> 24);
                buffer[bufferIndex++] = (byte) (v >> 16);
                buffer[bufferIndex++] = (byte) (v >> 8);
                buffer[bufferIndex++] = (byte) v;
            }

            // 写入当前页的数据
            output.write(buffer);
        }
    }


    /**
     * 从 DataInput 读取 int 数组。
     * <p>
     * 使用分页策略进行读取，在循环中复用缓冲区以减少对象分配。
     * 支持 null 值的识别，如果读取到魔法数字标识，则返回 null。
     *
     * @param input 数据输入流
     * @return 读取的 int 数组，如果读取到 null 标识则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    public static int[] readIntArray(DataInput input) throws IOException {
        int len = input.readInt();

        // 特殊处理：null 值标识（3 个 int = 12 字节）
        if (len == 12) {
            byte[] result = new byte[len];
            input.readFully(result);
            int[] ints = MyInts.fromByteArrayToArray(result);
            if (ints.length == 3 && ints[0] == ic1 && ints[1] == ic2 && ints[2] == ic3) {
                return null;
            } else {
                return ints;
            }
        }

        // 正常数组读取：使用分页策略和缓冲区复用
        int[] array = new int[len / 4];
        int pages = pages(len, pageSize);
        byte[] buffer = new byte[pageSize];
        int[] intBuffer = new int[perPage];
        int point = 0;

        for (int page = 0; page < pages; page++) {
            int from = page * pageSize;
            int to = Math.min(from + pageSize, len);
            int length = to - from;
            int intCount = length / 4;

            input.readFully(buffer, 0, length);
            MyInts.fromByteArrayToArray(buffer, intBuffer, length);
            System.arraycopy(intBuffer, 0, array, point, intCount);

            point += intCount;
        }
        return array;
    }


    /**
     * 将 float 数组写入到 DataOutput。
     * <p>
     * 使用分页策略，将数组分成多个页进行写入。
     * 如果数组为 null，会写入特殊的魔法数字标识。
     * float 值会先转换为 int bits，然后按照 int 数组的方式处理。
     *
     * @param array  要写入的 float 数组，可以为 null
     * @param output 数据输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void writeFloatArray(float[] array, DataOutput output) throws IOException {
        if (array == null) {
            int[] nullArray = nullMagic;
            writeIntArray(nullArray, output);
            return;
        }

        // 每个 float 对应 4 个字节
        int totalByte = array.length * 4;
        output.writeInt(totalByte);

        // 使用分页策略
        int pages = pages(array.length, perPage);

        for (int page = 0; page < pages; page++) {
            int from = page * perPage;
            int to = Math.min(from + perPage, array.length);
            int count = to - from;
            byte[] buffer = new byte[count * 4];

            // 将 float 数组转换为 byte 数组
            int bufferIndex = 0;
            for (int i = from; i < to; i++) {
                int v = Float.floatToIntBits(array[i]);
                buffer[bufferIndex++] = (byte) (v >> 24);
                buffer[bufferIndex++] = (byte) (v >> 16);
                buffer[bufferIndex++] = (byte) (v >> 8);
                buffer[bufferIndex++] = (byte) v;
            }

            // 写入当前页的数据
            output.write(buffer);
        }
    }

    /**
     * 从 DataInput 读取 float 数组。
     * <p>
     * 使用分页策略进行读取，在循环中复用缓冲区以减少对象分配。
     * 支持 null 值的识别，如果读取到魔法数字标识，则返回 null。
     * 先按照 int 数组读取，然后将 int bits 转换为 float 值。
     *
     * @param input 数据输入流
     * @return 读取的 float 数组，如果读取到 null 标识则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    public static float[] readFloatArray(DataInput input) throws IOException {
        int len = input.readInt();

        // 特殊处理：null 值标识（3 个 int = 12 字节）
        if (len == 12) {
            byte[] result = new byte[len];
            input.readFully(result);
            int[] ints = MyInts.fromByteArrayToArray(result);
            if (ints.length == 3 && ints[0] == ic1 && ints[1] == ic2 && ints[2] == ic3) {
                return null;
            }
            // 如果不是 null 标识，则转换为 float 数组
            float[] floats = new float[ints.length];
            for (int i = 0; i < ints.length; i++) {
                floats[i] = Float.intBitsToFloat(ints[i]);
            }
            return floats;
        }

        // 正常数组读取：使用分页策略和缓冲区复用
        float[] array = new float[len / 4];
        int pages = pages(len, pageSize);
        byte[] buffer = new byte[pageSize];
        int[] intBuffer = new int[perPage];
        int point = 0;

        for (int page = 0; page < pages; page++) {
            int from = page * pageSize;
            int to = Math.min(from + pageSize, len);
            int length = to - from;
            int floatCount = length / 4;

            input.readFully(buffer, 0, length);
            MyInts.fromByteArrayToArray(buffer, intBuffer, length);

            // 将 int bits 转换为 float
            for (int i = 0; i < floatCount; i++) {
                array[point + i] = Float.intBitsToFloat(intBuffer[i]);
            }

            point += floatCount;
        }
        return array;
    }

    /**
     * 将二维 int 数组写入到 DataOutput。
     * <p>
     * 先写入行数，然后逐行写入每行的 int 数组。
     *
     * @param array  要写入的二维 int 数组
     * @param output 数据输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void writeIntArray(int[][] array, DataOutput output) throws IOException {
        int line = array.length;
        output.writeInt(line);

        for (int i = 0; i < line; i++) {
            writeIntArray(array[i], output);
        }
    }

    /**
     * 从 DataInput 读取二维 int 数组。
     * <p>
     * 先读取行数，然后逐行读取每行的 int 数组。
     *
     * @param input 数据输入流
     * @return 读取的二维 int 数组
     * @throws IOException 如果发生 I/O 错误
     */
    public static int[][] readDoubleIntArray(DataInput input) throws IOException {
        int line = input.readInt();
        int[][] result = new int[line][];

        for (int i = 0; i < line; i++) {
            result[i] = readIntArray(input);
        }
        return result;
    }

    /**
     * 从 DataInput 读取 ArrayList。
     * <p>
     * 使用泛型和函数式接口，支持读取任意类型的 ArrayList。
     * 先读取列表大小，然后使用提供的 reader 函数逐个读取元素。
     *
     * @param <T>    列表元素的类型
     * @param input  数据输入流
     * @param reader 用于读取单个元素的函数
     * @return 读取的 ArrayList
     * @throws IOException 如果发生 I/O 错误
     */
    public static <T> ArrayList<T> readArrayList(DataInput input, Function<DataInput, T> reader) throws IOException {
        int size = input.readInt();
        ArrayList<T> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(reader.apply(input));
        }

        return result;
    }

    /**
     * 将 ArrayList 写入到 DataOutput。
     * <p>
     * 使用泛型和函数式接口，支持写入任意类型的 ArrayList。
     * 先写入列表大小，然后使用提供的 consumer 函数逐个写入元素。
     *
     * @param <T>      列表元素的类型
     * @param list     要写入的 ArrayList
     * @param consumer 用于写入单个元素的函数
     * @param output   数据输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static <T> void writeArrayList(
            ArrayList<T> list, BiConsumer<T, DataOutput> consumer, DataOutput output) throws IOException {
        output.writeInt(list.size());

        for (T val : list) {
            consumer.accept(val, output);
        }
    }

    /**
     * 将字符串列表写入到 DataOutput。
     * <p>
     * 使用高效的 UTF-8 编码方式和分页批量写入策略：
     * <ul>
     *   <li>先写入列表大小（int）</li>
     *   <li>使用固定大小的字节数组缓冲区，将多个字符串的数据合并批量写入</li>
     *   <li>对每个字符串，先写入其 UTF-8 字节数组长度（int），然后写入字节数组</li>
     * </ul>
     * 这种方式避免了 writeUTF 的 modified UTF-8 格式开销，直接使用标准 UTF-8 编码。
     * 通过批量合并写入，减少系统调用次数，提高 I/O 性能。
     * <p>
     * 如果列表为 null，会写入 -1 作为标识。
     *
     * @param list   要写入的字符串列表，可以为 null
     * @param output 数据输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void writeStringList(List<String> list, DataOutput output) throws IOException {
        if (list == null) {
            output.writeInt(-1);
            return;
        }

        output.writeInt(list.size());

        // 使用固定大小的字节数组缓冲区，手动管理写入位置
        byte[] buffer = new byte[STRING_PAGE_SIZE];
        int bufferPos = 0;

        for (String str : list) {
            if (str == null) {
                // 写入 -1 标识 null
                int value = -1;
                if (bufferPos + 4 > STRING_PAGE_SIZE) {
                    // 缓冲区满，先写入已有数据
                    output.write(buffer, 0, bufferPos);
                    bufferPos = 0;
                }
                buffer[bufferPos++] = (byte) (value >> 24);
                buffer[bufferPos++] = (byte) (value >> 16);
                buffer[bufferPos++] = (byte) (value >> 8);
                buffer[bufferPos++] = (byte) value;
            } else {
                byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                int length = bytes.length;
                int totalBytes = 4 + length; // int(4) + bytes

                // 检查缓冲区是否有足够空间
                if (bufferPos + totalBytes > STRING_PAGE_SIZE) {
                    // 缓冲区满，先写入已有数据
                    output.write(buffer, 0, bufferPos);
                    bufferPos = 0;
                }

                // 写入长度（int）
                buffer[bufferPos++] = (byte) (length >> 24);
                buffer[bufferPos++] = (byte) (length >> 16);
                buffer[bufferPos++] = (byte) (length >> 8);
                buffer[bufferPos++] = (byte) length;

                // 写入字节数组
                System.arraycopy(bytes, 0, buffer, bufferPos, length);
                bufferPos += length;
            }
        }

        // 写入剩余数据
        if (bufferPos > 0) {
            output.write(buffer, 0, bufferPos);
        }
    }

    /**
     * 从 DataInput 读取字符串列表。
     * <p>
     * 使用高效的 UTF-8 编码方式和缓冲区重用策略：
     * <ul>
     *   <li>先读取列表大小（int）</li>
     *   <li>使用可重用的字节数组缓冲区，避免频繁创建数组</li>
     *   <li>对每个字符串，先读取其 UTF-8 字节数组长度（int），然后读取字节数组并转换为字符串</li>
     * </ul>
     * 这种方式避免了 readUTF 的 modified UTF-8 格式开销，直接使用标准 UTF-8 编码。
     * 通过重用缓冲区，减少对象分配和 GC 压力，提高性能。
     * <p>
     * 如果读取到 -1，则返回 null。
     *
     * @param input 数据输入流
     * @return 读取的字符串列表，如果读取到 null 标识则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    public static ArrayList<String> readStringList(DataInput input) throws IOException {
        int size = input.readInt();
        if (size == -1) {
            return null;
        }

        ArrayList<String> result = new ArrayList<>(size);
        // 可重用的字节数组缓冲区
        byte[] buffer = new byte[STRING_BUFFER_SIZE];

        for (int i = 0; i < size; i++) {
            int length = input.readInt();
            if (length == -1) {
                result.add(null);
            } else {
                // 重用缓冲区读取字符串
                input.readFully(buffer, 0, length);
                result.add(new String(buffer, 0, length, java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        return result;
    }

    /**
     * 跳过 DataInput 中的字符串列表所占用的字节，不反序列化为 List。
     * <p>
     * 与 {@link #readStringList(DataInput)} 使用相同格式，仅消费字节而不分配列表。
     * 用于在存在缓存时跳过流中的 feature list，避免不必要的内存分配。
     *
     * @param input 数据输入流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void skipStringList(DataInput input) throws IOException {
        int size = input.readInt();
        if (size == -1) {
            return;
        }
        byte[] buffer = new byte[Math.min(STRING_BUFFER_SIZE, 8192)];
        for (int i = 0; i < size; i++) {
            int length = input.readInt();
            if (length > 0) {
                int remaining = length;
                while (remaining > 0) {
                    int toRead = Math.min(remaining, buffer.length);
                    input.readFully(buffer, 0, toRead);
                    remaining -= toRead;
                }
            }
        }
    }

}
