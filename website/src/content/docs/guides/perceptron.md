---
title: 感知机框架
description: znlp 的词性、NER、人名、拼音切分、中文分词共用同一套感知机框架，可以用自己的语料训练模型。
---

znlp 里有五个能力都建立在同一套感知机框架上：中文分词（CWS）、词性标注（POS）、命名实体（NER）、人名识别、拼音音节切分。框架本身是公开的，可以用自己的语料训练模型替换默认的。

包路径：`dev.znlp.perceptron`。

## 核心类型

| 类型 | 作用 |
| :--- | :--- |
| `PerceptronModel` | 运行时模型，持有特征集、标签数和参数数组，提供解码器 |
| `PerceptronInstance<E, InputSequence>` | **训练与推理的抽象点**，定义标签、特征提取、语料解析、评估 |
| `CharBasedPerceptronInstance` | 字符级任务的基类，分词/NER/人名/拼音切分都继承它 |
| `PerceptronTrainer<E, InputSequence>` | 训练器 |
| `PerceptronTrainModel` | 训练产物，可压缩、可落盘、可转运行时模型 |
| `FeatureExtractor` / `CharDSLFeatureExtractor` / `FeatureTemplate` | 特征模板 |
| `ViterbiPerceptronDecoder` / `OptimizedViterbiPerceptronDecoder` / `QuickPerceptronDecoder` | 解码器 |

## PerceptronModel

```java
// 构造
new PerceptronModel(
    ImmutableFeatureSet featureSet,
    int labelCount,
    float[] parameter,
    boolean decodeQuickModel
)

// 加载
static PerceptronModel read(File file)
static PerceptronModel readFromClasspath(String path)
static PerceptronModel readFromNlpResource(NlpResourceLoader loader, String path)

// 使用
model.getDecoder();      // 解码器
model.getFeatureSet();   // 特征集
```

`decodeQuickModel` 控制是否使用 `QuickPerceptronDecoder`——牺牲一点精度换取推理速度。

## 训练流程

```java
PerceptronTrainer<E, Input> trainer = new PerceptronTrainer<>(instance, workDir);

PerceptronTrainModel trainModel = trainer.train(
    trainFile,        // 训练语料
    evaluateFile,     // 评估语料
    iter,             // 迭代轮数
    threadNum,        // 线程数
    quickDecode       // 是否用快速解码
);
```

`workDir` 用于存放中间产物，训练中断后可以复用。

### 训练产物

```java
PerceptronModel runtime = trainModel.toModel();            // 转运行时模型

trainModel.compress(ratio, threshold);                     // 参数压缩，减小模型体积

trainModel.saveModelBinFile(file, featureFormat);          // 单文件 .bin
trainModel.saveModelToDir(dir);                            // 目录式（feature.dat / parameter.bin / ...）

static PerceptronTrainModel m = PerceptronTrainModel.readModelFromDir(dir);
```

目录式模型对应 `data/dat/cws-model/{feature.dat, feature.txt, parameter.bin}` 这种布局。`compress` 可以在模型过大时按比率和阈值裁剪低权重参数。

## 实现自己的 PerceptronInstance

这是复用框架的关键。需要实现：

| 方法 | 作用 |
| :--- | :--- |
| `getLables()` | 标签集 |
| `getFeatureFun()` | 返回 `FeatureExtractor`，定义特征模板 |
| `getAnnotateTextParser()` | 把标注语料的一行解析成训练样本 |
| `getEvaluateFunction()` | 评估函数 |
| `tansform(...)` | 输入序列到特征序列的转换 |

字符级任务（分词、NER、人名、拼音切分）直接继承 `CharBasedPerceptronInstance`，能省掉大量样板代码。

**参考实现**：`PosPerceptron.train` 是最清晰的完整范例，它扫描语料确定标签集后交给 `PerceptronTrainer`。CWS、NER、人名的训练代码结构完全一致，可以照着改。

## 现成的运行时模型类

不想训练、只想直接用模型时，这几个类提供了便捷的加载和推理入口：

### CwsPerceptron（分词）

```java
CwsPerceptron cws = Znlp.getService(CwsPerceptron.class);

List<String> words = cws.split("文本");

// 加载
static CwsPerceptron load(NlpResourceLoader loader)
static CwsPerceptron load(NlpResourceLoader loader, String modelDir)
static CwsPerceptron loadFromDir(File dir)

// 工具方法：把 BMES 标签数组还原成词
static List<String> labelsToWords(char[] text, int[] labels)
```

默认模型目录常量是 `znlp/cws-model`。

`Znlp.getService(CwsPerceptron.class)` 拿到的是单例，模型只加载一次。仓库的 `CwsPerceptronExample` 就是这么用的——它**不走流水线**，直接调 `cws.split`，适合对比感知机分词和词典分词的效果差异。

### PersonNamePerceptron（人名）

```java
PersonNamePerceptron pn = Znlp.getPersonNamePerceptron();

List<String> names = pn.findPersonName("文本");
List<String> names2 = pn.findPersonName(charArray);
```

使用 BMEO 标注，常量 `B=0 / M=1 / E=2 / O=3`。模型资源 `znlp/personname/nr-name.model`。

### PinyinSplitPerceptron（拼音切分）

```java
PinyinSplitPerceptron ps = Znlp.getService(PinyinSplitPerceptron.class);
List<String> syllables = ps.split("nihaoshijie");
```

模型资源 `znlp/pinyin/pinyin-split.model`。

### PosPerceptron / NERPerceptron

见[词性标注](pos-tagging/)和[命名实体识别](ner/)。

## 训练用的数据与工作目录

仓库约定：

- `data/work/` —— 训练工作目录，已有 `name-nr`、`ner-model`、`ner-model-dynamic`、`ner-model-new`、`pinyin-split`、`pos-model`、`pos.data` 等历史产物
- `data/dat/` —— 目录式模型（`cws-model/`、`ner-model/`）
- 外部大数据集放在项目根的**同级**目录 `znlp-data/`，其中 `data_train/{name-nr, ner, pinyin-split}` 是训练语料，**不随仓库分发**（见根目录 `data-dir-meta.txt`）

:::note[训练测试默认被排除]
Gradle 测试任务默认排除 `benchmark`、`train`、`performance` 三个 tag。跑训练相关测试：`./gradlew :znlp:test -PincludeTags=train`。
:::

## 相关

- [中文分词](segmentation/) —— 感知机配方 `SegmentRecipe.perceptron()`
- [词性标注](pos-tagging/) —— `PosPerceptron.train` 完整范例
- [性能与基准测试](../deployment/benchmarks/) —— 感知机分词与词典分词的速度对比
