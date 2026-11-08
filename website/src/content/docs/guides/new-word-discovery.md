---
title: 新词发现
description: 基于互信息与信息熵从语料中发现未登录词，两轮扫描引擎、打分策略与结果导出。
---

新词发现用于从大规模语料中自动找出词典里没有的候选词（未登录词），常见用途是扩充用户词典、构建领域词表。

## 门面入口

```java
import dev.znlp.Znlp;
import entity.newwords.dev.znlp.NewWord;

List<NewWord> words = Znlp.discoverNewWords(new File("/path/to/corpus"));
// 或传路径字符串
List<NewWord> words2 = Znlp.discoverNewWords("/path/to/corpus");
```

参数是**目录**，会递归扫描目录下的所有文本文件。门面版本使用默认配置、**自适应打分**、**排除核心词典**中已有的词，结果按分数降序排列。

:::note[门面版与 Builder 版的 verbose 差异]
`Znlp.discoverNewWords` 内部构建 `NewWordDiscoveryTool` 时把 `verbose` 设为 `false`；而 `Builder` 的默认值是 `true`。如果你直接用 Builder 又不想看到进度输出，需要显式 `.verbose(false)`。
:::

## NewWordDiscoveryTool

`newwords.dev.znlp.NewWordDiscoveryTool` 是完整的入口类，通过 Builder 配置：

```java
import newwords.dev.znlp.NewWordDiscoveryTool;
import engine.newwords.dev.znlp.INewWordFindEngine;

INewWordFindEngine engine = INewWordFindEngine.create();

NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(corpusDir, engine)
        .charset(StandardCharsets.UTF_8)   // 默认 UTF-8
        .verbose(true)                     // Builder 默认 true
        .build();

ArrayList<NewWord> words = tool.discover();
```

`discover()` 内部执行两轮扫描，流程是 `firstScan → finishFirst → secondScan → endSecond → result`。

### 导出结果

```java
tool.saveToFile(new File("newwords.txt"));

tool.saveToFile(file, includeHeader, charset);

tool.saveToFile(
    file,
    includeHeader,      // 是否写表头
    delimiter,          // 列分隔符
    chineseHeader,      // 表头是否用中文
    charset
);

// 静态方法，直接导出已有结果
NewWordDiscoveryTool.saveResults(words, file, ...);
```

## 引擎：INewWordFindEngine

`engine.newwords.dev.znlp.INewWordFindEngine` 是统计引擎接口，实现类为 `NewWordFindEngineOptimized`。

### 创建

```java
// 默认配置，不排除核心词典
INewWordFindEngine engine = INewWordFindEngine.create();

// 自定义配置 + 是否排除核心词典 + 核心词典实例
INewWordFindEngine engine = INewWordFindEngine.create(
        config, excludeCoreDict, coreDictionary);
```

`excludeCoreDict = true` 时会过滤掉已在核心词典中的词，只留下真正的未登录词。门面版本就是这么做的。

### 配置

```java
// Config 是 record(minGroup, maxGroup, minOccurCount)
INewWordFindEngine.Config config = INewWordFindEngine.Config.defaultConfig();
// 默认值：minGroup=3, maxGroup=12, minOccurCount=10
```

| 参数 | 默认 | 含义 |
| :--- | :--- | :--- |
| `minGroup` | 3 | 候选词最小长度（字） |
| `maxGroup` | 12 | 候选词最大长度（字） |
| `minOccurCount` | 10 | 最低出现次数，低于此值不参与统计 |

调小 `minOccurCount` 会召回更多低频候选但噪声上升；调大 `maxGroup` 会增加内存和耗时。

### 生命周期方法

如果要自己控制扫描过程（比如语料来自数据库而非文件）：

```java
engine.firstScan(text);    // 第一轮：统计 n-gram 频次
engine.finishFirst();      // 第一轮结束，计算互信息
engine.secondScan(text);   // 第二轮：统计左右邻接熵
engine.endSecond();        // 第二轮结束
List<NewWord> result = engine.result();

engine.setScorer(scorer);  // 替换打分器
engine.getScorer();
```

**两轮扫描是必须的**，且第一轮的所有文本喂完之后才能调 `finishFirst()`，第二轮同理。顺序错了统计结果无效。

## 打分策略

`scorer.newwords.dev.znlp.NewWordScorerFactory` 提供多种打分器：

| 工厂方法 | 说明 |
| :--- | :--- |
| `createDefault()` | 固定阈值打分 |
| `createAdaptive()` | 自适应打分（门面默认） |
| `createAdaptive(FilterStrategy)` | 自适应 + 指定过滤策略 |
| `createSemiAdaptive(...)` | 半自适应 |
| `createAdaptiveManual(...)` | 手动参数的自适应 |

自适应打分不依赖固定阈值，而是根据语料的统计分布动态决定截断点，因此换语料时不需要重新调参。这是门面选它作为默认值的原因。

## NewWord 结果实体

`entity.newwords.dev.znlp.NewWord` 暴露的 getter：

| 方法 | 含义 |
| :--- | :--- |
| `getWord()` | 候选词 |
| `getLen()` | 词长 |
| `getFreq()` | 出现频次 |
| `getDocFreq()` | 文档频次 |
| `getMi()` | 互信息（ Mutual Information ） |
| `getAvg_mi()` | 平均互信息 |
| `getEntropy()` | 信息熵 |
| `getLe()` / `getRe()` | 左熵 / 右熵 |
| `getLeftVariety()` / `getRightVariety()` | 左右邻接种类数 |
| `getIdf()` | 逆文档频率 |
| `getScore()` | 综合得分（排序依据） |

## 算法原理

判定一个字符串是不是词，靠两个指标：

- **互信息（MI）**衡量内部凝固度。一个真实词的各部分倾向于一起出现，互信息高；偶然相邻的字串互信息低。
- **信息熵**衡量边界自由度。真实词的左右邻接字应当多样（熵高）；如果某个字串右边永远只跟同一个字，它更可能是更长词的一部分而非独立词。

两轮扫描正对应这两个指标：第一轮统计 n-gram 频次算互信息，第二轮统计左右邻接算熵。

## 测试语料

仓库在 `data/newwords/newword_test/` 下提供了一批新闻文本，可以直接用来验证：

```java
List<NewWord> words = Znlp.discoverNewWords("data/newwords/newword_test");
words.stream().limit(50).forEach(w ->
    System.out.println(w.getWord() + "\t" + w.getFreq() + "\t" + w.getScore()));
```

另外根目录的 `data-dir-meta.txt` 说明了一个**不随仓库分发**的外部大数据集约定：放在项目根的同级目录 `znlp-data/` 下，其中 `newword_test_small` 是缩小版的新词发现语料。

## 相关

- 发现的新词如何回灌到分词：[中文分词](segmentation/)的用户词典部分
- 模块内部的详细说明：`newwords/README.md` 与 `newwords/scorer/README*.md`
