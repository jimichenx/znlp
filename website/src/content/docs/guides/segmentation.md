---
title: 中文分词
description: WordSegmenter 接口、SegmentRecipe 的三种配方与全部组合阶段、用户词典、纠错词典与线程安全约束。
---

分词是 znlp 的核心。对外只有两个类型需要关心：`WordSegmenter`（分词器）和 `SegmentRecipe`（配方）。

## WordSegmenter 接口

`segment.dev.znlp.WordSegmenter` 有 1 个抽象方法和 3 个 default 方法：

| 签名 | 说明 |
| :--- | :--- |
| `void segment(char[] text, Consumer<Token> consumer)` | **核心方法**，流式输出，不构造中间列表 |
| `void segment(String text, Consumer<Token> consumer)` | default，转 char[] 后委托上面的方法 |
| `List<Token> segment(String text)` | default，收集为**不可变列表**；空串返回空列表 |
| `List<String> segmentToStrings(String text)` | default，只取词面 |

:::caution[长文本请用流式版本]
`List<Token> segment(String)` 的 Javadoc 建议输入 **不超过 10000 字符**。更长的文本应使用 `Consumer<Token>` 版本边解析边消费，避免一次性构造巨大的 Token 列表。
:::

`WordSegmenter` 的实现类是 `PipelineSegmenter`，**线程安全，可多线程复用**。不要在每次请求时重新 `build()`——构建过程会加载词典和模型，代价很高。正确做法是启动时构建一次，作为单例持有。

## SegmentRecipe：三种基础配方

`recipe.segment.dev.znlp.SegmentRecipe` 是**不可变**的，所有 `with*` 方法返回新实例。内部有 `PathKind { VITERBI, LINEAR }` 枚举区分选路策略。

每个工厂都有带 `ServiceLocator` 参数的重载，用于从非默认的注册表取组件：

```java
static SegmentRecipe dictionary()
static SegmentRecipe dictionary(ServiceLocator locator)

static SegmentRecipe standard()
static SegmentRecipe standard(ServiceLocator locator)

static SegmentRecipe perceptron()
static SegmentRecipe perceptron(ServiceLocator locator)
```

| 配方 | 组成 | 选路 | 何时用 |
| :--- | :--- | :--- | :--- |
| `dictionary()` | 核心词典 + 原子识别 + 二元语法 | Viterbi | 只要切词，不需要词性和人名，追求吞吐 |
| `standard()` | `dictionary()` + 人名识别 + 词性标注 | Viterbi | 通用默认，等价于 `Znlp.segment` |
| `perceptron()` | CWS 感知机判定边界 + 原子补洞 | Linear | 统计模型分词，**不跑二元语法** |

`Znlp.recipe()` 是 `SegmentRecipe.standard()` 的别名，方便从门面直接链式组装。

## 全部组合方法

所有方法都返回 `SegmentRecipe`：

| 方法 | 作用 |
| :--- | :--- |
| `withPersonNames()` / `withoutPersonNames()` | 开关人名识别（`PersonNamePerceptron`） |
| `withPos()` / `withoutPos()` | 开关词性标注 |
| `withNer()` | 开启命名实体识别，**会自动打开 POS** |
| `withUserDictionary(UserDictionary...)` | 追加用户词典（可变参数） |
| `withUserDictionary(List<UserDictionary>)` | 追加多个用户词典 |
| `withCorrections(CorrectionDictionary)` | 追加纠错词典 |
| `withRules(RuleManager)` | 追加规则管理器 |
| `indexTerms()` | 在收集阶段为 Token 附加索引子词 |
| `addCandidate(CandidateSource)` | 追加候选词来源 |
| `addConstraint(SpanConstraint)` | 追加跨度约束 |
| `addAnnotator(TokenAnnotator)` | 追加 Token 标注器 |
| `addNormalizer(CharNormalize)` | 追加字符归一化器 |
| `build()` | 构建 `WordSegmenter`（`PipelineSegmenter`） |

:::note[withoutPos 在开启 NER 时无效]
`withNer()` 依赖词性信息作为特征，因此一旦 NER 打开，后续调用 `withoutPos()` 不会真正移除 POS。如果你既想要 NER 又想去掉词性开销，这在当前实现下做不到。
:::

## 词进入结果的三条路

理解这三种机制的差别，能避免绝大多数"配置了却不生效"的问题。`SegmentRecipe` 的类 Javadoc 对此有明确说明：

1. **核心词典 / 候选来源** —— 在词图上铺候选，参与 Viterbi 选路。这是主路径。
2. **`withRules(RuleManager)`** —— 同样铺候选，但会抬升词频，**只对词典配方（`dictionary()` / `standard()`）有效**，感知机配方走线性路径，不经过二元语法选路。
3. **`withUserDictionary(...)`** —— 在**选路完成之后**做合并，要求词的两端都落在已有词的边界上。用户词典**不进词图、不参与 Viterbi**。

第 3 点是关键：用户词典能把 `["长江", "大桥"]` 粘成 `["长江大桥"]`，但如果某个字符串已经被核心词典当成一个不可分的词，用户词典无法把它切开。

`withCorrections(CorrectionDictionary)` 也在选路之后处理，且**优先级高于用户词典**。

## 用户词典

`userdict.impl.segment.dev.znlp.UserDictionary` 接口只有一个方法：

```java
StringIntDATMap getTrie();   // 无词时返回 null
```

### MemUserDictionary

内存词典，**单线程写、多线程读**。

