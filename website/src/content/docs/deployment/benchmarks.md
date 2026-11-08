---
title: 性能与基准测试
description: znlp-benchmarks 模块的 JMH 基准运行方式、现有基准项与配置。
---

分词性能相关的基准测试放在独立模块 `znlp-benchmarks`，用 JMH 实现，**不会被 `./gradlew :znlp:test` 触发**。

## 运行

```bash
# 默认跑 dev.znlp.benchmark.segment 包下的全部基准
./gradlew :znlp-benchmarks:jmh

# 只跑某一个基准（正则匹配方法名）
./gradlew :znlp-benchmarks:jmh -PjmhArgs='SegmentBenchmark.defaultSegment$'
```

`jmh` 任务固定附加以下参数：

```
-rf json -rff build/results/jmh.json -foe true
```

即结果以 JSON 输出到 `znlp-benchmarks/build/results/jmh.json`，`-foe true` 表示某个基准失败时继续跑完其余的。任务堆上限 2 GB。

## 环境

| 项 | 值 |
| :--- | :--- |
| JMH 版本 | 1.37（`jmh-core` + `jmh-generator-annprocess`） |
| sourceSet | 独立的 `jmh` sourceSet，不在 `main` / `test` 里 |
| 运行时依赖 | `:znlp` + `:znlp-resources` + log4j |

## SegmentBenchmark

`segment.benchmark.dev.znlp.SegmentBenchmark` 是目前唯一的基准类。

### 测量配置

| 配置 | 值 |
| :--- | :--- |
| `@BenchmarkMode` | `AverageTime` |
| `@OutputTimeUnit` | `MICROSECONDS` |
| Warmup | 2 轮 × 500 ms |
| Measurement | 5 轮 × 500 ms |
| Fork | 1 |
| `@Param` | `{"short", "typical", "long"}` 三档文本长度 |

结果单位是 **μs/op**（越小越好）。

### 6 个基准项

| 方法 | 测什么 |
| :--- | :--- |
| `defaultSegment` | 默认分词器（词典 + 二元 + 人名 + POS） |
| `bigramOnlySegment` | 只有词典和二元语法，不含人名和词性 |
| `cwsPipelineSegment` | 感知机分词走完整流水线 |
| `cwsPerceptronSplit` | 感知机直接 `split`，不走流水线 |
| `defaultSegmentToStrings` | 默认分词 + `segmentToStrings`（只取词面） |
| `defaultSegmentConsumer` | 默认分词 + `Consumer<Token>` 流式版本 |

后两项专门用于量化**输出方式的开销**：`segmentToStrings` 要构造 `List<String>`，流式版本不构造中间集合。对比 `defaultSegment` 和 `defaultSegmentConsumer` 可以看出收集成列表的成本。

`bigramOnlySegment` 与 `defaultSegment` 的差值就是人名识别 + 词性标注的开销；`cwsPerceptronSplit` 与 `cwsPipelineSegment` 的差值是流水线其余阶段的开销。这几组对比是选型的主要依据。

### 语料

语料由 `SegmentCorpus.text(size)` **内置生成**，不读外部文件。所以基准结果是自包含、可复现的，不依赖 `data/` 或外部数据集。

:::note[内置语料不等于真实分布]
生成的语料用于横向对比不同配方是合适的，但**不能用来推断你业务文本上的绝对性能**。上线前建议用自己的真实语料另跑一轮。
:::

## 仓库里没有已记录的基准数字

`docs/设计架构/基准.md` 只写了方法论和命令，并明确指出：

- JSON 结果**不提交**到仓库
- **不要把基准数字当作 PR 的硬门禁**

仓库中 grep 不到任何 `us/op` 或 `ops/s` 形式的结果表。因此本站不提供性能数字——需要数据请自己跑：

```bash
./gradlew :znlp-benchmarks:jmh
cat znlp-benchmarks/build/results/jmh.json
```

:::tip[跑基准前先预热缓存]
首次运行会生成 `data/cache/` 下的派生缓存（二元语法表约 40 MB）。JMH 的 warmup 阶段会覆盖这部分，但如果你看到第一轮 fork 明显偏慢，先单独跑一次任意分词把缓存建好再测。
:::

## 性能优化方向

基于基准项的设计，常见的取舍是：

| 需求 | 选择 |
| :--- | :--- |
| 最高吞吐，不需要词性 | `SegmentRecipe.dictionary()` |
| 通用，需要词性 | `SegmentRecipe.standard()`（默认） |
| 需要地名/机构 | `standard().withNer()`，注意会强制带上 POS |
| 统计模型分词 | `SegmentRecipe.perceptron()` |
| 大文本 | `segment(char[], Consumer<Token>)`，避免构造 Token 列表 |
| 长驻服务 | 启动时 `build()` 一次，单例复用（`PipelineSegmenter` 线程安全） |

`List<Token> segment(String)` 的建议输入上限是 **10000 字符**，超过请用流式接口。

## 相关

- 设计文档：`docs/设计架构/基准.md`
- 配方对比：[中文分词](../guides/segmentation/)
- 容器内存与启动预热：[Spring Boot 集成](spring-boot/)
