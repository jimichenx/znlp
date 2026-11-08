package dev.znlp.base.bintrie;

/**
 * 字典树（Trie）接口，提供字符串键值对的存储和查询功能。
 * <p>
 * 字典树是一种树形数据结构，用于高效地存储和检索字符串集合。
 * 该接口定义了基本的查询方法，支持通过字符数组、字符序列和字符串进行查询。
 *
 * @param <T> 值的类型
 * @author jimichan
 */
public interface Trie<T> {

    /**
     * 通过字符数组查询对应的值。
     *
     * @param key 字符数组形式的键
     * @return 对应的值，如果不存在则返回null
     */
    T get(char[] key);

    /**
     * 通过字符序列查询对应的值。
     *
     * @param key 字符序列形式的键
     * @return 对应的值，如果不存在则返回null
     */
    T get(CharSequence key);

    /**
     * 通过字符数组的指定区间查询对应的值。
     *
     * @param key    字符数组形式的键
     * @param offset 起始偏移量
     * @param len    长度
     * @return 对应的值，如果不存在则返回null
     */
    T get(char[] key, int offset, int len);

    /**
     * 检查是否包含指定的键。
     *
     * @param key 字符串形式的键
     * @return 如果包含该键则返回true，否则返回false
     */
    boolean containsKey(String key);
}
