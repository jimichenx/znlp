---
title: 配置与数据目录
description: znlp 的配置加载顺序、全部配置项、多数据目录机制、资源查找顺序与模块开关。
---

## 配置加载

配置类是 `config.dev.znlp.ZnlpSystemConfig`，默认实例由 `SystemDefaultConfig` 在类静态块中初始化，加载链是：

1. 从 classpath 读取 **`znlp.properties`**
2. 用 **JVM 系统属性**覆盖其中的值

系统属性名格式为 `znlp.{kebab-case}`，例如 `-Dznlp.data-dir=/path/to/data`。

主库自带一份默认配置，随 jar 发布在 `znlp/src/main/resources/znlp.properties`：

```properties
data-dir=data,data/dat,../data/dat
cache-dir=data/cache
auto-download-resource=true
```

:::caution[实际默认值与 CONFIG_README 不一致]
仓库根目录的 `CONFIG_README.md` 写的默认值是 `data` 和 `cache`，但**打包进 jar 的 properties 实际是** `data,data/dat,../data/dat` 和 `data/cache`。以代码为准。
:::

## 配置项

| 配置项 | 系统属性 | 类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `data-dir` | `znlp.data-dir` | 逗号分隔的多路径 | `data,data/dat,../data/dat` | 数据目录，按顺序查找 |
| `cache-dir` | `znlp.cache-dir` | 路径 | `data/cache` | 派生缓存目录 |
| `auto-download-resource` | `znlp.auto-download-resource` | boolean | `true` | 资源缺失时是否尝试自动下载 |
| `disabled-modules` | `znlp.disabled-modules` | 逗号分隔的模块名 | 无 | 禁用指定模块 |

`data-dir` 有特殊处理：字段名是 `dataDirs`（复数，`List<String>`），配置时写单数的 `data-dir`，逗号分隔会被拆成列表。`getDataDir()` 返回列表的第一个，`getDataDirs()` 返回全部。

## 用 Builder 构造配置

```java
import config.dev.znlp.ZnlpSystemConfig;

ZnlpSystemConfig config = ZnlpSystemConfig.builder()
        .dataDir("/opt/znlp/data")
        .dataDirs(List.of("/opt/data", "/mnt/shared"))   // 或逗号分隔字符串
        .cacheDir("/var/cache/znlp")
        .autoDownloadResource(false)
        .disabledModules("highlight,converter")
        .build();               // 未设置的项为 null
        // .buildWithDefaults() // 未设置的项从 SystemDefaultConfig 继承
```

`build()` 和 `buildWithDefaults()` 的区别很重要：前者未设置项保持 `null`，后者从系统默认配置继承。

:::caution[Builder 与 SystemDefaultConfig 是两条线]
`CONFIG_README.md` 描述的优先级是「JVM 属性 > 配置文件 > 代码 Builder」，但代码事实是：Builder 构造的是一个**独立**的配置对象，它不会去覆盖 `SystemDefaultConfig` 的加载链。`Znlp` 门面和 `ZnlpSystem.getInstance()` 用的都是 `SystemDefaultConfig.getInstance()`，所以**通过 Builder 建的配置不会影响门面行为**，除非你把它传给一个自建的 `ZnlpSystem`——而这条路目前也走不通，见下。
:::

## 目前无法从包外自定义 ZnlpSystem

`ZnlpSystem.builder()` 返回的 `DefaultZnlpSystemBuilder` 是**包私有类**，它的 `build()` 方法也是**包私有**，而且这个 builder **没有任何 `withXxx` 配置方法**——只有两个构造器（默认取 `SystemDefaultConfig.getInstance()`，或接收一个 `ZnlpSystemConfig`）。

结果是：**包外代码无法通过 `ZnlpSystem.builder()...build()` 构建自定义系统实例**。实际可用的定制手段只有：

- JVM 系统属性（`-Dznlp.*`）
- classpath 上放自己的 `znlp.properties` 覆盖 jar 内的默认值
- `SystemDefaultConfig.reset()`（主要供测试用，会重新加载配置）

