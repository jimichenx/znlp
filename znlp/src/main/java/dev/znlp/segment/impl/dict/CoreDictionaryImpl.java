package dev.znlp.segment.impl.dict;

import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.base.dat.StringIntDATMap.DATMapMatcherInt;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

/**
 * 核心词典的实现类
 * <p>
 * 这是核心词典接口的默认实现，基于双数组Trie（DAT）结构存储约20万以上的常用词汇。
 * 词典以词语为key，词频为value进行存储。
 * </p>
 * <p>
 * 原始词典文件格式为每行一个词语和词频，用空格分隔：
 * <pre>
 * word1 freq1
 * word2 freq2
 * ...
 * </pre>
 * 为了提高加载速度，词典会被缓存为DAT二进制格式。
 * </p>
 *
 * @author jimichan
 */
public class CoreDictionaryImpl implements CoreDictionary {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(CoreDictionaryImpl.class);

    /**
     * 核心词典文件的资源路径
     */
    public static final String path = "core-dict/CoreDict.txt";

    /**
     * 词典中所有词语的词频总和
     */
    private final int totalFreq;

    /**
     * 基于双数组Trie结构的词典映射，用于高效存储和查询词语及其词频
     */
    private final StringIntDATMap trie;

    /**
     * 构造函数，直接传入词频总和和DAT字典树
     *
     * @param totalFreq 词典中所有词语的词频总和
     * @param trie      基于双数组Trie结构的词语和词频映射
     */
    public CoreDictionaryImpl(int totalFreq, StringIntDATMap trie) {
        this.totalFreq = totalFreq;
        this.trie = trie;
    }

    @Override
    public int totalFreq() {
        return totalFreq;
    }

    /**
     * 获取指定词语的词频
     * <p>
     * 如果词语不存在于词典中，返回0而不是-1。
     * </p>
     *
     * @param key 词语字符串
     * @return 词语的词频，如果词语不存在则返回0
     */
    public int wordFreq(String key) {
        int c = trie.get(key);
        if (c == -1) {
            c = 0;
        }
        return c;
    }

    /**
     * 根据词语ID获取该词语的词频
     *
     * @param wordID 词语ID
     * @return 词语的词频
     */
    @Override
    public int wordFreq(int wordID) {
        return trie.get(wordID);
    }

    @Override
    public int wordId(CharSequence key) {
        return trie.indexOf(key);
    }

    /**
     * 获取字符序列中指定位置和长度的词语对应的ID
     * <p>
     * 从指定的节点位置开始查找，用于优化连续匹配的性能。
     * </p>
     *
     * @param key     字符序列
     * @param pos     词语在字符序列中的起始位置
     * @param len     词语的长度
     * @param nodePos Trie树中的起始节点位置
     * @return 词语ID，如果词典中不存在该词语则返回-1
     */
    public int wordId(CharSequence key, int pos, int len, int nodePos) {
        return trie.indexOf(key, pos, len, nodePos);
    }

    @Override
    public int wordId(char[] chars, int pos, int len) {
        return trie.indexOf(chars, pos, len);
    }

    /**
     * 获取字符数组中指定位置和长度的词语对应的ID
     * <p>
     * 从指定的节点位置开始查找，用于优化连续匹配的性能。
     * </p>
     *
     * @param keyChars 字符数组
     * @param pos      词语在字符数组中的起始位置
     * @param len      词语的长度
     * @param nodePos  Trie树中的起始节点位置
     * @return 词语ID，如果词典中不存在该词语则返回-1
     */
    public int wordId(char[] keyChars, int pos, int len, int nodePos) {
        return trie.indexOf(keyChars, pos, len, nodePos);
    }


    /**
     * 判断词典中是否包含指定的词语
     *
     * @param key 待查询的词语字符串
     * @return 如果词典中包含该词语则返回true，否则返回false
     */
    @Override
    public boolean contains(String key) {
        return trie.indexOf(key) >= 0;
    }

    /**
     * 获取指定词语的ID
     *
     * @param word 词语字符串
     * @return 词语ID，如果词典中不存在该词语则返回-1
     */
    @Override
    public int getWordID(String word) {
        return trie.indexOf(word);
    }

    /**
     * 在文本的指定位置开始进行词典匹配
     *
     * @param text   待匹配的字符数组
     * @param offset 匹配起始位置在字符数组中的偏移量
     * @return 词典匹配器，用于遍历匹配结果
     */
    @Override
    public DATMapMatcherInt match(char[] text, int offset) {
        return trie.match(text, offset);
    }

    @Override
    public DATMapMatcherInt match(char[] text, int offset, int end) {
        return trie.match(text, offset, end);
    }

    /**
     * 获取词典中词语的总数量
     *
     * @return 词典中词语的数量
     */
    @Override
    public int size() {
        return trie.size();
    }
}
