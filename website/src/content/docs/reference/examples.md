---
title: 示例程序
description: znlp-examples 模块的四个可运行示例，覆盖感知机分词、默认流水线、Duckling 全维度与 FastText 分类。
---

`znlp-examples` 模块有 4 个示例类，都在 `dev.znlp.examples` 包下，可以用 Gradle 直接运行。

## 运行方式

```bash
# 默认运行 CwsPerceptronExample
./gradlew :znlp-examples:run

# 指定其他示例
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DefaultSegmenterExample
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DucklingExample
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.FastTextExample
```

机制说明（`znlp-examples/build.gradle.kts`）：模块应用了 `application` 插件，`mainClass` 取自 Gradle 属性 `-PmainClass=...`，**缺省值是 `examples.dev.znlp.CwsPerceptronExample`**。`run` 任务的 `maxHeapSize` 是 **2 GB**。

## CwsPerceptronExample

**默认示例。** 独立运行 CWS 感知机分词，**不走流水线**：

```java
CwsPerceptron cws = Znlp.getService(CwsPerceptron.class);
List<String> words = cws.split(text);
```

适合用来对比感知机分词与词典分词的效果差异。因为它绕过了 `PipelineSegmenter`，所以没有词性、人名、NER，也没有二元语法的影响——是观察纯统计模型行为的最干净入口。

相关文档：[感知机框架](../guides/perceptron/)

## DefaultSegmenterExample

演示三种分词形态的对比：

| 内容 | 代码 |
| :--- | :--- |
| 默认分词（词典 + 二元 + 人名 + POS） | `Znlp.segment(text)` |
| 标准配方追加 NER | `SegmentRecipe.standard().withNer().build()` |
| Duckling 结构化解析 | `Znlp.parse(text)` |

这是最贴近实际使用的示例，能直观看到 `Token` 的词性输出、NER 命中情况，以及 Duckling 与分词是**两条独立的路**。

相关文档：[中文分词](../guides/segmentation/)、[命名实体识别](../guides/ner/)、[Duckling](../guides/duckling/)

## DucklingExample

覆盖 Duckling 的全部已注册维度：数字、序数、金额、时间、百分比、距离、温度、年龄、电话、时长。

:::tip[用固定参考时钟保证可复现]
示例把参考时钟固定为 **2019-11-11**，所以「明天」「下周三」这类相对时间的解析结果是确定的。你自己写测试时也应该这么做——不传 `clock` 会用当前时刻，导致断言随运行日期漂移。
:::

```java
LocalDateTime clock = LocalDateTime.of(2019, 11, 11, 0, 0);
for (Dimension d : Znlp.parse(text, clock)) {
    System.out.println(d.getKind() + " " + d.getText()
            + " [" + d.getStart() + "," + d.getEnd() + "]");
}
```

注意 `getStart()` / `getEnd()` 是**闭区间**。另外 `DimensionKind` 里虽然有 `Email` 和 `Url`，但没有对应 grammar，示例中也不会出现这两类。

相关文档：[Duckling 结构化解析](../guides/duckling/)

## FastTextExample

演示文本分类的完整往返：

| 步骤 | API |
| :--- | :--- |
| 监督训练 | `FastText.trainSupervised(file, args)` |
| top-k 预测 | `model.predict(tokens, k, threshold)` |
| 全部标签预测 | `model.predict(tokens, Model.kUnlimitedPredictions, threshold)` |
| 评估 P@1 / R@1 | `model.test(file, 1, threshold, print)` |
| 模型落盘与加载 | `model.saveModel(file)` / `FastText.loadModel(file)` |

`saveModel` 产出的是 Facebook fastText v0.9.2 兼容的 `.bin`（非量化），可以被官方 CLI 读取；反过来官方训练的 `.bin` 也能被 `loadModel` 读入，但 `.ftz` 量化模型不支持。

相关文档：[FastText 文本分类](../guides/fasttext/)

## 自己写示例

在你的项目里复现这些示例，最小依赖是主库加资源 jar：

```java
import dev.znlp.Znlp;
import segment.dev.znlp.Token;

public class Demo {
    public static void main(String[] args) {
        for (Token t : Znlp.segment("中国科学院计算技术研究所位于北京市")) {
            System.out.println(t.word + "/" + t.getNature());
        }
    }
}
```

:::note[首次运行较慢]
第一次执行会把核心词典、二元语法表等解析成派生缓存写入 `cache-dir`（默认 `data/cache`，其中二元语法表约 40 MB）。之后启动直接读缓存，速度正常。这不是示例的问题，也不要在健康检查里把首次启动当作异常。
:::

如果抛 `ResourceNotFoundException`，先确认 `znlp-resources` 在**运行时** classpath 上——主库对它是 `runtimeOnly` 依赖，不会传递给你的项目。异常消息里会自动生成应该补的 Maven / Gradle 依赖片段。

## 相关

- [安装](../getting-started/installation/) —— 把两个 jar 放进 classpath
- [快速开始](../getting-started/quick-start/) —— 最小可运行代码
- [API 参考](api/) —— 全部公开签名