`ZnlpSystem` 本身是一个 record：`public record ZnlpSystem(ServiceRegistry registry)`。

## 生命周期

:::note[没有 close / shutdown]
`ZnlpSystem`、`ServiceRegistry`、`ModuleLoader` 中都**没有** close/shutdown/destroy 方法。系统随 JVM 生命周期存在，没有显式关闭 API。在容器环境里这意味着模型和词典缓存会一直占用堆内存直到进程退出——规划容器内存时要把它算进去。
:::

`ZnlpSystem.getInstance()` 是进程级单例（静态 Holder 惰性初始化）。构建流程是：new `ServiceRegistry` → 注册 `ZnlpSystemConfig` 单例 → `ModuleLoader.loadModules(registry, config)`。

## 资源查找顺序

`NlpResourceLoader` 为每个**存在的** dataDir 依次挂载两个 factory，最后加 classpath factory：

```
本地文件（dataDir 目录）
  ↓ 未命中
本地 Jar（dataDir 下的 jar 内查找）
  ↓ 未命中
Classpath（znlp-resources jar）
```

即 **本地文件 > 本地 Jar > Classpath**。资源加载后按 `path:charset` 缓存，同一资源不会重复读取。

这个顺序给了你一个覆盖内置资源的干净手段：把同名文件放到 dataDir 里，就会优先于 jar 内的版本被加载。

## 模块系统

`ModuleLoader` 扫描 classpath 中所有 `META-INF/znlp-modules.idx`，按 `SystemModule.getPriority()`（**数值越小越早加载**）依次加载，并根据 `disabledModules` 跳过被禁用的模块。

主库 `znlp-modules.idx` 的内容：

| 模块类 | 优先级 | 注册的服务 |
| :--- | :--- | :--- |
| `CoreModule`（名 `core`） | 10 | `NlpResourceLoader` |
| `SegmentModule` | — | `CoreDictionarySplitAlgorithm`、`CoreDictionary`、`BiGramTableDictionary`、`PersonNamePerceptron`、`CwsPerceptron`、`PosPerceptron`、`NERPerceptron`、`CwsSplitAlgorithm`、`ResourceUserDictionary`、`AtomSplitAlgorithm` |
| `PinyinModule` | — | `PinyinDict`、`PinyinSplitPerceptron`、`PinyinService` |
| `HighlightModule` | — | `SmartHighlighter` |
| `TransModule` | — | `TransformService` |

:::note[Duckling 不走 ServiceRegistry]
`DucklingParser` 自带静态单例（`DucklingParser.getInstance()`），不通过模块系统注册，因此**无法用 `disabled-modules` 关掉它**，也不受模块加载顺序影响。
:::

`disabled-modules` 的值是模块名（如 `core`），不是类名。禁用 `PinyinModule` 会连带影响依赖拼音的智能高亮。

## ServiceRegistry

`service.base.dev.znlp.ServiceRegistry` 实现 `ServiceLocator`，是基于接口的轻量依赖注入容器。注册 API：

```java
registerFactory(Class, ServiceFactory)
registerFactory(Class, ServiceFactory, boolean singleton)
registerFactory(Class, Class<? extends ServiceFactory>, boolean)
registerSingleton(Class, T)
registerClass(Class)
registerSingletonClass(Class)
registerPrototypeClass(Class)
registerClass(Class, boolean)

<T> T getService(Class)      // 支持继承/接口智能查找，找不到抛 ServiceNotFoundException
```

支持父子容器：`new ServiceRegistry(ServiceLocator parent)`，子容器查不到时会向上委托。

取服务：

```java
PosPerceptron pos = Znlp.getService(PosPerceptron.class);
```

`Znlp.getService` 委托给进程级 `ZnlpSystem`，返回的是注册表里的单例，可以放心持有。

## 相关

- 资源分发与离线部署：[资源下载与离线部署](resources/)
- 全部配置项速查：[配置项参考](../reference/configuration/)
- 模块与注册表设计：[架构设计](../reference/architecture/)
