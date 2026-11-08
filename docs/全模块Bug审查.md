# znlp 全模块 Bug 审查

审查日期：2026-08-27  
审查范围：`znlp` 主模块源码（不含 `archive/`、`mynlp/` 旧实现，除非对照行为差异）。  
审查方式：按功能模块对照源码，只记录能确认的逻辑错误；风格、注释笔误、未覆盖测试不单独成条。  
对照文档：`docs/分词模块审查.md`（分词侧若干项已在该文档中记录；本文覆盖全库，并更新默认路径变化）。

默认分词路径（已含词性）：

```
Znlp.segmenter()
  → BigramPlugin（核心词典 + 原子识别 + 二元 Viterbi）
  → PersonNamePlugin
  → PosPlugin
```

---

## 总表

| ID | 严重程度 | 模块 | 问题 | 默认路径是否命中 |
| :--- | :--- | :--- | :--- | :--- |
| ~~B01~~ | ~~高~~ | ~~感知机解码~~ | ~~Optimized Viterbi 通用路径在偶数长度序列上用错终点分数~~ | ~~已修~~ |
| B02 | 高 | 资源加载 | JAR 工厂把目录判成文件，本地 JAR 永远读不到 | 自动下载场景 |
| B03 | 高 | 资源加载 | `DictResources` 仍是旧 mynlp 路径，自动下载对不上 | 自动下载场景 |
| B04 | 高 | 资源加载 | dataDir 启动时不存在则永不注册 File/JAR 工厂 | 自动下载场景 |
| ~~B05~~ | ~~高~~ | ~~Duckling~~ | ~~`{num} Gain` 手写 pattern id=0，与数字规则冲突~~ | ~~已修~~ |
| ~~B06~~ | ~~高~~ | ~~Duckling 时间~~ | ~~「下周三」被 dayOffset 短路成「今天+7」~~ | ~~已修~~ |
| B07 | 高 | 纠错插件 | 有词典时处理器为空，插件装不上 | 需显式安装 |
| B08 | 中 | 感知机训练 | `decodeQuickModel` 参数被字段遮蔽，永远 false | 训练 |
| B09 | 中 | 感知机压缩 | 保留特殊特征后数组尺寸算错，可能越界 | 训练 compress |
| B10 | 中 | 感知机 Quick | `Float.MIN_VALUE` 当初值，全负分时标签为 -1 | quick decode |
| B11 | 中 | CWS | 未登录词 `Nature.newWord` 写不进词性/词频 | CWS 插件 |
| B12 | 中 | NER | 单字实体 `S` 当 `O` 丢掉；句末未闭合实体不 flush | NER 插件 |
| B13 | 中 | 人名 | 句末 B/M 无 E 时人名被丢；X/Y/Z 不清 span | 是 |
| B14 | 中 | 自定义词典 | 词已在词图中时不 `combine`，无法纠正路径 | 需安装插件 |
| B15 | 中 | 原子识别 | `put()` 替换同跨度词典节点，丢掉 wordID/freq | 是 |
| B16 | 中 | 流水线 | 单字符短路，不走词典/POS/人名 | 是 |
| B17 | 中 | 子词图 | `subWordnet` 不复制 wordID/freq/nature | 索引分词 |
| B18 | 中 | 配置 | `buildWithDefaults()` 只继承第一个 dataDir | Builder |
| B19 | 中 | 系统 | `builder(null)` 文档写回退默认，实际 NPE | Builder |
| B20 | 中 | 资源加载 | `getCacheDir()` 在 cacheDir=null 时 NPE | 不完整 config |
| B21 | 中 | 资源加载 | 下载后 JAR 索引不失效 | 自动下载 |
| B22 | 中 | 服务注册 | 负缓存与注册存在并发竞态 | 并发初始化 |
| B23 | 中 | 拼音 | `keepPunctuation(true)` 只保留空白，不保留标点 | 拼音 API |
| B24 | 中 | 新词发现 | 字符频次数组长度 65535，`\uFFFF` 越界 | 含该字符的语料 |
| ~~B25~~ | ~~中~~ | ~~Duckling~~ | ~~latent Duration 仍被 CandidateSelector 收下~~ | ~~已修~~ |
| ~~B26~~ | ~~中~~ | ~~Duckling~~ | ~~`24点` → hour=24，`toDateTime()` 抛异常~~ | ~~已修~~ |
| ~~B27~~ | ~~中~~ | ~~Duckling~~ | ~~表外年份节日 `computed.get` 为 null 后 NPE~~ | ~~已修~~ |
| B28 | 中 | StarSpace | 缺 base labels 时 `System.exit(0)` 杀 JVM | LabelDoc 预测 |
| ~~B29~~ | ~~中~~ | ~~FastText~~ | ~~`kUnlimitedPredictions` 仍把 k=-1 传给 loss~~ | ~~已修~~ |
| B30 | 低 | 高亮 | 非模糊模式下半截匹配残留高亮位 | `enableFuzzy=false` |
| B31 | 低 | 流水线 | 切分算法忽略 `isEnabled()` | 禁用组件 |
| B32 | 低 | Builder | `installPlugin` 无条件要求 ServiceLocator | 无 Locator 构建 |
| B33 | 低 | 资源加载 | 找不到资源时失败计数 +2 | 统计 |
| B34 | 低 | 门面 | `discoverNewWords` 空参抛 NPE，文档写 IAE | 门面 |
| B35 | 低 | 配置 | 空的 `data-dir=` 会走 `setDataDirs(null)` NPE | 空配置文件 |

