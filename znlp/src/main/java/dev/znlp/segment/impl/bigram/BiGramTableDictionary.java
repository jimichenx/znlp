package dev.znlp.segment.impl.bigram;

/**
 * 二元语法表词典接口
 * <p>
 * 二元语法表（BiGram Table）用于存储词与词之间的共现频率信息，
 * 是中文分词系统中用于计算词语转移概率的重要数据结构。
 * 该接口提供了查询两个词语之间二元语法频率的基本操作。
 * </p>
 * <p>
 * 二元语法表基于双数组Trie（DAT）结构实现，提供高效的查询性能。
 * 词典以"词语1@词语2"的形式作为key，共现频率作为value进行存储。
 * </p>
 *
 * @author jimichan
 */
public interface BiGramTableDictionary {

    /**
     * 获取两个词语ID之间的二元语法频率
     * <p>
     * 通过两个词语的ID查询它们之间的共现频率。
     * </p>
     *
     * @param wordId1 第一个词语的ID
     * @param wordId2 第二个词语的ID
     * @return 两个词语的共现频率，如果不存在则返回0
     */
    int bigramFrequency(int wordId1, int wordId2);

    /**
     * 获取两个词语之间的二元语法频率
     * <p>
     * 通过两个词语的字符串形式查询它们之间的共现频率。
     * </p>
     *
     * @param word1 第一个词语
     * @param word2 第二个词语
     * @return 两个词语的共现频率，如果不存在则返回0
     */
    int bigramFrequency(String word1, String word2);

    /**
     * 获取二元语法表中条目的总数量
     *
     * @return 二元语法表中条目的数量
     */
    int size();

    /**
     * 判断二元语法表中是否存在指定的词语对
     *
     * @param word1 第一个词语
     * @param word2 第二个词语
     * @return 如果存在该词语对则返回true，否则返回false
     */
    boolean contains(String word1, String word2);
}
