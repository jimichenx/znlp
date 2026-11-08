package dev.znlp.segment.impl.dict;


import org.jetbrains.annotations.NotNull;
import dev.znlp.base.dat.StringIntDATMap.DATMapMatcherInt;

/**
 * 核心词典接口
 * <p>
 * 核心词典是中文分词系统中的基础词典，包含约20万以上的常用词汇及其词频信息。
 * 该接口提供了词典的基本操作，包括词语查询、词频获取、匹配等功能。
 * 词典基于双数组Trie（DAT）结构实现，提供高效的词语查找和匹配性能。
 * </p>
 *
 * @author jimichan
 */
public interface CoreDictionary {

    /**
     * 在文本的指定位置开始进行词典匹配
     * <p>
     * 使用双数组Trie结构在文本的指定偏移位置开始匹配，返回匹配器对象，
     * 可用于遍历所有匹配的词语。
     * </p>
     *
     * @param text   待匹配的字符数组
     * @param offset 匹配起始位置在字符数组中的偏移量
     * @return 词典匹配器，用于遍历匹配结果
     */
    DATMapMatcherInt match(char[] text, int offset);

    /**
     * 在 {@code [offset, end)} 内匹配，结束位置不含。
     */
    DATMapMatcherInt match(char[] text, int offset, int end);

    /**
     * 获取词典中所有词语的词频总和
     *
     * @return 所有词语的词频总和
     */
    int totalFreq();

    /**
     * 获取字符数组中指定位置和长度的词语对应的ID
     *
     * @param chars 字符数组
     * @param pos   词语在字符数组中的起始位置
     * @param len   词语的长度
     * @return 词语ID，如果词典中不存在该词语则返回-1
     */
    int wordId(char[] chars, int pos, int len);

    /**
     * 获取字符序列对应的词语ID
     *
     * @param word 词语的字符序列
     * @return 词语ID，如果词典中不存在该词语则返回-1
     */
    int wordId(CharSequence word);

    /**
     * 根据词语ID获取该词语的词频
     *
     * @param wordID 词语ID
     * @return 词语的词频，如果ID无效则返回0或-1（具体取决于实现）
     */
    int wordFreq(int wordID);

    /**
     * 获取指定词语的ID
     * <p>
     * 与 {@link #wordId(CharSequence)} 功能相同，提供字符串参数的重载方法。
     * </p>
     *
     * @param word 词语字符串
     * @return 词语ID，如果词典中不存在该词语则返回-1
     */
    int getWordID(String word);

    /**
     * 判断词典中是否包含指定的词语
     *
     * @param word 待查询的词语，不能为null
     * @return 如果词典中包含该词语则返回true，否则返回false
     */
    boolean contains(@NotNull String word);

    /**
     * 获取词典中词语的总数量
     *
     * @return 词典中词语的数量
     */
    int size();
}
