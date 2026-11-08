---
title: 架构设计
description: znlp 的分层结构、模块加载机制、服务注册表与分词流水线的阶段划分。
---

## 分层概览

```
┌─────────────────────────────────────────────────┐
│  Znlp（静态门面）                              │  应用代码主要接触这一层
├─────────────────────────────────────────────────┤
│  ZnlpSystem（record，持有 ServiceRegistry）    │  进程级单例
├─────────────────────────────────────────────────┤
│  ServiceRegistry（基于接口的轻量 DI）            │  支持父子容器
├─────────────────────────────────────────────────┤
│  ModuleLoader + SystemModule 实现                │  扫描 znlp-modules.idx
├─────────────────────────────────────────────────┤
│  能力模块：Segment / Pinyin / Highlight / Trans  │
│  独立组件：Duckling（静态单例）/ FastText        │
├─────────────────────────────────────────────────┤
│  NlpResourceLoader + 三个 ResourceFactory        │  本地文件 > 本地 Jar > Classpath
└─────────────────────────────────────────────────┘
```

主库共 399 个 Java 源文件，包根 `dev.znlp`。另有 `dev.znlp.starspace` 包（StarSpace 移植），不在核心能力范围内。

## 模块加载

`ModuleLoader` 扫描 classpath 中**所有** `META-INF/znlp-modules.idx`（因此第三方 jar 也能贡献模块），按 `SystemModule.getPriority()` 升序加载（**数值越小越早**），并根据配置的 `disabledModules` 跳过指定模块。

主库 `znlp/src/main/resources/META-INF/znlp-modules.idx`：

| 模块 | 优先级 | 职责 |
| :--- | :--- | :--- |
| `CoreModule`（名 `core`） | 10 | 注册 `NlpResourceLoader`，是所有其他模块的前置 |
| `SegmentModule` | — | 分词、词性、NER、人名相关的 10 个服务 |
| `PinyinModule` | — | `PinyinDict`、`PinyinSplitPerceptron`、`PinyinService` |
| `HighlightModule` | — | `SmartHighlighter`（构造时注入 `PinyinService`） |
| `TransModule` | — | `TransformService` |

`CoreModule` 优先级为 10 且最先加载是必要的：其余模块都要通过 `NlpResourceLoader` 取资源。

:::note[Duckling 和 FastText 不在模块系统内]
`DucklingParser` 用自己的静态单例（`getInstance()`），`FastText` 是纯静态工厂 + record。两者都不注册到 `ServiceRegistry`，因此既不能通过 `disabled-modules` 关闭，也不参与模块加载顺序。这是有意的设计——它们是可选的、独立的能力。
:::

## ServiceRegistry

`service.base.dev.znlp.ServiceRegistry` 实现 `ServiceLocator`，是一个基于接口的轻量 DI 容器，不是 Spring 那种通用框架。

特点：

- **按接口注册、按接口查找**：`getService(Class)` 支持继承和接口的智能查找
- **三种生命周期**：`registerFactory`（工厂，可选单例）、`registerSingleton`（实例单例）、`registerClass` / `registerSingletonClass` / `registerPrototypeClass`（按类注册）
- **父子容器**：`new ServiceRegistry(ServiceLocator parent)`，子容器查不到时向上委托
- **找不到抛 `ServiceNotFoundException`**，不返回 null

`SegmentModule` 注册的 10 个服务：`CoreDictionarySplitAlgorithm`、`CoreDictionary`（factory 单例）、`BiGramTableDictionary`、`PersonNamePerceptron`、`CwsPerceptron`、`PosPerceptron`、`NERPerceptron`、`CwsSplitAlgorithm`、`ResourceUserDictionary`、`AtomSplitAlgorithm`。

## 资源加载

`NlpResourceLoader` 为每个**存在的** dataDir 依次挂两个 factory，最后加 classpath factory，形成三级回落：

```
FileNlpResourceFactory(dataDir[i])   目录下的散文件
JarNlpResourceFactory(dataDir[i])    目录下 jar 内的文件
...（对每个 dataDir 重复）
ClasspathNlpResourceFactory          znlp-resources jar
```

即 **本地文件 > 本地 Jar > Classpath**。资源按 `path:charset` 缓存，同一资源不重复读取。

派生缓存（如二元语法表约 40 MB）写入 `cache-dir`，通过资源旁的 `.hash` 文件做失效判断。

