---
title: 资源下载与离线部署
description: 模型与词典的分发方式、资源包清单、自动下载机制的现状与已知的路径前缀缺陷。
---

znlp 的所有词典和模型都以资源形式分发，运行时通过 `NlpResourceLoader` 加载。生产部署前需要搞清楚资源从哪来、放在哪、缺失时会发生什么。

## 资源随 jar 分发

模型和词典全部打包在 **`znlp-resources`** jar 中（该模块无 Java 源码，`version = 1.0.0`），位于 `src/main/resources/znlp/` 下：

| 目录 / 文件 | 用途 |
| :--- | :--- |
| `core-dict/CoreDict.txt`（+ `.bigram`、`.hash`） | 核心词典与二元语法表 |
| `pos-model/pos.model` | 词性标注模型 |
| `ner/ner.model` | 命名实体模型（ns / nt） |
| `personname/nr-name.model` | 人名模型 |
| `cws-model/{feature.dat, parameter.bin}` | 中文分词感知机模型 |
| `pinyin/{pinyin.txt, pinyin-split.model}` | 拼音词典与切分模型 |
| `ts-dict/{s2t.txt, t2s.txt}` | 繁简转换词典 |
| `duckling.model` | Duckling 消歧模型 |
| `stopword-dict/stopwords.txt` | 停用词 |
| `custom-dict/`（6 个 txt） | 自定义词库、地名、人名、机构名等 |

主库对它是 `runtimeOnly(project(":znlp-resources"))`，**不会传递依赖**。你的项目必须显式把 `znlp-resources` 放到运行时 classpath，否则一律抛 `ResourceNotFoundException`。

:::tip[离线部署最简单的形态]
因为资源在 classpath 上，**只要两个 jar 都在，就完全不需要网络和外部数据目录**。首次运行会在 `cache-dir` 生成派生缓存，之后启动读缓存。这是最省心的离线部署方式。
:::

## 派生缓存

首次加载原始资源后，znlp 会把解析结果写入 `cache-dir`（默认 `data/cache`）：

| 缓存文件 | 来源 |
| :--- | :--- |
| `CoreDict_<hash>` | 核心词典 |
| `BiGramTable_v2_<hash>` | 二元语法表，**约 40 MB** |
| `S2T_Dict_<hash>` / `T2S_Dict_<hash>` | 繁简词典 |
| `feature-cache/` | 特征缓存 |
| `{ner, nr-name, pinyin-split, pos}_model_cache_featureSet.dat` | 各模型的特征集 |

`<hash>` 来自资源旁的 `.hash` 校验文件，**原始资源变了缓存会自动失效重建**。

生产部署建议：

- 把 `cache-dir` 指向容器内可写的持久路径，或至少给足临时空间（40 MB+）
- 首次启动明显偏慢是正常现象，健康检查的初始延迟要留够
- 也可以在构建镜像时先跑一次预热，把 `data/cache` 一起打进镜像，避免线上首次启动抖动

## 资源查找顺序

对每个**存在的** `data-dir` 依次尝试，最后回落到 classpath：

```
本地文件（dataDir 目录）→ 本地 Jar（dataDir 下的 jar 内）→ Classpath
```

因此覆盖内置资源的做法是：把同名文件放进某个 dataDir，它会优先于 jar 内版本被加载。默认 `data-dir=data,data/dat,../data/dat`，相对路径是**相对于进程工作目录**解析的——在容器或 systemd 里启动时工作目录往往不是项目根，这点容易踩坑，建议生产环境一律用绝对路径。

## 自动下载机制

`auto-download-resource=true`（默认）时，资源找不到会触发 `NlpResourceLoader.tryDownload`：按资源路径前缀判定归属，从华为云镜像下载资源 jar 到**第一个 dataDir**：

```
https://repo.huaweicloud.com/repository/maven/{group路径}/{artifactId}/{version}/{artifactId}-{version}.jar
```

`DictResources` 只登记一个坐标：`dev.znlp:znlp-resources:0.1.0`。所有词典与模型都在这个纯资源 jar 内，jar 内根目录与运行时路径前缀一致，都是 `znlp/`，因此判定只看前缀，不维护逐条资源白名单。

:::caution[自动下载仍不可用]
坐标已指向本项目，但仓库没有配置 `maven-publish`，`dev.znlp:znlp-resources` 未发布到任何仓库，下载必然 404。

**实践结论：不要依赖自动下载做部署兜底。** 请确保 `znlp-resources` jar 在 classpath 上，或把资源文件预置到 dataDir。
:::

## 资源缺失时的排查

抛出的 `ResourceNotFoundException` 消息里会**自动生成 Maven / Gradle 依赖片段**，提示你应该补哪个资源包。排查顺序：

1. `znlp-resources` 是否在**运行时** classpath 上？（`compileOnly` / 漏声明是常见原因）
2. `data-dir` 是相对路径吗？进程工作目录是否符合预期？
3. 如果是 fat jar / shaded jar 部署，资源是否被打包进去了？`NlpResourceLoader` 依赖 classpath 扫描，shade 时容易把 `znlp/` 下的资源漏掉或被合并覆盖。
4. `disabled-modules` 是否禁用了提供该资源的模块？

## 外部大数据集

仓库根目录的 `data-dir-meta.txt` 约定了一个**不随仓库分发**的外部数据集位置：项目根的**同级**目录 `znlp-data/`。

```
znlp-data/
├── data_corpus/{agnews, backoff2005, cncorpus, conll, hotel, pku, pku2014}
├── data_train/{name-nr, ner, pinyin-split}
└── newword_test_small
```

这些是训练和评测语料，**运行时不需要**。只有你要自己训练模型时才需要准备。

仓库内随附的、运行时也不是必需的数据：

| 路径 | 内容 |
| :--- | :--- |
| `data/cache/` | 派生缓存，可再生 |
| `data/dat/` | 目录式模型（`cws-model/`、`ner-model/`） |
| `data/newwords/newword_test/` | 新词发现测试语料 |
| `data/work/` | 历史训练工作目录 |

## 相关

- 配置项与查找顺序：[配置与数据目录](configuration/)
- 容器化与框架集成：[Spring Boot 集成](spring-boot/)
