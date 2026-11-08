# ES 分词插件与词库服务

> 状态：**未开工的设计讨论**，不是已确定方案。
> 起因：讨论「如果要做 Elasticsearch 分词插件，难度大不大、是否要针对每个 ES 版本做一个」，延伸出词库管理与一个独立的词库 HTTP 服务。
> 本文记录已核实的事实、已形成的判断、以及尚未解决的问题。后续真正排期时，先读「实施顺序」和「未决问题」两节。

置信度标注约定：**〔已验证〕**= 读过源码确认；**〔需核实〕**= 依据记忆或行业惯例，动手前必须查目标版本源码/文档；**〔判断〕**= 主观取舍，可以推翻。

---

## 一、结论摘要

1. **分词器到 Lucene `TokenStream` 的适配是小工作**（200 行内）。现有 API 的形状几乎就是 TokenStream，`Token` 已带 `offset` / `posInc` / `subword`，`WordSegmentReader.segment(Reader)` 已是拉模式迭代器。〔已验证〕
2. **必须逐 ES 版本出包**，但**源码不需要逐版本分叉**：用 CI 矩阵预构建 + 确定性下载 URL，网站只做字符串拼接。真正撕裂代码的是跨大版本（8→9），不是跨 minor。
3. **把耦合下沉到 Lucene，不要绑 ES**。中间加一层 `znlp-lucene`，一个 jar 覆盖 ES 8.x / 9.x / Solr / 直接用 Lucene 的项目，把版本压力压缩到最薄的一层注册壳上。〔判断〕
4. **词库不能放在分词热路径上查 ES**。ES 索引只做管理面，插件读「编译后的带版本快照」，走 HTTP。
5. **动态词库要按风险分级**，不能笼统宣传「动态词库」。停用词可实时；用户词典需 reindex；核心词典扩词应走版本发布；二元统计表永远不动态。〔判断〕
6. **词库 HTTP 服务应该比 ES 插件先做**，因为它可以兼容 IK 的 `remote_ext_dict`，从而在 ES 插件存在之前就拿到用户。〔判断〕

---

## 二、已验证：现有代码对这套方案的支持度

### 2.1 分词接口已经是对的形状〔已验证〕