---

## 1. 感知机框架

涉及：`dev.znlp.perceptron`。POS、NER、人名、CWS、拼音切分都走这套解码。

### B01 高 — Optimized Viterbi 通用路径终点分数用错

**文件：** `perceptron/decoder/OptimizedViterbiPerceptronDecoder.java`（约 141–154、329–359 行）

`viterbiForwardGeneral` 交换的是方法局部引用，调用方的 `scoreLast` / `scoreNow` 不会跟着换。4 标签特化路径有 `arraycopy` 把最终分数拷回去，通用路径没有。`decode()` 在 `sentenceLength > 1` 时又无条件覆盖为原来的 `scoreLast`：

```java
float[] finalScores = ((sentenceLength - 1) & 1) == 1 ? scoreNow : scoreLast;
if (sentenceLength > 1) {
    finalScores = scoreLast; // 交换后scoreLast总是持有最终结果
}
```

两段注释互相矛盾。序列长度为偶数时（前向步数 = n−1 为奇数），最终分数在原始 `scoreNow` 里，却用上一时刻的 `scoreLast` 选终点，整条回溯都会偏。

- CWS / 拼音切分是 4 标签，走特化路径，不受影响。
- **POS（默认分词器）、NER（8 标签）、人名（7 标签）走通用路径，会中。**

**触发：** 默认分词器标注偶数个词的句子；人名识别偶数长度字符序列；NER 偶数个 token。  
**修复：** 删掉 `if (sentenceLength > 1) finalScores = scoreLast`；通用路径在奇数次交换后把最终分数拷回调用方数组（与 4 标签分支一致），或改为返回最终数组引用。

### B08 中 — 训练时 `decodeQuickModel` 被写死

**文件：** `perceptron/train/PerceptronTrainModel.java`（约 48–78 行）

```java
private final boolean decodeQuickModel = false;
public PerceptronTrainModel(..., boolean decodeQuickModel) {
    // 参数被字段遮蔽，从未赋给 this.decodeQuickModel
    resetDecoder();
}
```

构造参数被忽略，训练始终 Viterbi，保存的模型标志也永远是 `false`。`PosPerceptron.train(..., true)` 不会按快速解码训练。

**修复：** 去掉字段上的 `= false`，改为 `this.decodeQuickModel = decodeQuickModel`。

