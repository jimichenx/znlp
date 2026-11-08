---
title: 常见问题
description: 资源加载失败、性能问题、文档与代码不一致之处，以及当前版本的已知限制。
---

## 已知限制与文档偏差

以下内容都经过源码核对。仓库根 `README.md` 和 `CONFIG_README.md` 中有若干描述与代码不符，**请以本页为准**。

### 1. 自动下载资源当前不会成功

`DictResources` 只登记一个坐标 `dev.znlp:znlp-resources:0.1.0`，判定按资源路径的 `znlp/` 前缀，与运行时请求路径一致。但仓库**没有配置 `maven-publish`**，该 artifact 未发布到任何仓库，下载必然 404。

**应对**：不要依赖自动下载兜底。确保 `znlp-resources` jar 在运行时 classpath 上，或把资源预置到 dataDir。把 `auto-download-resource` 设为 `false` 反而更好——资源缺失会立即失败，而不是先做一次注定失败的网络请求。

### 2. 包外无法构建自定义 ZnlpSystem

`ZnlpSystem.builder()` 返回的 `DefaultZnlpSystemBuilder` 是**包私有类**，其 `build()` 也是**包私有**，而且该 builder **没有任何 `withXxx` 配置方法**。

**应对**：定制配置只能用 JVM 系统属性（`-Dznlp.*`）、classpath 上放自己的 `znlp.properties`，或 `SystemDefaultConfig.reset()`。`ZnlpSystemConfig.builder()` 构造的对象不会影响 `Znlp` 门面的行为。

### 3. `pinyinString` 输出不带声调

方法的 Javadoc 写「带声调拼音」，但实现走 `PinyinResult.asString()` → `getPinyinWithoutTone()`，**实际不带声调**。

**应对**：需要声调时用 `Znlp.pinyin(text).getPinyinList()` 拿 `Pinyin` 枚举。

### 4. NER 只支持两类实体

标签集硬编码为 `targetPos = {"ns", "nt"}`，完整标签 `B-ns M-ns E-ns B-nt M-nt E-nt O S`。即**只有地名和机构团体**。

人名由独立的 `PersonNamePerceptron` 负责（`standard()` 配方默认开启）；时间、数字、金额由 Duckling 负责。

**应对**：需要通用实体抽取就把三者组合起来。要增加新实体类型必须改 `NERPerceptronInstance` 并重新训练，无法通过配置实现。

### 5. Duckling 不支持 Email 和 Url

`DimensionKind` 声明了 `Email(8)` 和 `Url(9)`，但 `grammar/Grammars.java` 只注册了 10 个语法：Number、Ordinal、Money、DateTime、Percentage、Distance、Temperature、Age、PhoneNum、Duration。**没有 Email / Url 的 grammar**。

`Date(6)` 也没有独立实现——日期与时间共用 `DateTimeDimension`，其 `getKind()` 返回 `Time`。

**应对**：URL 和邮箱识别在分词侧的规则包 `segment/impl/rule/`，不在 Duckling。

### 6. FastText 没有词向量查询 API

代码中不存在 `getWordVector` / `nearestNeighbor`。只能自己用 `dict` 把词映射到行号，再访问 `model.wi()`（输入矩阵）或 `FastText.getOutput()`（输出矩阵）。

另外 `loadModel` **不支持 `.ftz` 量化模型**（源码中显式拒绝）。

### 7. 未配置 Maven 发布

所有 `build.gradle.kts` 中都没有 `maven-publish` / `publishing` / `signing`。`dev.znlp:znlp:0.1.0` **无法从任何远程仓库拉取**。

**应对**：见[安装](../getting-started/installation/)的三种方式（构建 jar 后文件依赖、Gradle 复合构建、源码子项目）。

### 8. CONFIG_README.md 已过时

缺失内容：`dataDirs` 多目录、`disabled-modules`。默认值写的是 `data` / `cache`，而打包的 properties 实际是 `data,data/dat,../data/dat` / `data/cache`。优先级描述（「JVM 属性 > 配置文件 > 代码 Builder」）与代码事实不符。文末引用的 `docs/DEVELOPMENT.md` 和 `docs/DEPLOYMENT.md` **不存在**。

### 9. 没有 close / shutdown

`ZnlpSystem`、`ServiceRegistry`、`ModuleLoader` 都没有关闭方法，模型和词典缓存常驻堆内存直到进程退出。容器环境规划内存时要把这部分算进去，且不要指望销毁 Bean 能释放。

## 资源与部署

### 抛 ResourceNotFoundException

按顺序排查：

1. **`znlp-resources` 在运行时 classpath 上吗？** 主库对它是 `runtimeOnly`，**不会传递依赖**，你必须显式声明。这是最常见的原因。
2. `data-dir` 用了相对路径吗？相对路径按**进程工作目录**解析，容器和 systemd 里往往不是你以为的目录。
3. fat jar / shaded jar 部署时，资源被打进去了吗？`unzip -l app.jar | grep znlp/` 确认。
4. `disabled-modules` 是否禁用了提供该资源的模块？

异常消息里会自动生成对应的 Maven / Gradle 依赖片段。把 `dev.znlp` 日志级别调到 `DEBUG` 能看到资源查找的完整过程。

### 首次启动特别慢

正常现象。首次运行要把原始资源解析成派生缓存写入 `cache-dir`，其中**二元语法表缓存约 40 MB**。之后启动直接读缓存。

K8s 的 `startupProbe` / `initialDelaySeconds` 要留足时间，否则会在预热完成前被反复重启。更好的做法是构建镜像时预热一次，把 `data/cache` 打进镜像。

### 容器文件系统只读，缓存写不进去

