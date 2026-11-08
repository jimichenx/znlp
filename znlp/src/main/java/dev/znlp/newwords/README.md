# 新词发现模块 (NewWords Discovery)

## 概述

新词发现模块是一个基于统计方法的中文新词自动发现工具。该模块通过两轮扫描算法，基于互信息（MI）、信息熵（Entropy）等统计指标，从大规模文本语料中自动识别出可能是新词的高频词组。

## 核心功能

- **批量处理**：自动扫描目录下的所有文本文件，支持递归遍历
- **两轮扫描算法**：第一轮统计基本频次，第二轮计算统计指标
- **智能过滤**：支持核心词典过滤，排除已知词汇
- **灵活打分**：提供多种打分器实现，支持自定义评分策略
- **结果导出**：支持将新词结果导出为CSV/TSV格式文件
- **内存优化**：采用紧凑数据结构，适合处理大规模语料

## 模块结构

```
newwords/
├── NewWordDiscoveryTool.java      # 工具类，提供批量处理接口
├── engine/                        # 引擎模块
│   ├── INewWordFindEngine.java    # 引擎接口
│   ├── NewWordFindEngine.java     # 标准引擎实现
│   └── NewWordFindEngineOptimized.java  # 优化版引擎实现
├── scorer/                        # 打分器模块
│   ├── NewWordScorer.java         # 打分器接口
│   ├── NewWordScorerFactory.java  # 打分器工厂
│   ├── SimpleNewWordScorer.java   # 简单打分器
│   └── AdaptiveNewWordScorer.java # 自适应打分器（推荐）
├── counter/                       # 高频统计模块
│   ├── IFrequentItemsCounter.java # 统计器接口
│   ├── FrequentItemsCounter.java  # 标准实现
│   └── FrequentItemsCounterOptimized.java  # 优化实现
└── entity/                        # 实体类
    ├── NewWord.java               # 新词结果实体
    ├── WordInfo.java              # 词信息统计实体
    ├── WordCount.java             # 词频统计
    └── IntCount.java              # 整数计数器
```

## 核心算法

### 两轮扫描算法

#### 第一轮扫描：基本频次统计

1. **NGram提取**：从文本中提取长度为 `minGroup` 到 `maxGroup` 的所有字符组合
2. **字符过滤**：过滤掉标点、ASCII字符、空白字符等无效字符
3. **频次统计**：使用 `IFrequentItemsCounter` 统计每个词组的出现频次
4. **候选筛选**：保留出现频次 >= `minOccurCount` 的词组作为候选

#### 第二轮扫描：统计指标计算

1. **邻字统计**：统计每个候选词的左右邻字及其频次
2. **互信息计算**：计算候选词的互信息（MI）和平均互信息（AvgMI）
   - 互信息衡量字符之间的内聚程度
   - 公式：`MI = log2(P(AB) / (P(A) * P(B)))`
3. **信息熵计算**：计算左右熵（Left Entropy, Right Entropy）
   - 熵值衡量词边界的自由度，熵值越高表示边界越清晰
   - 公式：`Entropy = -Σ P(x) * log(P(x))`
4. **IDF计算**：计算逆文档频率（Inverse Document Frequency）
   - 衡量词在文档中的分布情况

### 打分和过滤

通过 `NewWordScorer` 接口实现：
- **过滤**：根据阈值过滤低质量候选词
- **评分**：计算综合得分，用于排序

## 设计思路

### 1. 模块化设计

- **引擎层**：负责核心算法执行，提供 `INewWordFindEngine` 接口
- **打分层**：独立的打分器模块，支持策略模式，易于扩展
- **统计层**：高频统计器，支持内存受限场景下的流式处理
- **工具层**：`NewWordDiscoveryTool` 提供便捷的批量处理接口

### 2. 内存优化

#### WordInfo 内存优化

`WordInfo` 类采用紧凑数据结构，显著降低内存占用：

- **左右邻字统计**：使用开放寻址哈希表（`int[]`）替代 `HashMap<Character, IntCount>`
  - 每对 `(char, count)` 仅占用 8 字节（vs HashMap 的 ~40 字节）
  - 100 个邻字仅需 ~1KB（vs HashMap 的 ~5KB）

- **文档ID集合**：使用开放寻址哈希表替代 `BitSet`
  - 避免 `BitSet` 在大 docId 时的内存爆炸问题
  - 达到最大容量限制后自动转为计数模式

#### 引擎优化

`NewWordFindEngineOptimized` 相比标准版本：
- 避免循环中重复调用 `toCharArray()`
- 使用 `char[]` 数组索引直接操作，减少 `String.substring()` 调用
- 优化循环结构，减少不必要的字符检查

### 3. 可扩展性

#### 打分器扩展

通过 `NewWordScorer` 接口，可以轻松实现自定义打分策略：

```java
public class CustomScorer implements NewWordScorer {
    @Override
    public float score(NewWord newWord) {
        // 自定义评分逻辑
    }
    
    @Override
    public boolean accept(WordInfo wordInfo) {
        // 自定义过滤逻辑
    }
}
```