### B09 中 — compress 保留特殊特征后数组越界

**文件：** `perceptron/train/PerceptronTrainModel.java`（约 266–273 行）

`index <= labelCount` 的转移 / BOS 特征即使进了 `filterSet` 也会拷贝保留，但 `newParameter` 按 `featureList.size() - filterSet.size()` 分配。多拷的那几维会 `ArrayIndexOutOfBoundsException`。

**触发：** `compress(ratio, threshold)`，且 0..labelCount 号特征因低分进了过滤集。  
**修复：** 先从 `filterSet` 去掉必须保留的特殊特征，再算 `newSize`。

### B10 中 — Quick 解码用 `Float.MIN_VALUE` 当初值

**文件：** `perceptron/decoder/QuickPerceptronDecoder.java`（约 91 行）

`Float.MIN_VALUE` 是最小**正**浮点（约 `1.4E-45`），不是最负值。所有标签得分 `≤ 0` 时选不出标签，`maxIndex` 一直是 `-1`。感知机权重经常全负或为 0。

**触发：** `decodeQuickModel=true` 的模型。默认推理走 Optimized Viterbi，这条不在默认路径上。  
**修复：** 改成 `-Float.MAX_VALUE` 或 `Float.NEGATIVE_INFINITY`。

---

## 2. 资源加载与配置

涉及：`base/resources`、`base/common/DictResources`、`config/`、`ZnlpSystem`。

### B02 高 — JAR 工厂把「目录」判成「文件」

**文件：** `base/resources/JarNlpResourceFactory.java`（85–87 行）

注释写「存在且为目录」，代码却是 `!baseDir.isFile()`。数据目录 `isFile()` 为 false，条件为 true，直接 `return null`。旧版 Kotlin 是 `baseDir.isFile`（文件才跳过）。

**触发：** `dataDir` 指向真实目录，其中有 `mynlp-resource-*.jar`（或自动下载下来的 JAR）。`FileNlpResourceFactory` 只认散文件，JAR 内条目永远走不到。  
**修复：** `if (!baseDir.exists() || !baseDir.isDirectory()) return null;`

### B03 高 — DictResources 路径仍是旧 mynlp 名

**文件：** `base/common/DictResources.java`（27–107 行）

注册表仍是无前缀路径，例如 `core-dict/CoreDict.txt`、`cws-model/feature.dat`。实际加载已改为 `znlp/` 前缀（`CoreDictionaryFactory`、`CwsPerceptron`、`znlp-resources/...`）。`tryDownload` 用调用方传入的 path 做 `map.containsKey`，新路径命中失败，不会下载，错误信息也没有 Maven 依赖提示。

**修复：** 把 map 的 key 改成当前路径（`znlp/core-dict/...` 等），并与 `znlp-resources` 里实际文件对齐。

### B04 高 — dataDir 启动时不存在则永不注册工厂

**文件：** `base/resources/NlpResourceLoader.java`（105–117 行）

只对 `exists()` 的目录注册 File/JAR 工厂。默认 `data-dir=data,data/dat,../data/dat` 在进程启动时通常还不存在，因此只剩 Classpath 工厂。`tryDownload` 即使把 JAR 写到 `data/`，后续也不会再扫该目录。工厂列表是一次性的。

**修复：** 始终为配置的 dataDir 注册工厂（目录不存在时 load 返回 null 即可）；下载成功后重建工厂列表或 `mkdirs()`。

### B21 中 — 下载后 JAR 索引不失效

**文件：** `JarNlpResourceFactory.java`（127–141 行）

`indexCache` 第一次扫描后永久缓存。即便修好 `isFile`、目录在启动时已存在：第一次 miss 会缓存空 map → 下载 JAR → 第二次仍用空索引。

**修复：** 下载完成后清空 `indexCache`，或按 JAR 目录 mtime 重建索引。

### B18 中 — `buildWithDefaults()` 只继承第一个 dataDir

