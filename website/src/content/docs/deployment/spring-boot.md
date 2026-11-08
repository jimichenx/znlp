---
title: Spring Boot 集成
description: 把 znlp 接入 Spring Boot 的正确姿势：单例持有分词器、资源打包、容器内存与启动预热。
---

znlp 没有专门的 Spring Boot starter，但接入很直接。关键是把重量级对象做成单例，以及确保资源在 fat jar 里能被找到。

## 依赖

先按[安装](../getting-started/installation/)把两个 jar 引入。Spring Boot 项目通常打成 fat jar，所以要注意资源打包问题（见下文）。

```kotlin
dependencies {
    implementation(files("libs/znlp-0.1.0.jar"))
    runtimeOnly(files("libs/znlp-resources-1.0.0.jar"))
}
```

## 把分词器做成 Bean

:::caution[绝对不要在请求路径里 build()]
`SegmentRecipe.build()` 会加载词典和模型，代价极高。`WordSegmenter` 的实现 `PipelineSegmenter` 是**线程安全**的，必须在启动时构建一次并作为单例复用。每个请求都 build 一次是最常见的性能事故。
:::

```java
@Configuration
public class NlpConfig {

    @Bean
    public WordSegmenter wordSegmenter(@Value("${app.nlp.domain-terms:}") List<String> terms) {
        MemUserDictionary dict = new MemUserDictionary();
        if (!terms.isEmpty()) {
            dict.addWords(terms);          // 批量加载，不要循环 addWord
        }

        return SegmentRecipe.standard()
                .withNer()                 // 需要地名/机构时开启（会强制打开 POS）
                .withUserDictionary(dict)
                .build();
    }
}
```

服务层注入使用：

```java
@Service
public class TokenizeService {

    private final WordSegmenter segmenter;

    public TokenizeService(WordSegmenter segmenter) {
        this.segmenter = segmenter;
    }

    public List<String> tokenize(String text) {
        return segmenter.segmentToStrings(text);
    }
}
```

其他能力同理做成 Bean：`PosPerceptron`、`NERPerceptron`、`PinyinService`、`SmartHighlighter`、`TransformService` 都可以用 `Znlp.getService(Xxx.class)` 取到注册表里的单例，直接暴露成 Bean 即可，不需要自己管理生命周期。

## 配置传递

znlp 的配置来自 JVM 系统属性和 classpath 上的 `znlp.properties`，**不是** Spring 的 `Environment`。想让 `application.yml` 里的配置生效，需要在 Spring 启动早期把它桥接成系统属性：

```java
@Component
public class ZnlpPropertyBridge {

    public ZnlpPropertyBridge(@Value("${znlp.data-dir:}") String dataDir,
                               @Value("${znlp.cache-dir:}") String cacheDir) {
        if (!dataDir.isEmpty()) System.setProperty("znlp.data-dir", dataDir);
        if (!cacheDir.isEmpty()) System.setProperty("znlp.cache-dir", cacheDir);
    }
}
```

:::caution[必须在 ZnlpSystem 初始化之前设置]
`SystemDefaultConfig` 在**类静态块**里完成配置加载。一旦任何代码触发了 `ZnlpSystem` 或 `Znlp` 的类加载，配置就固化了，之后再设系统属性无效。所以这个桥接 Bean 必须先于所有 NLP Bean 初始化——用 `@Order`、`@DependsOn`，或者干脆在 `main()` 里 `SpringApplication.run` 之前设置。
:::

另一种更省事的做法是直接用 JVM 参数：

```bash
java -Dznlp.data-dir=/opt/znlp/data \
     -Dznlp.cache-dir=/var/cache/znlp \
     -jar app.jar
```

## 容器化注意事项

### 内存

`ZnlpSystem`、`ServiceRegistry`、`ModuleLoader` 都**没有 close/shutdown 方法**，模型和词典缓存会常驻堆内存直到进程退出。规划容器内存时要把这部分算进去，并且不要指望通过销毁 Bean 释放。

派生缓存中的二元语法表约 **40 MB**，加上核心词典和各模型，堆需要留足余量。仓库自带的示例和基准任务都把 `maxHeapSize` 设为 2 GB，可作为参考量级。

### 首次启动慢

首次运行要把原始资源解析成缓存文件写入 `cache-dir`。K8s 的 `initialDelaySeconds` / `startupProbe` 要留够时间，否则会在预热完成前被判定失败并反复重启。

两种解法：

1. **构建镜像时预热**：在 Dockerfile 里跑一次分词，把生成的 `data/cache` 一起打进镜像。线上启动直接读缓存。
2. **应用内预热**：启动时主动触发一次分词，配合 `ApplicationReadyEvent`。

```java
@Component
public class NlpWarmup {

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        Znlp.segment("预热");   // 触发词典、模型与缓存加载
    }
}
```

### 工作目录

默认 `data-dir=data,data/dat,../data/dat` 是**相对路径**，相对进程工作目录解析。容器里的工作目录通常不是你以为的那个，建议一律用绝对路径显式配置。

### 只读文件系统

如果容器以只读文件系统运行，`cache-dir` 必须挂到可写的 volume，否则缓存写入会失败。

## fat jar 里的资源

Spring Boot 的可执行 jar 用嵌套 jar 结构，znlp 的 `NlpResourceLoader` 走 classpath 扫描（`ClasspathNlpResourceFactory`），正常情况下能读到 `BOOT-INF/lib/znlp-resources-*.jar` 里的资源。

如果用了 `maven-shade-plugin` 或 Gradle 的 shadow 插件把资源合并进单一 jar，要注意 `znlp/` 目录下的同名文件可能被覆盖或丢失。出问题时先确认资源是否真的在最终产物里：

```bash
unzip -l build/libs/app.jar | grep znlp/
```

## Web 层示例

```java
@RestController
@RequestMapping("/api/nlp")
public class NlpController {

    private final WordSegmenter segmenter;

    public NlpController(WordSegmenter segmenter) {
        this.segmenter = segmenter;
    }

    @PostMapping("/segment")
    public List<TokenView> segment(@RequestBody SegmentRequest req) {
        return segmenter.segment(req.text()).stream()
                .map(t -> new TokenView(t.word, t.getNatureString(), t.getOffset()))
                .toList();
    }
}
```

:::note[长文本用流式接口]
`List<Token> segment(String)` 建议输入不超过 10000 字符。接口层应当限制请求体大小，或者对长文本改用 `segment(text, Consumer<Token>)` 边解析边写出响应。
:::

## 日志

主库对 `slf4j-api` 和 `log4j-api` 都是 `compileOnly`，内部有自适应层。Spring Boot 默认带 Logback + SLF4J，znlp 会自动接入，**无需额外配置**。把 `dev.znlp` 的日志级别调到 `DEBUG` 可以看到资源查找和下载尝试的详细信息，排查 `ResourceNotFoundException` 时很有用。

## 相关

- [资源下载与离线部署](resources/) —— 资源分发细节
- [配置与数据目录](configuration/) —— 配置加载顺序
- [性能与基准测试](benchmarks/) —— 选型与调优数据
