/**
 * DoubleArrayTrieMap: Java implementation of Darts (Double-ARray Trie System)
 * <p/>
 * <p>
 * Copyright(C) 2001-2007 Taku Kudo &lt;taku@chasen.org&gt;<br />
 * Copyright(C) 2009 MURAWAKI Yugo &lt;murawaki@nlp.kuee.kyoto-u.ac.jp&gt;
 * Copyright(C) 2012 KOMIYA Atsushi &lt;komiya.atsushi@gmail.com&gt;
 * </p>
 * <p/>
 * <p>
 * The contents of this file may be used under the terms of either of the GNU
 * Lesser General Public License Version 2.1 or later (the "LGPL"), or the BSD
 * License (the "BSD").
 * </p>
 */

/*
 * 源代码参考和部分引用来自 https://github.com/hankcs/HanLP https://github.com/NLPchina/ansj_seg
 */
package dev.znlp.base.dat;


import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * 双数组Trie树映射类，提供键值对的高效存储和查询功能。
 * <p>
 * 该类将字符串键映射到任意类型的值，通过双数组Trie实现高效的字符串匹配和查询。
 * <p>
 * 主要功能：
 * - 精确匹配查询
 * - 前缀搜索
 * - 多模式匹配
 * - 键值对更新
 *
 * @param <T> 值的类型
 * @author jimichan
 */
public class DoubleArrayTrieMap<T> {

    private final ArrayList<T> values;
    private final DoubleArrayTrie dat;

    /**
     * 构造一个双数组Trie映射。
     *
     * @param dat     双数组Trie对象
     * @param values  值列表，与dat中的键一一对应
     */
    public DoubleArrayTrieMap(DoubleArrayTrie dat, ArrayList<T> values) {
        this.values = values;
        this.dat = dat;
    }

    /**
     * 从有序的TreeMap构造双数组Trie映射。
     *
     * @param treeMap 有序的键值对映射
     */
    public DoubleArrayTrieMap(TreeMap<String, T> treeMap) {
        ArrayList<String> keys = new ArrayList<>(treeMap.size());
        ArrayList<T> values = new ArrayList<>(treeMap.size());
        treeMap.forEach((key, value) -> {
            keys.add(key);
            values.add(value);
        });
        this.dat = DoubleArrayTrie.builder().build(keys);
        this.values = values;
    }

    /**
     * 从键列表和值列表构造双数组Trie映射。
     *
     * @param keys   键列表，必须有序
     * @param values 值列表，与键列表一一对应
     */
    public DoubleArrayTrieMap(List<String> keys, ArrayList<T> values) {
        this(DoubleArrayTrie.builder().build(keys), values);
    }

    /**
     * 获取双数组Trie映射的匹配器，用于多模式匹配。
     *
     * @param text   要匹配的文本
     * @param offset 匹配的起始位置
     * @return 匹配器对象
     */
    public MapMatcher match(String text, int offset) {
        return new MapMatcher(dat.matcher(text, offset));
    }

    /**
     * 获取双数组Trie映射的匹配器，用于多模式匹配。
     *
     * @param text   要匹配的字符数组
     * @param offset 匹配的起始位置
     * @return 匹配器对象
     */
    public MapMatcher match(char[] text, int offset) {
        return new MapMatcher(dat.matcher(text, offset));
    }

    public MapMatcher match(char[] text, int offset, int end) {
        return new MapMatcher(dat.matcher(text, offset, end));
    }

    /**
     * 获取双数组Trie映射的匹配器，用于多模式匹配，从文本起始位置开始匹配。
     *
     * @param text 要匹配的文本
     * @return 匹配器对象
     */
    public MapMatcher match(String text) {
        return new MapMatcher(dat.matcher(text, 0));
    }

    /**
     * 获取双数组Trie映射的匹配器，用于多模式匹配，从字符数组起始位置开始匹配。
     *
     * @param text 要匹配的字符数组
     * @return 匹配器对象
     */
    public MapMatcher match(char[] text) {
        return new MapMatcher(dat.matcher(text, 0));
    }

    /**
     * 从左到右取最长词，命中后从该词的下一个位置继续。
     */
    public MapLongMatcher matchLong(char[] text) {
        return new MapLongMatcher(dat.matcherLong(text));
    }

    public MapLongMatcher matchLong(char[] text, int offset, int end) {
        return new MapLongMatcher(dat.matcherLong(text, offset, end));
    }

    public MapLongMatcher matchLong(String text) {
        return new MapLongMatcher(dat.matcherLong(text));
    }

    /**
     * 双数组Trie映射的匹配器，用于多模式匹配。
     */
    public class MapMatcher {

        private final DATMatcher datMatcher;

        /**
         * 构造匹配器。
         *
         * @param datMatcher 双数组Trie匹配器
         */
        public MapMatcher(DATMatcher datMatcher) {
            this.datMatcher = datMatcher;
        }

        /**
         * 查找下一个匹配项。
         *
         * @return 是否找到匹配项
         */
        public boolean next() {
            return datMatcher.next();
        }

        /**
         * 获取当前匹配项的起始位置。
         *
         * @return 起始位置
         */
        public int getBegin() {
            return datMatcher.getBegin();
        }

        /**
         * 获取当前匹配项的长度。
         *
         * @return 匹配长度
         */
        public int getLength() {
            return datMatcher.getLength();
        }