**文件：** `config/ZnlpSystemConfigBuilder.java`（111–113 行）

默认配置从 `znlp.properties` 读到 3 个目录。`buildWithDefaults()` 却用 `getDataDir()` / `setDataDir()`，只拷贝第一项。`setDataDir` 会整表替换成单元素列表。默认单例 `SystemDefaultConfig.getInstance()` 不受影响。

**触发：** `ZnlpSystemConfig.builder().cacheDir(...).buildWithDefaults()` 且未设 dataDirs。  
**修复：** `config.setDataDirs(new ArrayList<>(defaultConfig.getDataDirs()))`。

### B19 中 — `ZnlpSystem.builder(null)` 随后 NPE

**文件：** `DefaultZnlpSystemBuilder.java`（45–48、86 行）

Javadoc 写「config 为 null 将使用默认配置」，实现却把 null 原样保存。`ModuleLoader.loadModules(registry, config)` 里 `config.getDisabledModules()` 会 NPE。

**修复：** `config == null` 时回退 `SystemDefaultConfig.getInstance()`。

### B20 中 — `getCacheDir()` 在 cacheDir=null 时 NPE

**文件：** `NlpResourceLoader.java`（94–99 行）

`new File((String) null)` 抛 NPE。同仓库 `CacheFileUtils` 已有 cacheDir → dataDir → tmpdir 的回退，这里没有。`ZnlpSystem.builder(ZnlpSystemConfig.builder().dataDir("foo").build())` 不会走 `buildWithDefaults()`。

**修复：** 与 `CacheFileUtils` 同样回退。

### B33 低 — 找不到资源时失败计数 +2

**文件：** `NlpResourceLoader.java`（184–209 行）

`resource == null` 时先 `failedLoadCount++` 再抛异常；外层 `catch` 再加一次。

### B35 低 — 空的 `data-dir=` 触发 setDataDirs(null) NPE

**文件：** `config/ConfigLoader.java`（81–91 行）；`ZnlpSystemConfig.java`（35–37 行）

空字符串被改成 null 再交给 List setter，`new ArrayList<>(null)` NPE。异常被吃掉，stderr 打失败日志。`dataDirs` 保持构造时的空列表，功能上像「没配」，但会误报。

**修复：** `setDataDirs` 对 null 当成空列表；或 ConfigLoader 对 List 空值传 `Collections.emptyList()`。

---

## 3. 服务注册与门面

涉及：`base/service/ServiceRegistry`、`Znlp`。

### B22 中 — 负缓存与注册的并发竞态

**文件：** `base/service/ServiceRegistry.java`（68–71、236–243 行）

`registerFactory` 先 `put` 再 `lookupCache.clear()`。`getService` 用 `computeIfAbsent` 把「无匹配」写成 `NO_SERVICE`。时序：线程 A 的 mapping 看到未注册 → 线程 B register + clear → 线程 A 把 `NO_SERVICE` 写回缓存。之后该类型会走自动创建或抛 `ServiceNotFoundException`，绕过已注册的单例。

**触发：** 一线程 `getService`（尚未注册），另一线程同时 `registerFactory` / `registerSingleton`。  
**修复：** 注册时用世代号；或查找时不要负缓存；或 `computeIfAbsent` 完成后若 stamp 变化则重查。

### B34 低 — `discoverNewWords` 空参抛 NPE

**文件：** `Znlp.java`（217–218、247–248 行）

`segment` / `pinyin` / `highlight` 对 null 抛 `IllegalArgumentException`。`discoverNewWords` 用 `Objects.requireNonNull`，抛的是 NPE，但 JavaDoc 写 `@throws IllegalArgumentException`。

---

## 4. 分词流水线

涉及：`segment/PipelineSegmenter`、`PipelineSegmenterBuilder`、`graph/`。  
已修且不再列为 bug：`VertexHelper.newBegin()` 误用 `Nature.newWord`、二元表 `\u0001` 被 trim、人名切开「长江」。

