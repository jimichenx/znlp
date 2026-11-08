# 自适应新词打分器 - 变更日志

## 版本 v1.0 (2026-01-14)

### 🎯 核心改进

#### 问题

- ❌ 原有 `AdvancedNewWordScorer` 需要手动设置多个阈值（minMi, minEntropy等）
- ❌ 不同语料需要不同阈值，用户难以确定最佳参数
- ❌ 缺乏对词完整性的检查，会保留很多词组碎片

#### 解决方案

✅ **新增 `AdaptiveNewWordScorer`** - 自适应打分器

### 📦 新增文件

1. **AdaptiveNewWordScorer.java** - 自适应打分器核心实现
    - 支持3种工作模式：ADAPTIVE（完全自动）、SEMI_ADAPTIVE（半自动）、MANUAL（手动）
    - 支持3种过滤策略：STRICT（严格）、BALANCED（平衡）、LOOSE（宽松）
    - 自动学习阈值（基于百分位数）
    - 词完整性检查（左右熵平衡性）
    - 5级渐进式过滤

2. **AdaptiveScorerUsageExample.java** - 使用示例
    - 完全自适应模式示例
    - 不同策略对比示例
    - 半自适应模式示例
    - 与手动模式对比

3. **ADAPTIVE_SCORER_README.md** - 详细文档
    - 问题分析（CSV结果分析）
    - 改进方案详解
    - 使用方式说明
    - 效果对比

4. **CHANGELOG_ADAPTIVE.md** - 本文件

### 🔧 修改文件

1. **NewWordFindEngine.java**
    - 修改 `result()` 方法，支持自动触发自适应打分器的学习过程

2. **NewWordFindEngineOptimized.java**
    - 修改 `result()` 方法，支持自动触发自适应打分器的学习过程

3. **NewWordScorerFactory.java**
    - 新增 `RECOMMENDED_SCORER` 常量（使用自适应打分器）
    - 新增 `createAdaptive()` 方法
    - 新增 `createAdaptive(FilterStrategy)` 方法
    - 新增 `createSemiAdaptive()` 方法
    - 新增 `createAdaptiveManual()` 方法

### ✨ 主要特性

#### 1. 零配置使用

```java
// 旧方式：需要手动设置
AdvancedNewWordScorer scorer = AdvancedNewWordScorer.create(
                3.0f, 2.0f, 2, 5, 2  // 怎么确定这些值？
        );

// 新方式：零配置
AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
```

#### 2. 自动学习阈值

- 基于统计分布（百分位数）
- 根据过滤策略自动调整
- 无需人工干预

#### 3. 智能过滤策略

```java
// 严格模式（保留前25%）
AdaptiveNewWordScorer.createAdaptive(FilterStrategy.STRICT);

// 平衡模式（保留前50%，推荐）
AdaptiveNewWordScorer.

createAdaptive(FilterStrategy.BALANCED);

// 宽松模式（保留前70%）
AdaptiveNewWordScorer.

createAdaptive(FilterStrategy.LOOSE);
```

#### 4. 词完整性检查

```java
// 自动过滤左右熵不平衡的词
// 例如："交申请"（左熵0.38）会被过滤
// 保留："进博会"（左熵3.50、右熵4.63）
```

#### 5. 改进的评分公式

- 提高熵平衡性权重（0.8 → 2.0）
- 新增词长惩罚因子
- 更重视词的完整性

#### 6. 多级过滤机制

```
第1级：基本统计阈值
第2级：熵和互信息
第3级：邻字多样性
第4级：词完整性检查 ★ 新增
第5级：综合评分阈值
```

### 📊 效果分析

基于 `/data/newword_test_result.csv` (27,193个词) 的分析：

**低质量词特征**（后100行，评分6-8分）：

- 左熵或右熵极低（< 1.0）
- 左右熵严重不平衡（比例 > 3:1）
- 通常是词组碎片（如 "交申请"、"关事项"）

**高质量词特征**（前100行，评分977-324分）：

- 左右熵都较高（> 3.0）
- 左右熵相对平衡（比例 < 2:1）
- 完整的词（如 "进博会"、"疫情防控"）

**改进效果**：

- ✅ 自动过滤低质量词
- ✅ 保留高质量词
- ✅ 无需手动调整阈值

### 🔄 兼容性

完全向后兼容，可直接替换：

```java
// 旧代码
NewWordScorer scorer = AdvancedNewWordScorer.create();

// 新代码（无缝替换）
NewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
```