```java
MemUserDictionary dict = new MemUserDictionary();
// 或从已有的 TreeMap 构造（会拷贝副本，不持有原 map）
MemUserDictionary dict2 = new MemUserDictionary(treeMap);

dict.addWord("长江大桥");
dict.addWords(List.of("感知机", "双数组"));
dict.replaceAll(Stream.of("全量", "替换"));
dict.removeWord("感知机");
dict.clear();
dict.rebuild();          // 手动触发重建
```

每次写操作都会重建并重新发布整棵双数组 Trie，内部使用固定权重 `WEIGHT = 1000`。词会同时以**原词**和 `CharNormUtils.convert` 的**规范化形式**入库，所以繁体或全角输入也能命中。

:::caution[不要在运行时高频写 MemUserDictionary]
写时重建整棵 Trie 的代价与词典规模成正比。适合启动时一次性批量加载（用 `addWords` 或 `replaceAll`，而不是循环 `addWord`）。需要运行时热更新的场景请用 `FileUserDictionary`。
:::

### FileUserDictionary

文件词典，靠 `refresh()` 热更新：

```java
FileUserDictionary dict = new FileUserDictionary(new File("custom-dict.txt"));
FileUserDictionary dict2 = new FileUserDictionary(file, StandardCharsets.UTF_8);

dict.refresh();          // 重新读取文件并发布新 Trie
```

`refresh()` 有多个重载，可以在文件未变化时跳过重建。

### ResourceUserDictionary

访问内置词库的双数组 Trie，无需自己加载文件。内置词库路径常量：

| 资源 | 路径 |
| :--- | :--- |
| 自定义词典 | `znlp/custom-dict/CustomDictionary.txt` |
| 上海地名 | `znlp/custom-dict/上海地名.txt` |
| 人名词典 | `znlp/custom-dict/人名词典.txt` |
| 全国地名大全 | `znlp/custom-dict/全国地名大全.txt` |
| 机构名词典 | `znlp/custom-dict/机构名词典.txt` |
| 现代汉语补充词库 | `znlp/custom-dict/现代汉语补充词库.txt` |

对应 getter 为 `getCustomDictDat()` 等。

## Token

`segment.dev.znlp.Token` 是 `public final class`，实现 `Serializable` 和 `WordAndNature`。

| 成员 | 类型 | 说明 |
| :--- | :--- | :--- |
| `word` | `public final String` | 词面，**公开字段，直接访问** |
| `offset` | `public int` | 在原文中的起始偏移，**公开字段** |
| `getNature()` / `setNature(Nature)` | `Nature` | 词性枚举 |
| `getNatureString()` | `String` | 词性字符串，null 时返回 `""` |
| `getNatureName()` | `String` | 词性名称 |
| `getOffset()` / `setOffset(int)` | `int` | 偏移的 getter/setter |
| `getSubword()` / `setSubword(List<Token>)` | `List<Token>` | 子词（配合 `indexTerms()`） |
| `hasSubword()` | `boolean` | 是否有子词 |
| `getWord()` | `String` | `word` 的 getter |
| `getCustomFlag()` / `setCustomFlag(String)` | `String` | 自定义标记位 |
| `length()` | `int` | 词面长度 |
| `getPosInc()` / `setPosInc(int)` | `int` | 位置增量，setter 链式返回 `this` |

`toString()` 输出 `word/nature` 形式，调试时很方便。

构造器有三个：`Token(String)`、`Token(String, Nature)`、`Token(String, Nature, int offset)`。

## Nature 词性集

`segment.dev.znlp.Nature` 是枚举，基于北大标注集并做了扩展，共 48 个常量：

```
a  Ag  ad  an  b  Bg  c  d  Dg  e  f  h  i  j  k  l  m  Mg
n  nr  ns  nt  nx  nz  Ng  o  p  q  r  Rg  s  t  Tg  u  v
vd  vn  Vg  w  y  Yg  z  xx  mq  x  begin  end  newWord
```

其中 `nr` 是人名、`ns` 是地名、`nt` 是机构团体。`begin` / `end` / `newWord` 是框架内部使用的扩展值，`xx` 表示未识别。

## 完整示例：一个可复用的分词服务

```java
import segment.dev.znlp.Token;
import segment.dev.znlp.WordSegmenter;
import userdict.impl.segment.dev.znlp.MemUserDictionary;
import recipe.segment.dev.znlp.SegmentRecipe;

public class SegmentService {

    // 启动时构建一次，线程安全，可全局复用
    private final WordSegmenter segmenter;

    public SegmentService(List<String> domainTerms) {
        MemUserDictionary dict = new MemUserDictionary();
        dict.addWords(domainTerms);          // 批量加载，避免循环 addWord

        this.segmenter = SegmentRecipe.standard()
                .withNer()                   // 追加地名 / 机构识别
                .withUserDictionary(dict)
                .indexTerms()                // 需要子词时开启
                .build();
    }

    public List<Token> tokenize(String text) {
        return segmenter.segment(text);
    }

    // 大文本走流式，不构造中间列表
    public void tokenizeLarge(String text, Consumer<Token> sink) {
        segmenter.segment(text, sink);
    }
}
```

## 相关

- 词性标注细节：[词性标注](pos-tagging/)
- NER 支持的实体类型：[命名实体识别](ner/)
- 感知机分词与自训模型：[感知机框架](perceptron/)
- 分词性能数据与基准运行方式：[性能与基准测试](../deployment/benchmarks/)
