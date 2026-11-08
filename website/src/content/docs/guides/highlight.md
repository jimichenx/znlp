---
title: 智能高亮
description: 搜索结果标红，支持拼音匹配与模糊匹配，可输出 HTML 或结构化片段。
---

智能高亮用于搜索结果的关键词标红。它不是简单的字符串 `indexOf`，而是支持**拼音匹配**（搜 `licai` 能高亮「理财」）和**模糊匹配**（允许中间夹干扰字符）。

## 门面入口

```java
import dev.znlp.Znlp;

// 单条，默认配置（<em> 标签，拼音 + 模糊均开启）
String html = Znlp.highlight("理财产品", "licai");
// -> <em>理财</em>产品

// 单条，自定义配置
String html2 = Znlp.highlight(text, query, config);

// 批量：query 只预处理一次，比循环调用单条版本快
List<String> results = Znlp.highlight(texts, query);
List<String> results2 = Znlp.highlight(texts, query, config);
```

:::tip[批量场景一定要用批量 API]
`highlight(List<String>, String)` 会把 query 的拼音转换和归一化只做一次，然后复用到所有文本上。在搜索结果页这种「一个 query 对几十条文本」的场景，比循环调用单条版本有明显优势。
:::

### 结构化片段输出

如果不想让 znlp 生成 HTML（比如前端要自己渲染样式、或者输出目标是 App 而非网页），用 `highlightFragments`：

```java
import highlight.dev.znlp.Fragment;

List<Fragment> fragments = Znlp.highlightFragments(text, query);
List<Fragment> fragments2 = Znlp.highlightFragments(text, query, config);

// 批量
List<List<Fragment>> all = Znlp.highlightFragments(texts, query);
List<List<Fragment>> all2 = Znlp.highlightFragments(texts, query, config);
```

`Fragment` 是一个 record：

```java
public record Fragment(String text, boolean highlighted) {}
```

自己渲染：

```java
StringBuilder sb = new StringBuilder();
for (Fragment f : fragments) {
    if (f.highlighted()) {
        sb.append("<mark>").append(escape(f.text())).append("</mark>");
    } else {
        sb.append(escape(f.text()));
    }
}
```

:::caution[HTML 版本不做转义]
`Znlp.highlight` 直接拼接标签，**不会对原文做 HTML 转义**。如果文本来自用户输入，直接用它的输出会有 XSS 风险。这种场景请用 `highlightFragments` 自己渲染并转义。
:::

## SmartHighlightConfig

`highlight.dev.znlp.SmartHighlightConfig` 是一个 record `(String highlightTag, boolean enablePinyin, boolean enableFuzzy)`，无参构造默认为 `("em", true, true)`。

### 链式 with 方法

```java
SmartHighlightConfig config = new SmartHighlightConfig()
        .withHighlightTag("mark")
        .withEnablePinyin(true)
        .withEnableFuzzy(false);
```

### Builder

```java
SmartHighlightConfig config = SmartHighlightConfig.builder()
        .highlightTag("span class=\"hl\"")
        .enablePinyin(true)
        .enableFuzzy(true)
        .build();
```

| 参数 | 默认 | 作用 |
| :--- | :--- | :--- |
| `highlightTag` | `em` | 包裹标签名，输出 `<tag>...</tag>` |
| `enablePinyin` | `true` | 允许拼音匹配 |
| `enableFuzzy` | `true` | 允许跳过干扰字符的非连续匹配 |

关闭 `enablePinyin` 和 `enableFuzzy` 后行为最接近普通关键词高亮，速度也最快。

## 匹配规则

理解这两条规则能解释大部分"为什么这里被高亮了"的疑问：

### 拼音匹配

仅当 **query 是纯 ASCII** 且 `enablePinyin = true` 时启用（内部由 `QueryContext.usePinyin` 标记）。逐字符用 `PinyinService.charPinyin` 做音节前缀对齐，**支持跨音节的部分匹配**——所以 `licai` 能对上「理财」，`li` 也能匹配「理」。

如果 query 里含中文，走的是另一条路。

### 字符集合匹配

query **不是纯 ASCII** 时，采用字符集合匹配：文本中出现的、属于 query 的字符**全部**会被高亮（`matchText`）。

:::caution[中文 query 是"字集合"而非"子串"匹配]
这一点很容易误解。`highlight("张三和李四", "张三")` 会把「张」和「三」都高亮，而不是只高亮连续的「张三」。如果你需要严格的子串匹配语义，当前实现不满足，需要自己在 `highlightFragments` 结果上做二次处理，或者关闭 fuzzy 后自行判断连续性。
:::

### 模糊匹配

`enableFuzzy = true` 时允许跳过干扰字符做非连续匹配；`false` 时一旦断开就重置匹配状态，只保留连续命中。

### 相邻合并

高亮位置用 `BitSet` 记录，输出时会**合并相邻的高亮位**。所以「理财」两个字会输出成一个 `<em>理财</em>`，而不是 `<em>理</em><em>财</em>`。

## SmartHighlighter 服务

脱离门面时可以直接取服务：

```java
import highlight.dev.znlp.SmartHighlighter;

SmartHighlighter highlighter = Znlp.getService(SmartHighlighter.class);

highlighter.highlight(text, query, config);
highlighter.highlight(texts, query, config);
highlighter.highlightFragments(text, query, config);
highlighter.highlightFragments(texts, query, config);
```

`SmartHighlighter` 的构造器注入 `PinyinService`——这说明**高亮功能依赖拼音模块**。如果通过 `disabled-modules` 禁用了 `PinyinModule`，高亮的拼音匹配能力会受影响。

## 相关

- 拼音转换细节：[拼音](pinyin/)
- 模块内示例：`highlight/README.md`（含 `licai` 高亮「理财」等用例，与代码行为一致）
