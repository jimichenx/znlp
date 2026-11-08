# AdaptiveNewWordScorer - 自适应新词打分器

## 概述

`AdaptiveNewWordScorer` 是对原有 `AdvancedNewWordScorer` 的重大改进，解决了**阈值依赖调用者手动设置**的核心痛点。

## 核心问题分析

### 原算法的问题

通过分析 `/data/newword_test_result.csv` 的结果（共27,193个词），发现以下问题：

#### 1. 阈值完全依赖手动设置

```java
// 旧方式：需要手动设置多个阈值，非常不智能
AdvancedNewWordScorer scorer = AdvancedNewWordScorer.create(
    3.0f,  // minMi - 怎么确定这个值？
    2.0f,  // minEntropy - 为什么是2.0？
    2,     // minAccessorVariety
    5,     // minFreq
    2      // minDocFreq
);
```

**问题**：

- ❌ 用户不知道设置什么值合适
- ❌ 不同语料需要不同阈值
- ❌ 需要反复尝试才能找到最佳参数
- ❌ 参数之间有相互影响，难以调优

#### 2. 低质量词的特征（CSV后100行分析）

从评分最低的词（评分6-8分）来看，它们有明显特征：

| 词语  | 评分   | 频次  | 左熵       | 右熵       | 问题        |
|-----|------|-----|----------|----------|-----------|
| 交申请 | 7.76 | 191 | **0.38** | 2.47     | 左熵极低，不完整  |
| 关事项 | 7.76 | 398 | **0.48** | 2.56     | 左熵极低，词组碎片 |
| 中国证 | 7.76 | 419 | 0.69     | **0.69** | 左右熵都低     |
| 门制定 | 7.75 | 171 | **0.59** | 2.83     | 左熵低       |

**特征总结**：

- 左熵或右熵**极低**（< 1.0），说明不是完整的词
- 左右熵**严重不平衡**（比例 > 3:1）
- 通常是词组的一部分，而非独立的词

#### 3. 高质量词的特征（CSV前100行分析）

从评分最高的词（评分977-324分）来看：

| 词语   | 评分     | 频次    | 左熵   | 右熵   | 特点       |
|------|--------|-------|------|------|----------|
| 进博会  | 811.21 | 18808 | 3.50 | 4.63 | 左右熵都高且平衡 |
| 疫情防控 | 732.27 | 19773 | 4.29 | 3.87 | 高频、平衡    |
| 新冠病毒 | 657.19 | 9950  | 4.53 | 3.93 | 完整性好     |
| 脱贫攻坚 | 551.88 | 7000  | 4.20 | 4.03 | 熵平衡      |

**特征总结**：

- 左熵和右熵都较高（通常 > 3.0）
- 左右熵相对平衡（比例 < 2:1）
- 高互信息
- 合理的文档分布

---

## 改进方案

### 核心改进

#### 1. **自动学习阈值**

基于统计分布自动确定最佳阈值，无需人工设置：

```java
// 新方式：完全自动，零配置
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
```

**工作原理**：

1. 收集所有候选词的各项指标（MI、熵、评分等）
2. 按百分位数确定阈值（例如：中位数、75分位数）
3. 根据过滤策略自动调整

#### 2. **智能过滤策略**

提供三种预设策略，适应不同场景：

| 策略           | 百分位    | 适用场景     | 特点            |
|--------------|--------|----------|---------------|
| **STRICT**   | 保留前25% | 需要高精确度   | 严格过滤，只保留最高质量词 |
| **BALANCED** | 保留前50% | 通用场景（推荐） | 平衡准确率和召回率     |
| **LOOSE**    | 保留前70% | 需要高召回率   | 宽松过滤，尽可能多召回   |

```java
// 严格模式
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive(
                AdaptiveNewWordScorer.FilterStrategy.STRICT
        );

// 平衡模式（默认）
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();

// 宽松模式
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive(
        AdaptiveNewWordScorer.FilterStrategy.LOOSE
);
```

#### 3. **词的完整性检查**

新增**左右熵平衡性检查**，过滤不完整的词：

