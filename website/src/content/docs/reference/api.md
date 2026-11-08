---
title: API 参考
description: Znlp 门面全部静态方法，以及各模块核心类型的公开签名速查。
---

本页列出经过源码核对的公开 API。包根为 `dev.znlp`。

## Znlp 门面

`dev.znlp.Znlp` —— `public final class`，构造器私有，全部静态。

### 服务与分词器

| 签名 | 说明 |
| :--- | :--- |
| `static <T> T getService(Class<T> serviceClass)` | 从进程级 `ZnlpSystem` 取服务 |
| `static SegmentRecipe recipe()` | 等价 `SegmentRecipe.standard()` |
| `static WordSegmenter segmenter()` | 默认分词器，惰性单例，线程安全 |
| `static List<Token> segment(String text)` | 默认分词；`null` 抛 `IllegalArgumentException` |
| `static List<String> segmentToStrings(String text)` | 只返回词面 |
| `static PersonNamePerceptron getPersonNamePerceptron()` | 默认分词器所用的人名模型 |

### 拼音

| 签名 | 说明 |
| :--- | :--- |
| `static PinyinResult pinyin(String text)` | 拼音转换入口，返回可配置的结果对象 |
| `static String pinyinString(String text)` | 拼音串，空格分隔 —— **实际不带声调**（Javadoc 有误） |
| `static String pinyinHeadString(String text)` | 拼音首字母串，空格分隔 |

### 繁简转换

| 签名 | 说明 |
| :--- | :--- |
| `static String s2t(String text)` | 简体转繁体 |
| `static String t2s(String text)` | 繁体转简体 |

### Duckling

| 签名 | 说明 |
| :--- | :--- |
| `static List<Dimension> parse(String text)` | 结构化解析，参考时间取当前时刻 |
| `static List<Dimension> parse(String text, LocalDateTime clock)` | 指定参考时钟 |

### 新词发现

| 签名 | 说明 |
| :--- | :--- |
| `static List<NewWord> discoverNewWords(File corpusDir)` | 递归扫描目录，默认配置 + 自适应打分 + 排除核心词典，按分数降序 |
| `static List<NewWord> discoverNewWords(String corpusDirPath)` | 路径字符串版 |

### 智能高亮

| 签名 | 说明 |
| :--- | :--- |
| `static String highlight(String text, String query)` | 默认配置（`em` 标签，拼音 + 模糊开启） |
| `static String highlight(String text, String query, SmartHighlightConfig config)` | 自定义配置 |
| `static List<String> highlight(List<String> texts, String query)` | 批量，query 只预处理一次 |
| `static List<String> highlight(List<String> texts, String query, SmartHighlightConfig config)` | 批量自定义配置 |
| `static List<Fragment> highlightFragments(String text, String query)` | 输出结构化片段，不生成 HTML |
| `static List<Fragment> highlightFragments(String text, String query, SmartHighlightConfig config)` | 同上自定义配置 |
| `static List<List<Fragment>> highlightFragments(List<String> texts, String query)` | 批量片段 |
| `static List<List<Fragment>> highlightFragments(List<String> texts, String query, SmartHighlightConfig config)` | 批量片段自定义配置 |

## SegmentRecipe

`recipe.segment.dev.znlp.SegmentRecipe` —— 不可变，`with*` 返回新实例。

### 静态工厂

| 签名 | 组成 |
| :--- | :--- |
| `static SegmentRecipe dictionary()` / `dictionary(ServiceLocator)` | 核心词典 + 原子识别 + 二元 Viterbi |
| `static SegmentRecipe standard()` / `standard(ServiceLocator)` | `dictionary()` + 人名 + 词性 |
| `static SegmentRecipe perceptron()` / `perceptron(ServiceLocator)` | CWS 感知机边界 + 原子补洞，线性路径 |

### 链式方法（均返回 `SegmentRecipe`）

```java
withPersonNames() / withoutPersonNames()
withPos() / withoutPos()          // NER 开启时 withoutPos 无效
withNer()                         // 自动打开 POS
withUserDictionary(UserDictionary...)
withUserDictionary(List<UserDictionary>)
withCorrections(CorrectionDictionary)
withRules(RuleManager)            // 仅对词典配方有效
indexTerms()
addCandidate(CandidateSource)
addConstraint(SpanConstraint)
addAnnotator(TokenAnnotator)
addNormalizer(CharNormalize)
build()                           // -> WordSegmenter (PipelineSegmenter)
```

内部枚举：`PathKind { VITERBI, LINEAR }`。

## WordSegmenter

`segment.dev.znlp.WordSegmenter`

