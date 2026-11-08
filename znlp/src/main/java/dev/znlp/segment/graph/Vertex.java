
package dev.znlp.segment.graph;


import dev.znlp.segment.DictionaryAbsWords;
import dev.znlp.segment.Nature;
import dev.znlp.segment.WordAndNature;

/**
 * 词图（Wordnet）中的词节点（Vertex），表示句子中一个可能的词。
 * 
 * <p>该类具有双重功能：
 * <ul>
 *   <li>作为词图中的一个顶点，存储词的基本信息（长度、词性、词频等）</li>
 *   <li>作为双向链表节点，通过next/prev指针连接同一字符位置的不同词节点</li>
 * </ul>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>存储词的基本信息：长度、词性、词频、词典ID等</li>
 *   <li>支持最短路径计算：存储前驱节点和权重信息</li>
 *   <li>提供词的真实文本获取：通过vertexRow获取原始文本中的词</li>
 * </ul>
 * 
 * @author jimichan
 */
public class Vertex implements WordAndNature {

    /**
     * 词的长度，即词包含的字符个数。
     */
    public int length;

    /**
     * 该词节点所属的{@link VertexRow}（字符位置行）。
     */
    VertexRow vertexRow;

    /**
     * 双向链表的下一个节点，用于连接同一字符位置的其他词节点。
     */
    Vertex next;
    
    /**
     * 双向链表的上一个节点，用于连接同一字符位置的其他词节点。
     */
    Vertex prev;

    ////////////////////////////////////////////

    /**
     * 核心词典的ID，用于表示词在词典中的唯一标识。
     * 不同词ID之间可能存在ngram统计关系。
     */
    public int wordID = -1;

    /**
     * 词的词性（如名词、动词等）。
     */
    public Nature nature;

    /**
     * 词的频率，用于二元模型等分词算法中的概率计算。
     */
    public int freq = 0;

    /**
     * 缓存的原始文本中的真实词，避免重复计算。
     */
    private String realWord;

    // ########################################//
    // 在最短路径相关计算中用到的变量 //
    // ########################################//

    /**
     * 最短路径中到达该节点的前驱节点。
     */
    public Vertex from;

    /**
     * 最短路径中到达该节点的权重值。
     */
    public double weight;

    /// //////////////////////////////////////////

    /**
     * 创建一个指定长度的词节点。
     * 
     * @param length 词的长度
     */
    public Vertex(int length) {
        this.length = (short) length;
    }

    /**
     * 创建一个指定长度、词典ID和频率的词节点。
     * 
     * @param length 词的长度
     * @param wordID 核心词典的ID
     * @param freq   词的频率
     */
    public Vertex(int length, int wordID, int freq) {
        this.length = (short) length;
        this.wordID = wordID;
        this.freq = freq;
    }

    void reuse(int length, int wordID, int freq) {
        this.length = length;
        this.wordID = wordID;
        this.freq = freq;
        this.nature = null;
        this.from = null;
        this.weight = 0;
        this.realWord = null;
        this.next = null;
        this.prev = null;
    }

    void clearRoute() {
        this.from = null;
        this.weight = 0;
        this.realWord = null;
    }

    void detach() {
        this.nature = null;
        this.from = null;
        this.weight = 0;
        this.realWord = null;
        this.next = null;
        this.prev = null;
        this.vertexRow = null;
    }

    /**
     * 复制一个词节点对象，仅复制长度和词信息相关字段。
     * 
     * @param node 要复制的词节点
     */
    public Vertex(Vertex node) {
        this.length = node.length;
        this.wordID = node.wordID;
    }


    /**
     * 获取词的真实文本。
     * 
     * @return 词的真实文本
     */
    @Override
    public String getWord() {
        return realWord();
    }

    /**
     * 获取词的词性名称。
     * 
     * @return 词性名称，如果词性为null则返回空字符串
     */
    @Override
    public String getNatureName() {
        if (nature != null) {
            return nature.name();
        } else {
            return "";
        }
    }

    /**
     * 复制词节点对象，仅复制长度和词信息相关字段。
     * 
     * @return 新创建的词节点对象
     */
    public Vertex copy() {
        return new Vertex(this);
    }

