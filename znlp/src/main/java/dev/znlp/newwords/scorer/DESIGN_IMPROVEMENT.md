# 设计改进：统计信息自动注入机制

## 问题背景

在最初的设计中，`AdvancedNewWordScorer` 需要三个全局统计信息：

- `totalCharCount` - 语料总字符数
- `totalDocCount` - 总文档数
- `charFreqArray` - 字符频率数组

这些信息用于计算NPMI（归一化点互信息）等高级指标。

## 原设计的问题

### 问题1：创建时机受限

```java
// ❌ 原设计：必须在扫描完成后才能创建scorer
engine.firstScan(doc1);
engine.

finishFirst();
engine.

secondScan(doc1);
engine.

endSecond();  // 统计信息在这里才完整

// 只能在这之后创建scorer
NewWordScorer scorer = NewWordScorerFactory.createAdvanced(
        engine.getZiCountTotal(),
        engine.getDocCount(),
        engine.getZiFreqArray()
);
engine.

setScorer(scorer);
```

**问题：**

- 用户必须记住正确的创建顺序
- 不能在扫描前就配置好scorer
- 违反了"先配置，后运行"的直觉

### 问题2：责任分配不清

```java
// 用户需要自己管理统计信息的传递
NewWordScorer scorer = NewWordScorerFactory.createAdvanced(
                engine.getZiCountTotal(),    // 用户需要知道从哪里获取
                engine.getDocCount(),         // 用户需要记住传递顺序
                engine.getZiFreqArray()       // 增加使用复杂度
        );
```

**问题：**

- 引擎内部的统计信息暴露给用户
- 用户需要了解scorer的内部需求
- 增加了API的使用复杂度

### 问题3：灵活性受限

```java
// 无法在运行前就设置scorer
engine.setScorer(scorer);  // ❌ 这时候scorer还没有统计信息，无法正常工作
engine.

firstScan(doc1);
// ...
```

## 改进后的设计

### 核心思想

**"由引擎负责统计信息的管理和注入"**

- 用户只需创建和设置scorer
- 引擎在适当时机自动注入统计信息
- scorer可以选择使用或忽略这些信息

### 设计要点

#### 1. 接口增强

```java
public interface NewWordScorer {
    float score(NewWord newWord);

    boolean accept(WordInfo wordInfo);

    String getName();

    /**
     * 由引擎自动调用，设置统计信息
     */
    default void setStatistics(int totalCharCount, int totalDocCount, int[] charFreqArray) {
        // 默认实现为空，不需要的scorer无需实现
    }
}
```

**优点：**

- 使用default方法，向后兼容
- 不需要统计信息的scorer（如DefaultNewWordScorer）无需修改
- 统一的统计信息注入接口

#### 2. 自动注入机制

```java
// NewWordFindEngine.java
public void endSecond() {
    // ... 计算统计信息 ...

    // 自动设置打分器的统计信息
    if (scorer != null) {
        scorer.setStatistics(ziCountTotal, docCount, ziFreqArray);
    }
}
```

**优点：**

- 用户无需关心统计信息的传递
- 引擎在计算完成后自动注入
- 解耦了scorer的创建和统计信息的设置

#### 3. 简化的工厂方法

```java
// ✅ 改进后：不需要统计信息
public static NewWordScorer createAdvanced() {
    return AdvancedNewWordScorer.create();
}

public static NewWordScorer createAdvanced(
        float minMi, float minEntropy, int minAccessorVariety,
        int minFreq, int minDocFreq) {
    return AdvancedNewWordScorer.create(
            minMi, minEntropy, minAccessorVariety, minFreq, minDocFreq
    );
}
```

**优点：**

- 简化了API
- 参数更少，更易理解
- 关注业务参数（阈值），而非技术细节（统计信息）

## 使用方式对比

### 原设计（❌ 复杂）

