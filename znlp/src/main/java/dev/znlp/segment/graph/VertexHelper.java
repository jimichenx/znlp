
package dev.znlp.segment.graph;


import dev.znlp.segment.Nature;

/**
 * 词图顶点辅助工具类，提供创建特殊顶点的静态方法。
 * 
 * <p>该类用于生成词图的起始节点（Start）和结束节点（End），
 * 这些节点是词图结构的重要组成部分，用于路径计算的起点和终点。
 *
 * @author jimichan
 */
public abstract class VertexHelper {

    /**
     * 默认的词频值，用于初始化起始和结束节点的频率。
     */
    private static final int total = 25146057 / 10;

    /**
     * 创建一个线程安全的起始节点（Start节点）。
     * 
     * <p>起始节点具有以下特点：
     * <ul>
     *   <li>长度为1</li>
     *   <li>词性为 {@link Nature#begin}，wordID 对应词典中的 {@code 0Begin}</li>
     *   <li>具有默认的词频值</li>
     * </ul>
     * 
     * @return 起始节点（Start Vertex）
     */
    public static Vertex newBegin() {
        Vertex v = new Vertex(1);
        v.setAbsWordNatureAndFreq(Nature.begin, total);
        return v;
    }

    /**
     * 创建一个线程安全的结束节点（End节点）。
     * 
     * <p>结束节点具有以下特点：
     * <ul>
     *   <li>长度为0</li>
     *   <li>词性为Nature.end</li>
     *   <li>具有默认的词频值</li>
     * </ul>
     * 
     * @return 结束节点（End Vertex）
     */
    public static Vertex newEnd() {
        Vertex v = new Vertex(0);
        v.setAbsWordNatureAndFreq(Nature.end, total);
        return v;
    }


}
