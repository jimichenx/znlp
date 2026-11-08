package dev.znlp.fasttext.dictionary;

import dev.znlp.fasttext.utils.HashUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 快速词映射类，用于高效管理词到ID的映射和词的属性。
 * 采用哈希表+线性探测的方式实现快速查找，是FastText词典的核心数据结构。
 */
public class FastWordMap {

    /**
     * 词哈希到词ID的映射数组，用于快速查找词对应的ID
     */
    public int[] wordHash2WordId;
    
    /**
     * 词列表，存储所有词的Entry对象
     */
    public final List<Entry> wordList;
    
    /**
     * 标签前缀，用于区分词和标签
     */
    public final String label;

    /**
     * 线性探测表长度：至少比词表多一个空槽，避免 size=1/2 时 find 死循环。
     */
    public static int hashTableSize(int vocabSize) {
        if (vocabSize <= 0) {
            return 1;
        }
        return Math.max((int) Math.ceil(vocabSize / 0.75), vocabSize + 1);
    }

    /**
     * 构造 FastWordMap 实例。
     *
     * @param label            标签前缀
     * @param vocabSize        哈希表容量
     * @param initWordListSize 词列表初始容量
     */
    public FastWordMap(String label, int vocabSize, int initWordListSize) {
        this.label = label;
        this.wordHash2WordId = new int[Math.max(vocabSize, 1)];
        Arrays.fill(wordHash2WordId, -1);
        this.wordList = new ArrayList<>(Math.max(16, initWordListSize));
    }

    /**
     * 构造FastWordMap实例（从已有数据）
     *
     * @param wordHash2WordId 词哈希到词ID的映射数组
     * @param wordList 词列表
     * @param label 标签前缀
     */
    public FastWordMap(int[] wordHash2WordId, List<Entry> wordList, String label) {
        this.wordHash2WordId = wordHash2WordId;
        this.wordList = wordList;
        this.label = label;
    }

    /**
     * 获取词列表大小
     *
     * @return 词列表大小
     */
    public int size() {
        return wordList.size();
    }

    /**
     * 根据ID获取词的Entry对象
     *
     * @param id 词ID
     * @return 词的Entry对象
     */
    public Entry get(int id) {
        return wordList.get(id);
    }

    /**
     * 添加一个词到词映射中
     *
     * @param w 词
     * @param count 词的计数
     */
    public void add(String w, int count) {
        int h = find(w);
        int id = wordHash2WordId[h];

        if (id == -1) {
            wordHash2WordId[h] = wordList.size();
            Entry entry = new Entry(w, count, getType(w));
            wordList.add(entry);
        } else {
            wordList.get(id).count += count;
        }
    }

    /**
     * 根据ID获取词
     *
     * @param id 词ID
     * @return 词
     */
    public String getWord(int id) {
        return wordList.get(id).word;
    }

    /**
     * 根据ID获取词的类型
     *
     * @param id 词ID
     * @return 词的类型
     */
    public EntryType getType(int id) {
        return wordList.get(id).type;
    }

    /**
     * 根据词获取词的类型
     *
     * @param w 词
     * @return 词的类型
     */
    public EntryType getType(String w) {
        return w.startsWith(label) ? EntryType.label : EntryType.word;
    }

    /**
     * 根据词获取词ID
     *
     * @param w 词
     * @return 词ID，不存在则返回-1
     */
    public int getId(String w) {
        int id = find(w);
        return id == -1 ? -1 : wordHash2WordId[id];
    }

    /**
     * 根据词和哈希值获取词ID
     *
     * @param w 词
     * @param h 哈希值
     * @return 词ID，不存在则返回-1
     */
    public int getId(String w, int h) {
        int id = find(w, h);
        return id == -1 ? -1 : wordHash2WordId[id];
    }

    /**
     * 查找词在哈希表中的位置
     *
     * @param w 词
     * @return 词在哈希表中的位置
     */
    public int find(String w) {
        return find(w, HashUtils.fnv1aHash(w));
    }

    /**
     * 根据词和哈希值查找词在哈希表中的位置
     *
     * @param w 词
     * @param hash 哈希值
     * @return 词在哈希表中的位置
     */
    public int find(String w, int hash) {
        int word2intSize = wordHash2WordId.length;
        int id = Integer.remainderUnsigned(hash, word2intSize);
        int start = id;
        while (wordHash2WordId[id] != -1 && !wordList.get(wordHash2WordId[id]).word.equals(w)) {
            id = (id + 1) % word2intSize;
            if (id == start) {
                throw new IllegalStateException("Word hash table is full");
            }
        }
        return id;
    }
    
    /**
     * 初始化词哈希到词ID的映射数组
     */
    public void initWordHash2WordId() {
        Arrays.fill(wordHash2WordId, -1);
        for (int i = 0; i < size(); i++) {
            int h = find(wordList.get(i).word);
            wordHash2WordId[h] = i;
        }
    }
}
