---
title: FastText 文本分类
description: Facebook fastText v0.9.2 兼容的 Java 实现，支持监督分类训练、预测、评估与模型读写。
---

`fasttext.dev.znlp.FastText` 是 Facebook fastText 的 Java 移植，对齐 **v0.9.2** 的 `.bin` 格式（`FILEFORMAT_MAGIC = 793712314`、`FILEFORMAT_VERSION = 12`），可以直接读写官方训练出的模型。

:::note[不在 Znlp 门面里]
FastText 没有对应的 `Znlp` 静态方法，需要直接操作 `FastText` 类。这与 `docs/设计架构/FastText.md` 的描述一致——它被定位为一个独立的、可选的能力，而不是核心 NLP 流水线的一部分。
:::

`FastText` 本身是一个 record：`FastText(Args args, Dictionary dict, Model model)`。

## 监督分类

### 训练

```java
import fasttext.dev.znlp.FastText;
import args.fasttext.dev.znlp.Args;

Args args = new Args();
args.dim = 100;
args.epoch = 5;
args.lr = 0.05;
args.wordNgrams = 2;
args.label = "__label__";

FastText model = FastText.trainSupervised(new File("train.txt"), args);
```

训练语料每行一条样本，标签以 `args.label` 前缀开头：

```
__label__体育 姚明 出席 发布会
__label__财经 股市 收盘 上涨
```

### 预测

```java
import fasttext.dev.znlp.ScoreLabelPair;

List<String> tokens = List.of("股市", "收盘", "上涨");

// top-k 预测，带阈值
List<ScoreLabelPair> preds = model.predict(tokens, 3, 0.0f);

// 返回全部标签（k 传 -1，即 Model.kUnlimitedPredictions）
List<ScoreLabelPair> all = model.predict(tokens, Model.kUnlimitedPredictions, 0.0f);

// 已编码的词 id 版本，省去查词典
List<ScoreIdPair> byId = model.predict(3, wordsIntArrayList, 0.0f);
```

:::caution[id 版本仅支持监督模型]
`predict(int k, IntArrayList words, float threshold)` 在非 `sup` 模型上会抛异常。
:::

### 评估

```java
FastTextTestResult r = model.test(new File("test.txt"), 1, 0.0f, true);
// 或已解析的样本
FastTextTestResult r2 = model.test(samples, 1, 0.0f, true);
```

返回 P@k 和 R@k。最后一个参数 `print` 控制是否输出到控制台。

## 词向量

### 训练

```java
Args args = new Args();
args.model = ModelName.sg;     // skip-gram；也可选 cbow
args.dim = 100;
args.ws = 5;                   // 窗口大小
args.neg = 5;                  // 负采样数量
args.loss = LossName.ns;
args.minCount = 5;

FastText model = FastText.train(new File("corpus.txt"), args);
// 或从内存样本训练
FastText model2 = FastText.train(List.of(sampleLines), args);
```

`ModelName` 枚举有三个值：`cbow`、`sg`、`sup`。训练词向量时要显式设置 `args.model`，因为默认值是 `sg`。

:::caution[没有词向量查询 API]
代码中**不存在** `getWordVector` / `nearestNeighbor` 之类的方法。要取词向量只能自己访问矩阵：`model.wi()` 拿输入矩阵，`FastText.getOutput()` 拿输出矩阵，然后用 `dict` 把词映射到行号。如果你需要的是"给一个词返回它的向量"这种开箱即用能力，当前版本需要自己封装一层。
:::

## 模型读写

```java
model.saveModel(new File("model.bin"));          // Facebook 兼容 .bin，非量化
FastText loaded = FastText.loadModel(new File("model.bin"));
```

`loadModel` 支持官方 fastText 训练出的 `.bin` 文件。

:::caution[不支持 .ftz 量化模型]
`loadModel` 在 :265-267 显式拒绝量化模型。如果你手上有官方 CLI 用 `-quant` 产出的 `.ftz` 文件，需要先重新导出非量化版本。
:::

## Args 默认值

`args.fasttext.dev.znlp.Args` 的字段都是 public，直接赋值即可。默认值：

| 字段 | 默认 | 字段 | 默认 |
| :--- | :--- | :--- | :--- |
| `dim` | 100 | `epoch` | 5 |
| `lr` | 0.05 | `lrUpdateRate` | 100 |
| `minCount` | 5 | `minCountLabel` | 0 |
| `wordNgrams` | 1 | `bucket` | 2000000 |
| `minn` | 3 | `maxn` | 6 |
| `t` | 1e-4 | `ws` | 5 |
| `neg` | 5 | `loss` | `LossName.ns` |
| `model` | `ModelName.sg` | `maxVocabSize` | 500000 |
| `seed` | 0 | `thread` | CPU 核数 - 2（最少 2） |
| `label` | `"__label__"` | | |

`minn` / `maxn` 是 subword 的 n-gram 范围，设为 0 可关闭 subword 以减小模型体积和内存占用。

## 完整示例

仓库自带的 `FastTextExample` 演示了分类训练、`predict`、无限 k、`test` 的 P@1/R@1，以及 `saveModel`/`loadModel` 往返：

```bash
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.FastTextExample
```

## 相关

- 设计文档：`docs/设计架构/FastText.md`
- 模块内说明：`fasttext/README.md`