#### 统计器扩展

通过 `IFrequentItemsCounter` 接口，可以替换不同的统计实现：
- 标准实现：适合中小规模语料
- 优化实现：适合大规模语料，内存占用更低

### 4. 配置灵活性

#### 引擎配置

通过 `INewWordFindEngine.Config` 类配置核心参数：

- `minGroup`：最小词组长度（推荐：2-3）
- `maxGroup`：最大词组长度（推荐：8-12）
- `minOccurCount`：最小出现频次（根据语料规模调整）

#### 打分器配置

提供多种预设打分器：
- **自适应打分器**（推荐）：零配置，自动学习最佳阈值
- **简单打分器**：基于互信息和熵的基础评分
- **手动模式**：完全自定义阈值

## 使用示例

### 基本使用

```java
// 1. 创建引擎配置
INewWordFindEngine.Config config = new INewWordFindEngine.Config(2, 8, 5);

// 2. 创建引擎（使用优化版）
INewWordFindEngine engine = INewWordFindEngine.create(
    config, 
    false,  // 不排除核心词典
    null    // 核心词典（可选）
);

// 3. 设置打分器（使用自适应打分器，推荐）
engine.setScorer(NewWordScorerFactory.createAdaptive());

// 4. 使用工具类批量处理
File dataDir = new File("/path/to/text/files");
NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(dataDir, engine)
    .charset(StandardCharsets.UTF_8)
    .verbose(true)
    .build();

// 5. 执行新词发现
ArrayList<NewWord> newWords = tool.discover();

// 6. 保存结果
tool.saveToFile(new File("result.csv"));
```

### 手动控制两轮扫描

```java
// 创建引擎
INewWordFindEngine engine = INewWordFindEngine.create();

// 设置打分器
engine.setScorer(NewWordScorerFactory.createAdaptive());

// 第一轮扫描
engine.firstScan("文档1内容");
engine.firstScan("文档2内容");
engine.finishFirst();

// 第二轮扫描
engine.secondScan("文档1内容");
engine.secondScan("文档2内容");
engine.endSecond();

// 获取结果
ArrayList<NewWord> results = engine.result();
```

### 自定义打分器

```java
// 创建自定义打分器
NewWordScorer customScorer = NewWordScorerFactory.createAdaptiveManual(
    2.0f,    // 最小互信息
    1.0f,    // 最小熵
    2,       // 最小左右邻字种类数
    5,       // 最小词频
    2        // 最小文档频率
);

engine.setScorer(customScorer);
```

## 统计指标说明

### 互信息 (MI)

- **定义**：衡量字符之间的内聚程度
- **计算**：`MI = log2(P(AB) / (P(A) * P(B)))`
- **意义**：值越大，表示字符组合越紧密，越可能是词

### 平均互信息 (AvgMI)

- **定义**：互信息除以词长度
- **意义**：消除长度影响，便于比较不同长度的词

### 信息熵 (Entropy)

- **定义**：衡量词边界的自由度
- **计算**：`Entropy = -Σ P(x) * log(P(x))`
- **意义**：值越大，表示边界越清晰，越可能是独立词

### 左右熵 (LE, RE)

- **左熵**：词左侧邻字的熵值
- **右熵**：词右侧邻字的熵值
- **最小熵**：取左右熵的最小值，用于判断词边界

### IDF (逆文档频率)

- **定义**：衡量词在文档中的分布情况
- **计算**：`IDF = log10(总文档数 / 包含该词的文档数)`
- **意义**：值越大，表示词分布越分散，越可能是通用词

### 左右邻字种类数 (Accessor Variety)

- **定义**：词左右两侧出现的不同字符种类数
- **意义**：种类数越多，表示词边界越清晰

## 性能优化建议

1. **参数调优**：
   - 小语料（<1MB）：`minOccurCount=3-5`
   - 中等语料（1-10MB）：`minOccurCount=5-10`
   - 大语料（>10MB）：`minOccurCount=10-20`

2. **使用优化版引擎**：
   - 优先使用 `NewWordFindEngineOptimized`
   - 通过 `INewWordFindEngine.create()` 自动创建优化版

3. **内存管理**：
   - 对于超大规模语料，考虑分批处理
   - 使用 `IFrequentItemsCounterOptimized` 降低内存占用

4. **打分器选择**：
   - 推荐使用自适应打分器，无需手动调参
   - 如需精确控制，使用手动模式

## 注意事项

1. **字符编码**：确保文本文件编码与配置一致（默认UTF-8）
2. **核心词典**：如需排除已知词汇，需提供 `CoreDictionary` 实例
3. **打分器设置**：必须在调用 `result()` 前设置打分器
4. **内存限制**：处理超大规模语料时注意内存使用，必要时调整 `IFrequentItemsCounter` 容量

## 相关文档

- [打分器使用说明](scorer/README.md)
- [自适应打分器说明](scorer/ADAPTIVE_SCORER_README.md)