    /**
     * 设定抽象词的词性和频率。
     * 
     * @param nature 词性
     * @param freq   频率
     * @return 当前词节点对象（支持链式调用）
     */
    public Vertex setAbsWordNatureAndFreq(Nature nature, int freq) {
        this.wordID = DictionaryAbsWords.nature2id(nature);
        if (wordID >= 0 && wordID <= DictionaryAbsWords.MaxId) {
            this.nature = nature;
            this.freq = freq;
        }

        return this;
    }

    /**
     * 设定抽象词的词性，使用默认频率(10000)。
     * 
     * @param nature 词性
     * @return 当前词节点对象（支持链式调用）
     */
    public Vertex setAbsWordNatureAndFreq(Nature nature) {
        return setAbsWordNatureAndFreq(nature, 10000);
    }

    /**
     * 判断当前词是否为抽象词。
     * 
     * @return 如果是抽象词则返回true，否则返回false
     */
    public boolean isAbsWord() {
        return wordID >= 0 && wordID <= DictionaryAbsWords.MaxId;
    }

    /**
     * 获取抽象词的标签。
     * 
     * @return 抽象词的标签，如果不是抽象词则返回null
     */
    public String absWordLabel() {
        if (isAbsWord()) {
            return DictionaryAbsWords.id2label(wordID);
        } else {
            return null;
        }
    }

    /**
     * 判断当前词的词性是否为指定词性。
     * 
     * @param nature 要检查的词性
     * @return 如果词性匹配则返回true，否则返回false
     */
    public boolean isNature(Nature nature) {
        return this.nature != null && nature == this.nature;
    }

    /**
     * 获取当前词的接续行（即当前词结束位置的下一个字符位置的{@link VertexRow}）。
     * 如果当前词是结束节点（length=0）或接续行为空，则返回null。
     * 
     * @return 接续的VertexRow，如果不存在则返回null
     */
    public VertexRow follow() {
        if (length == 0) {
            return null;
        }

        VertexRow follow = vertexRow.wordnet.getRow(vertexRow.rowNum + length);

        if (follow == null || follow.isEmpty()) {
            return null;
        }

        return follow;
    }

    /**
     * 获取原始文本中的真实词。
     * 如果尚未计算过，则从vertexRow中提取并缓存。
     * 
     * @return 原始文本中的真实词
     */
    /**
     * 词性标注已经生成过词面时直接复用，否则现取现建，不写回顶点。
     */
    public String realWord() {
        if (realWord != null) {
            return realWord;
        }
        return new String(vertexRow.wordnet.charArray, vertexRow.wordnet.origin + vertexRow.rowNum, length);
    }

    public void rememberSurface(String word) {
        this.realWord = word;
    }

    /**
     * 获取当前词在原始文本中的起始偏移量。
     * 
     * @return 起始偏移量
     */
    public int offset() {
        return vertexRow.rowNum;
    }

    // 注意：hashCode和equals方法只根据length字段进行比较

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + length;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Vertex other = (Vertex) obj;
        return length == other.length;
    }

    @Override
    public String toString() {
        return "Vertex [length=" + length + "]";
    }

    /**
     * 获取双向链表中的下一个节点。
     * 
     * @return 下一个节点
     */
    public Vertex next() {
        return next;
    }

    public Vertex prev() {
        return prev;
    }

    /**
     * 获取词的长度。
     * 
     * @return 词的长度
     */
    public int length() {
        return length;
    }

    /**
     * 获取当前词节点所属的{@link VertexRow}。
     * 
     * @return 所属的VertexRow
     */
    public VertexRow getVertexRow() {
        return vertexRow;
    }

    /**
     * 获取当前词节点在词图中的行号（智能下标）。
     * 
     * @return 行号（智能下标）
     */
    public int getRowNum() {
        return vertexRow.rowNum;
    }

    /**
     * 获取双向链表中的下一个节点（与next()方法相同）。
     * 
     * @return 下一个节点
     */
    public Vertex getNext() {
        return next;
    }

}