```java
void segment(char[] text, Consumer<Token> consumer)   // 抽象，核心方法
void segment(String text, Consumer<Token> consumer)   // default
List<Token> segment(String text)                      // default，不可变列表，建议 ≤10000 字符
List<String> segmentToStrings(String text)            // default
```

## Token

`segment.dev.znlp.Token` —— `public final class implements Serializable, WordAndNature`

| 成员 | 说明 |
| :--- | :--- |
| `public final String word` | 词面（公开字段） |
| `public int offset` | 原文偏移（公开字段） |
| `Token(String)` / `Token(String, Nature)` / `Token(String, Nature, int)` | 构造器 |
| `getNature()` / `setNature(Nature)` | 词性枚举 |
| `getNatureString()` | 词性串，null 时返回 `""` |
| `getNatureName()` | 词性名称 |
| `getWord()` | `word` 的 getter |
| `getOffset()` / `setOffset(int)` | 偏移 |
| `getSubword()` / `setSubword(List<Token>)` / `hasSubword()` | 子词（配合 `indexTerms()`） |
| `getCustomFlag()` / `setCustomFlag(String)` | 自定义标记 |
| `length()` | 词面长度 |
| `getPosInc()` / `setPosInc(int)` | 位置增量，setter 链式返回 `this` |
| `toString()` | 输出 `word/nature` |

## 用户词典

`userdict.impl.segment.dev.znlp.UserDictionary`

```java
StringIntDATMap getTrie();     // 唯一方法；无词时返回 null
```

**MemUserDictionary**（单线程写、多线程读，写时重建整棵双数组）

```java
MemUserDictionary()
MemUserDictionary(TreeMap<String, Integer>)   // 拷贝副本
rebuild()
addWord(String)
addWords(Collection<String>)
replaceAll(Stream<String>)
removeWord(String)
clear()
getTrie()
```

内部固定权重 `WEIGHT = 1000`；词同时以原词和 `CharNormUtils.convert` 规范化形式入库。

**FileUserDictionary**

```java
FileUserDictionary(File)
FileUserDictionary(File, Charset)
refresh()          // 多个重载
getTrie()
```

**ResourceUserDictionary** —— 内置词库 DAT 访问，路径常量：`znlp/custom-dict/` 下的 `CustomDictionary.txt`、`上海地名.txt`、`人名词典.txt`、`全国地名大全.txt`、`机构名词典.txt`、`现代汉语补充词库.txt`；getter 如 `getCustomDictDat()`。

## 感知机模型

### PosPerceptron（`segment.impl.pos`）

```java
List<String> decode(List<String> sentence)
static PosPerceptron loadFromClasspath(String path)
static PosPerceptron load(NlpResourceLoader loader, String path)
static PerceptronTrainModel train(File trainFile, File evalFile, File workDir, int iter, int threadNum)
```

模型资源：`znlp/pos-model/pos.model`

### NERPerceptron（`segment.impl.ner`）

```java
List<String> decode(List<WordAndNature> sentence)
static NERPerceptron loadFromModelDir(File dir)
static NERPerceptron loadFromClasspath(String path)
static NERPerceptron loadFromResource(NlpResourceLoader loader, String path)
```

标签集：`B-ns M-ns E-ns B-nt M-nt E-nt O S`；模型资源：`znlp/ner/ner.model`

### CwsPerceptron

```java
List<String> split(String text)
decode(...)
static List<String> labelsToWords(char[] text, int[] labels)
static CwsPerceptron load(NlpResourceLoader loader)
static CwsPerceptron load(NlpResourceLoader loader, String modelDir)
static CwsPerceptron loadFromDir(File dir)
```

默认模型目录：`znlp/cws-model`

### PersonNamePerceptron

```java
findPersonName(String) / findPersonName(char[])
decode(...)
// BMEO 常量：B=0, M=1, E=2, O=3
```

模型资源：`znlp/personname/nr-name.model`

### 框架（`dev.znlp.perceptron`）

```java
// PerceptronModel
new PerceptronModel(ImmutableFeatureSet, int labelCount, float[] parameter, boolean decodeQuickModel)
static read(File) / readFromClasspath(String) / readFromNlpResource(NlpResourceLoader, String)
getDecoder() / getFeatureSet()

// PerceptronTrainer<E, InputSequence>
new PerceptronTrainer<>(instance, workDir)
PerceptronTrainModel train(File trainFile, File evalFile, int iter, int threadNum, boolean quickDecode)

// PerceptronTrainModel
toModel()
compress(double ratio, double threshold)
saveModelBinFile(File, FeatureFormat)
saveModelToDir(File)
static readModelFromDir(File)
```

