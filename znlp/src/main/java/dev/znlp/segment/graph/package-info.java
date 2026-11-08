package dev.znlp.segment.graph;

/**
 * <h2>词图（Wordnet）数据结构包</h2>
 * <p>
 * 本包提供了用于中文分词的核心数据结构——词图（Wordnet）。
 * 词图是指句子中所有可能的词构成的图结构，是分词算法的基础数据结构。
 *
 * <h3>设计目标</h3>
 * <ul>
 *     <li><b>性能优化</b>：更快的速度、更低的内存占用、尽量实现zero-copy</li>
 *     <li><b>解耦设计</b>：使用BitSet描述路径，避免与Wordnet结构耦合</li>
 *     <li><b>降低复杂度</b>：便于规则程序和识别器进行词语重新划分</li>
 * </ul>
 *
 * <h3>核心组件</h3>
 * <dl>
 *     <dt>{@link dev.znlp.segment.graph.Wordnet}</dt>
 *     <dd>词图核心数据结构，表示句子中所有可能的词构成的图。
 *         使用{@code VertexRow[]}数组存储每一行的节点，实现{@link CharSequence}接口。
 *         支持智能下标索引（-1表示Start，charSize表示End）和子词图提取。</dd>
 *
 *     <dt>{@link dev.znlp.segment.graph.Vertex}</dt>
 *     <dd>词图节点，表示一个可能的词（顶点）。包含词的长度、词典ID、词频、词性等信息。
 *         同时作为链表节点，通过next/prev指针连接，支持最短路径计算（from、weight属性）。</dd>
 *
 *     <dt>{@link dev.znlp.segment.graph.VertexRow}</dt>
 *     <dd>行容器，管理某个字符位置的所有可能词节点。使用双向链表存储Vertex，
 *         按长度从小到大排序，保证同一行中相同长度的Vertex唯一性。</dd>
 *
 *     <dt>{@link dev.znlp.segment.graph.Wordpath}</dt>
 *     <dd>分词路径描述，使用{@link java.util.BitSet}描述文本的切分方式。
 *         BitSet下标[i]为true表示在位置i后插入分割。支持合并、分裂、拆分操作，
 *         提供WordPointer迭代器遍历路径中的词。</dd>
 *
 *     <dt>{@link dev.znlp.segment.pipeline.BestPathAlgorithm}</dt>
 *     <dd>最佳路径算法接口，定义从词图中选择最优路径的方法。
 *         支持多种实现：Viterbi（维特比）、Dijkstra、NShort、前向最大路径等算法。</dd>
 *
 *     <dt>{@link dev.znlp.segment.graph.VertexHelper}</dt>
 *     <dd>顶点辅助工具，提供线程安全的起始和结束节点创建方法。</dd>
 * </dl>
 *
 * <h3>数据结构示例</h3>
 * <pre>
 * #S# [0] #Start 品 [1] 品质 质 [2] 质 和 [3] {和、和服} 服 [4] 服务 务 [5] 务 #E# [6] #End
 * </pre>
 * <p>
 * 其中：
 * <ul>
 *     <li>方括号中的数字表示字符位置（sindex，智能下标）</li>
 *     <li>每个位置可能包含多个不同长度的词</li>
 *     <li>Start和End是特殊的标记节点</li>
 * </ul>
 *
 * <h3>设计亮点</h3>
 * <ul>
 *     <li><b>零拷贝优化</b>：直接使用原始{@code char[]}数组，避免字符串复制</li>
 *     <li><b>内存优化</b>：去除中间节点，使用链表直接连接，减少对象创建</li>
 *     <li><b>路径解耦</b>：Wordpath使用BitSet独立描述路径，不依赖Wordnet内部结构</li>
 *     <li><b>增量排序</b>：VertexRow中节点按长度排序，便于查找和插入操作</li>
 *     <li><b>灵活扩展</b>：支持上下文存储（context），便于业务规则扩展</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *     <li><b>构建词图</b>：将词典匹配结果构建成词图结构</li>
 *     <li><b>路径选择</b>：使用最佳路径算法选择最优分词结果</li>
 *     <li><b>规则处理</b>：通过Wordpath进行词语重新划分和调整</li>
 *     <li><b>识别器</b>：支持命名实体识别等后续处理</li>
 * </ol>
 *
 * <h3>典型使用流程</h3>
 * <pre>
 * // 1. 创建词图
 * Wordnet wordnet = new Wordnet(charArray);
 *
 * // 2. 添加词典匹配的词（Vertex）
 * wordnet.put(offset, length);
 *
 * // 3. 选择最佳路径
 * BestPathAlgorithm algorithm = new ViterbiAlgorithm();
 * Wordpath path = algorithm.select(wordnet);
 *
 * // 4. 遍历路径中的词
 * for (Vertex vertex : path.iteratorVertex()) {
 *     // 处理每个词
 * }
 * </pre>
 *
 * @author jimichan
 * @since 1.0
 */