### B16 中 — 单字符短路整条流水线

**文件：** `segment/PipelineSegmenter.java`（285–290 行）

`text.length == 1` 直接吐 `Nature.x` 或 `Nature.w`，不建词图、不跑词典 / POS / 人名。`"的"`、`"我"` 没有真实词性。空串返回空列表，单字走这条捷径，行为不一致。

**修复：** 单字也走完整流水线，或至少查核心词典再标 POS。

### B31 低 — 切分算法忽略 `isEnabled()`

**文件：** `PipelineSegmenter.java`（340–342 行）

后处理器检查 `isEnabled()`，`WordSplitAlgorithm.fill()` 不检查。`disableComponent(AtomSplitAlgorithm.class)` 无效。

### B32 低 — `installPlugin` 无条件要求 ServiceLocator

**文件：** `PipelineSegmenterBuilder.java`（754–755 行）

实例已经加完之后仍无条件 `requireServiceLocator()`。只提供实例的插件（如修好后的 `CorrectionPlugin(dict)`）在 `builder()` 无 Locator 时会失败。

**修复：** 仅当某个 `getXxxTypes()` 非空或 `getBestPathAlgorithmType()` 非 null 时才要求 Locator。

### B17 中 — 子词图丢掉 wordID/freq

**文件：** `segment/graph/Wordnet.java`（89–93 行）

`subWordnet` 只用 `getOrCrete(v.length)`，不复制 `wordID` / `freq` / `nature`。`SmartSubwordComputer` 在子图上再跑 Viterbi 时几乎只靠平滑项。

**修复：** 复制 `wordID`、`freq`、`nature`（`Vertex` 拷贝构造目前也不拷这两个字段）。

---

## 5. 分词插件

### B07 高 — 纠错插件装不上

**文件：** `segment/impl/correction/CorrectionPlugin.java`（35–41 行）

有词典时 `getProcessorTypes()` 返回空集，又没覆盖 `getProcessors()`，处理器根本不会挂上。无词典时声明 `CorrectionWordpathProcessor.class`，但 `SegmentModule` 没注册 `CorrectionDictionary`，DI 容易失败。默认路径 `correction-dict/correction_dict.txt` 仓库里也不存在。

**触发：** `installPlugin(new CorrectionPlugin(dict))` 或无参构造。  
**修复：** 有词典时 `getProcessors()` 返回 `new CorrectionWordpathProcessor(dictionary)`；无词典再走类型 + 工厂；资源路径加上 `znlp/` 并放入文件。

### B11 中 — CWS 未登录词写不上

**文件：** `segment/impl/cws/CwsSplitAlgorithm.java`（50–52 行）

```java
vertex.setAbsWordNatureAndFreq(Nature.newWord);
```

`DictionaryAbsWords.nature2id` 不含 `newWord`，返回 `-1`，`nature` / `freq` 都不会写。OOV 顶点保持 `wordID=-1, freq=0, nature=null`。Viterbi 把 `freq==0` 当 1，也走不了抽象词 bigram。POS 里 `nature == Nature.newWord` 的覆盖分支实际到不了。

**修复：** `vertex.nature = Nature.newWord` 并设合理 `freq`，不要走 `setAbsWordNatureAndFreq`。

### B12 中 — NER 丢掉单字实体

**文件：** `segment/impl/ner/NerProcessor.java`（48–70 行）；标签定义在 `NERPerceptronInstance`（含无类型 `"S"`）

`"S".equals(tag)` 与 `"O"` 同等处理，单 token 地名 / 机构既不改词性也不合并。句末若停在 `B-` / `M-` 而没有 `E-`，实体被静默丢弃。标签表只有 `"S"` 没有 `"S-ns"` / `"S-nt"`，即使处理 `S` 也无法区分类型。

**修复：** `S` 至少不要当 `O` 清 span；循环结束后 flush 未闭合的 `B-/M-`；标签改回 `S-ns` / `S-nt`。