## 拼音

### PinyinService（`dev.znlp.pinyin`）

```java
PinyinResult convert(String text)
List<String> split(String text)          // 感知机音节切分
List<String> split(char[] text)
SimplePinyin[] charPinyin(char c)
CustomPinyin getCustomPinyin()
void rebuild()                           // 自定义读音变更后必须调用
```

### PinyinResult

```java
fuzzy(boolean) / keepPunctuation(boolean) / keepNumber(boolean)
keepAlpha(boolean) / keepOthers(boolean)          // 均返回 this
asString() / asString(String splitter)
asHeadString() / asHeadString(String splitter)
asList() / asHeadList() / getPinyinList()
```

`keepNumber` 与 `keepAlpha` 默认 `true`。模糊音映射：`zh→z ch→c sh→s eng→en ang→an ing→in iang→ian uang→uan`。

### CustomPinyin

```java
put(String text, String pinyin)
remove(String text)
getMap()
```

枚举包 `pinyin.enums`：`Pinyin`、`SimplePinyin`、`PinyinHead`、`Shengmu`、`Yunmu`。

## 繁简转换

`converter.dev.znlp.TransformService`

```java
String s2t(String) / String t2s(String)
String s2t(char[]) / String t2s(char[])
```

资源：`znlp/ts-dict/s2t.txt`、`znlp/ts-dict/t2s.txt`

## 智能高亮

### SmartHighlighter（`dev.znlp.highlight`）

构造器注入 `PinyinService`。

```java
highlight(String, String, SmartHighlightConfig)
highlight(List<String>, String, SmartHighlightConfig)
highlightFragments(String, String, SmartHighlightConfig)
highlightFragments(List<String>, String, SmartHighlightConfig)
```

### SmartHighlightConfig

```java
record SmartHighlightConfig(String highlightTag, boolean enablePinyin, boolean enableFuzzy)
// 无参默认 ("em", true, true)
withHighlightTag(String) / withEnablePinyin(boolean) / withEnableFuzzy(boolean)
static builder()  ->  Builder.highlightTag() / enablePinyin() / enableFuzzy() / build()
```

### Fragment

```java
record Fragment(String text, boolean highlighted)
```

## Duckling

### DucklingParser（`dev.znlp.duckling`）

```java
static DucklingParser getInstance()
DucklingParser(RuleSet)              // 例如 new DefaultRuleSet(List<Rule>)
generateAll(String)                  // 调试：全部图节点，不消歧
```

### Dimension（`duckling.support`）

```java
getText() / getStart() / getEnd()    // 闭区间 [start, end]
with(String, int, int)
abstract DimensionKind getKind()
```

`DimensionKind`：`Number(0) AmountOfMoney(1) Ordinal(2) Temperature(3) Duration(4) Time(5) Date(6) PhoneNumber(7) Email(8) Url(9) Distance(10) Age(11) Percentage(12)`（括号内为消歧优先级）。

:::caution[Email 与 Url 无对应 grammar]
实际注册的语法只有 10 个：Number、Ordinal、Money、DateTime、Percentage、Distance、Temperature、Age、PhoneNum、Duration。`Email` 和 `Url` 永远不会被返回；`Date` 与 `Time` 共用 `DateTimeDimension`，其 `getKind()` 返回 `Time`。
:::

子类取值：

```java
NumberDimension    : getValue():double, isInt(), getInt()
MoneyDimension     : getValue():float, getUnit():MoneyUnit(仅 RMB), getGrain():int(元1/角2/分3)
DateTimeDimension  : toDateTime():LocalDateTime, getGrain(), getYear/Month/Day/Hour/Minute/Second(),
                     isFuture(), getFutureList()          // hour==24 归一为次日 00:00
其余：OrdinalDimension, PercentageDimension, DistanceDimension,
      TemperatureDimension, AgeDimension, PhoneNumDimension, DurationDimension
```

消歧模型：classpath `znlp/duckling.model`（由 `MlModel.load()` 读取）。

## 新词发现

### NewWordDiscoveryTool（`dev.znlp.newwords`）

```java
new Builder(File directory, INewWordFindEngine engine)
    .charset(Charset)      // 默认 UTF-8
    .verbose(boolean)      // Builder 默认 true；Znlp 门面用 false
    .build()

ArrayList<NewWord> discover()
saveToFile(File)
saveToFile(File, boolean includeHeader, Charset)
saveToFile(File, boolean includeHeader, String delimiter, boolean chineseHeader, Charset)
static saveResults(...)
```

### INewWordFindEngine（`newwords.engine`）

