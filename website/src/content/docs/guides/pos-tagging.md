---
title: 词性标注
description: 在分词流水线中启用词性标注，以及用 PosPerceptron 独立标注和训练自己的词性模型。
---

znlp 的词性标注由感知机实现（`PosPerceptron`），既可以作为分词流水线的一个阶段，也可以脱离流水线独立使用。

## 在分词流水线中使用

`SegmentRecipe.standard()` **默认已开启词性标注**，所以 `Znlp.segment` 返回的每个 `Token` 都带 `Nature`：

```java
import dev.znlp.Znlp;
import segment.dev.znlp.Token;

for (Token t : Znlp.segment("自然语言处理技术是人工智能的重要领域")) {
    System.out.println(t.word + "/" + t.getNature());
}
```

如果用的是 `dictionary()` 配方，需要显式打开：

```java
WordSegmenter segmenter = SegmentRecipe.dictionary().withPos().build();
```

反过来，想去掉词性标注以降低开销：

```java
WordSegmenter fast = SegmentRecipe.standard().withoutPos().build();
```

:::caution[开启 NER 后 withoutPos 无效]
`withNer()` 把词性作为特征输入，因此 NER 打开时调用 `withoutPos()` 不会真正移除词性阶段。想要"有 NER 无 POS"的组合在当前实现下不可行。
:::

## 词性标注集

`segment.dev.znlp.Nature` 枚举基于北大标注集扩展，共 48 个常量。常用部分：

| 标签 | 含义 | 标签 | 含义 |
| :--- | :--- | :--- | :--- |
| `n` | 名词 | `nr` | 人名 |
| `ns` | 地名 | `nt` | 机构团体 |
| `nz` | 其他专名 | `nx` | 非语素字 |
| `v` | 动词 | `vd` | 副动词 |
| `vn` | 名动词 | `a` | 形容词 |
| `ad` | 副形词 | `an` | 名形词 |
| `d` | 副词 | `m` | 数词 |
| `q` | 量词 | `mq` | 数量词 |
| `p` | 介词 | `c` | 连词 |
| `u` | 助词 | `e` | 叹词 |
| `w` | 标点 | `t` | 时间词 |
| `f` | 方位词 | `s` | 处所词 |
| `r` | 代词 | `xx` | 未识别 |

完整列表：`a Ag ad an b Bg c d Dg e f h i j k l m Mg n nr ns nt nx nz Ng o p q r Rg s t Tg u v vd vn Vg w y Yg z xx mq x begin end newWord`。

其中 `Ag` / `Dg` / `Mg` / `Ng` / `Rg` / `Tg` / `Vg` / `Yg` / `Bg` 是语素类标签，`begin` / `end` / `newWord` 是框架内部扩展值，正常分词结果里一般不会出现。

`Token` 提供三种读法：

```java
Nature n = token.getNature();          // 枚举，可能为 null
String s = token.getNatureString();    // null 时返回 ""，不会返回 "null"
String name = token.getNatureName();   // 名称
```

## 独立使用 PosPerceptron

已经分好词、只想补词性时，可以直接用 `pos.impl.segment.dev.znlp.PosPerceptron`，跳过整条流水线：

```java
import pos.impl.segment.dev.znlp.PosPerceptron;

PosPerceptron pos = Znlp.getService(PosPerceptron.class);

List<String> words = List.of("自然语言", "处理", "技术");
List<String> tags = pos.decode(words);   // 返回与输入等长的词性标签串
```

`getService` 拿到的是注册表中的单例，模型只加载一次。

### 加载模型

```java
static PosPerceptron loadFromClasspath(String path)
static PosPerceptron load(NlpResourceLoader loader, String path)
```

默认模型资源路径是 `znlp/pos-model/pos.model`，打包在 `znlp-resources` jar 中。

### 训练自己的模型

```java
static PerceptronTrainModel train(
    File trainFile,     // 训练语料
    File evalFile,      // 评估语料
    File workDir,       // 工作目录，存放中间产物
    int iter,           // 迭代轮数
    int threadNum       // 线程数
)
```

该方法会**自动扫描语料确定标签集**再训练，不需要手工指定标签列表。产出的 `PerceptronTrainModel` 可以 `toModel()` 得到运行时模型，或 `saveModelBinFile(File, FeatureFormat)` / `saveModelToDir(File)` 落盘。

训练相关的通用机制（特征模板、压缩、评估）见[感知机框架](perceptron/)。

:::note[训练测试默认被排除]
Gradle 测试任务默认排除 `benchmark`、`train`、`performance` 三个 tag。要跑训练相关的测试需显式指定：`./gradlew :znlp:test -PincludeTags=train`。
:::

## 相关

- 分词配方与流水线组合：[中文分词](segmentation/)
- 基于词性的实体识别：[命名实体识别](ner/)