### B13 中 — 人名句末不 flush

**文件：** `segment/impl/personname/PersonNamePerceptron.java`（55–76 行）

只在遇到 `E` 时收录人名。句末是 `B` / `M` 没有 `E` 则丢掉。`X` / `Y` / `Z` 也不清 `nameStart`。默认分词器装着人名插件，且会叠加 B01。

**修复：** 循环结束后若 `nameStart != -1`，把 `[nameStart, len)` 收成一个人名；`O` / `X` / `Y` / `Z` 都应结束当前跨度。

### B14 中 — 自定义词典不纠正路径

**文件：** `segment/impl/userdict/UserDictionaryProcessor.java`（63–68 行）

`!willCutOtherWords` 时，若词图里已有同跨度顶点（核心词典已有该词），不调用 `combine`。Viterbi 若选了更碎的切分，用户词典无法合并回来。

**修复：** 只要 `!willCutOtherWords` 就 `wordPath.combine(offset, length)`。

### B15 中 — 原子识别覆盖词典节点

**文件：** `segment/impl/atom/AtomSplitAlgorithm.java`（78–79 行）

`wordnet.put(offset, length)` 对相同 length 是替换。数字 / 时间与词典词同跨度时，丢掉原 `wordID/freq`，改成抽象词 ID。之后 bigram 走的是 `m` / `t` 等，不是原词。

**触发：** 如「2024年」既在核心词典、又被原子模板命中。  
**修复：** 已有节点只补 `nature`，或 `getOrCreate` 后按需更新，不要整节点替换。

---

## 6. Duckling 结构化解析

`Znlp.parse` 走 `DucklingParser.getInstance()`，不经过 `ZnlpSystem`，`disabled-modules` 管不到。

### B05 高 — 时长规则 pattern ID 冲突

**文件：** `duckling/grammar/duration/DurationGrammar.java`（22–29 行）；`duckling/pattern/DucklingPattern.java`（18–28 行）

`{num} Gain` 手写了 `FilterDucklingPattern(id=0)`，而 `RuleBuilder` 的静态 `patternIdCounter` 从 0 起，第一个模式是 `NumberGrammar` 的 `\\d+`。`DucklingPattern.equals/hashCode` 只比 `id`，HashSet / HashMap 会把两个模式当成同一个。

**触发：** `parse("一小时")` / `"1小时30分"`。时长规则的第二段会去读 `pattern2Node[0]`（数字正则命中），时间粒度节点进不了该槽。  
**修复：** 改成 `RuleBuilder.filter(TimeGrainNodeValue.class, …)`，让计数器分配唯一 ID；不要手写 `id=0`。

### B06 高 — 「下周三」被 dayOffset 短路

**文件：** `duckling/grammar/time/date/DateNodeValue.java`（192–195 行）；`DateRules.java`（191–198 行）

`outputDay` 只要 `dayOffset != null` 就 `today.plusDays(dayOffset)` 并返回，后面的 weekday 分支成死代码。「下周三」把 `dayOffset=7` 和 `weekday` 写在一起，结果变成「今天 + 7 天」，不是「下个星期三」。

**触发：** 参考日 2019-11-11（周一）时，「下周日」应为 2019-11-24，实际是 11-18。`ComprehensiveTest` 把「下周日 / 下个星期天」标成 known failure 跳过。  
**修复：** weekday 优先于纯 dayOffset；「下 + 周X」应先落到本周该星期，再加一周（若已过则再加）。不要用 `+7` 当 dayOffset 短路。

### B25 中 — latent Duration 仍被选中

**文件：** `duckling/CandidateSelector.java`（46–49 行）

`isCandidate` 对 `Dimension` 一律接收，不看 `isLatent()`。`DurationDimension` 继承 `Dimension`，「分」会被标 latent 以免和金额冲突，但选择器仍会收下。Duration 优先级高于 AmountOfMoney。若 B05 修好，「5分」可能优先变成 5 分钟而不是 5 角。

