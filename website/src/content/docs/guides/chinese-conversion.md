---
title: 繁简转换
description: 简体与繁体双向转换，基于 Aho-Corasick 多模式匹配与长词优先策略。
---

## 门面入口

```java
import dev.znlp.Znlp;

Znlp.s2t("汉字");   // 簡 → 繁：漢字
Znlp.t2s("漢字");   // 繁 → 簡：汉字
```

两个方法都是静态的，内部通过 `TransformService` 完成，服务实例在注册表中是单例，词典只加载一次。

## TransformService 接口

`converter.dev.znlp.TransformService` 提供 `String` 和 `char[]` 两套重载：

| 方法 | 说明 |
| :--- | :--- |
| `String s2t(String text)` | 简体转繁体 |
| `String t2s(String text)` | 繁体转简体 |
| `String s2t(char[] text)` | 字符数组版，避免额外的字符串拷贝 |
| `String t2s(char[] text)` | 字符数组版 |

需要脱离门面使用时：

```java
TransformService transform = Znlp.getService(TransformService.class);
String traditional = transform.s2t("简体中文");
```

`char[]` 重载适合在已经有字符缓冲的场景（比如分词流水线的 `segment(char[], Consumer<Token>)`）中做原地转换，省去一次 `String` 构造。

## 实现要点

转换基于 **Aho-Corasick 自动机**做词典多模式匹配，并采用**长词优先**策略。这一点很重要，因为繁简转换不是逐字映射：

- 「里面」的「里」在繁体中是「裡」（内部含义）还是「里」（长度单位、姓氏），取决于上下文词。
- 「头发」的「发」对应「髮」，而「发展」的「发」对应「發」。

长词优先保证了「里面」整体匹配到「裡面」，而不是把「里」单字转换。因此**词典覆盖率直接决定转换质量**，遇到专有名词转换错误时，通常是对应词条不在词典中。

## 资源文件

| 资源 | 路径 |
| :--- | :--- |
| 简转繁词典 | `znlp/ts-dict/s2t.txt` |
| 繁转简词典 | `znlp/ts-dict/t2s.txt` |

两个词典都带 `.hash` 校验文件，首次加载后会生成 `S2T_Dict_<hash>` 和 `T2S_Dict_<hash>` 缓存文件写入 `cache-dir`（默认 `data/cache`），后续启动直接读缓存。

:::note[资源缺失时的排查]
繁简转换的词典随 `dev.znlp:znlp-resources` 资源包发布。如果抛 `ResourceNotFoundException`，先确认 `znlp-resources` 在运行时 classpath 上——主库对它是 `runtimeOnly` 依赖，不会传递给你的项目。详见[资源下载与离线部署](../deployment/resources/)。
:::

## 与分词的关系

繁简转换**不在分词流水线内**，`Znlp.segment` 不会自动做转换。但用户词典在入库时会同时保存原词和 `CharNormUtils.convert` 的规范化形式，所以即使用户词典里写的是简体，繁体输入也可能命中。

如果你的输入是繁简混杂的，建议在进入分词前统一转换：

```java
String normalized = Znlp.t2s(rawText);
List<Token> tokens = Znlp.segment(normalized);
```

注意这样得到的 `Token.offset` 是相对于**转换后**文本的。由于繁简转换是等长映射（一个汉字对一个汉字），偏移通常仍然可用，但如果原文包含需要多字展开的词，就要自行重新对齐。

## 相关

- [智能高亮](highlight/) —— 同样依赖字符归一化
- [配置与数据目录](../deployment/configuration/) —— 缓存目录位置
