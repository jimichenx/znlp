package dev.znlp.perceptron;

/**
 * 当前线程正在解码的字符窗口。未绑定时，窗口就是整个数组。
 * <p>
 * 感知机特征按窗口内下标取字，这样词图可以共享原文，不必为每句复制 {@code char[]}。
 */
public final class CharWindow {

    private static final ThreadLocal<int[]> STATE = ThreadLocal.withInitial(() -> new int[]{0, -1});

    private CharWindow() {
    }

    public static void bind(int origin, int length) {
        int[] state = STATE.get();
        state[0] = origin;
        state[1] = length;
    }

    public static void clear() {
        int[] state = STATE.get();
        state[0] = 0;
        state[1] = -1;
    }

    public static int length(char[] text) {
        int length = STATE.get()[1];
        return length < 0 ? text.length : length;
    }

    public static char at(char[] text, int position) {
        return text[STATE.get()[0] + position];
    }
}