| 位置 | 事实 | 对插件的意义 |
| :--- | :--- | :--- |
| [Token.java:29](../../znlp/src/main/java/dev/znlp/segment/Token.java#L29) | `offset` 字段 | → `OffsetAttribute` |
| [Token.java:31](../../znlp/src/main/java/dev/znlp/segment/Token.java#L31) | `posInc`，默认 1 | → `PositionIncrementAttribute`。纯 NLP 库基本用不到这个字段，留着说明当时就按索引场景设计 |
| [Token.java:36](../../znlp/src/main/java/dev/znlp/segment/Token.java#L36) | `subword` 子词列表 | 一个词位挂多个 term（IK `mode=smart` 那种），用 `posInc=0` 追加 |
| [WordSegmenter.java:37](../../znlp/src/main/java/dev/znlp/segment/WordSegmenter.java) | `segment(char[], Consumer<Token>)` | 推模式核心方法 |
| [WordSegmentReader.java:66](../../znlp/src/main/java/dev/znlp/segment/WordSegmentReader.java) | `segment(Reader) -> TokenSequence` | **拉模式**，ES 传字段的入口本来就是 `Reader`，直接对上 |
| [LexerIterator.java:16](../../znlp/src/main/java/dev/znlp/segment/reader/LexerIterator.java) | `implements Iterator<Token>`，内部 `ArrayDeque` 缓冲 | `incrementToken()` 就是 `hasNext/next` |
| [WordSegmenter.java](../../znlp/src/main/java/dev/znlp/segment/WordSegmenter.java) javadoc | 实现类无状态、可多线程共享 | 「进程内加载一次、全节点共享」成立，这是 ES 插件最关键的前提 |
| [ZnlpSystem.java](../../znlp/src/main/java/dev/znlp/ZnlpSystem.java) | `getInstance()` 静态 Holder | 天然单例 |

**一个要注意的点**〔已验证〕：`LexerIterator` **不展开 subword**，[LexerIterator.java:66](../../znlp/src/main/java/dev/znlp/segment/reader/LexerIterator.java#L66) 附近只给子词补算了段落偏移。子词展开要在适配层或一个新的 reader 装饰器里做，并且要给子词设 `posInc=0`。

### 2.2 打包条件已经比较干净〔已验证〕

- `settings.gradle.kts` 有 `znlp` / `znlp-resources` / `znlp-examples` / `znlp-benchmarks` 四个模块，Java 17。
- `znlp` 的核心依赖只有 `compileOnly` 的 `annotations` / `slf4j-api` / `log4j-api` —— **运行时零第三方依赖**，这对避开 ES 的 JarHell 极其有利。**打包时不要 shade 任何 `org.apache.lucene.*` / `org.elasticsearch.*` / `com.fasterxml.jackson.*` 进去。**
- 全仓库目前没有任何 Lucene / ES / Analyzer / TokenStream 相关代码，是绿地。
- License 是 Apache 2.0，与 ES 生态兼容。

### 2.3 二元表按「词面」存储 —— 动态扩词在算法上是安全的〔已验证〕

这是最关键的一条发现，差点误判成否决项。

`wordId` **不是稳定 ID**，而是排序后核心词典里的下标：[CoreDictionaryImpl.java:89](../../znlp/src/main/java/dev/znlp/segment/impl/dict/CoreDictionaryImpl.java#L89) `wordId(key) → trie.indexOf(key)`，且 `wordFreq(wordID) → trie.get(wordID)`。

**如果** 35MB 的 `CoreDict.bigram.txt` 存的是 wordID，那么往核心词典加一个词会让其后所有 ID 全体平移，二元表静默错位、输出垃圾统计，动态扩词直接不可行。

实际不是。文件格式是词面：

```
0Begin
	6 慰问信 陪伴 年复一年 ...
```

非缩进行是左上下文词，缩进行是 `\t频率 wordB1 wordB2 ...`（[BiGramTableDictionaryFactory.java:218](../../znlp/src/main/java/dev/znlp/segment/impl/bigram/BiGramTableDictionaryFactory.java#L218) `parseSourceFile`）。ID 在**加载时**才通过 `coreDictionary.getWordID()` 解析（[:281](../../znlp/src/main/java/dev/znlp/segment/impl/bigram/BiGramTableDictionaryFactory.java#L281) 左侧、[:293](../../znlp/src/main/java/dev/znlp/segment/impl/bigram/BiGramTableDictionaryFactory.java#L293) 右侧），查不到的对被 `idB >= 0` 丢弃。

**结论**：动态加词不会让二元表错乱，只是新词没有二元接续统计 —— 优雅降级。抽象词用 `\u0001` 前缀钉在 ID 0~9（[DictionaryAbsWords.java:16](../../znlp/src/main/java/dev/znlp/segment/DictionaryAbsWords.java#L16)）也正好躲过了平移问题。

### 2.4 动手前必须先修的潜在 bug：bigram 磁盘缓存键不完整〔已验证〕

`create()` 只用 **bigram 源文件**的 hash 做缓存键：

- [:109](../../znlp/src/main/java/dev/znlp/segment/impl/bigram/BiGramTableDictionaryFactory.java#L109) `String hash = readHash(dictResource);`
- [:113](../../znlp/src/main/java/dev/znlp/segment/impl/bigram/BiGramTableDictionaryFactory.java#L113) `tryLoadFromCache(hash)`
- [:151](../../znlp/src/main/java/dev/znlp/segment/impl/bigram/BiGramTableDictionaryFactory.java#L151) `CacheFileUtils.getCacheFile(config, CACHE_FILE_PREFIX + hash)`

但缓存文件里存的是**解析后的 `(idA, idB)` 整数对**。

今天核心词典是 classpath 里的静态文件，两者永远同步，问题没暴露。**一旦词库可以动态变**：核心词典变了、bigram 文件没变 → 缓存命中 → 拿着旧 ID 查新词典 → **所有二元频率静默读错，不报任何错**。

修法二选一：
- 缓存键改成 `hash(coreDict) + "|" + hash(bigram)`；
- 或在缓存文件头写入 coreDict hash，加载时校验，不匹配则重建。

**这一条要排在所有动态词库工作之前。** 它是那种会毁掉线上效果但没有日志的 bug。

### 2.5 资源分发管道已存在，但它只解决分发不解决热更新〔已验证〕

`NlpResourceLoader` 已经是工厂链 SPI：

- [NlpResourceFactory.java:29](../../znlp/src/main/java/dev/znlp/base/resources/NlpResourceFactory.java#L29) 工厂接口
- [NlpResourceLoader.java:105](../../znlp/src/main/java/dev/znlp/base/resources/NlpResourceLoader.java#L105) 装配顺序：`dataDirs(File+Jar) → classpath`
- 外围已有 `cacheDir` 本地缓存、`hashResource()` 校验、`clearCache()`、`getStatistics()`、`resourceExists()`
- 甚至已有一个从远程 Maven 仓库拉 jar 的实现（[:376](../../znlp/src/main/java/dev/znlp/base/resources/NlpResourceLoader.java#L376)）

所以加一个 `HttpNlpResourceFactory` 是一个类的事。

**但要看清边界**：`CoreDictionaryFactory` 在 `create()` 里一次性加载并构建 DAT，之后不可变。资源工厂再丰富，**也不会让一个正在运行的节点换掉词典**。

热更新需要一套目前完全不存在、且独立于资源加载的机制：

```
轮询版本 → 版本变了才拉全量 → 构建新的不可变词典实例
        → AtomicReference 替换 → 失败保留旧实例并告警
```

工作量估计要把这两件事分开算，别按「加个 factory」估。

---

## 三、ES 版本矩阵

### 3.1 事实与置信度

| 论断 | 置信度 |
| :--- | :--- |
| ES 加载插件时读 `plugin-descriptor.properties` 的 `elasticsearch.version` 与运行版本比对，**不匹配拒绝加载**（记忆中是完整版本号含 patch） | 〔需核实〕拿目标版本 `PluginDescriptor.java` 确认。这条决定 3.3 的 descriptor 改写方案能不能做 |
| 同一大版本内插件 API 稳定，源码不需要分叉 | 〔较有信心〕 |
| 6→7 从 `getTokenizerFactory()` 换成构造函数注入 + `AnalysisProvider<T>`；8→9 升 Lucene 10、要求 Java 21、Security Manager 机制变更 | 〔较有信心〕具体项按版本核实 |
| `@Inject` 构造函数只声明需要的参数，所以用低版本 API 编译的 jar 通常能在高版本加载 | 〔需核实〕 |
| ES 用 `FilterClassLoader` 屏蔽 `java.*` / `org.apache.lucene.*` / `org.elasticsearch.*`，插件不能自带同名类 | 〔需核实〕 |
| 8.x 需要 `plugin-security.policy` 才能读插件目录外文件或开网络连接 | 〔需核实〕 |
| **Elastic Cloud / Serverless 不允许安装自定义插件**，只开放官方捆绑插件 | 〔有信心〕 |
| IK 的 `remote_ext_dict` 是「轮询 URL + `Last-Modified`/`ETag` + 304 + 全量重读纯文本」 | 〔需核实协议细节〕按 IK 文档核对 |
| 大量 IK 用户现状是 nginx + cron 生成 words.txt | 〔判断〕 |

最后一条 Cloud 限制要尽早写进 README：它意味着目标用户是**自建 ES 的团队**，托管 ES 用户拿不到插件。

### 3.2 模块划分

```
znlp/             现有纯 Java，零运行时依赖，永远不为版本重编
znlp-lucene/      Analyzer + TokenStream + 子词展开。只跟 Lucene major 走
znlp-es-plugin/   只剩 Plugin / AnalysisPlugin / Settings 注册，跟 ES 版本走
znlp-solr/        顺手就有；Solr 的版本耦合比 ES 温和
```

Lucene 的 `TokenStream` / `CharTermAttribute` / `AnalysisModule` SPI 在 8→9 之间基本没动〔需核实〕，所以 `znlp-lucene` 一个 jar 可覆盖 ES 8.x + 9.x + Solr + 直接用 Lucene 的项目。**版本矩阵的压力全部集中在最薄的那层壳上。**

IK 本身也是这个结构：核心是 Lucene Analyzer，ES 插件是壳。〔判断，但符合公开认知〕

### 3.3 分发策略：预构建矩阵，不做在线编译

- **不要**按请求触发 Gradle 构建。一次 1~2 分钟，不适合在线。
- CI 矩阵预构建（GitHub Actions `strategy.matrix.es`），产出发布到 GitHub Release，URL 规律固定：

  ```
  https://github.com/zaprt/znlp/releases/download/es-8.17.3/znlp-es-8.17.3.zip
  ```

- 网站的版本下拉框**只做字符串拼接**，把 URL 填进 `bin/elasticsearch-plugin install <url>`。无后端、无存储，现有 Astro 静态站即可承载。
- 想支持长尾版本再补兜底：**编译时对齐要支持的最低 ES 版本**（`compileOnly org.elasticsearch:elasticsearch:8.11.x`）以取最小 API 面，然后对未测试版本用 JSZip 在浏览器里替换 descriptor 现打 zip。这条是「用户自选风险」，必须标 **best-effort** 并在文档里写明「出问题请先降级到测过的版本」。descriptor 字段（`java.version`、`classname`、9.x 新增项）要按大版本分别维护模板。〔需核实〕

**不要宣称「兼容所有 ES」。** 选定 1 个大版本 + 3~5 个具体版本号。

### 3.4 采用漏斗（按性价比排序）

| 层级 | 给用户什么 | 成本 |
| :--- | :--- | :--- |
| 1 | `docker run` 一行起一个带分词的 ES，**镜像 tag 就是 ES 版本** | CI 加一个 buildx job，多架构 amd64/arm64（arm64 对 Apple Silicon 很重要） |
| 2 | 选版本 → 复制 `elasticsearch-plugin install <url>` | 静态页拼字符串 |
| 3 | 选版本 → 直接下载（含 sha512） | Release 资产 |
| 4 | 未测试版本：浏览器现打 zip | JSZip，一次性 |
| 5 | 在线 `_analyze` 对比 IK | 要后端，**最后做** |

第 1 层比所有打包技巧都有效：它消灭的是「我得先有一个 ES 才能试」这一步。README 第一屏就该是那一行 `docker run`。

第 5 层是中文分词插件最有说服力的展示（左 IK 右 znlp），但需要能跑 ES 的沙箱，且不要指望在浏览器里跑。建议靠 1~3 层先收集用户，有人问了再做。

---

## 四、资源瘦身：比版本号更大的摩擦

当前 `znlp-resources` 全量 **247MB**：

| 目录 | 大小 | 搜索分词是否需要 |
| :--- | :--- | :--- |
| `cws-model` | 176M | **不需要**（感知机分词模型，默认走二元路径） |
| `core-dict` | 36M | 需要（`CoreDict.bigram.txt` 35M + `CoreDict.txt` 2.5M） |
| `personname` | 10M | 待定 |
| `pos-model` | 9.2M | 可选 |
| `ner` | 7.7M | 不需要 |
| `custom-dict` | 6.6M | 需要（用户词典基线） |
| `ts-dict` | 1.1M | 不需要 |
| `pinyin` | 532K | 不需要 |
| `stopword-dict` | 8K | 需要 |

**先做一个 Gradle 任务产出 `znlp-resources-search`**（`core-dict` + `stopword-dict`，可选 `pos-model`），目标 45MB 以下。

不做这一步的代价：每个用户 `docker pull` 多下 200MB、每个节点启动多加载 200MB 进内存、10 节点集群多 2GB。**这个摩擦比版本号大得多，而且是用户每天都在付的成本。**

---

## 五、动态词库

### 5.1 按风险分级，不要笼统宣传

| 级别 | 内容 | 改的是什么 | 动态更新 | 需要 reindex |
| :--- | :--- | :--- | :--- | :--- |
| L1 | 停用词表 | token 取舍 | 可实时 | 不需要 |
| L2 | 用户词典 / 机构名 / 人名 | 边界，选路后合并 | 安全 | 需要 |
| L3 | 核心词典扩词 | 词图候选 + 词频 | 可行但影响 Viterbi | 必须 |
| L4 | 二元统计表 | 全库统计 | **不动态，走版本发布** | — |

L1/L2 做热更新，L3 做发版，L4 永远静态。**把四件事都说成「动态词库」会误导用户** —— 用户对「动态」的期待其实是 L3，而 L3 恰恰是最不该热的那层。〔判断〕

L2 的语义限制来自 [SegmentRecipe.java](../../znlp/src/main/java/dev/znlp/segment/recipe/SegmentRecipe.java)：用户词典在**选路之后**合并，不进词图、不参与 Viterbi，两端必须已落在词边界上，**不能从别的词中间切开**。它是独立 ID 空间，热更新零风险，代价是能力受限 —— 它没法把「长江大桥」从「长江/大桥」里救出来。

### 5.2 为什么不能从 ES 索引直接读

| 问题 | 说明 | 置信度 |
| :--- | :--- | :--- |
| 递归 | 词典索引自己的 mapping 必须是 `keyword`，否则分析器解析词典、词典定义分析器 | 〔必然〕 |
| 重入 | 分析器跑在 ES 自己的写入/搜索线程上，从里面再发集群请求，恢复期/关闭期/线程池打满时会自锁 | 〔判断〕 |
| 延迟 | 分词是每字段每文档的热路径，一次网络往返直接判死刑 | 〔必然〕 |
| **启动死结** | 节点启动要加载插件 → 插件要读索引 → 索引要等节点加入集群才能读。**集群起不来** | 〔判断，但这条足以否决〕 |

正确形态是把 ES 彻底摘出词库链路，中间加一个「编译」步骤：

```
用户 CRUD  →  znlp_dict 索引 (keyword 映射，管理面/审计面)
                    ↓  显式触发或定时任务（集群外）
             扫描全量 → 生成带版本号的 words.txt.gz 快照 → HTTP / 对象存储
                    ↓  插件只发 HTTP GET，轮询版本
             构建新的不可变 DAT → AtomicReference 替换 → 旧的可 GC
```

插件这一层只认一个 HTTP 端点，**零 ES 内部 API 依赖**：不参与启动死结、集群红了也能起、不需要网络权限、不碰 `Client` 注入（8.x 起在分析器上下文里已被收紧、9.x 大概率没了〔需核实〕）、离线可测、版本可回滚。

### 5.3 动态加词 ≠ 生效，而且可能掉召回

必须对用户讲清楚的一条：

加了「长江大桥」进词库。查询分析器现在把它切成一个 token，但**索引里的老文档是按「长江/大桥」两个 term 建的**，`"长江大桥"` 这个 term 在倒排里根本不存在。结果是**召回下降，不是上升**。删词同理。

这是搜索场景下动态词库的根本陷阱，IK 用户大量在踩而不自知。〔判断，行业共识〕

对策：
- 把这条警告写进 README 第一屏，而不是藏在文档深处。
- 附一个 reindex 助手：`_update_by_query` + 按时间分批 + 索引别名原子切换。**别人藏着，你讲清楚，这本身是差异化。**
- 默认保守方案：**索引分析器与查询分析器锁同一个词库版本，靠 reindex 推进版本**；只把 L1（停用词）开放成真正实时。
- 「索引侧冻结版本 + 查询侧动态」的方案存在，但有 offset 一致性、高亮等代价，**不要一开始就上**。

### 5.4 两条能省掉大量复杂度的简化

- **别做增量同步，做全量快照。** `_seq_no` + 软删除做 delta 要为 tombstone 写一堆代码，还要处理「重启时落在哪个位点」。20 万词的 `word freq nature` gzip 后 1~2MB，5 分钟全量拉一次毫无压力。全量重建 + 原子替换的代码量和故障面约为增量的三分之一。
- **重建必须有「失败保旧」语义。** reload 失败、返回空、解析异常 —— 任何一种都必须保留上一个好版本并告警，**绝不能因为拉取失败把词库清空**。空词典等于分词全碎、召回崩盘，而且没人会立刻发现。

**待实测**：重建瞬间新旧两份 DAT 共存，ES 堆有界。20 万词 DAT 的双份峰值能否接受、要不要放堆外，这个数得跑出来才知道。

---

## 六、词库 HTTP 服务（建议先于 ES 插件做）

### 6.1 为什么先做这个

**只要服务提供 `GET /v1/dicts/{name}/words.txt`（text/plain + ETag + 304），今天在用 IK `remote_ext_dict` 的团队就能直接接上，一行 ES 插件都不用装。**〔判断，前提是 5.4/3.1 的协议核实通过〕

于是：
- 用户获取不依赖 ES 插件存在，也不受版本矩阵牵连。
- 能在真实流量下验证词库模型、权限、审计、reload 语义。
- 等 ES 插件出来时，用户的词库已经在这里 —— **迁移成本变成零**。

ES 插件是「装完就忘」的东西，词库服务是「每周都有理由打开一次」的东西。**后者才是留存资产。**

而且这个词库服务**不是 ES 的附属品**：`SegmentRecipe.standard().withUserDictionary(dict)` 那条路，纯 Java 库用户也能拉同一个端点，不用再各自维护 `CustomDictionary.txt`。**它属于 znlp 核心。**

### 6.2 差异化不在 CRUD，在变更影响分析

诚实的前提：nginx + cron 生成 words.txt 已能满足 90% 的 CRUD 需求。如果这个服务只是「带 API 的词表文件服务器」，没有存在价值。〔判断〕

三个护城河功能，每一个都直接来自第二节挖出的代码约束：

1. **冲突预检**。用户加「长江大桥」到 L2，按 5.1 的规则它很可能根本不生效（被「长江」「大桥」夹住）。服务在写入时就试跑样例文本，告知「这个词在 87% 的语境里不会生效，要生效得进核心词典」。这个能力别人没有，能省用户几周 debug。
2. **分词 diff 预览**。`POST /v1/dicts/{name}/preview`，传文本 + 候选变更（add/drop），返回 before/after tokens 与差异。纯函数、无状态、复用 znlp 自己。一个接口把服务从「文件服务器」变成「分析工具」。
3. **reindex 必要性评估**。给一批样本 doc id，用候选词库重新分词，报告「有 X% 文档的 term 集合会变，建议 reindex / 可安全发布」。把 5.3 那个危险运维动作变成可量化决策。

三者的共性：**都需要在服务里跑 znlp 本身**。所以服务必须是 Java + 依赖 znlp，不能用 Go 重写一个 —— 这是它成立的前提，也是它的成本。

### 6.3 v1 接口面（刻意极小）

| 端点 | 作用 |
| :--- | :--- |
| `GET /v1/dicts/{name}/words.txt` | text/plain，ETag / 304。**IK 兼容层** |
| `GET /v1/dicts/{name}/version` | 极便宜的版本轮询，含内容 hash |
| `POST /v1/dicts/{name}/words` | 增删改，返回新 version |
| `POST /v1/dicts/{name}/preview` | 分词 diff |
| `POST /v1/dicts/{name}/publish` | draft → published，版本切换点 |
| `GET /v1/dicts/{name}/export?dir=` | 静态导出给 nginx / S3 |

draft/published 要分离：词库变更不该提交即生效，要有「编译 → 预览 → 发布」三步。这也是审计能对上的前提。

版本号要单调且显式（如 `acme@2026-10-20.3` 或内容 hash）。**允许回滚**（这是刚需），只是要记录并上报。

### 6.4 已定的设计取舍

- **存储：默认 SQLite 单文件。** 自托管工具的部署故事应该是「一个 jar + 一个文件」，不要逼用户先备 Postgres。注意 `sqlite-jdbc` 带 native 库，**只在服务端，绝不能进 ES 插件包**。
- **谁编译 DAT：v1 只出纯文本，插件自己构建。** 理由：IK 兼容、curl 可调试、服务保持「哑」且可回滚。
  - 代价要认清：10 节点 = 10 次全量解析 + 10 次内存峰值 + **节点间几秒到几分钟的词库版本漂移**。搜索场景可接受。
  - 三个必做项：① `version` 端点保证 90% 轮询命中 304；② 服务端缓存 gzip 字节，别每次重压；③ **启动拉取加随机抖动**，防集群重启时 10 节点同时打服务（thundering herd，且那时词库服务往往正是最脆弱的）。
- **认证不能省。** 词库内容不机密，但**写权限是真实攻击面**：往生产词库塞一批停用词就能静默打垮别人的搜索，塞一个百万词词库能让节点 OOM。读用只读长效 token，写单独一套凭据，两者不通用。
- **审计**：一张 append-only 变更记录（谁、何时、改了什么、发布成哪个 version）。没有它，线上召回掉了没人查得出来。这是运维能否信任这个服务的分水岭。
- **降级**：服务挂掉，插件保留上一份好词库并告警（见 5.4）。

### 6.5 范围纪律

这个东西会长成一个小产品：DB、认证、审计、CLI、Docker、文档。而**真实资产是那个 NLP 库**。v1 守住三条不扩张：

- **不做 Web UI**（OpenAPI 页 + CLI 够用，UI 是无底洞）。
- **不做多租户 / RBAC**（一个 `name` 参数就是全部隔离需求，有人提再加）。
- **不做增量同步**（见 5.4）。

保留 `export` 静态目录逃生口，让不想跑服务的人能用 nginx。**有它在，「必须部署我的服务」这个门槛就消失了** —— 而门槛是开源项目最贵的东西。

---

## 七、实施顺序

| 步骤 | 内容 | 为什么在这个位置 | 估量 |
| :--- | :--- | :--- | :--- |
| **0** | 修 2.4 的 bigram 缓存键 | 不修，后面所有动态词库都在埋雷 | 小 |
| **1** | `znlp-resources-search` 瘦身资源集 | 不瘦身，后面每一步的代价都放大 5 倍 | 小 |
| **2** | `znlp-lucene`（Analyzer + TokenStream + 子词展开） | 所有下游的地基，且**不依赖最终选哪个 ES 版本** | 中 |
| **3** | 词库 reload 机制（版本轮询 + 不可变实例 + `AtomicReference` + 失败保旧） | 2.5 说的目前完全缺失的那一环。纯库侧，与 ES 无关 | 中 |
| **4** | 词库 HTTP 服务 v1（6.3 六个端点 + SQLite + CLI + export） | 能靠 IK 兼容独立拿用户，**可先于 5~7 发布** | 中 |
| **5** | `znlp-es-plugin` 壳 + 单一 ES 版本打通 + Docker 镜像 | 到这一步才真正面对 ES | 中 |
| **6** | CI 版本矩阵 + 确定性 URL + 网站版本选择器 | 有人用了再做长尾 | 中 |
| **7** | 变更影响分析三功能（6.2） | 差异化卖点，等真实用户提问后再做 | 中~大 |
| **8** | 在线 `_analyze` playground | 需要后端沙箱，最后做 | 大 |

**验证性第一步建议**：只做 2 + 5 的最小版（单一 ES 版本、本地容器、`_analyze` API 对比 IK），确认业务上真的要用，再投入 6~8。〔判断〕

---

## 八、未决问题

1. **词库的 source of truth 放哪？**（阻塞 6.3 的 API 形态）
   - **权威源在服务里**：体验完整（CRUD + 审计 + 版本 + 预览），但逼用户迁移词库数据。
   - **权威源在用户自己的系统**（业务 DB / 代码仓库 / 内部后台），服务只做只读出口/同步目标：零迁移成本，但审计和版本语义只是「缓存」而非「真相」。
   - 这个选择会决定 API 是 **CRUD 型**还是**同步型**。**这是下一个要先定的问题。**
2. 多架构 Docker（amd64/arm64）用 buildx 还是分别出镜像。
3. ES 8 与 ES 9 是否同时支持，还是只做 8.x。取决于目标用户实际在跑什么。
4. OpenSearch 要不要支持 —— 它是从 ES 7.10 fork 的另一条版本线，descriptor 字段是 `opensearch.version`，同时支持意味着矩阵直接翻倍。**建议一开始只做 ES。**〔判断〕
5. 子词（`subword`）默认开还是关，以及开了之后对索引体积与高亮 offset 单调性的影响。〔需实测〕
6. 词库服务的 HA：它是所有节点的依赖，挂了要靠「保旧」扛，但长期挂掉需要运维可见性。要不要给个「上次成功拉取时间」的上报通道 —— 注意插件主动上报等于把耦合请回来。
7. CI 矩阵覆盖哪几个具体 ES 版本号。

---

## 附：本次讨论中修正过的一个判断

初看 `wordId = trie.indexOf(key)` 时，我判断「二元表按 wordID 存储，动态加词会让全表静默错位，因此动态扩词不可行」。

读 `parseSourceFile` / `parseBigramLine` 后推翻了：文件存词面，ID 加载时解析，缺失对被丢弃。**这个否决项其实不成立。**

留下的是它的孪生问题 —— 2.4 的缓存键不完整，那个才是真雷。记在这里是为了说明：〔已验证〕之前的推断在被源码推翻时应立即改正，不要把错误结论带进排期。
