/**
 * <h2>分词结果收集与子词切分包</h2>
 * <p>
 * 本包提供了从词图（Wordnet）和最优路径（Wordpath）中收集分词结果的核心组件，
 * 以及对分词结果进行子词切分的扩展功能。这些组件是分词流水线的最后阶段，
 * 负责将内部数据结构转换为用户可用的分词结果。
 *
 * <h3>设计目标</h3>
 * <ul>
 *     <li><b>灵活收集</b>：支持从不同的分词算法结果中收集词语</li>
 *     <li><b>子词切分</b>：为搜索引擎等场景提供细粒度的子词切分能力</li>
 *     <li><b>可扩展性</b>：通过策略模式支持多种子词切分算法</li>
 *     <li><b>词典补全</b>：为基于统计的分词器补充词典信息</li>
 * </ul>
 *
 * <h3>核心组件</h3>
 * <dl>
 *     <dt>{@link dev.znlp.segment.collector.SentenceCollector}</dt>
 *     <dd>分词结果收集器的默认实现，实现了 {@link dev.znlp.segment.pipeline.WordTermCollector} 接口。
 *         负责从 Wordpath 中遍历词语，应用子词切分策略，并将结果发送给消费者。
 *         支持配置多个子词切分器和词典信息补全器。</dd>
 *
 *     <dt>{@link dev.znlp.segment.collector.SentenceCollectorBuilder}</dt>
 *     <dd>SentenceCollector 的构建器，提供流式 API 配置收集器的各种策略：
 *         <ul>
 *             <li>indexSubword() - 索引子词切分</li>
 *             <li>smartSubword() - 智能子词切分</li>
 *             <li>ruleBaseSubword() - 基于规则的子词切分</li>
 *             <li>fillCoreDict() - 补充核心词典信息</li>
 *             <li>fillCustomDict() - 补充自定义词典信息</li>
 *         </ul>
 *     </dd>
 *
 *     <dt>{@link dev.znlp.segment.collector.SubwordComputer}</dt>
 *     <dd>子词切分计算器接口，定义了子词切分的标准方法。
 *         实现类负责将一个词语切分为多个子词，结果保存在 WordTerm 的 subword 字段中。
 *         支持责任链模式，多个切分器可以按顺序执行，直到某个切分器成功处理。</dd>
 *
 *     <dt>{@link dev.znlp.segment.collector.SubwordInfoSetup}</dt>
 *     <dd>子词信息设置接口，用于在收集结果前补充词图中的词典信息。
 *         主要用于感知机、CRF 等统计分词器，这些分词器的词图中缺少词典信息，
 *         需要通过词典匹配来补充子词信息。</dd>
 * </dl>
 *
 * <h3>子词切分实现</h3>
 * <dl>
 *     <dt>{@link dev.znlp.segment.collector.IndexSubwordComputer}</dt>
 *     <dd><b>索引子词切分</b>：收集词语的所有可能子词组合，用于搜索引擎索引。
 *         <p>例如："北京大学" → ["北京", "大学", "京大"]</p>
 *         <p>特点：</p>
 *         <ul>
 *             <li>基于词图中已有的词语信息</li>
 *             <li>可配置最小子词长度（默认2）</li>
 *             <li>适合搜索召回场景</li>
 *         </ul>
 *     </dd>
 *
 *     <dt>{@link dev.znlp.segment.collector.SmartSubwordComputer}</dt>
 *     <dd><b>智能子词切分</b>：使用 Viterbi 算法选择最佳的子词切分方式。
 *         <p>例如："省政府" → ["省", "政府"]（如果二元语法支持）</p>
 *         <p>特点：</p>
 *         <ul>
 *             <li>考虑词语间的二元语法关系</li>
 *             <li>2字词不切分，3字词需要二元语法支持</li>
 *             <li>人名（Nature.nr）不切分</li>
 *             <li>支持黑名单回调函数</li>
 *             <li>适合需要高质量子词的场景</li>
 *         </ul>
 *     </dd>
 *
 *     <dt>{@link dev.znlp.segment.collector.RuleDictSubwordComputer}</dt>
 *     <dd><b>基于规则的子词切分</b>：使用预定义的规则词典进行切分。
 *         <p>规则格式：</p>
 *         <ul>
 *             <li>"ABC" → 不切分，返回 [3]</li>
 *             <li>"A/BC" → 切分为 A 和 BC，返回 [1, 2]</li>
 *             <li>"A?C" → 通配符匹配，如 "ABC" → [1, 2]</li>
 *         </ul>
 *         <p>特点：</p>
 *         <ul>
 *             <li>支持精确匹配和通配符匹配</li>
 *             <li>规则可动态添加和重建</li>
 *             <li>适合特定领域的切分需求</li>
 *         </ul>
 *     </dd>
 * </dl>
 *
 * <h3>词典信息补全实现</h3>
 * <dl>
 *     <dt>{@link dev.znlp.segment.collector.CoreDictSubwordInfoSetup}</dt>
 *     <dd><b>核心词典补全</b>：使用核心词典匹配文本，将匹配到的词语添加到词图中。
 *         <p>应用场景：</p>
 *         <ul>
 *             <li>感知机分词后补充词典信息</li>
 *             <li>为子词切分提供更多候选词</li>
 *         </ul>
 *     </dd>
 *
 *     <dt>{@link dev.znlp.segment.collector.CustomDictSubwordInfoSetup}</dt>
 *     <dd><b>自定义词典补全</b>：使用自定义词典（StringIntDATMap）匹配文本，
 *         将匹配到的词语添加到词图中。
 *         <p>应用场景：</p>
 *         <ul>
 *             <li>领域专用词典补充</li>
 *             <li>用户自定义词汇识别</li>
 *         </ul>
 *     </dd>
 * </dl>
 *
 * <h3>工作流程</h3>
 * <pre>
 * 1. 词典信息补全阶段（SubwordInfoSetup）
 *    ├─ 遍历 setupList
 *    └─ 调用 fill() 方法补充词图信息
 *
 * 2. 词语遍历阶段
 *    ├─ 从 Wordpath 获取词语迭代器
 *    ├─ 遍历每个 Vertex
 *    └─ 构造 WordTerm 对象
 *
 * 3. 子词切分阶段（SubwordComputer）
 *    ├─ 遍历 subwordComputer 列表
 *    ├─ 调用 run() 方法尝试切分
 *    └─ 如果某个切分器成功，跳过后续切分器
 *
 * 4. 结果输出阶段
 *    └─ 将 WordTerm 发送给 Consumer
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 示例1：构建带索引子词切分的收集器
 * SentenceCollector collector = new SentenceCollectorBuilder()
 *     .indexSubword(2)  // 最小子词长度为2
 *     .fillCoreDict(coreDictionary)
 *     .build();
 *
 * // 示例2：构建带智能子词切分的收集器
 * SentenceCollector collector = new SentenceCollectorBuilder()
 *     .smartSubword(algorithm, coreDictionary, biGramDict)
 *     .fillCoreDict(coreDictionary)
 *     .build();
 *
 * // 示例3：构建带规则子词切分的收集器
 * DefaultSubwordRuleDict ruleDict = new DefaultSubwordRuleDict();
 * ruleDict.add("北京/大学");  // 北京大学 → 北京 + 大学
 * ruleDict.add("中/?国");    // 中华国、中美国 → 中 + 华国
 * // 规则在 add 时即完成编译，无需再调用构建方法
 *
 * SentenceCollector collector = new SentenceCollectorBuilder()
 *     .ruleBaseSubword(Collections.singletonList(ruleDict))
 *     .build();
 *
 * // 示例4：在分词流水线中使用
 * Lexer lexer = PipelineLexerBuilder.builder()
 *     .setBestPathAlgorithm(new ViterbiBestPathAlgorithm())
 *     .addWordSplitAlgorithm(new CoreDictionarySplitAlgorithm())
 *     .setCollector(collector)  // 设置收集器
 *     .build();
 * </pre>
 *
 * <h3>设计模式</h3>
 * <ul>
 *     <li><b>策略模式</b>：SubwordComputer 定义了子词切分策略接口</li>
 *     <li><b>责任链模式</b>：多个 SubwordComputer 按顺序执行，直到某个成功处理</li>
 *     <li><b>构建器模式</b>：SentenceCollectorBuilder 提供流式 API 配置收集器</li>
 *     <li><b>模板方法模式</b>：SentenceCollector 定义了收集流程的骨架</li>
 * </ul>
 *
 * <h3>扩展指南</h3>
 * <p>
 * 要实现自定义的子词切分算法：
 * <ol>
 *     <li>实现 {@link dev.znlp.segment.collector.SubwordComputer} 接口</li>
 *     <li>在 run() 方法中实现切分逻辑</li>
 *     <li>将切分结果设置到 term.setSubword()</li>
 *     <li>返回 true 表示处理成功，false 表示未处理</li>
 *     <li>通过 SentenceCollectorBuilder 注册自定义切分器</li>
 * </ol>
 * </p>
 *
 * <h3>性能考虑</h3>
 * <ul>
 *     <li><b>子词切分开销</b>：索引子词切分最快，智能子词切分需要额外的 Viterbi 计算</li>
 *     <li><b>词典匹配开销</b>：基于 DAT 结构，匹配效率高</li>
 *     <li><b>责任链优化</b>：将最常用的切分器放在前面，减少不必要的计算</li>
 *     <li><b>按需切分</b>：只有需要子词的场景才配置子词切分器</li>
 * </ul>
 *
 * <h3>应用场景</h3>
 * <ul>
 *     <li><b>搜索引擎</b>：使用索引子词切分提高召回率</li>
 *     <li><b>文本分析</b>：使用智能子词切分获得更细粒度的语义单元</li>
 *     <li><b>领域应用</b>：使用规则子词切分处理特定领域的切分需求</li>
 *     <li><b>统计分词增强</b>：为感知机等统计分词器补充词典信息</li>
 * </ul>
 *
 * @author jimichan
 * @see dev.znlp.segment.pipeline.WordTermCollector
 * @see dev.znlp.segment.Token
 * @see dev.znlp.segment.graph.Wordnet
 * @see dev.znlp.segment.graph.Wordpath
 * @since 1.0
 */
package dev.znlp.segment.collector;

