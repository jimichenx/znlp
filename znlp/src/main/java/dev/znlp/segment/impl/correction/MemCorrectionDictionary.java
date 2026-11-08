package dev.znlp.segment.impl.correction;

import dev.znlp.base.dat.DoubleArrayTrieMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * 内存中的纠错词典。
 * <p>
 * 一个实例只由一个线程写入。{@link #getTrie()} 可以同时被分词线程读取。
 * 写入先改规则表，再发布一棵新的双数组；已经取走的树保持不变，下一句分词看到新树。
 *
 * @author jimichan
 */
public class MemCorrectionDictionary implements CorrectionDictionary {

    private final TreeMap<String, CorrectionWord> dict;

    private volatile DoubleArrayTrieMap<CorrectionWord> trie;

    public MemCorrectionDictionary(Map<String, CorrectionWord> dict) {
        this.dict = new TreeMap<>(Objects.requireNonNull(dict));
        publish();
    }

    public MemCorrectionDictionary() {
        this.dict = new TreeMap<>();
        publish();
    }

    /**
     * 按当前规则表再发布一次。
     */
    public void rebuild() {
        publish();
    }

    /**
     * 添加一条纠错规则并立即发布。例如：第几套/房。
     */
    public void addRule(String rule) {
        putRule(dict, rule);
        publish();
    }

    /**
     * 加入一批规则，只发布一次。任一规则无法解析时，已发布的树和原规则表都不变。
     */
    public void addRules(Collection<String> rules) {
        Objects.requireNonNull(rules);
        if (rules.isEmpty()) {
            return;
        }
        TreeMap<String, CorrectionWord> extra = new TreeMap<>();
        for (String rule : rules) {
            putRule(extra, rule);
        }
        dict.putAll(extra);
        publish();
    }

    /**
     * 用这批规则替换全部规则，只发布一次。流在中途失败时，已发布的树和原规则表都不变。
     */
    public void replaceAll(Stream<String> rules) {
        Objects.requireNonNull(rules);
        TreeMap<String, CorrectionWord> next = new TreeMap<>();
        rules.forEach(rule -> putRule(next, rule));
        dict.clear();
        dict.putAll(next);
        publish();
    }

    public void removeRule(String word) {
        dict.remove(word);
        publish();
    }

    public void clear() {
        dict.clear();
        publish();
    }

    @Override
    public DoubleArrayTrieMap<CorrectionWord> getTrie() {
        return trie;
    }

    private void publish() {
        trie = dict.isEmpty() ? null : new DoubleArrayTrieMap<>(dict);
    }

    private static void putRule(TreeMap<String, CorrectionWord> target, String rule) {
        CorrectionWord correctionWord = CorrectionWord.parse(rule);
        target.put(correctionWord.path(), correctionWord);
    }
}