`auto-download-resource=true` 时，资源缺失会尝试从华为云 Maven 镜像下载——但**当前因路径前缀不匹配而实际不会触发**，详见[配置项参考](configuration/)。

## 分词流水线

`SegmentRecipe.build()` 产出 `PipelineSegmenter`，按**固定阶段**执行。理解阶段顺序是理解各种 `with*` 语义的前提：

```
1. 字符归一化（addNormalizer）
2. 原子识别（AtomSplitAlgorithm）—— 数字、字母、标点等不可再分的片段
3. 候选铺设：
     - 核心词典（CoreDictionary）
     - 规则（withRules，抬升词频）
     - 额外候选源（addCandidate）
4. 选路：
     - 词典配方 → 二元语法 Viterbi（PathKind.VITERBI）
     - 感知机配方 → CWS 感知机线性解码（PathKind.LINEAR），不跑二元语法
5. 约束过滤（addConstraint）
6. 选路后处理（顺序很重要）：
     a. 纠错词典（withCorrections）—— 优先级高于用户词典
     b. 用户词典（withUserDictionary）—— 合并相邻词，两端须落在已有词边界
7. 标注：
     - 人名（PersonNamePerceptron）
     - 词性（PosPerceptron）
     - NER（NerProcessor，合并 B/M/E 跨度后写回 Nature）
     - 自定义标注器（addAnnotator）
8. 索引子词（indexTerms）
```

三种「词进入结果」的路径语义（来自 `SegmentRecipe` 类 Javadoc）：

| 机制 | 时机 | 是否参与 Viterbi |
| :--- | :--- | :--- |
| 核心词典 / `addCandidate` | 选路前铺候选 | 是 |
| `withRules` | 选路前铺候选并抬词频 | 是（**仅词典配方有效**） |
| `withUserDictionary` | **选路后**合并 | 否 |
| `withCorrections` | **选路后**，优先级高于用户词典 | 否 |

用户词典不进词图这一点解释了两个常见现象：它能把 `["长江","大桥"]` 粘成 `["长江大桥"]`，但无法切开一个已被核心词典认定的词；而且它的权重是固定的 `WEIGHT = 1000`，不参与概率竞争。

## 感知机框架

五个能力共用 `dev.znlp.perceptron`：

```
PerceptronInstance（抽象点：标签集、特征提取、语料解析、评估）
        ↓  CharBasedPerceptronInstance（字符级任务基类）
PerceptronTrainer.train(...)
        ↓
PerceptronTrainModel ──compress()──→ saveModelToDir() / saveModelBinFile()
        ↓ toModel()
PerceptronModel ──getDecoder()──→ Viterbi / OptimizedViterbi / Quick Decoder
```

复用这套框架训练新任务，只需实现 `PerceptronInstance`（`getLables`、`getFeatureFun`、`getAnnotateTextParser`、`getEvaluateFunction`、`tansform`），其余交给 `PerceptronTrainer`。`PosPerceptron.train` 是最清晰的完整范例。

特征模板在 `perceptron/feature/` 下（`CharDSLFeatureExtractor`、`FeatureTemplate` 等），用 DSL 描述字符级特征窗口。

## 设计文档

仓库 `docs/设计架构/` 下有更深入的原始设计文档：

| 文档 | 内容 |
| :--- | :--- |
| `分词.md` | 分词配方与阶段划分的设计 |
| `Duckling.md` | 规则图解析 + 消歧的设计 |
| `FastText.md` | fastText 移植的设计与格式兼容 |
| `基准.md` | JMH 基准的方法论与命令 |
| `测试.md` | 测试策略与 tag 约定 |
| `README.md` | 设计文档索引 |

模块内另有若干 README，与代码抽查一致，可作为补充：`highlight/README.md`、`newwords/README.md`、`newwords/scorer/README*.md`、`fasttext/README.md`、`base/resources/README.md`、`starspace/README_*.md`。

:::note[仓库根 README 的几处过时描述]
本站内容以源码为准。已核对出的偏差：`Znlp.pinyinString` 实际不带声调；NER 只支持 `ns`/`nt` 两类；Duckling 不支持 Email/Url；`ZnlpSystem.builder()` 返回包私有类型因而包外不可用；FastText 无词向量查询 API；项目未配置 Maven 发布。完整清单见[常见问题](faq/)。
:::

## 相关

- [API 参考](api/) —— 全部公开签名
- [配置与数据目录](../deployment/configuration/) —— 模块与注册表的配置面
- [中文分词](../guides/segmentation/) —— 流水线各阶段的用法
