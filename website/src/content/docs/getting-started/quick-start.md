---
title: 快速开始
description: 用 Znlp 门面完成分词、拼音、繁简转换与结构化解析，并用 SegmentRecipe 组装自定义分词器。
---

本页假设你已经按[安装](installation/)把 `znlp` 和 `znlp-resources` 都放进了 classpath。

## 门面入口：Znlp

`dev.znlp.Znlp` 是一个 `public final class`，构造器私有，全部能力通过静态方法暴露。绝大多数场景只需要它，不必接触 `ZnlpSystem`。

### 分词

```java
import dev.znlp.Znlp;
import segment.dev.znlp.Token;
import java.util.List;

List<Token> tokens = Znlp.segment("自然语言处理技术是人工智能的重要领域");
for (Token token : tokens) {
    System.out.println(token.word + "/" + token.getNature());
}
```

`Token` 的 `word` 是 `public final String` 字段，可以直接访问；词性通过 `getNature()` 拿 `Nature` 枚举，或用 `getNatureString()` 拿字符串（词性为 null 时返回空串而不是 `"null"`）。

只要词面不要词性时用：

```java
List<String> words = Znlp.segmentToStrings("南京市长江大桥");
```

`Znlp.segment(null)` 会抛 `IllegalArgumentException`，空字符串返回空列表。

### 默认分词器是什么

`Znlp.segment` 背后是 `SegmentRecipe.standard().build()`，即**核心词典 + 二元语法 Viterbi + 原子识别 + 人名识别 + 词性标注**。它是惰性初始化的单例（内部 `DefaultSegmenterHolder`），构建出的 `WordSegmenter` 实现是 `PipelineSegmenter`，**可以安全地在多线程间复用**，不需要每次请求都新建。

如果你想要一个长期持有的实例：

```java
import segment.dev.znlp.WordSegmenter;

WordSegmenter segmenter = Znlp.segmenter();
List<Token> tokens = segmenter.segment("文本");
```

### 拼音

```java
Znlp.pinyinString("你好znlp");      // ni hao znlp
Znlp.pinyinHeadString("复旦大学");     // f d d x
```

:::caution[pinyinString 输出不带声调]
`Znlp.pinyinString` 的 Javadoc 写的是"带声调"，但实现走的是 `getPinyinWithoutTone()`，**实际输出不带声调**。如果需要声调或更细的控制（模糊音、保留标点、保留数字字母），用 `Znlp.pinyin(text)` 拿到 `PinyinResult` 再链式配置，详见[拼音](../guides/pinyin/)。
:::

字母和数字默认原样保留（`keepAlpha` / `keepNumber` 默认为 `true`），所以 `znlp` 不会被转掉。

### 繁简转换

```java
Znlp.s2t("汉字");   // 漢字
Znlp.t2s("漢字");   // 汉字
```

### 结构化解析（Duckling）

Duckling **独立于分词流水线**，不会被 `Znlp.segment` 触发，需要显式调用：

```java
import support.duckling.dev.znlp.Dimension;
import java.time.LocalDateTime;
import java.util.List;

List<Dimension> dims = Znlp.parse("明天下午3点见面，电话13800138000");

// 相对时间需要参考时钟才能归一化
List<Dimension> withClock = Znlp.parse("明天", LocalDateTime.of(2026, 8, 26, 12, 0));
```

不传 `clock` 时以当前时刻为参考。返回的 `Dimension` 的 `getStart()` / `getEnd()` 是**闭区间**。

## 用 SegmentRecipe 组装自定义分词器

当默认配方不满足需求时，用 `SegmentRecipe`。它是**不可变**的，每个 `with*` 都返回新实例，所以可以安全地共享一个基础配方再派生变体。

```java
import segment.dev.znlp.WordSegmenter;
import recipe.segment.dev.znlp.SegmentRecipe;

// 纯感知机分词：CWS 感知机判定边界 + 原子补洞，不跑二元语法
WordSegmenter cws = SegmentRecipe.perceptron().build();

// 标准配方追加 NER（withNer 会自动打开 POS）
WordSegmenter ner = SegmentRecipe.standard().withNer().build();

// 只要词典和二元语法，不要人名和词性（更快）
WordSegmenter light = SegmentRecipe.dictionary().build();
```

三个静态工厂的区别：

| 工厂 | 组成 | 适用 |
| :--- | :--- | :--- |
| `dictionary()` | 核心词典 + 原子识别 + 二元 Viterbi | 追求速度，不需要词性 |
| `standard()` | `dictionary()` + 人名 + 词性 | 通用场景，等价于 `Znlp.segment` |
| `perceptron()` | CWS 感知机边界 + 原子补洞，线性路径 | 统计模型分词，不跑二元语法 |

### 挂用户词典

```java
import userdict.impl.segment.dev.znlp.MemUserDictionary;

MemUserDictionary dict = new MemUserDictionary();
dict.addWord("长江大桥");
dict.addWords(List.of(" znlp", "感知机"));

WordSegmenter custom = SegmentRecipe.standard()
        .withUserDictionary(dict)
        .build();
```

:::tip[用户词典的合并时机很关键]
用户词典**不进词图、不参与 Viterbi**，而是在选路完成之后做合并，且要求词的两端都落在已有词的边界上。所以它能把已有的切分结果"粘"成一个词，但无法强行切开一个已被词典认定的词。理解这一点能避免大部分"加了词典却不生效"的困惑，详见[中文分词](../guides/segmentation/)。
:::

`MemUserDictionary` 是**单线程写、多线程读**：每次写入都会重建并重新发布整棵双数组 Trie。适合启动时批量加载；运行时高频写入应改用 `FileUserDictionary` 配合 `refresh()`。

## 运行仓库自带的示例

`znlp-examples` 模块有 4 个可直接运行的示例：

```bash
# 默认运行 CwsPerceptronExample
./gradlew :znlp-examples:run

# 指定其他示例
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DucklingExample
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DefaultSegmenterExample
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.FastTextExample
```

`run` 任务的堆上限是 2 GB。各示例的完整说明见[示例程序](../reference/examples/)。

## 接下来

- 每项能力的详细用法在「核心能力」分组
- 上生产前务必读[配置与数据目录](../deployment/configuration/)和[资源下载与离线部署](../deployment/resources/)
