/**
 * 结构化感知机（Structured Perceptron）实现包。
 * <p>
 * 本包提供了结构化感知机模型的完整实现，包括模型训练、解码和评估等功能。
 * <p>
 * <h2>结构化感知机的基本能力和原理</h2>
 * <p>
 * 结构化感知机是一种用于序列标注任务的线性分类模型，能够处理结构化输出问题。
 * 与传统的多分类感知机不同，结构化感知机考虑了序列中相邻标签之间的依赖关系，
 * 通过引入转移特征来建模标签之间的转移概率。
 * <p>
 * <h3>核心特性：</h3>
 * <ul>
 *   <li><b>稀疏特征表示</b>：使用稀疏特征向量，只存储激活的特征索引，提高计算效率</li>
 *   <li><b>多标签支持</b>：每个特征对应多个标签的权重，相当于为每个标签维护一个独立的感知机</li>
 *   <li><b>转移特征</b>：建模相邻标签之间的转移概率，支持序列标注任务</li>
 *   <li><b>Viterbi解码</b>：考虑全局最优的序列标注，通过动态规划找到最优标签序列</li>
 *   <li><b>快速解码</b>：对于不需要考虑转移概率的场景，提供快速贪心解码模式</li>
 *   <li><b>平均感知机</b>：支持平均感知机训练算法，提高模型泛化能力</li>
 * </ul>
 * <p>
 * <h3>工作原理：</h3>
 * <p>
 * 1. <b>特征提取</b>：对于输入序列中的每个位置，提取一组特征（如当前词、上下文、字符特征等），
 *    这些特征被编码为稀疏向量，只记录激活的特征索引。
 * <p>
 * 2. <b>评分计算</b>：对于每个可能的标签，计算该位置的得分：
 *    <pre>
 *    score(label) = Σ(parameter[featureIndex * labelCount + label])
 *    </pre>
 *    其中，featureIndex 是特征的索引，labelCount 是标签总数。
 * <p>
 * 3. <b>转移概率</b>：在Viterbi解码中，还考虑从前一个标签转移到当前标签的概率：
 *    <pre>
 *    transition_score = parameter[preLabel * labelCount + curLabel]
 *    </pre>
 * <p>
 * 4. <b>解码策略</b>：
 *    <ul>
 *      <li><b>快速解码</b>：不考虑转移概率，每个位置独立选择得分最高的标签</li>
 *      <li><b>Viterbi解码</b>：使用动态规划，找到全局最优的标签序列</li>
 *    </ul>
 * <p>
 * 5. <b>训练过程</b>：使用结构化感知机算法或平均感知机算法，通过比较预测标签和真实标签，
 *    更新相应的参数权重。
 * <p>
 * <h2>parameter数组的布局结构</h2>
 * <p>
 * parameter是一个一维的float数组，逻辑上按照特征-标签的二维结构组织。
 * 数组的总长度为：<code>featureSet.size() * labelCount</code>
 * <p>
 * <h3>基本布局：</h3>
 * <p>
 * 数组按以下方式组织：
 * <pre>
 * parameter = [
 *   F0: [w(0,0), w(0,1), ..., w(0,labelCount-1)],  // 特征0在所有标签上的权重
 *   F1: [w(1,0), w(1,1), ..., w(1,labelCount-1)],  // 特征1在所有标签上的权重
 *   ...
 *   FN: [w(N,0), w(N,1), ..., w(N,labelCount-1)]   // 特征N在所有标签上的权重
 * ]
 * </pre>
 * <p>
 * 其中，w(i,j) 表示特征i在标签j上的权重值。
 * <p>
 * <h3>访问方式：</h3>
 * <p>
 * 要访问特征featureIndex在标签labelIndex上的权重，使用以下公式：
 * <pre>
 * parameter[featureIndex * labelCount + labelIndex]
 * </pre>
 * <p>
 * <h3>特殊特征布局：</h3>
 * <p>
 * 数组的前 <code>(labelCount + 1)</code> 个特征位置被保留用于特殊特征：
 * <ul>
 *   <li><b>位置 0 到 labelCount-1</b>：用于标签间的转移特征</li>
 *   <li><b>位置 labelCount</b>：用于BOS（Begin of Sequence）特征，表示序列开始时的标签转移</li>
 * </ul>
 * <p>
 * 转移特征的访问方式：
 * <pre>
 * // 从前一个标签preLabel转移到当前标签curLabel的权重
 * parameter[preLabel * labelCount + curLabel]
 *
 * // BOS转移到标签labelIndex的权重（序列第一个位置）
 * parameter[labelCount * labelCount + labelIndex]
 * </pre>
 * <p>
 * <h3>示例：</h3>
 * <p>
 * 假设有4个标签（labelCount=4），特征总数为10000（包括特殊特征），则：
 * <ul>
 *   <li>数组总长度 = 10000 * 4 = 40000</li>
 *   <li>前5个特征位置（索引0-4）为特殊特征</li>
 *   <li>特征0-3：标签间转移特征，占用位置 [0-3, 4-7, 8-11, 12-15]</li>
 *   <li>特征4：BOS特征，占用位置 [16-19]</li>
 *   <li>特征5及以后：普通特征，每个特征占用4个连续位置</li>
 * </ul>
 * <p>
 * 例如，要访问普通特征100在标签2上的权重：
 * <pre>
 * parameter[100 * 4 + 2]  // 即 parameter[402]
 * </pre>
 * <p>
 * 要访问从标签1转移到标签3的转移权重：
 * <pre>
 * parameter[1 * 4 + 3]  // 即 parameter[7]
 * </pre>
 * <p>
 * 要访问BOS转移到标签0的权重：
 * <pre>
 * parameter[4 * 4 + 0]  // 即 parameter[16]
 * </pre>
 *
 * @author jimichan
 */
package dev.znlp.perceptron;