**修复：** `Dimension` 也要 `!v.isLatent()`。

### B26 中 — `24点` 无法转 LocalDateTime

**文件：** `duckling/grammar/time/time/TimeRules.java`（19 行）；`DateTimeDimension.java`（90–92 行）

小时允许 `0..24`。`24点` 得到 `hour=24`，`LocalDateTime.of(..., 24, ...)` 抛 `DateTimeException`。

**修复：** 小时限制到 `0..23`，或把 24 点归一成次日 00:00。

### B27 中 — 表外年份节日 NPE

**文件：** `DateNodeValue.java`（208–210 行）

带显式年份的节日：`computed.get(targetYear)` 为 null 时直接 `ymd.year()` NPE。春节表只到 2060。

**触发：** `"3000年春节"`、`"2061年春节"`。  
**修复：** `ymd == null` 时返回 null，让规则失败而不是崩。

---

## 7. 拼音

### B23 中 — `keepPunctuation` 只保留空白

**文件：** `pinyin/PinyinResult.java`（244–256 行）

`keepPunctuation(true)` 判断的是 `Character.isWhitespace`，不是标点。`，。！？` 在 `keepOthers=false`（默认）时被丢掉。方法名和实现不符。`shouldKeepNonPinyinCharacter` 同样错误。

**触发：** `convert("你好，世界").keepPunctuation(true).asString()`，逗号消失。  
**修复：** 用 `Characters.isPunctuation` 判断标点；空白单独用 `keepOthers` 或新开关。

---

## 8. 新词发现

### B24 中 — 字符频次数组少一格

**文件：** `newwords/engine/NewWordFindEngine.java`（54–70 行）；`NewWordFindEngineOptimized.java` 同样

`ziFreqArray`、`filterStartChar`、`filterContainsChar` 长度都是 **65535**，合法下标 0..65534。`char` 最大是 65535（`\uFFFF`）。

**触发：** 语料里出现 `\uFFFF` 时越界。  
**修复：** 数组改成 `65536`，循环到 `i < 65536`。

---

## 9. 智能高亮

### B30 低 — 非模糊模式下半截匹配残留高亮

**文件：** `highlight/SmartHighlighter.java`（260–276 行）

非模糊拼音匹配失败时 `queryIndex` 复位并 `i--`，但已经 `highlighted.set` 的位不清。半截匹配会留下高亮。

**触发：** `enableFuzzy=false`（默认是 true），例如 query=`beijing`，text=`北dummy`：`北` 仍被标上。  
**修复：** 复位时清掉本轮已点亮的区间，或先记候选再在整词成功后提交。

---

## 10. FastText / StarSpace

### B28 中 — StarSpace 用 `System.exit(0)` 杀 JVM

**文件：** `starspace/StarSpace.java`（204–207 行）

`fileFormat == LabelDoc` 且未提供 base labels 时直接 `System.exit(0)`，会杀掉整个进程（包括嵌入该库的服务）。

**修复：** 抛 `IllegalArgumentException`。

### ~~B29 中 — FastText 无限预测结果恒为空~~（已修）

**文件：** `fasttext/Model.java`

已改为 `loss.predict(kk, …)`。同期还补了官方兼容 save/load、`trainSupervised` 不再原地改 Args / 关子词、临时文件与 Reader 关闭、官方 P@k、UTF-8 哈希与子词切分、负采样/小词表/零 label 边界。

---

## 可疑项（未当成确认 bug）