```java
private boolean checkEntropyBalance(WordInfo wordInfo) {
    float le = wordInfo.getLe();
    float re = wordInfo.getRe();

    // 如果任意一边熵太低，直接拒绝
    if (le < 0.5f || re < 0.5f) {
        return false; // 拒绝 "交申请"（左熵0.38）
    }

    // 检查左右熵的比例
    float imbalance = Math.max(le, re) / Math.min(le, re);
    return !(imbalance > learnedMaxEntropyImbalance); // 不平衡度过高
}
```

**效果**：

- ✅ 过滤掉 "交申请"（左熵0.38）
- ✅ 过滤掉 "关事项"（左熵0.48）
- ✅ 保留 "进博会"（左熵3.50、右熵4.63）

#### 4. **多级过滤机制**

采用5级渐进式过滤，从粗到精：

```
第1级：基本统计阈值（词频、文档频率）
   ↓
第2级：熵和互信息阈值
   ↓
第3级：邻字多样性检查
   ↓
第4级：词的完整性检查（左右熵平衡）★ 新增
   ↓
第5级：综合评分阈值
```

#### 5. **改进的评分公式**

加强了对完整性的重视：

```java
float score = 
    npmi * 3.5f +              // NPMI（提高权重）
    av * 2.0f +                // 邻字多样性（提高权重）
    entropyScore * 2.5f +      // 熵（提高权重）
    docSpreadScore * 1.0f +    // 文档分布
    freqScore * 0.5f +         // 词频（降低权重，避免过度偏向高频词）
    entropyBalance * 2.0f +    // 熵平衡性（大幅提高权重）★ 改进
    lengthPenalty * 1.0f;      // 词长惩罚 ★ 新增
```

**改进点**：

- 熵平衡性权重：0.8 → **2.0**（提高2.5倍）
- 新增词长惩罚：3-4字词加分，过长/过短词降权

---

## 使用方式

### 方式1：完全自适应（推荐）

```java
// 最简单的方式，零配置
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();

List<NewWord> results = NewWordDiscoveryTool.discover(
        corpusDir,
        3, 12, 10,  // 基本参数
        scorer,      // 使用自适应打分器
        null, false
);

System.out.

println("发现新词: "+results.size());
```

### 方式2：半自适应（可微调）

```java
// 保留前30%的词（更严格）
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createSemiAdaptive(
                0.7f,  // 百分位数（0.7 = 保留前30%）
                AdaptiveNewWordScorer.FilterStrategy.BALANCED
        );

// 查看学习到的阈值
System.out.

println("最小互信息: "+scorer.getLearnedMinMi());
        System.out.

println("最小熵: "+scorer.getLearnedMinEntropy());
        System.out.

println("最小评分: "+scorer.getLearnedMinScore());
```

### 方式3：手动模式（兼容旧版）

```java
// 如果你仍想手动设置阈值
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createManual(
                3.0f,  // minMi
                2.0f,  // minEntropy
                2,     // minAccessorVariety
                5,     // minFreq
                2      // minDocFreq
        );
```

---

## 效果对比

### 对比1：过滤质量

| 方法    | 手动模式         | 自适应模式     | 改进    |
|-------|--------------|-----------|-------|
| 配置难度  | ⭐⭐⭐⭐⭐        | ⭐         | 降低80% |
| 适应性   | ❌ 需要针对不同语料调整 | ✅ 自动适应    | -     |
| 完整性检查 | ❌ 无          | ✅ 左右熵平衡检查 | 新增    |
| 阈值选择  | ❌ 依赖经验       | ✅ 基于统计分布  | 智能    |

### 对比2：实际结果

基于 `/data/newword_test_result.csv` 的分析：

**手动模式**（使用默认阈值）：

- 保留了很多低质量词（如 "交申请"、"关事项"）
- 需要反复调整阈值

**自适应模式**：

- 自动识别并过滤低质量词
- 左右熵不平衡的词（如左熵<1.0）被自动过滤
- 保留高质量词（如 "进博会"、"疫情防控"）

---

## 工作原理详解

### 阈值学习流程

```
1. 输入：所有候选词（endSecond后）
   ↓
2. 收集指标：MI、熵、评分、熵不平衡度等
   ↓
3. 统计分析：排序、计算百分位数
   ↓
4. 确定阈值：
   - learnedMinMi = 第P分位数
   - learnedMinEntropy = 第P分位数
   - learnedMinScore = 第P分位数
   - learnedMaxEntropyImbalance = 第(1-P)分位数
   ↓
5. 策略调整：根据FilterStrategy微调阈值
   ↓
6. 应用过滤：accept() 使用学习到的阈值
```