```java
static create()                                                  // 默认配置，不排除核心词典
static create(Config, boolean excludeCoreDict, CoreDictionary)   // 实现类 NewWordFindEngineOptimized

record Config(int minGroup, int maxGroup, int minOccurCount)     // 默认 (3, 12, 10)
static Config.defaultConfig()

firstScan(String) / finishFirst() / secondScan(String) / endSecond() / result()
setScorer(...) / getScorer()
```

### NewWordScorerFactory（`newwords.scorer`）

```java
createDefault() / createAdaptive() / createAdaptive(FilterStrategy)
createSemiAdaptive(...) / createAdaptiveManual(...)
```

### NewWord（`newwords.entity`）

```java
getWord() getLen() getFreq() getDocFreq() getMi() getAvg_mi() getEntropy()
getLe() getRe() getIdf() getScore() getLeftVariety() getRightVariety()
```

## FastText

`fasttext.dev.znlp.FastText` —— `record FastText(Args args, Dictionary dict, Model model)`，对齐 Facebook fastText v0.9.2（magic `793712314`，version `12`）。

```java
// 分类
List<ScoreLabelPair> predict(Iterable<String> tokens, int k, float threshold)
List<ScoreIdPair> predict(int k, IntArrayList words, float threshold)   // 仅 sup 模型
FastTextTestResult test(File, int k, float threshold, boolean print)
FastTextTestResult test(Iterable<SampleLine>, int k, float threshold, boolean print)
static FastText trainSupervised(File file, Args args)

// 词向量
static FastText train(File, Args)
static FastText train(List<Iterable<SampleLine>>, Args)

// 存取
saveModel(File)                    // Facebook 兼容 .bin，非量化
static FastText loadModel(File)    // 不支持 .ftz 量化模型

getOutput()                        // 输出矩阵
model.wi()                         // 输入矩阵
```

:::caution[没有词向量查询方法]
代码中不存在 `getWordVector` / `nearestNeighbor`。取向量需自行用 `dict` 映射行号后访问 `wi()` / `getOutput()`。
:::

`Args` 公开字段默认值见 [FastText 文本分类](../guides/fasttext/)。

## 系统与服务注册

### ZnlpSystem

```java
record ZnlpSystem(ServiceRegistry registry)
static ZnlpSystem getInstance()                      // 进程级单例
static DefaultZnlpSystemBuilder builder()            // 返回类型为包私有类
static DefaultZnlpSystemBuilder builder(ZnlpSystemConfig)
<T> T getService(Class<T> clazz)                       // 找不到抛 ServiceNotFoundException
```

:::caution[包外无法构建自定义实例]
`DefaultZnlpSystemBuilder` 是包私有类，其 `build()` 也是包私有，且**没有任何 `withXxx` 方法**。包外代码无法通过 builder 构建自定义 `ZnlpSystem`。定制只能靠 JVM 属性 / classpath 上的 `znlp.properties` / `SystemDefaultConfig.reset()`。
:::

无 close / shutdown / destroy 方法。

### ZnlpSystemConfig（`dev.znlp.config`）

```java
List<String> getDataDirs() / String getDataDir()       // 后者返回第一个
String getCacheDir() / Boolean getAutoDownloadResource() / String getDisabledModules()
static ZnlpSystemConfigBuilder builder()
```

### ZnlpSystemConfigBuilder

```java
create() / dataDir(String) / dataDirs(List<String>) / dataDirs(String 逗号分隔)
cacheDir(String) / autoDownloadResource(boolean) / disabledModules(String)
build()                 // 未设置项为 null
buildWithDefaults()     // 未设置项继承 SystemDefaultConfig
```

setter 在 `ZnlpSystemConfig` 上均为 package-private。

### ServiceRegistry（`base.service`，实现 `ServiceLocator`）

```java
registerFactory(Class, ServiceFactory)
registerFactory(Class, ServiceFactory, boolean singleton)
registerFactory(Class, Class<? extends ServiceFactory>, boolean)
registerSingleton(Class, T)
registerClass(Class) / registerSingletonClass(Class) / registerPrototypeClass(Class)
registerClass(Class, boolean)
<T> T getService(Class)
new ServiceRegistry(ServiceLocator parent)             // 支持父子容器
```

## Nature

`segment.dev.znlp.Nature` —— 枚举，48 个常量：

```
a Ag ad an b Bg c d Dg e f h i j k l m Mg
n nr ns nt nx nz Ng o p q r Rg s t Tg u v
vd vn Vg w y Yg z xx mq x begin end newWord
```

## 相关

- [配置项参考](configuration/) —— 全部配置项与资源清单
- [示例程序](examples/) —— 可运行的示例代码
