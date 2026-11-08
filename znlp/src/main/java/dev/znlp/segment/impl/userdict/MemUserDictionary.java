package dev.znlp.segment.impl.userdict;

import dev.znlp.base.common.CharNormUtils;
import dev.znlp.base.dat.StringIntDATMap;

import java.util.Collection;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * 内存中的用户词典。
 * <p>
 * 一个实例只由一个线程写入。{@link #getTrie()} 可以同时被分词线程读取。
 * 写入先改词表，再发布一棵新的双数组；已经取走的树保持不变，下一句分词看到新树。
 *
 * @author jimichan
 */
public class MemUserDictionary implements UserDictionary {

    private static final int WEIGHT = 1000;

    private final TreeMap<String, Integer> dict;

    private volatile StringIntDATMap trie;

    public MemUserDictionary() {
        this.dict = new TreeMap<>();
        publish();
    }

    /**
     * 用一份词表的副本构建。之后原映射的修改不会进入本词典。
     */
    public MemUserDictionary(TreeMap<String, Integer> dict) {
        this.dict = new TreeMap<>(Objects.requireNonNull(dict));
        publish();
    }

    /**
     * 按当前词表再发布一次。
     */
    public void rebuild() {
        publish();
    }

    /**
     * 加入一个词并立即发布。原词和规范化后的词都进入词表。
     */
    public void addWord(String word) {
        putWord(dict, word);
        publish();
    }

    /**
     * 加入一批词，只发布一次。任一词无法写入时，已发布的树和原词表都不变。
     */
    public void addWords(Collection<String> words) {
        Objects.requireNonNull(words);
        if (words.isEmpty()) {
            return;
        }
        TreeMap<String, Integer> extra = new TreeMap<>();
        for (String word : words) {
            putWord(extra, word);
        }
        dict.putAll(extra);
        publish();
    }

    /**
     * 用这批词替换全部词条，只发布一次。流在中途失败时，已发布的树和原词表都不变。
     */
    public void replaceAll(Stream<String> words) {
        Objects.requireNonNull(words);
        TreeMap<String, Integer> next = new TreeMap<>();
        words.forEach(word -> putWord(next, word));
        dict.clear();
        dict.putAll(next);
        publish();
    }

    public void removeWord(String word) {
        String normalizedWord = CharNormUtils.convert(word);
        dict.remove(word);
        dict.remove(normalizedWord);
        publish();
    }

    public void clear() {
        dict.clear();
        publish();
    }

    @Override
    public StringIntDATMap getTrie() {
        return trie;
    }

    private void publish() {
        trie = dict.isEmpty() ? null : new StringIntDATMap(dict);
    }

    private static void putWord(TreeMap<String, Integer> target, String word) {
        String normalized = CharNormUtils.convert(word);
        target.put(word, WEIGHT);
        target.put(normalized, WEIGHT);
    }
}