        /**
         * 获取当前匹配项的值。
         *
         * @return 匹配的值，未找到返回null
         */
        public T getValue() {
            int index = datMatcher.getIndex();
            if (index == -1) {
                return null;
            } else {
                return values.get(index);
            }
        }

        /**
         * 获取当前匹配项的索引。
         *
         * @return 索引值
         */
        public int getIndex() {
            return datMatcher.getIndex();
        }
    }

    /**
     * 最长匹配。{@link #next()} 为真时，起点和长度对应当前最长词。
     */
    public class MapLongMatcher {

        private final DATLongMatcher matcher;

        public MapLongMatcher(DATLongMatcher matcher) {
            this.matcher = matcher;
        }

        public boolean next() {
            return matcher.next();
        }

        public int getBegin() {
            return matcher.getBegin();
        }

        public int getLength() {
            return matcher.getLength();
        }

        public T getValue() {
            int index = matcher.getIndex();
            if (index < 0) {
                return null;
            }
            return values.get(index);
        }

        public int getIndex() {
            return matcher.getIndex();
        }
    }

    /**
     * 获取键值对的数量。
     *
     * @return 键值对数量
     */
    public int size() {
        return values.size();
    }

    /**
     * 查找指定键的索引。
     *
     * @param key 要查找的键
     * @return 找到返回索引，否则返回-1
     */
    public int indexOf(CharSequence key) {
        return dat.indexOf(key, 0, 0, 0);
    }

    /**
     * 从指定位置开始查找键的索引。
     *
     * @param key     要查找的键
     * @param pos     开始查找的位置
     * @param len     查找的长度，&lt;=0表示整个键
     * @param nodePos 开始查找的节点位置，&lt;=0表示根节点
     * @return 找到返回索引，否则返回-1
     */
    public int indexOf(CharSequence key, int pos, int len, int nodePos) {
        return dat.indexOf(key, pos, len, nodePos);
    }

    /**
     * 查找字符数组键的索引。
     *
     * @param chars 要查找的字符数组
     * @param pos   开始查找的位置
     * @param len   查找的长度
     * @return 找到返回索引，否则返回-1
     */
    public int indexOf(char[] chars, int pos, int len) {
        return dat.indexOf(chars, pos, len, 0);
    }

    /**
     * 从指定位置开始查找字符数组键的索引。
     *
     * @param keyChars 要查找的字符数组
     * @param pos      开始查找的位置
     * @param len      查找的长度，&lt;=0表示整个数组
     * @param nodePos  开始查找的节点位置，&lt;=0表示根节点
     * @return 找到返回索引，否则返回-1
     */
    public int indexOf(char[] keyChars, int pos, int len, int nodePos) {
        return dat.indexOf(keyChars, pos, len, nodePos);
    }

    /**
     * 精确查询指定键的值。
     *
     * @param key 要查找的键
     * @return 找到返回对应的值，否则返回null
     */
    public T get(CharSequence key) {
        int index = indexOf(key);
        if (index >= 0) {
            return values.get(index);
        }
        return null;
    }

    /**
     * 从指定位置开始查询键的值。
     *
     * @param key    要查找的键
     * @param offset 开始查找的偏移量
     * @param length 查找的长度
     * @return 找到返回对应的值，否则返回null
     */
    public T get(CharSequence key, int offset, int length) {
        int index = indexOf(key, offset, length, 0);
        if (index >= 0) {
            return values.get(index);
        }
        return null;
    }

    /**
     * 查询字符数组键的值。
     *
     * @param key 要查找的字符数组
     * @return 找到返回对应的值，否则返回null
     */
    public T get(char[] key) {
        int index = indexOf(key, 0, key.length, 0);
        if (index >= 0) {
            return values.get(index);
        }
        return null;
    }

    /**
     * 从指定位置开始查询字符数组键的值。
     *
     * @param key    要查找的字符数组
     * @param offset 开始查找的偏移量
     * @param len    查找的长度
     * @return 找到返回对应的值，否则返回null
     */
    public T get(char[] key, int offset, int len) {
        int index = indexOf(key, offset, len, 0);
        if (index >= 0) {
            return values.get(index);
        }
        return null;
    }

    /**
     * 获取指定索引的值。
     *
     * @param index 索引值
     * @return 对应的值
     */
    public T getValueAt(int index) {
        return values.get(index);
    }

    /**
     * 检查是否包含指定的键。
     *
     * @param key 要检查的键
     * @return 包含返回true，否则返回false
     */
    public boolean containsKey(String key) {
        return indexOf(key) >= 0;
    }

    /**
     * 更新指定键的值。
     *
     * @param key   要更新的键
     * @param value 新的值
     * @return 更新成功返回true，否则返回false（键不存在）
     */
    public boolean set(String key, T value) {
        int index = indexOf(key);
        if (index >= 0) {
            values.set(index, value);
            return true;
        }
        return false;
    }

    /**
     * 从值数组中提取指定下标的值，不进行参数校验。
     *
     * @param index 下标
     * @return 对应的值
     */
    public T get(int index) {
        return values.get(index);
    }

    /**
     * 获取内部的DoubleArrayTrie对象。
     *
     * @return DoubleArrayTrie对象
     */
    public DoubleArrayTrie getDat() {
        return dat;
    }

    /**
     * 获取值列表。
     *
     * @return 值列表
     */
    public ArrayList<T> getValues() {
        return values;
    }
}