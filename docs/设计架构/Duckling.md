# Duckling 结构化解析

znlp 的 Duckling 模块从 Facebook [Duckling](https://github.com/facebook/duckling) 的思路移植到 Java：用规则自底向上组出候选，再用朴素贝叶斯在重叠跨度上消歧。它**不进入默认分词流水线**，由 `Znlp.parse` 或 `DucklingParser` 单独调用。

源码根包：`dev.znlp.duckling`。

## 能解析什么

默认装入全部语法（见 `Grammars`）：

| 维度 | 语法类 | 典型输入 |
| :--- | :--- | :--- |
| 数字 | `NumberGrammar` | `123`、`三点一四`、`一万零四十` |
| 序数 | `OrdinalGrammar` | `第一` |
| 金额 | `MoneyGrammar` | `5元5角` |
| 时间/日期 | `DateTimeGrammar` | `明天下午3点`、`下周日`、`中秋节` |
| 百分比 | `PercentageGrammar` | `5%`、`百分之三点一四` |
| 距离 | `DistanceGrammar` | `5公里` |
| 温度 | `TemperatureGrammar` | `25摄氏度` |
| 年龄 | `AgeGrammar` | `30岁` |
| 电话 | `PhoneNumGrammar` | `13800138000` |
| 时长 | `DurationGrammar` | `一小时30分` |

相对时间依赖参考时钟（`LocalDateTime clock`）。不传时用 `LocalDateTime.now()`。

## 调用入口

```java
List<Dimension> dims = Znlp.parse("明天下午3点见面，电话13800138000");
List<Dimension> withClock = Znlp.parse("明天", LocalDateTime.of(2019, 11, 11, 0, 0));
```

只跑数字等子集时，自己组 `RuleSet`：

```java
DucklingParser numbers = new DucklingParser(
        new DefaultRuleSet(new NumberGrammar().rules()));
```

全局全量解析器用 `DucklingParser.getInstance()`，`RuleSet` 只构建一次。

可运行示例：`./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DucklingExample`

## 流水线

```
文本
  │
  ▼
DefaultRuleSet.execute          图解析，产出全部 DkNode
  │  1. DAT（失败指针）扫词典
  │  2. 正则扫剩余 TextPattern
  │  3. 最多 30 轮：脏规则用相邻节点做 production
  │  4. 新节点按类型套 Filter，再进入下一轮
  ▼
CandidateSelector               丢掉 latent，按跨度贪心挑不重叠的候选
  │  更长优先 → 更早起点 → 同类看 Naive Bayes → 维度优先级
  ▼
FinalNodeValue.output(clock)    相对时间等在这里落地成 Dimension
  ▼
List<Dimension>                 带原文切片与 [start, end]（两端包含）
```

图解析是 chart parser：词典/正则铺叶子，组合规则把相邻节点拼成更大节点。规则段数通常不超过 3。

## 规则与 Pattern

一条 `Rule` = 有序列的 `DucklingPattern` + `production`。

| Pattern | 作用 |
| :--- | :--- |
| `DictPattern` | 词表，由整句 DAT 一次扫出后按词回填 |
| `RegexPattern` | 正则；`RuleBuilder.pattern(String)` 在字面量或 `a\|b\|c` 时会改成词典 |
| `FilterPattern` | 匹配已有节点的值类型与谓词（如「整数且是数位串」） |

`RuleBuilder` 示例：

```java
RuleBuilder.rule("money {int} unit")
        .filter(NumberNodeValue.class, n -> true)
        .pattern("块|元|圆|角|毛|分|元整")
        .production((text, nodes) -> { ... })
        .build();
```

注意：`RuleBuilder` 的 rule / pattern id 是**进程内递增**的，不是 `List` 下标。`DefaultRuleSet` 必须用 `rulesById[id]`，不能按列表位置取规则。

## 图解析引擎（`DefaultRuleSet`）

每句新建内部 `Reaction`，索引复用构造期算好的结构：

- **词典**：全部 `DictPattern` 词打进一棵 DAT，并 `enableFailure`，避免每个字符从根重启。
- **Filter**：按 `Class` 索引，新节点只探相关过滤器。
- **脏规则**：某 pattern 有新命中时，只重跑依赖它的规则。
- **相邻匹配**：`Rule.matches` 第 2 列起按起点排序后二分，不是全量笛卡尔积。
- **去重**：`RepeatKey(ruleId + 最多 3 段 span)`，避免同一规则同一跨度反复 production。

上限：`ITERATIONS_MAX = 30`，`MAX_STASH_SIZE = 1024`。节点过多会提前停。

## 消歧（`CandidateSelector`）

只有 `Dimension` 或 `FinalNodeValue` 且 **非 latent** 才进入候选。例如金额规则会把「5分」做成钱，时长里未落地的「5分」标成 latent，避免抢跨度。

排序：

1. 跨度更长优先
2. 起点更靠前
3. 同一 `DimensionKind` 用 `MlModel.trueProbalog`（按规则名做分类器 id，子节点规则名为特征）
4. `DimensionKind` 的 `priority`（数字 0 低于时间 5，同类冲突时时间更容易留下）

选出的集合两两不重叠，再按起点排成输出顺序。

模型文件：`classpath:znlp/duckling.model`。缺失时分类器为空，同类只靠跨度/优先级。

## 数字规则要点

`NumberGrammar` 是组合爆炸最敏感的一块：

- 阿拉伯数字四类正则（千分位小数 / 千分位整数 / 小数 / 整数）合成一条，从特殊到一般。
- 中文数位拼接**只向左加一位**，跨度最多 4（覆盖「一九九八」），避免「一二三四五…」节点爆炸。
- `十`/`拾` 的值是 10，**不是**数位串；`一`～`九`、`零`/`〇` 才是。
- 两个数位串之间不用「相邻量级相加」（那条规则留给「一万二千」「1亿2000万」），否则「一九九八」会被加成 36。

完整写法（「一百一十」「一千零一」）由量级规则覆盖；口语省略量级（「一百一」=110）目前仍可能拆成两个数。

## 时间规则要点

`DateTimeGrammar` 由 `GrainRules`、`DateRules`、`HolidayRules`、`TimeRules` 拼成。

- 相对日期依赖传入的 `clock`。回归样例里参考日是 2019-11-11。
- `hour == 24` 在 `DateTimeDimension` 构造时归一成次日 00:00。
- 春节表只到 2060 年；更晚的「××年春节」应得不到结果，不能 NPE。

## 如何加一种维度

1. 定义 `XxxDimension extends Dimension`，给出 `getKind()`。
2. 写 `XxxGrammar implements DucklingGrammar`：`rules()` + 尽量补 `examples()`。
3. 在 `Grammars` 静态块里 `grammars.add(new XxxGrammar())`。
4. 若新 kind 会和现有跨度冲突，考虑 `makeLatent()` 或调整 `DimensionKind` 优先级。
5. 把例子加进 `examples()`，由 `ComprehensiveTest` / `DucklingGateTest` 守门。

只测单一语法时不要用全量 `DucklingParser.getInstance()`，否则时间/金额会抢走数字跨度。

## 源码地图

```
duckling/
  DucklingParser.java       门面：generateAll / parse
  CandidateSelector.java    消歧
  ParserMatch.java          选中的节点 + 跨度
  grammar/
    DucklingGrammar.java    语法接口
    Grammars.java           默认语法注册表
    number/ money/ time/ …  各维度
  rule/
    Rule / RuleBuilder      规则与 DSL
    RuleSet / DefaultRuleSet 图解析
  pattern/                  Dict / Regex / Filter
  support/                  Dimension、DkNode、TextPosition
  ml/                       Naive Bayes 模型与 Example
```

`TextPosition` 的 `end` **包含**最后一个字符下标；`getLength()` 为 `end - start + 1`。和 Java 半开区间不同。