### 与引擎的集成

引擎会自动触发学习过程：

```java
// 在 NewWordFindEngine.result() 中
public ArrayList<NewWord> result() {
    // 检测到自适应打分器，自动触发学习
    if (scorer instanceof AdaptiveNewWordScorer) {
        AdaptiveNewWordScorer adaptiveScorer = (AdaptiveNewWordScorer) scorer;
        if (!adaptiveScorer.isThresholdsLearned()) {
            // 传入所有候选词进行学习
            adaptiveScorer.learnThresholds(new ArrayList<>(candidateMap.values()));
        }
    }

    // 然后正常过滤和评分
    ...
}
```

**对用户透明**：用户无需调用任何额外方法，一切都是自动的。

---

## 性能影响

| 操作   | 时间复杂度      | 影响       |
|------|------------|----------|
| 阈值学习 | O(N log N) | 一次性，影响很小 |
| 过滤   | O(N)       | 与手动模式相同  |
| 评分   | O(N)       | 与原算法相同   |

**总结**：学习过程只在第一次 `result()` 时执行一次，对性能影响微乎其微。

---

## 注意事项

### 1. 何时使用自适应模式？

✅ **推荐使用**：

- 不确定阈值如何设置
- 语料特点未知
- 希望获得最佳结果
- 希望简化配置

❌ **不推荐使用**：

- 已有明确的阈值要求（如业务规定）
- 需要完全可控的过滤逻辑

### 2. 策略选择建议

| 场景   | 推荐策略     | 理由        |
|------|----------|-----------|
| 首次使用 | BALANCED | 平衡准确率和召回率 |
| 构建词典 | STRICT   | 只要高质量词    |
| 探索发现 | LOOSE    | 尽可能多召回    |
| 生产环境 | BALANCED | 稳定可靠      |

### 3. 与旧代码兼容

完全兼容！可以无缝替换：

```java
// 旧代码
NewWordScorer scorer = AdvancedNewWordScorer.create();

// 新代码（只需改类名）
NewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
```

---

## 总结

### 核心优势

1. **✅ 零配置**：无需设置复杂阈值
2. **✅ 自动适应**：不同语料自动调整
3. **✅ 智能过滤**：基于统计分布
4. **✅ 完整性检查**：左右熵平衡验证
5. **✅ 多种策略**：适应不同场景
6. **✅ 完全兼容**：可直接替换旧代码

### 使用建议

- 🌟 **首次使用**：`AdaptiveNewWordScorer.createAdaptive()`
- 🎯 **需要控制**：使用 `createSemiAdaptive()` 调整百分位数
- 📊 **分析对比**：对比不同策略的结果
- 🔧 **特殊需求**：使用 `createManual()` 手动设置

---

## 示例代码

完整示例请参考：`AdaptiveScorerUsageExample.java`

```java
// 最简单的使用方式
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
List<NewWord> results = NewWordDiscoveryTool.discover(
        new File("data/corpus"),
        3, 12, 10,
        scorer,
        null, false
);

System.out.

println("发现 "+results.size() +" 个新词");
```

---

## 技术细节

### 评分公式详解

```java
score = npmi * 3.5            // 归一化点互信息（-1到1）
      + av * 2.0              // 最小邻字多样性
      + entropyScore * 2.5    // log(entropy+1) * 2
      + docSpreadScore * 1.0  // 文档分布得分（0.5-2.0）
      + freqScore * 0.5       // log(freq+1) / 5
      + entropyBalance * 2.0  // (minEntropy/maxEntropy) * 3
      + lengthPenalty * 1.0   // 词长因子（0.3-1.2）
```

### 完整性检查详解

```java
// 检查1：单边熵过低
if (le < 0.5f || re < 0.5f) {
    return false;  // 过滤 "交申请"（le=0.38）
}

// 检查2：左右熵不平衡
float imbalance = max(le, re) / min(le, re);
if (imbalance > threshold) {  // threshold根据策略自动学习
    return false;  // 过滤不平衡的词
}
```

---

**作者**：jimichan  
**版本**：v1.0  
**日期**：2026-01-14
