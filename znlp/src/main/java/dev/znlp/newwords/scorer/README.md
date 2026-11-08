# 新词打分器使用说明

## 概述

本目录包含了新词发现的打分和过滤算法。目前提供了两种打分器实现：

1. **DefaultNewWordScorer** - 默认打分器，基于平均互信息和左右熵的综合打分算法
2. **AdvancedNewWordScorer** - 改进的综合打分器，结合多种现代方法

## AdvancedNewWordScorer - 改进的综合打分器

### 算法特点

改进的打分器结合了多种现代新词发现方法：

1. **归一化点互信息（NPMI）**
    - 相比传统MI，NPMI将值归一化到[-1, 1]区间，更稳定
    - 公式：NPMI = PMI / (-log P(AB))
    - 权重：3.0（权重最高）

2. **Accessor Variety（左右邻接多样性）**
    - 统计词左右两侧出现的不同字符种类数
    - 取左右邻字种类数的最小值作为评估指标
    - 权重：1.5

3. **分支熵**
    - 使用信息熵衡量词边界的自由度
    - 对数缩放：log(entropy + 1) * 2.0
    - 权重：2.0

4. **文档分布得分**
    - 词分散在多个文档中比集中在单一文档更好
    - 考虑平均每文档出现次数和文档覆盖率
    - 权重：1.0

5. **词频得分**
    - 对数缩放避免过度偏向高频词
    - 公式：log(freq + 1) / 5.0
    - 权重：0.5

6. **左右熵平衡性**
    - 左右熵越接近，说明词边界越清晰
    - 使用比值衡量：min(le, re) / max(le, re)
    - 权重：0.8

### 使用方法

#### 1. 在NewWordFindEngine中使用（推荐）

```java
// 创建新词发现引擎
INewWordFindEngine.Config config = new INewWordFindEngine.Config(2, 5, 3);
INewWordFindEngine engine = INewWordFindEngine.create(config, false, null);

// 创建并设置打分器（可以在扫描前就设置）
NewWordScorer scorer = NewWordScorerFactory.createAdvanced();
engine.

setScorer(scorer);

// 第一轮扫描
engine.

firstScan(document1);
engine.

firstScan(document2);
// ... 更多文档
engine.

finishFirst();

// 第二轮扫描
engine.

secondScan(document1);
engine.

secondScan(document2);
// ... 更多文档
engine.

endSecond();  // 引擎会自动设置scorer需要的统计信息

// 获取结果
ArrayList<NewWord> results = engine.result();
```

**关键点说明：**

- 打分器可以在任何时候创建和设置，不需要等到扫描完成
- `endSecond()` 会自动调用 `scorer.setStatistics()` 设置统计信息
- 打分器是否使用这些统计信息由其实现决定（DefaultNewWordScorer不需要，AdvancedNewWordScorer需要）

#### 2. 使用自定义阈值

```java
// 创建使用自定义阈值的打分器
NewWordScorer scorer = NewWordScorerFactory.createAdvanced(
    0.0f,    // minMi - 最小互信息
    0.5f,    // minEntropy - 最小熵
    2,       // minAccessorVariety - 最小左右邻字种类数
    3,       // minFreq - 最小词频
    2        // minDocFreq - 最小文档频率
);

engine.setScorer(scorer);

// 然后进行扫描...
// engine.firstScan(...);
// engine.endSecond();  // 自动设置统计信息
```

#### 3. 在扫描后切换打分器

如果你需要在扫描完成后切换打分器，需要手动设置统计信息：

```java
// 扫描已完成
engine.endSecond();

// 创建新的打分器
NewWordScorer newScorer = NewWordScorerFactory.createAdvanced();
engine.

setScorer(newScorer);

// 手动设置统计信息（因为endSecond已经调用过了）
newScorer.

setStatistics(
        engine.getZiCountTotal(),
    engine.

getDocCount(),
    engine.

getZiFreqArray()
);

// 获取结果
ArrayList<NewWord> results = engine.result();
```

### 默认过滤阈值

| 参数                 | 默认值 | 说明                       |
|--------------------|-----|--------------------------|
| minMi              | 0.0 | 最小互信息（NPMI范围[-1,1]，降低阈值） |
| minEntropy         | 0.5 | 最小熵阈值                    |
| minAccessorVariety | 2   | 最小左右邻字种类数                |
| minFreq            | 3   | 最小词频                     |
| minDocFreq         | 2   | 最小文档频率                   |

### 相比DefaultNewWordScorer的改进

1. **更稳定的互信息计算**：使用NPMI代替传统MI，避免对低频词的过度惩罚
2. **边界多样性评估**：引入Accessor Variety，评估词的边界自由度
3. **文档分布考虑**：避免倾向于集中在单一文档的词
4. **多维度综合评估**：6个维度加权组合，更全面地评估词的质量
5. **熵平衡性**：考虑左右熵的平衡性，提高边界识别准确度

### 适用场景

- **大规模语料**：改进的算法在大规模语料上表现更稳定
- **多文档语料**：充分利用文档分布信息
- **领域术语发现**：通过Accessor Variety更好地识别专业术语
- **需要高召回率**：通过降低MI阈值，提高召回率

## DefaultNewWordScorer - 默认打分器

### 使用方法

```java
// 使用默认打分器
NewWordScorer scorer = NewWordScorerFactory.createDefault();
engine.

setScorer(scorer);

// 或使用自定义阈值
NewWordScorer scorer = DefaultNewWordScorer.create(3.0f, 0.5f);
engine.

setScorer(scorer);
```

### 算法公式

```
score = avg_mi + log₂((le·e^re + re·e^le) / |le - re|)
```

### 默认过滤阈值

- minMi: 3.0
- minEntropy: 0.5

## 如何选择打分器

1. **DefaultNewWordScorer**：
    - 简单快速，适合初次使用
    - 对互信息要求较高，精确度高但召回率可能较低
    - 适合中小规模语料

2. **AdvancedNewWordScorer**：
    - 综合多种现代方法，更全面
    - 适合大规模、多文档语料
    - 对边界和分布有更精细的控制
    - 召回率更高，适合发现更多候选词

## 自定义打分器

如需实现自己的打分器，只需实现 `NewWordScorer` 接口：

```java
public interface NewWordScorer {
    /**
     * 计算新词的综合得分
     */
    float score(NewWord newWord);

    /**
     * 判断候选词是否通过过滤
     */
    boolean accept(WordInfo wordInfo);

    /**
     * 获取打分器名称
     */
    String getName();
}
```
