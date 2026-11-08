package dev.znlp.base.common;

import java.util.Map;

/**
 * 以字符（char）为键的原始类型映射接口。
 * <p>
 * 该接口扩展了标准的Map接口，提供了针对char类型键的优化方法，
 * 避免了Character对象的装箱和拆箱操作，提高了性能。
 *
 * @param <V> 值的类型
 */
public interface CharObjectMap<V> extends Map<Character, V> {

    /**
     * 映射中的原始类型条目，由{@link #entries()}的迭代器提供。
     *
     * @param <V> 值的类型
     */
    interface PrimitiveEntry<V> {
        /**
         * 获取该条目的键。
         *
         * @return 字符键
         */
        char key();

        /**
         * 获取该条目的值。
         *
         * @return 值
         */
        V value();

        /**
         * 设置该条目的值。
         *
         * @param value 新值
         */
        void setValue(V value);
    }

    /**
     * 获取指定键对应的值。
     *
     * @param key 键
     * @return 对应的值，如果不存在则返回null
     */
    V get(char key);

    /**
     * 将指定的键值对放入映射中。
     *
     * @param key   键
     * @param value 值
     * @return 该键之前对应的值，如果不存在则返回null
     */
    V put(char key, V value);

    /**
     * 移除指定键的条目。
     *
     * @param key 要移除的键
     * @return 该键之前对应的值，如果不存在则返回null
     */
    V remove(char key);

    /**
     * 获取遍历映射中所有原始类型条目的可迭代对象。
     * <p>
     * 作为优化，迭代器返回的{@link PrimitiveEntry}可能会在迭代过程中发生变化。
     * 调用者不应依赖{@link PrimitiveEntry}键/值的稳定性。
     *
     * @return 原始类型条目的可迭代对象
     */
    Iterable<PrimitiveEntry<V>> entries();

    /**
     * 检查映射中是否包含指定键的值。
     *
     * @param key 要检查的键
     * @return 如果包含该键则返回true，否则返回false
     */
    boolean containsKey(char key);
}
