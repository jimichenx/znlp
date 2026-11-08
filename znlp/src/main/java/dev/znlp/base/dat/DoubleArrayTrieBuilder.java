package dev.znlp.base.dat;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * 双数组Trie构建器接口，定义了构建双数组Trie的方法。
 * <p>
 * 实现该接口的类负责将有序的字符串列表转换为高效的双数组Trie结构，
 * 支持从TreeSet或List<String>构建双数组Trie。
 */
public interface DoubleArrayTrieBuilder {

    /**
     * 从有序的TreeSet构建双数组Trie。
     * <p>
     * TreeSet 本身有序且无重复。空集合会在 {@link #build(List)} 被拒绝。
     *
     * @param set 有序的字符串集合
     * @return 构建好的DoubleArrayTrie对象
     */
    default DoubleArrayTrie build(TreeSet<String> set) {
        return this.build(new ArrayList<>(set));
    }

    /**
     * 从有序的字符串列表构建双数组Trie。
     * <p>
     * 列表必须按字典序排列且不能有重复键，否则抛出 {@link IllegalArgumentException}。
     *
     * @param keys 有序的字符串列表
     * @return 构建好的DoubleArrayTrie对象
     */
    DoubleArrayTrie build(List<String> keys);
}
