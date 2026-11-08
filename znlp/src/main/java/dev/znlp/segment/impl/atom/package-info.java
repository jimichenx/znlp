/**
 * 原子级别分词插件包
 * <p>
 * 本包提供了基于规则的原子级别分词算法，用于识别时间、数字、数量词、英文单词等特殊词语模式。
 * 通过高性能的模式匹配技术，在一次扫描中识别多种字符串模式。
 * </p>
 *
 * <h3>核心组件</h3>
 * <dl>
 *     <dt>{@link dev.znlp.segment.impl.atom.AtomSplitAlgorithm}</dt>
 *     <dd>原子分词算法的主要实现类，实现了 {@link dev.znlp.segment.pipeline.WordSplitAlgorithm} 接口。
 *         通过模板匹配的方式识别各种特殊模式的词语。</dd>
 *
 *     <dt>{@link dev.znlp.segment.impl.atom.TemplateType}</dt>
 *     <dd>模板类型枚举，定义了支持的模式类型：
 *         <ul>
 *             <li>TIME - 时间表达式（如：2023年12月1日、三天、5小时）</li>
 *             <li>NUMBER - 数字表达式（如：123、-456、三点五）</li>
 *             <li>MQ - 数量词（如：三个、5公里、两斤）</li>
 *             <li>WORD - 英文单词</li>
 *             <li>CONNECT - 连接符</li>
 *         </ul>
 *     </dd>
 *
 *     <dt>{@link dev.znlp.segment.impl.atom.AtomTemplateParser}</dt>
 *     <dd>模板解析器，用于解析模板表达式并生成所有可能的展开结果。
 *         支持以下格式：
 *         <ul>
 *             <li>{Z[1,2]} - 重复次数列表</li>
 *             <li>{Z[1-2]} - 重复次数范围</li>
 *             <li>{(日|月)} - 可选项</li>
 *         </ul>
 *     </dd>
 *
 *     <dt>{@link dev.znlp.segment.impl.atom.DefaultTemplate}</dt>
 *     <dd>默认模板定义类，包含了常用的时间、数字、数量词等模板规则。</dd>
 * </dl>
 *
 * <h3>工作原理</h3>
 * <pre>
 * 1. 快速扫描阶段
 *    ├─ 检查文本中是否包含数字或字母
 *    └─ 如果不包含，直接跳过后续处理
 *
 * 2. 字符归一化阶段
 *    ├─ 将阿拉伯数字归一化为 'N'
 *    ├─ 将英文字母归一化为 'A'
 *    ├─ 将中文数字归一化为 'Z'
 *    └─ 将小数点归一化为 'N'
 *
 * 3. 模式匹配阶段
 *    ├─ 使用 DAT (Double Array Trie) 进行高效匹配
 *    ├─ 匹配预定义的模板模式
 *    └─ 将匹配结果添加到词网中
 *
 * 4. 特殊模式识别阶段
 *    ├─ Email 地址识别
 *    └─ 复杂字母数字组合识别（如：iphone7、fm101.1）
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 示例1：使用默认模板
 * AtomSplitAlgorithm algorithm = new AtomSplitAlgorithm();
 *
 * // 示例2：在分词流水线中使用
 * Lexer lexer = PipelineLexerBuilder.builder()
 *     .addWordSplitAlgorithm(new AtomSplitAlgorithm())
 *     .setBestPathAlgorithm(new ViterbiBestPathAlgorithm())
 *     .build();
 *
 * // 示例3：自定义模板
 * AtomSplitAlgorithm.AtomSplitAlgorithmTemplateProvider customProvider =
 *     new AtomSplitAlgorithm.AtomSplitAlgorithmTemplateProvider() {
 *         public StringIntDATMap load() {
 *             // 加载自定义模板
 *             return myCustomTemplates;
 *         }
 *     };
 * AtomSplitAlgorithm algorithm = new AtomSplitAlgorithm(customProvider);
 * </pre>
 *
 * <h3>性能特点</h3>
 * <ul>
 *     <li><b>快速预扫描</b>：通过快速检查避免不必要的处理</li>
 *     <li><b>单次扫描</b>：只需遍历文本一次即可识别所有模式</li>
 *     <li><b>高效匹配</b>：使用 DAT 数据结构进行快速模式匹配</li>
 *     <li><b>内存优化</b>：使用 Set 存储中文数字字符集，查询效率高</li>
 * </ul>
 *
 * <h3>扩展指南</h3>
 * <p>
 * 要添加新的模式识别规则：
 * <ol>
 *     <li>在 {@link dev.znlp.segment.impl.atom.DefaultTemplate} 中添加新的模板规则</li>
 *     <li>使用 addTemplate() 方法添加模板，支持模板语法的自动展开</li>
 *     <li>确保新模板关联到正确的 {@link dev.znlp.segment.impl.atom.TemplateType}</li>
 *     <li>重新构建 DAT 数据结构</li>
 * </ol>
 * </p>
 *
 * @author jimichan
 * @see dev.znlp.segment.pipeline.WordSplitAlgorithm
 * @see dev.znlp.segment.graph.Wordnet
 * @since 1.0
 */
package dev.znlp.segment.impl.atom;