| 模块 | 位置 | 说明 |
| :--- | :--- | :--- |
| POS | `PosPerceptronInstance` | 运行时用 `Nature.values()` 去掉 begin/end/newWord 再排序。模型只存 `labelCount` 不存标签名。若 `pos.model` 按语料扫描的子集训练，ID 会对错。未拆模型头，不断言。 |
| NER | `NERPerceptron.labels[id]` | 无越界保护（POS 有）。Viterbi 一般给出合法 ID；若将来开 quick decode 会 AIOOBE。 |
| 感知机 | `MynlpLegacyPerceptronModelLoader` | 不校验 `pSize == featureSize * labelCount`。尺寸不对会在解码时越界。 |
| CWS | `CwsPlugin` | 感知机只填候选，最终可能被二元 Viterbi 改写。更像设计选择。 |
| 流水线 | `segment(char[])` | 有 `CharNormalize` 时原地改调用方数组。`segment(String)` 无此问题。 |
| 词图 | `Wordnet.findDangling()` | 算完后 `return null`，方法不可用。当前无调用。 |
| 词图 | `Wordpath.WordPointer.next()` | `nextSetBit` 若返回 -1，`len` 可能为负。正常 `connect` 会置上终点位。 |
| 资源 | `tryLoadResource` | 不走 `validateResourcePath`，与 `loadResource` 防护不一致。当前内部调用都是常量路径。 |
| 配置 | `SystemDefaultConfig` | `-Dznlp.cache-dir=` 会把 cacheDir 设成 `""`，`new File("")` 变成用户目录。 |
| 拼音 | `Tex2PinyinComputer` | 用拼音数组长度当字长做最长匹配。自定义拼音字数与拼音数不一致时会错位。系统词典若始终 1 字 1 音则不会爆。 |
| Duckling | `NumberGrammar` | 超长数字 `Long.parseLong` 无本地 try-catch，依赖上层吞异常。 |
| Duckling | `Grain.justStep` | 只覆盖 Year→Second，周合并不了。更像覆盖缺口。 |
| StarSpace | `Args.cosine` | 空文档零向量 → 0/0=NaN。 |
| StarSpace | `ModelTrainer` | `numThreads = max(thread,2)-1`，4 线程实际 3。可能是照搬 C++。 |
| 新词 | `WordInfo` IDF | 超容量后 `docCountMode` 计的是出现次数不是文档数。 |
| base | `AhoCorasickDoubleArrayTrie` | `p = b + c + 1` 无边界检查。高码点 + 大 base 可能越界（HanLP 老问题）。 |
| base | `BinTrieTree` | 前缀迭代器 `level = prefix.length()` 带 FIXME，前缀 ≥2 时 buffer 填充可能错。 |
| 门面 | `Znlp.parse` | Duckling 不在 `META-INF/znlp-modules.idx` 里，模块开关管不到。属边界未收拢。 |

---

## 建议修复顺序

1. **B01** Optimized Viterbi 通用路径：打在默认分词器的 POS 和人名上，优先修。修完后人名句末 flush（B13）的收益才看得见。
2. **B02 + B04 + B03 + B21**：资源自动下载全链路目前是断的，四条叠在一起。
3. **B05 + B06**：Duckling 时长和时间是对外 `parse` 的主路径。B25 要等 B05 修好后再处理，否则「5分」语义会变。
4. **B07** 纠错插件：当前完全不可用。
5. **B08 / B09 / B10**：训练与压缩，不影响现成模型推理（除将来开 quick decode）。
6. 其余中低项按使用频率：B16 单字符、B15 原子覆盖、B11 CWS OOV、B12 NER `S`、B14 用户词典、B23 拼音标点、B28 `System.exit`。

---

## 与既有文档的差异

`docs/分词模块审查.md` 写于 POS 尚未装入默认分词器时。当前 `Znlp.segmenter()` 已安装 `PosPlugin`，因此：

- 「默认分词器没有词性」不再成立。
- B01 会打在默认词性结果上，不只是可选插件。
- 该文档中的 P0 三项（起始节点、二元表 trim、人名切开长江）仍然是已修状态。
- 该文档中的纠错 / 自定义词典 / 原子覆盖 / CWS OOV / NER `S` / 单字符短路，与本文对应条目一致，仍未修。
