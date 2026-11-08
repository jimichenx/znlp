---
title: 命名实体识别
description: znlp 的感知机 NER 只识别地名（ns）和机构团体（nt）两类实体，人名由独立模型负责。
---

:::caution[先看清楚支持范围]
znlp 的 NER **只识别两类实体：地名（`ns`）和机构团体（`nt`）**。标签集硬编码在 `NERPerceptronInstance` 中（`targetPos = {"ns", "nt"}`），完整标签为：

```
B-ns  M-ns  E-ns  B-nt  M-nt  E-nt  O  S
```

**人名不由 NER 负责**，而是走独立的 `PersonNamePerceptron` 模型（在 `standard()` 配方里默认开启）。时间、数字、金额等也不属于 NER，它们由 [Duckling](duckling/) 处理。如果你的需求是通用实体抽取（人名、机构、地名、时间、数量全覆盖），需要把这三者组合起来。
:::

## 在分词流水线中开启

NER **默认不开启**，需要显式追加：

```java
import segment.dev.znlp.WordSegmenter;
import recipe.segment.dev.znlp.SegmentRecipe;
import segment.dev.znlp.Token;

WordSegmenter segmenter = SegmentRecipe.standard().withNer().build();

for (Token t : segmenter.segment("中国科学院位于北京市")) {
    System.out.println(t.word + "/" + t.getNature());
}
```

命中的实体，其 `Token.getNature()` 会被设为 `Nature.ns` 或 `Nature.nt`，由 `NerProcessor` 在合并 B/M/E 跨度之后写回。

:::note[withNer 会自动打开 POS]
NER 把词性当作特征，所以调用 `withNer()` 时 POS 阶段会被强制开启，之后的 `withoutPos()` 不生效。这意味着开启 NER 必然带来词性标注的开销。
:::

因为 `WordSegmenter` 线程安全，这个 `segmenter` 应该作为单例长期持有，而不是每次调用都 `build()`。

## 独立使用 NERPerceptron

已有分词和词性结果时，可以跳过流水线直接调用 `ner.impl.segment.dev.znlp.NERPerceptron`：

```java
import ner.impl.segment.dev.znlp.NERPerceptron;
import segment.dev.znlp.WordAndNature;

NERPerceptron ner = Znlp.getService(NERPerceptron.class);

List<String> labels = ner.decode(sentence);   // sentence 是 List<WordAndNature>
```

注意 `decode` 的入参是 `List<WordAndNature>` 而不是 `List<String>`——NER 需要词性作为特征。`Token` 本身就实现了 `WordAndNature`，所以可以直接把分词结果传进来。

### 加载模型

```java
static NERPerceptron loadFromModelDir(File dir)
static NERPerceptron loadFromClasspath(String path)
static NERPerceptron loadFromResource(NlpResourceLoader loader, String path)
```

默认模型资源是 `znlp/ner/ner.model`，随 `znlp-resources` jar 分发。`loadFromModelDir` 用于加载自己训练出的目录式模型。

## 人名识别

人名走的是另一条路径。`PersonNamePerceptron` 在 `standard()` 配方中默认启用，可以用 `withoutPersonNames()` 关闭：

```java
WordSegmenter noNames = SegmentRecipe.standard().withoutPersonNames().build();
```

独立使用：

```java
import personname.impl.segment.dev.znlp.PersonNamePerceptron;

PersonNamePerceptron pn = Znlp.getPersonNamePerceptron();   // 门面提供的快捷方法
List<String> names = pn.findPersonName("文本");
// 也有 char[] 重载：pn.findPersonName(char[])
```

`PersonNamePerceptron` 使用 BMEO 标注，常量为 `B=0 / M=1 / E=2 / O=3`。模型资源是 `znlp/personname/nr-name.model`。

`Znlp.getPersonNamePerceptron()` 返回的就是默认分词器所用的那个实例，因此它和 `Znlp.segment` 的人名结果一致。

## 组合出完整的实体抽取

由于三类能力分散在不同组件，一个覆盖人名 / 地名 / 机构 / 时间 / 数量的抽取流程大致是这样：

```java
// 1. 分词 + POS + 人名 + NER 一次拿到
WordSegmenter segmenter = SegmentRecipe.standard().withNer().build();
List<Token> tokens = segmenter.segment(text);

for (Token t : tokens) {
    switch (t.getNature()) {
        case nr -> handlePerson(t);        // 人名（PersonNamePerceptron）
        case ns -> handleLocation(t);      // 地名（NER）
        case nt -> handleOrganization(t);  // 机构（NER）
        default -> { }
    }
}

// 2. 时间、数字、金额、电话等结构化维度另走 Duckling
List<Dimension> dims = Znlp.parse(text);
```

`Dimension` 的 `getStart()` / `getEnd()` 是闭区间偏移，可以和 `Token.offset` 对齐做归并。

## 训练自己的 NER 模型

`NERPerceptron` 基于通用感知机框架，训练流程与 POS 一致：实现 `PerceptronInstance`，交给 `PerceptronTrainer.train`，产出 `PerceptronTrainModel` 后用 `saveModelToDir(File)` 落盘，运行时通过 `NERPerceptron.loadFromModelDir(File)` 加载。

仓库的 `data/work/` 下有 `ner-model`、`ner-model-dynamic`、`ner-model-new` 等历史训练工作目录可作参考。完整的训练机制见[感知机框架](perceptron/)。

:::note[扩充实体类型需要改代码]
`targetPos` 和标签集是硬编码的，无法通过配置增加实体类型。要识别新的实体类别，需要修改 `NERPerceptronInstance` 并用对应标注的语料重新训练。
:::

## 相关

- [中文分词](segmentation/) —— 配方组合与 Token 结构
- [词性标注](pos-tagging/) —— Nature 标签全集
- [Duckling 结构化解析](duckling/) —— 时间、数字、金额等维度
- [感知机框架](perceptron/) —— 训练自己的模型
