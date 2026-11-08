package dev.znlp.segment.reader;

import dev.znlp.base.dat.DoubleArrayTrieMap;

import java.util.TreeMap;

/**
 * 停用词词典,基于DAT的实现
 * <p>
 * 可以动态新增、删减停用词。修改后需要[rebuild]操作。
 *
 * @author jimichan
 */
public class DefaultStopWordDict implements StopWordDict {

    // value true 表示是停用词，false 表示不是停用词
    private final TreeMap<String, Boolean> map = new TreeMap<>();

    private DoubleArrayTrieMap<Boolean> dat;

    private boolean isEmpty = true;

    public DefaultStopWordDict() {
        TreeMap<String, Boolean> initialMap = new TreeMap<>();
        initialMap.put("This Is Empty Flag", true);
        this.dat = new DoubleArrayTrieMap<>(initialMap);
    }

    public void clear() {
        map.clear();
    }

    public void rebuild() {
        if (map.isEmpty()) {
            isEmpty = true;
            return;
        }

        isEmpty = false;
        dat = new DoubleArrayTrieMap<>(map);
    }

    public void add(Iterable<String> words) {
        for (String word : words) {
            map.put(word, true);
        }
    }

    public void add(String word) {
        map.put(word, true);
    }

    public void addNoStop(String word) {
        map.put(word, false);
    }

    public void remove(String word) {
        map.remove(word);
    }

    /**
     * 丢掉编辑缓冲。已经 {@link #rebuild()} 的查询结果还在。
     * 之后的新增从空表开始，再次 rebuild 会替换掉当前词典。
     */
    void discardEditBuffer() {
        map.clear();
    }

    boolean hasPendingEdits() {
        return !map.isEmpty();
    }

    @Override
    public boolean contains(String word) {
        if (isEmpty) {
            return false;
        }
        return Boolean.TRUE.equals(dat.get(word));
    }
}