---
title: Duckling 结构化解析
description: 从文本中抽取数字、金额、时间、电话等结构化维度，基于规则图解析加机器学习消歧。
---

Duckling 把自然语言中的数量、时间、联系方式等抽取成结构化对象。它**完全独立于分词流水线**，不会被 `Znlp.segment` 触发。

## 入口

```java
import dev.znlp.Znlp;
import support.duckling.dev.znlp.Dimension;

// 以当前时刻为参考时间
List<Dimension> dims = Znlp.parse("明天下午3点见面，电话13800138000");

// 指定参考时钟，相对时间才能正确归一化
List<Dimension> dims2 = Znlp.parse("明天", LocalDateTime.of(2026, 8, 26, 12, 0));
```

:::caution[相对时间必须传 clock]
「明天」「下周三」「三小时后」这类表达依赖参考时间。不传 `clock` 时用当前时刻，在测试和批处理场景会得到不可复现的结果。**测试里务必显式传入固定时钟。**
:::

### DucklingParser

需要更多控制时直接用 `duckling.dev.znlp.DucklingParser`：

```java
DucklingParser parser = DucklingParser.getInstance();   // 静态单例

// 只启用部分语法
DucklingParser custom = new DucklingParser(new DefaultRuleSet(List.of(rule1, rule2)));

// 调试：返回全部图节点，不做消歧
List<...> all = parser.generateAll(text);
```

`generateAll` 对排查"为什么没抽到"很有用——它会返回消歧前的所有候选节点，可以看到语法规则匹配到了什么、又是被哪一步淘汰的。

## Dimension

`support.duckling.dev.znlp.Dimension` 是抽象基类：

| 方法 | 说明 |
| :--- | :--- |
| `getText()` | 原文中匹配到的字面 |
| `getStart()` / `getEnd()` | 位置偏移，**闭区间** `[start, end]` |
| `with(String, int, int)` | 构造副本 |
| `getKind()` | 抽象方法，返回 `DimensionKind` |

:::note[闭区间]
`getStart()` 和 `getEnd()` 都是**包含**的，区间长度是 `end - start + 1`。和 Java 常见的半开区间习惯不同，做偏移计算时容易出错。
:::

## 支持的维度

`DimensionKind` 枚举定义了 13 个值，括号内是消歧优先级（越小越优先）：

```
Number(0)  AmountOfMoney(1)  Ordinal(2)  Temperature(3)  Duration(4)
Time(5)    Date(6)           PhoneNumber(7)  Email(8)  Url(9)
Distance(10)  Age(11)  Percentage(12)
```

:::caution[Email 和 Url 实际不支持]
`DimensionKind` 里虽然声明了 `Email(8)` 和 `Url(9)`，但 `grammar/Grammars.java` 的静态块**只注册了 10 个语法**：Number、Ordinal、Money、DateTime、Percentage、Distance、Temperature、Age、PhoneNum、Duration。没有 Email 和 Url 对应的 grammar，所以 `Znlp.parse` **永远不会返回这两类结果**。URL 和邮箱的识别在分词侧的规则包里（`segment/impl/rule/`），不在 Duckling。
:::

同样，`Date(6)` 没有独立的 grammar 和 Dimension 子类——日期和时间共用 `DateTimeDimension`，其 `getKind()` 返回 `Time`。

### 各子类取值 API

**NumberDimension**

```java
double v = num.getValue();
boolean isInt = num.isInt();
int i = num.getInt();       // 会截断小数部分
```

**MoneyDimension**

```java
float v = money.getValue();
MoneyUnit unit = money.getUnit();   // 目前仅 RMB
int grain = money.getGrain();       // 元=1、角=2、分=3
```

**DateTimeDimension**（在 `grammar/time/` 包下）

```java
LocalDateTime dt = time.toDateTime();
String grain = time.getGrain();     // 时间粒度：年/月/日/时/分/秒
int y = time.getYear(), mo = time.getMonth(), d = time.getDay();
int h = time.getHour(), mi = time.getMinute(), s = time.getSecond();

boolean future = time.isFuture();
List<...> candidates = time.getFutureList();   // 未来多义时间的候选
```

:::note[24 点归一化]
`hour == 24` 会被归一为**次日的 00:00**。所以「明天24点」得到的是后天零点，这是有意的设计而非 bug。
:::

其余子类：`OrdinalDimension`（序数）、`PercentageDimension`、`DistanceDimension`、`TemperatureDimension`、`AgeDimension`、`PhoneNumDimension`、`DurationDimension`。

## 消歧

同一段文本可能匹配多个语法规则（比如「3点」既是 Number 也是 Time），Duckling 用机器学习模型消歧：`CandidateSelector` 内部通过 `MlModel.load()` 从 classpath 读取 **`znlp/duckling.model`**。

这意味着消歧行为不能靠配置调整，要改只能重新训练模型。

## 完整示例

```java

import java.time.LocalDateTime;

LocalDateTime clock = LocalDateTime.of(2019, 11, 11, 0, 0);

for(
Dimension d :Znlp.

parse("明天下午3点开会，预算5000元，联系13800138000",clock)){
        System.out.

printf("%s [%d,%d] %s%n",
       d.getKind(),d.

getStart(),d.

getEnd(),d.

getText());

        if(d instanceof
DateTimeDimension time){
        System.out.

println("  -> "+time.toDateTime() +" grain="+time.

getGrain());
        }else if(d instanceof
MoneyDimension money){
        System.out.

println("  -> "+money.getValue() +" "+money.

getUnit());
        }else if(d instanceof
PhoneNumDimension phone){
        System.out.

println("  -> "+phone.getText());
        }
        }
```

仓库自带的 `DucklingExample` 覆盖了全部维度，并用固定参考时钟 2019-11-11 保证可复现：

```bash
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.DucklingExample
```

## 相关

- 设计文档：`docs/设计架构/Duckling.md`
- 与 NER 的分工：[命名实体识别](ner/)
