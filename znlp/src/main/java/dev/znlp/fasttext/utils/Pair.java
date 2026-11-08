package dev.znlp.fasttext.utils;

import java.io.Serializable;

/**
 * 泛型键值对类，用于存储键值对数据。
 * 实现了Serializable接口，支持序列化，
 * 提供了便捷的静态工厂方法用于创建实例。
 *
 * @param <K>    键的类型
 * @param <V>    值的类型
 * @param first  键
 * @param second 值
 */
public record Pair<K, V>(K first, V second) implements Serializable {
    /**
     * 构造Pair实例
     *
     * @param first  键
     * @param second 值
     */
    public Pair {
    }

    /**
     * 创建Pair实例的静态工厂方法
     *
     * @param first  键
     * @param second 值
     * @param <K>    键的类型
     * @param <V>    值的类型
     * @return Pair实例
     */
    public static <K, V> Pair<K, V> of(K first, V second) {
        return new Pair<>(first, second);
    }
}
