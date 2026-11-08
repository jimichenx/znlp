package dev.znlp.newwords.entity;

/**
 * 简单的整数计数器
 */
public class IntCount {
    private int value = 1;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void increment() {
        this.value++;
    }

    public void addValue(int count) {
        this.value += count;
    }
}