把 `cache-dir` 挂到可写 volume。缓存是**可再生**的，删掉整个目录是安全的，代价只是下次启动变慢。

## 使用问题

### 加了用户词典但不生效

用户词典**不进词图、不参与 Viterbi**，而是在选路完成**之后**做合并，且要求词的两端都落在已有词的边界上。

所以：能把 `["长江", "大桥"]` 粘成 `["长江大桥"]`；但**无法切开**一个已被核心词典认定的词。如果你期望的是后者，用户词典做不到，需要走 `withRules(RuleManager)` 或 `addCandidate(CandidateSource)`（这两者在选路前铺候选），或者调整核心词典。

另外 `MemUserDictionary` 用固定权重 `WEIGHT = 1000`，不参与概率竞争。

### 用户词典更新不生效

`MemUserDictionary` 是**单线程写、多线程读**，每次写操作都重建并重新发布整棵双数组 Trie。

- 批量加载请用 `addWords(Collection)` 或 `replaceAll(Stream)`，**不要循环 `addWord`**（每次都重建整棵树）
- 需要运行时热更新请改用 `FileUserDictionary` + `refresh()`
- 词会同时以原词和 `CharNormUtils.convert` 的规范化形式入库，所以简体词典也能命中繁体输入

### 自定义拼音读音不生效

`CustomPinyin.put` 只写映射表，不影响已构建的引擎。**改完必须调用 `PinyinService.rebuild()`。**

### 高亮结果和预期不一样

两条规则容易误解：

- **query 是纯 ASCII** 且 `enablePinyin=true` 时走拼音匹配，支持跨音节部分匹配（`licai` 高亮「理财」，`li` 也能匹配「理」）。
- **query 含中文**时走**字符集合匹配**：文本中出现的、属于 query 的字符**全部**高亮，而不是只高亮连续子串。所以 `highlight("张三和李四", "张三")` 会把「张」和「三」都标出来。

需要严格子串语义的话，当前实现不满足，得在 `highlightFragments` 结果上自行二次处理。

`enableFuzzy=true` 允许跳过干扰字符做非连续匹配；设为 `false` 则断开即重置。相邻高亮位会自动合并（用 `BitSet`），所以「理财」输出成一个 `<em>理财</em>`。

### 高亮输出有 XSS 风险

`Znlp.highlight` 直接拼接标签，**不对原文做 HTML 转义**。文本来自用户输入时，请用 `highlightFragments` 拿 `Fragment(text, highlighted)` 自己渲染并转义。

### `withoutPos()` 没用

如果之前调了 `withNer()`，POS 会被强制打开（NER 把词性当特征），此时 `withoutPos()` 不生效。「有 NER 无 POS」的组合当前做不到。

### 相对时间解析结果不对

「明天」「下周三」「三小时后」依赖参考时钟。`Znlp.parse(text)` 不传 clock 时用**当前时刻**，测试和批处理会得到不可复现的结果。**始终显式传 `LocalDateTime`。**

另外 `hour == 24` 会被归一为**次日 00:00**，所以「明天24点」得到的是后天零点，这是有意设计。

### `Dimension` 的偏移对不上

`getStart()` / `getEnd()` 是**闭区间** `[start, end]`，长度是 `end - start + 1`，与 Java 常见的半开区间习惯不同。

### Token 数量太大导致 OOM

`List<Token> segment(String)` 建议输入**不超过 10000 字符**。长文本请用 `segment(char[], Consumer<Token>)` 流式处理，边解析边消费，不构造中间列表。

### 每个请求都 build() 一个分词器，很慢

`SegmentRecipe.build()` 会加载词典和模型，代价极高。`PipelineSegmenter` 是**线程安全**的，启动时构建一次作为单例复用即可。这是最常见的性能事故。

## 性能与选型

### 想要最高吞吐

用 `SegmentRecipe.dictionary()`——只有核心词典、原子识别和二元语法，不含人名识别和词性标注。需要词性时用 `standard()`。

`znlp-benchmarks` 里的 `bigramOnlySegment` 与 `defaultSegment` 的差值就是人名 + 词性的开销，可以量化这个取舍。

### 仓库里找不到性能数字

`docs/设计架构/基准.md` 明确写了 JSON 结果**不提交**、基准数字**不作为 PR 硬门禁**，所以仓库中没有任何已记录的数据。自己跑：

```bash
./gradlew :znlp-benchmarks:jmh
cat znlp-benchmarks/build/results/jmh.json
```

### 训练相关测试没跑

Gradle 测试任务默认排除 `benchmark`、`train`、`performance` 三个 tag：

```bash
./gradlew :znlp:test -PincludeTags=train
```

## 版本与环境

| 项 | 值 |
| :--- | :--- |
| JDK | **17+**（`sourceCompatibility` / `targetCompatibility` 均为 17） |
| Gradle | 9.2.1（仓库自带 wrapper） |
| 主库坐标 | `dev.znlp:znlp:0.1.0` |
| 资源坐标 | `dev.znlp:znlp-resources:1.0.0` |
| 运行时强制依赖 | **无** |
| 日志 | `slf4j-api` / `log4j-api` 均为 `compileOnly`；都没有时退回 JDK `java.util.logging` |

日志框架是可选的：宿主应用提供了 SLF4J 或 Log4j2 就自动接入，否则用 JDK logging 兜底，不必为 znlp 额外引入日志依赖。

## 相关

- [安装](../getting-started/installation/) —— 三种引入方式
- [配置项参考](configuration/) —— 全部配置项与资源清单
- [资源下载与离线部署](../deployment/resources/) —— 离线部署形态
- [Spring Boot 集成](../deployment/spring-boot/) —— 容器化注意事项
