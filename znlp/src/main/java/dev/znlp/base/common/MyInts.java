package dev.znlp.base.common;

/**
 * @author jimichan
 */
public class MyInts {

    public static byte[] toByteArray(int[] value, int fromIndex, int toIndex) {
        toIndex = Math.min(value.length, toIndex);
        byte[] bytes = new byte[(toIndex - fromIndex) * 4];
        int point = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            int v = value[i];
            bytes[point++] = (byte) (v >> 24);
            bytes[point++] = (byte) (v >> 16);
            bytes[point++] = (byte) (v >> 8);
            bytes[point++] = (byte) v;
        }
        return bytes;
    }

    public static int[] fromByteArrayToArray(byte[] bytes) {
        return fromByteArrayToArray(bytes, new int[bytes.length / 4], bytes.length);
    }

    public static int[] fromByteArrayToArray(byte[] bytes, int[] result, int bytesLen) {
        int intCount = bytesLen / 4;
        for (int i = 0, len = intCount; i < len; i++) {
            int from = i * 4;
            byte b1 = bytes[from++];
            byte b2 = bytes[from++];
            byte b3 = bytes[from++];
            byte b4 = bytes[from++];
            result[i] = b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
        }
        return result;
    }
}
