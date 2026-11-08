# 智能高亮 (Smart Highlight)

`dev.znlp.highlight` 包提供了强大的智能文本高亮功能，专为搜索和检索场景设计。

## 核心能力

与其他简单的关键词替换不同，znlp 的智能高亮具备以下特性：

1.  **拼音匹配**: 支持输入拼音高亮中文文本。
    *   例如：输入 `licai` 可以高亮 "理财"。
2.  **模糊匹配 (Fuzzy Match)**: 支持非连续字符匹配，适应用户输入的不完整或简写查询。
    *   例如：输入 `理财` 可以高亮 "理A财"（跳过中间的干扰字符）。
3.  **智能分词边界**: 能够识别并合并相邻的高亮区域，避免产生破碎的标签。
4.  **结构化输出**: 不仅支持返回带 HTML 标签的字符串，还支持返回结构化的 `Fragment` 列表，方便前端自定义渲染。
5.  **高性能**: 针对批量处理进行了优化，复用查询预处理结果，并采用零拷贝匹配算法。

## API 使用指南

推荐通过 `dev.znlp.Znlp` 门面类使用高亮功能。

### 1. 基础文本高亮 (HTML 输出)

最简单的用法，直接返回包含 `<em>` 标签的字符串。

```java
String text = "这里有理财产品";
String query = "licai"; // 用户输入拼音

// 输出: 这里有<em>理财</em>产品
String html = Znlp.highlight(text, query);
```

### 2. 高亮片段

要自己渲染命中和未命中的段落时，用 `highlightFragments`。它和 `highlight` 使用同一套匹配，返回的是片段而不是 HTML。

```java
String text = "这里有理财产品";
String query = "理财";

List<Fragment> fragments = Znlp.highlightFragments(text, query);

for (Fragment f : fragments) {
    if (f.highlighted()) {
        // 渲染高亮样式，例如红色字体
        renderRed(f.text()); 
    } else {
        // 渲染普通文本
        renderNormal(f.text());
    }
}
// 结果片段: 
// 1. [text="这里有", highlighted=false]
// 2. [text="理财", highlighted=true]
// 3. [text="产品", highlighted=false]
```

### 3. 自定义配置

通过 `SmartHighlightConfig` 可以自定义高亮标签、开启/关闭拼音或模糊匹配。

```java
// 创建配置：使用 <b> 标签，关闭模糊匹配
SmartHighlightConfig config = SmartHighlightConfig.builder()
    .highlightTag("b")       // 默认是 "em"
    .enableFuzzy(false)      // 默认是 true
    .enablePinyin(true)      // 默认是 true
    .build();

String html = Znlp.highlight("理A财", "理财", config);
// 因为关闭了模糊匹配，"理A财" 不会被高亮
```

### 4. 批量处理

对于列表数据，使用批量接口可获得更好性能。

```java
List<String> texts = Arrays.asList("理财产品", "基金定投");
String query = "li";

// 批量高亮
List<String> results = Znlp.highlight(texts, query);
```

## 核心类说明

*   **`SmartHighlighter`**: 核心逻辑实现类。通常不需要直接实例化，通过 `Znlp` 调用即可。
*   **`SmartHighlightConfig`**: 配置类（不可变），用于定制高亮行为。
*   **`Fragment`**: 结构化数据类，包含 `text` 和 `highlighted` 状态。
