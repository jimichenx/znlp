---
title: 配置项参考
description: 全部配置项、系统属性、默认值、资源清单与模块名的速查表。
---

## 配置项总表

| 配置项 | 系统属性 | 类型 | 打包默认值 |
| :--- | :--- | :--- | :--- |
| `data-dir` | `znlp.data-dir` | 逗号分隔多路径 | `data,data/dat,../data/dat` |
| `cache-dir` | `znlp.cache-dir` | 路径 | `data/cache` |
| `auto-download-resource` | `znlp.auto-download-resource` | boolean | `true` |
| `disabled-modules` | `znlp.disabled-modules` | 逗号分隔模块名 | 无 |

设置方式（三选一）：

```bash
# 1. JVM 系统属性（优先级最高）
java -Dznlp.data-dir=/opt/znlp/data -jar app.jar
```

```properties
# 2. classpath 上的 znlp.properties（会覆盖 jar 内自带的默认配置）
data-dir=/opt/znlp/data
cache-dir=/var/cache/znlp
auto-download-resource=false
```

```java
// 3. Builder —— 注意：不影响 Znlp 门面，见下方说明
ZnlpSystemConfig config = ZnlpSystemConfig.builder()
        .dataDir("/opt/znlp/data")
        .cacheDir("/var/cache/znlp")
        .autoDownloadResource(false)
        .buildWithDefaults();
```

:::caution[关于优先级和 Builder 的两个常见误解]
**误解一**：`CONFIG_README.md` 写「JVM 属性 > 配置文件 > 代码 Builder」。代码事实是加载链只有两步——classpath 读 `znlp.properties`，再用 JVM 属性覆盖。Builder 构造的是**独立对象**，不在这条链上。

**误解二**：以为 Builder 建的配置能改变 `Znlp.segment` 的行为。不能。门面走 `SystemDefaultConfig.getInstance()`，Builder 的配置除非传给一个自建的 `ZnlpSystem`，否则不起作用——而 `DefaultZnlpSystemBuilder` 是包私有的，包外建不出来。

**实践结论**：要改配置，用 JVM 属性或替换 classpath 上的 `znlp.properties`。
:::

## 默认值来源

jar 内自带的 `znlp/src/main/resources/znlp.properties`：

```properties
data-dir=data,data/dat,../data/dat
cache-dir=data/cache
auto-download-resource=true
```

:::note[与 CONFIG_README 的差异]
`CONFIG_README.md` 声称默认值是 `data` 和 `cache`，与实际打包内容不符。以代码为准。`CONFIG_README.md` 还遗漏了 `dataDirs` 多目录和 `disabled-modules` 两项，并引用了并不存在的 `docs/DEVELOPMENT.md` 和 `docs/DEPLOYMENT.md`。
:::

## data-dir 的语义

`dataDirs` 是 `List<String>`，按声明顺序查找资源。对**每个存在的**目录依次挂 `FileNlpResourceFactory`（目录文件）和 `JarNlpResourceFactory`（目录下 jar 内查找），全部未命中后回落到 `ClasspathNlpResourceFactory`：

```
dataDir[0] 目录文件 → dataDir[0] 内 jar → dataDir[1] 目录文件 → ... → classpath
```

`getDataDir()` 返回列表第一个（也是自动下载的写入目标），`getDataDirs()` 返回全部。

**相对路径相对进程工作目录解析。** `data,data/dat,../data/dat` 这组默认值是为「在项目根或子模块目录下用 Gradle 跑」设计的——从 `znlp-examples/` 启动时 `../data/dat` 才能命中项目根的 `data/dat`。容器或 systemd 环境下工作目录不同，务必改成绝对路径。

## cache-dir 的语义

存放**可再生**的派生缓存。原始资源变化时（通过 `.hash` 校验）缓存自动失效重建，所以删掉整个目录是安全的，代价只是下次启动变慢。

| 缓存文件 | 大小量级 |
| :--- | :--- |
| `BiGramTable_v2_<hash>` | **约 40 MB** |
| `CoreDict_<hash>` | 较大 |
| `S2T_Dict_<hash>` / `T2S_Dict_<hash>` | 中等 |
| `feature-cache/` | 中等 |
| `{ner, nr-name, pinyin-split, pos}_model_cache_featureSet.dat` | 小 |

只读文件系统的容器必须把 `cache-dir` 挂到可写 volume。

## disabled-modules 的取值

值是**模块名**而非类名。主库 `META-INF/znlp-modules.idx` 注册的模块：

| 模块类 | 模块名 | 优先级 | 禁用后果 |
| :--- | :--- | :--- | :--- |
| `CoreModule` | `core` | 10 | 资源加载器缺失，**基本不可用** |
| `SegmentModule` | — | — | 分词、词性、NER、人名全部失效 |
| `PinyinModule` | — | — | 拼音失效，**智能高亮的拼音匹配也受影响** |
| `HighlightModule` | — | — | 智能高亮失效 |
| `TransModule` | — | — | 繁简转换失效 |

