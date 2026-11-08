package dev.znlp.fasttext.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 小端写入，对齐 Facebook FastText 二进制格式（x86 原生字节序）。
 */
public final class LittleEndianData {

    private LittleEndianData() {
    }

    public static void writeInt(OutputStream out, int v) throws IOException {
        out.write(v);
        out.write(v >>> 8);
        out.write(v >>> 16);
        out.write(v >>> 24);
    }

    public static void writeLong(OutputStream out, long v) throws IOException {
        writeInt(out, (int) v);
        writeInt(out, (int) (v >>> 32));
    }

    public static void writeDouble(OutputStream out, double v) throws IOException {
        writeLong(out, Double.doubleToRawLongBits(v));
    }

    public static void writeBoolean(OutputStream out, boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    /**
     * 以 C 字符串形式写入 UTF-8 文本（结尾 0 字节）。
     */
    public static void writeCString(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(0);
    }

    public static void writeFloats(OutputStream out, float[] data) throws IOException {
        int chunk = 1024;
        byte[] buf = new byte[chunk * 4];
        int i = 0;
        while (i < data.length) {
            int n = Math.min(chunk, data.length - i);
            ByteBuffer.wrap(buf, 0, n * 4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(data, i, n);
            out.write(buf, 0, n * 4);
            i += n;
        }
    }
}