```java
// 1. 先扫描
engine.firstScan(doc1);
engine.

finishFirst();
engine.

secondScan(doc1);
engine.

endSecond();

// 2. 获取统计信息（用户需要知道）
int totalChars = engine.getZiCountTotal();
int totalDocs = engine.getDocCount();
int[] charFreq = engine.getZiFreqArray();

// 3. 创建scorer（需要传递统计信息）
NewWordScorer scorer = NewWordScorerFactory.createAdvanced(
        totalChars, totalDocs, charFreq
);

// 4. 设置scorer
engine.

setScorer(scorer);

// 5. 获取结果
ArrayList<NewWord> results = engine.result();
```

### 新设计（✅ 简单）

```java
// 1. 创建并设置scorer（可以在最开始）
NewWordScorer scorer = NewWordScorerFactory.createAdvanced();
engine.

setScorer(scorer);

// 2. 扫描
engine.

firstScan(doc1);
engine.

finishFirst();
engine.

secondScan(doc1);
engine.

endSecond();  // 自动注入统计信息

// 3. 获取结果
ArrayList<NewWord> results = engine.result();
```

## 设计原则体现

### 1. 单一职责原则（SRP）

- **Engine**：负责扫描、统计、管理scorer
- **Scorer**：负责评分和过滤
- **用户**：只需配置参数和提供数据

### 2. 依赖倒置原则（DIP）

- Engine依赖于NewWordScorer接口
- 具体的scorer实现由用户选择
- 统计信息通过接口注入，而非构造时传递

### 3. 开闭原则（OCP）

- 通过default方法扩展接口，无需修改现有实现
- 新的scorer可以选择使用或忽略统计信息
- 现有代码无需修改

### 4. 最少知识原则（LoD）

- 用户无需了解engine内部的统计信息结构
- Scorer的创建不依赖engine的内部状态
- 降低了类之间的耦合度

## 特殊场景处理

### 场景1：在扫描后切换scorer

```java
// 扫描完成
engine.endSecond();

// 切换到新的scorer
NewWordScorer newScorer = NewWordScorerFactory.createAdvanced();
engine.

setScorer(newScorer);

// 手动设置统计信息（因为endSecond已经调用过）
newScorer.

setStatistics(
        engine.getZiCountTotal(),
    engine.

getDocCount(),
    engine.

getZiFreqArray()
);

ArrayList<NewWord> results = engine.result();
```

**说明：**

- endSecond只会对当时的scorer设置统计信息
- 如果之后切换scorer，需要手动设置
- 这是一个特殊场景，大多数用户不会遇到

### 场景2：自定义scorer不需要统计信息

```java
public class SimpleScorer implements NewWordScorer {
    @Override
    public float score(NewWord newWord) {
        return newWord.getFreq();  // 简单的频率打分
    }

    @Override
    public boolean accept(WordInfo wordInfo) {
        return wordInfo.getCount() > 5;
    }

    @Override
    public String getName() {
        return "SimpleScorer";
    }

    // 不需要实现setStatistics，使用default空实现
}
```

**说明：**

- 不需要统计信息的scorer无需实现setStatistics
- 引擎会调用，但scorer可以忽略
- 保持了接口的简洁性

## 性能考虑

### 内存开销

- 统计信息本身已经在engine中存在
- 通过引用传递，不增加额外内存
- Scorer只持有引用，不复制数据

### 计算开销

- setStatistics只是简单的赋值操作
- 不涉及复杂计算
- 对性能影响可忽略

## 测试验证

```bash
# 编译验证
cd /Users/jimichan/zaprt-projects/znlp
./gradlew compileJava

# 运行示例
./gradlew run --args="dev.znlp.newwords.scorer.AdvancedScorerExample"
```

## 总结

这次设计改进的核心价值：

1. **简化API**：用户不需要了解统计信息的细节
2. **提高灵活性**：scorer可以在任何时候创建和设置
3. **责任明确**：引擎负责统计，scorer负责评分
4. **易于扩展**：新的scorer可以自由选择是否使用统计信息
5. **向后兼容**：现有代码无需修改

这是一个典型的"从实现驱动到接口驱动"的重构案例，体现了良好的OOP设计原则。