数值越小越早加载。`disabled-modules=highlight,converter` 这样的写法按逗号分隔匹配模块名。

:::note[Duckling 无法禁用]
`DucklingParser` 用自带的静态单例，不经过模块系统，因此 `disabled-modules` 对它无效。
:::

## 资源清单

全部位于 `znlp-resources` jar 的 `src/main/resources/znlp/` 下。

### 随 jar 分发的资源

| 路径 | 用途 |
| :--- | :--- |
| `core-dict/CoreDict.txt` | 核心词典 |
| `core-dict/CoreDict.bigram.txt` | 二元语法表 |
| `pos-model/pos.model` | 词性标注 |
| `ner/ner.model` | 命名实体（ns / nt） |
| `personname/nr-name.model` | 人名 |
| `cws-model/feature.dat` | 分词感知机特征 |
| `cws-model/parameter.bin` | 分词感知机参数 |
| `pinyin/pinyin.txt` | 拼音词典 |
| `pinyin/pinyin-split.model` | 拼音音节切分模型 |
| `ts-dict/s2t.txt` | 简转繁 |
| `ts-dict/t2s.txt` | 繁转简 |
| `duckling.model` | Duckling 消歧 |
| `stopword-dict/stopwords.txt` | 停用词 |
| `custom-dict/CustomDictionary.txt` | 自定义词库 |
| `custom-dict/上海地名.txt` | 上海地名 |
| `custom-dict/人名词典.txt` | 人名 |
| `custom-dict/全国地名大全.txt` | 全国地名 |
| `custom-dict/机构名词典.txt` | 机构名 |
| `custom-dict/现代汉语补充词库.txt` | 补充词库 |

各 Factory 请求资源时使用的路径**都带 `znlp/` 前缀**（如 `znlp/core-dict/CoreDict.txt`）。

### 自动下载的 Maven 坐标

全库资源只有一个坐标（`DictResources`）：

```
dev.znlp:znlp-resources:0.1.0
```

所有词典与模型都在这一个纯资源 jar 内，jar 内根目录与运行时路径前缀一致，都是 `znlp/`。因此 `tryDownload` 与错误提示只按前缀 `znlp/` 判定资源归属，不再维护逐条资源的白名单。

下载 URL 模板：

```
https://repo.huaweicloud.com/repository/maven/{group路径}/{artifactId}/{version}/{artifactId}-{version}.jar
```

:::caution[自动下载仍不可用]
坐标已指向本项目，但仓库当前**没有配置 `maven-publish`**，`dev.znlp:znlp-resources` 没有发布到任何仓库，下载必然 404。

**部署时不要依赖自动下载兜底。** 把 `auto-download-resource` 设为 `false` 反而更诚实——资源缺失会直接快速失败，而不是先尝试一次注定失败的网络请求。
:::

资源最终找不到时抛 `ResourceNotFoundException`，异常消息里会自动生成对应的 Maven / Gradle 依赖片段。

## 构建与测试相关配置

| 项 | 值 |
| :--- | :--- |
| Java | `sourceCompatibility = targetCompatibility = 17` |
| Gradle wrapper | 9.2.1 |
| `group` | `dev.znlp` |
| 主库 `version` / `archivesName` | `0.1.0` / `znlp` |
| 资源库 `version` / `archivesName` | `1.0.0` / `znlp-resources` |
| 测试框架 | JUnit Jupiter 5.10.1 |
| 测试默认排除 tag | `benchmark`、`train`、`performance` |
| 覆盖排除 | `-PincludeTags=train` |
| 测试 / 示例 / 基准堆上限 | 2 GB |
| Maven publishing | **未配置**（无 `maven-publish` / `publishing` / `signing`） |

主库依赖：`runtimeOnly(project(":znlp-resources"))`；`compileOnly` 的 `org.jetbrains:annotations:26.0.2-1`、`org.slf4j:slf4j-api:2.0.16`、`org.apache.logging.log4j:log4j-api:2.24.3`。测试运行时另有 log4j-core 2.25.3 + log4j-slf4j2-impl 2.24.3。**运行时零强制第三方依赖。**

## 外部数据集（不随仓库分发）

根目录 `data-dir-meta.txt` 约定放在项目根的**同级**目录：

```
znlp-data/
├── data_corpus/{agnews, backoff2005, cncorpus, conll, hotel, pku, pku2014}
├── data_train/{name-nr, ner, pinyin-split}
└── newword_test_small
```

仅训练和评测需要，运行时不需要。

## 相关

- 加载顺序与模块系统详解：[配置与数据目录](../deployment/configuration/)
- 资源分发与离线部署：[资源下载与离线部署](../deployment/resources/)
