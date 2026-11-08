# znlp

znlp 是一个模块化的 Java 中文 NLP 工具包，提供分词、词性标注、拼音、繁简转换、新词发现和智能高亮等能力。

## 核心功能

- **分词**：默认词典 + 二元语法 + 人名 + 词性标注。感知机分词、NER、规则、用户词典用 `SegmentRecipe` 组合
- **词性标注**：感知机 POS，已装入默认分词器
- **命名实体**：感知机 NER（地名 ns、机构 nt），按需安装插件
- **结构化解析**：Duckling，数字 / 金额 / 时间 / 电话等
- **拼音**：中文转拼音及拼音切分
- **繁简转换**：简体 / 繁体互转
- **新词发现**：互信息与信息熵
- **智能高亮**：支持拼音与模糊匹配
- **感知机框架**：词性、NER、人名、拼音切分、中文分词均可复用

## 快速开始

常用能力都走 `Znlp` 门面，不必先拿 `ZnlpSystem`。

### 中文分词

默认分词器是词典 + 二元语法 + 原子识别 + 人名 + 词性标注：

```java
import dev.znlp.Znlp;
import segment.dev.znlp.Token;

List<Token> tokens = Znlp.segment("自然语言处理技术是人工智能的重要领域");
for (Token token : tokens) {
    System.out.println(token.word + "/" + token.getNature());
}

List<String> words = Znlp.segmentToStrings("南京市长江大桥");
```

需要感知机分词、NER 或用户词典时，用配方：

```java
import segment.dev.znlp.WordSegmenter;
import userdict.impl.segment.dev.znlp.MemUserDictionary;
import recipe.segment.dev.znlp.SegmentRecipe;

WordSegmenter cws = SegmentRecipe.perceptron().build();

WordSegmenter ner = SegmentRecipe.standard().withNer().build();

MemUserDictionary dict = new MemUserDictionary();
dict.addWord("长江大桥");
WordSegmenter custom = SegmentRecipe.standard().withUserDictionary(dict).build();
```

### 结构化解析（Duckling）

独立于分词，不进入默认流水线：

```java
import support.duckling.dev.znlp.Dimension;
import java.time.LocalDateTime;

List<Dimension> dims = Znlp.parse("明天下午3点见面，电话13800138000");
List<Dimension> withClock = Znlp.parse("明天", LocalDateTime.of(2026, 8, 26, 12, 0));
```

更完整的场景见 `znlp-examples` 里的 `DucklingExample`：

```bash
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DucklingExample
```

### FastText 文本分类

```bash
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.FastTextExample
```

### 拼音

```java
Znlp.pinyinString("你好znlp");      // ni hao znlp
Znlp.pinyinHeadString("复旦大学");     // f d d x
```

### 繁简转换

```java
Znlp.s2t("汉字");
Znlp.t2s("漢字");
```

## 架构

- **Znlp**：静态门面（分词、拼音、Duckling `parse` 等）
- **ZnlpSystem**：配置与组件生命周期
- **ServiceRegistry**：基于接口的轻量依赖注入
- **分词配方**：`SegmentRecipe` 按固定阶段组合词典、感知机、NER、用户词典，设计见 [docs/设计架构/分词.md](docs/设计架构/分词.md)
- **Duckling**：规则图解析 + 消歧，设计见 [docs/设计架构/Duckling.md](docs/设计架构/Duckling.md)
- **FastText**：分类与词向量，设计见 [docs/设计架构/FastText.md](docs/设计架构/FastText.md)

## 配置

优先级从高到低：

1. JVM 系统属性：`-Dznlp.data-dir=/path/to/data`
2. classpath 配置文件：`znlp.properties`
3. 代码：`ZnlpSystem.builder()`

| 配置项 | 说明 | 默认值 |
| :--- | :--- | :--- |
| `data-dir` | 数据目录 | `data` |
| `cache-dir` | 缓存目录 | `cache` |
| `auto-download-resource` | 缺失资源是否自动下载 | `true` |

## 分词性能基准

持续优化分词请跑独立模块（JMH，不进 `./gradlew :znlp:test`）：

```bash
./gradlew :znlp-benchmarks:jmh
```

说明见 [docs/设计架构/基准.md](docs/设计架构/基准.md)。