### 📝 使用建议

1. **首次使用**：
   ```java
   AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
   ```

2. **需要更多控制**：
   ```java
   AdaptiveNewWordScorer scorer = AdaptiveNewWordScorer.createSemiAdaptive(
       0.7f,  // 保留前30%
       FilterStrategy.BALANCED
   );
   ```

3. **不同场景**：
    - 构建词典 → 使用 `STRICT` 策略
    - 通用场景 → 使用 `BALANCED` 策略（推荐）
    - 探索发现 → 使用 `LOOSE` 策略

### 🎓 学习资源

- **详细文档**：`ADAPTIVE_SCORER_README.md`
- **使用示例**：`AdaptiveScorerUsageExample.java`
- **原理说明**：`DESIGN_IMPROVEMENT.md`

### 🚀 未来计划

- [ ] 支持更多自定义策略
- [ ] 增加评分可视化工具
- [ ] 提供更多统计分析功能
- [ ] 支持在线学习和调整

### 👤 作者

**jimichan**

### 📅 日期

2026-01-14

---

## 技术要点

### 阈值学习算法

```java
// 1. 收集所有候选词的指标
List<Float> miList = collectMetrics(allWords, word -> word.getMi());
List<Float> entropyList = collectMetrics(allWords, word -> word.getEntropy());
List<Float> scoreList = collectMetrics(allWords, word -> score(word));

// 2. 排序
Collections.

sort(miList);
Collections.

sort(entropyList);
Collections.

sort(scoreList);

// 3. 计算百分位数
int index = (int) (list.size() * percentile);
learnedMinMi =miList.

get(index);

learnedMinEntropy =entropyList.

get(index);

learnedMinScore =scoreList.

get(index);

// 4. 根据策略调整
switch(filterStrategy){
        case STRICT:learnedMinMi =

max(learnedMinMi, 3.0f); break;
        case BALANCED:learnedMinMi =

max(learnedMinMi, 1.0f); break;
        case LOOSE:learnedMinMi =

max(learnedMinMi, 0.0f); break;
        }
```

### 完整性检查算法

```java
private boolean checkEntropyBalance(WordInfo wordInfo) {
    float le = wordInfo.getLe();
    float re = wordInfo.getRe();
    
    // 检查1：单边熵过低
    if (le < 0.5f || re < 0.5f) {
        return false;  // 拒绝不完整的词
    }
    
    // 检查2：左右熵不平衡
    float imbalance = Math.max(le, re) / Math.min(le, re);
    return !(imbalance > learnedMaxEntropyImbalance);  // 拒绝不平衡的词
}
```

### 自动集成机制

```java
// 引擎会在 result() 时自动触发学习
public ArrayList<NewWord> result() {
    if (scorer instanceof AdaptiveNewWordScorer) {
        AdaptiveNewWordScorer adaptiveScorer = (AdaptiveNewWordScorer) scorer;
        if (!adaptiveScorer.isThresholdsLearned()) {
            adaptiveScorer.learnThresholds(new ArrayList<>(candidateMap.values()));
        }
    }
    // ... 后续过滤和评分
}
```

---

## 常见问题 (FAQ)

### Q1: 自适应模式适合所有场景吗？

**A**: 适合大多数场景。如果你有明确的阈值要求（如业务规定），可以使用手动模式。

### Q2: 如何选择过滤策略？

**A**:

- 首次使用或通用场景 → `BALANCED`
- 需要高质量词典 → `STRICT`
- 需要高召回率 → `LOOSE`

### Q3: 会影响性能吗？

**A**: 学习过程时间复杂度 O(N log N)，只执行一次，影响微乎其微。

### Q4: 可以查看学习到的阈值吗？

**A**: 可以：

```java
System.out.println("最小互信息: "+scorer.getLearnedMinMi());
        System.out.

println("最小熵: "+scorer.getLearnedMinEntropy());
        System.out.

println("最小评分: "+scorer.getLearnedMinScore());
```

### Q5: 如何与旧代码集成？

**A**: 只需替换打分器创建代码，其他完全不变：

```java
// 旧代码
NewWordScorer scorer = AdvancedNewWordScorer.create();

// 新代码
NewWordScorer scorer = AdaptiveNewWordScorer.createAdaptive();
```

---

**变更类型**：新增功能 (Feature)  
**影响范围**：新词发现模块  
**破坏性变更**：无  
**兼容性**：完全兼容
