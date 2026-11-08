package dev.znlp.fasttext.dictionary;

import dev.znlp.base.common.IntArrayList;

/**
 * 词典条目类，用于存储词典中的词及其属性。
 * 包含词、词频、类型和子词列表等信息，是词典的基本组成单元。
 */
public class Entry {
    /**
     * 词的字符串表示
     */
    public final String word;
    
    /**
     * 词的出现频率
     */
    public long count;
    
    /**
     * 词的类型（词或标签）
     */
    public final EntryType type;
    
    /**
     * 词的子词列表
     */
    public IntArrayList subwords;

    /**
     * 构造Entry实例
     *
     * @param word 词的字符串表示
     * @param count 词的出现频率
     * @param type 词的类型
     */
    public Entry(String word, long count, EntryType type) {
        this.word = word;
        this.count = count;
        this.type = type;
        this.subwords = new IntArrayList(0);
    }
}
