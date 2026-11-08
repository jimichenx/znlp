package dev.znlp.segment;

import dev.znlp.segment.graph.Vertex;

/**
 * 词和词性访问接口。
 * <p>
 * 该接口定义了获取词语和词性信息的标准方法，用于在分词系统中统一访问词的基本属性。
 * 实现了该接口的类可以表示一个带有词性标注的词语单元。
 * <p>
 * 主要实现类包括：
 * <ul>
 *   <li>{@link Token} - 词切分单元，用户可以直接访问此单词的全部属性</li>
 *   <li>{@link Vertex} - WordNode 中的节点，用于分词图结构</li>
 * </ul>
 * <p>
 * 词性信息基于 {@link Nature} 枚举，采用北大词性标注集标准。
 *
 * @see Token
 * @see Nature
 * @see Vertex
 */
public interface WordAndNature {

    /**
     * 获取词语文本。
     * <p>
     * 返回该词语在原始文本中的实际字符串内容。
     *
     * @return 词语文本，不应为 null
     */
    String getWord();

    /**
     * 获取词性名称。
     * <p>
     * 返回词性的字符串表示，通常对应 {@link Nature} 枚举的 {@code name()} 方法返回值。
     * 如果词性未设置，应返回空字符串。
     *
     * @return 词性名称字符串，如果未设置词性则返回空字符串
     * @see Nature#name()
     */
    String getNatureName();
}
