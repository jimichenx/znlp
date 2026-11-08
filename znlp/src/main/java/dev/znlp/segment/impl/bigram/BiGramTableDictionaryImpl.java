package dev.znlp.segment.impl.bigram;

import dev.znlp.base.common.CSRSparseMatrix;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.segment.impl.dict.CoreDictionary;

/**
 * 二元语法表词典的实现类
 * <p>
 * 这是二元语法表词典接口的默认实现，基于CSR稀疏矩阵（CSRSparseMatrix）结构存储词与词之间的共现频率信息。
 * 采用整型存储，高性能。表示一个词接着另外一个词的概率次数。
 * </p>
 * <p>
 * 该实现使用CSR稀疏矩阵格式，适合存储稀疏的二元语法数据，内存占用小且查询效率高。
 * 矩阵的行和列分别对应第一个词和第二个词的ID，值表示共现频率。
 * </p>
 *
 * @author jimichan
 */
public class BiGramTableDictionaryImpl implements BiGramTableDictionary {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BiGramTableDictionaryImpl.class);

    /**
     * 二元语法表文件的资源路径
     */
    public static final String path = "core-dict/CoreDict.bigram.txt";

    /**
     * 基于CSR稀疏矩阵结构的二元语法表，用于高效存储和查询词语对及其频率
     */
    private final CSRSparseMatrix matrix;

    /**
     * 核心词典，用于获取词语ID
     */
    private final CoreDictionary coreDictionary;

    /**
     * 二元语法表中条目的总数量（非零元素的数量）
     */
    private final int size;

    /**
     * 构造函数，直接传入CSR稀疏矩阵和核心词典
     *
     * @param matrix        基于CSR稀疏矩阵结构的二元语法表
     * @param coreDictionary 核心词典，用于获取词语ID
     */
    public BiGramTableDictionaryImpl(CSRSparseMatrix matrix, CoreDictionary coreDictionary) {
        this.matrix = matrix;
        this.coreDictionary = coreDictionary;
        // 计算非零元素的数量
        this.size = calculateSize(matrix);
    }

    /**
     * 计算矩阵中非零元素的数量
     *
     * @param matrix CSR稀疏矩阵
     * @return 非零元素的数量
     */
    private int calculateSize(CSRSparseMatrix matrix) {
        int[] values = matrix.getValues();
        return values != null ? values.length : 0;
    }

    @Override
    public int bigramFrequency(int wordId1, int wordId2) {
        return matrix.get(wordId1, wordId2);
    }

    @Override
    public int bigramFrequency(String word1, String word2) {
        if (word1 == null || word2 == null) {
            return 0;
        }
        int id1 = coreDictionary.getWordID(word1);
        if (id1 < 0) {
            return 0;
        }
        int id2 = coreDictionary.getWordID(word2);
        if (id2 < 0) {
            return 0;
        }
        return matrix.get(id1, id2);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(String word1, String word2) {
        if (word1 == null || word2 == null) {
            return false;
        }
        return bigramFrequency(word1, word2) > 0;
    }

    /**
     * 获取底层的CSRSparseMatrix实例
     * <p>
     * 该方法提供对底层数据结构的访问，主要用于高级操作或性能优化场景。
     * </p>
     *
     * @return CSRSparseMatrix实例
     */
    public CSRSparseMatrix getMatrix() {
        return matrix;
    }
}
