# 新词打分器模块 - 快速导航

## 📌 快速开始（零配置）

```java
// 最简单的方式 - 完全自动，无需设置任何参数
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();

// 使用示例
INewWordFindEngine.Config config = new INewWordFindEngine.Config(3, 12, 10);
INewWordFindEngine engine = INewWordFindEngine.create(config, false, null);
engine.

setScorer(scorer);

NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(corpusDir, engine).build();
List<NewWord> results = tool.discover();
```

---

## 🎯 为什么推荐 AdaptiveNewWordScorer？

| 优势          | 说明               |
|-------------|------------------|
| ✅ **零配置**   | 无需手动设置任何阈值参数     |
| ✅ **智能过滤**  | 自动学习最佳阈值，基于统计分布  |
| ✅ **完整性检查** | 自动过滤词组碎片（如"交申请"） |
| ✅ **适应性强**  | 自动适应不同语料特点       |

---

## 📚 文档导航

| 文档                                  | 用途            | 推荐度   |
|-------------------------------------|---------------|-------|
| **AdaptiveScorerUsageExample.java** | 完整的使用示例（4个示例） | ⭐⭐⭐⭐⭐ |
| **ADAPTIVE_SCORER_README.md**       | 详细的使用指南和原理说明  | ⭐⭐⭐⭐⭐ |
| **CHANGELOG_ADAPTIVE.md**           | 变更日志和技术细节     | ⭐⭐⭐   |
| **README.md**                       | 原有文档（详细API说明） | ⭐⭐⭐   |

---

## 🚀 快速使用

### 1. 完全自适应（推荐）

```java
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
```

### 2. 不同策略

```java
// 严格模式（高精确度）
AdaptiveNewWordScorer.createAdaptive(FilterStrategy.STRICT);

// 宽松模式（高召回率）
AdaptiveNewWordScorer.

createAdaptive(FilterStrategy.LOOSE);
```

### 3. 半自适应（可微调）

```java
// 保留前30%的词
AdaptiveNewWordScorer.createSemiAdaptive(0.7f,FilterStrategy.BALANCED);
```

---

**推荐阅读顺序**：

1. 本文档（快速了解）
2. `AdaptiveScorerUsageExample.java`（看示例）
3. `ADAPTIVE_SCORER_README.md`（深入理解）
