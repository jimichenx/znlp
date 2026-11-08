---
title: 拼音
description: 中文转拼音、拼音首字母、拼音音节切分、模糊音与自定义读音。
---

## 门面入口

```java
import dev.znlp.Znlp;

Znlp.pinyinString("你好znlp");     // ni hao znlp
Znlp.pinyinHeadString("复旦大学");    // f d d x
PinyinResult result = Znlp.pinyin("你好");   // 拿到结果对象做进一步配置
```

:::caution[pinyinString 不带声调]
`Znlp.pinyinString` 的 Javadoc 写的是"带声调拼音"，但实现调用的是 `PinyinResult.asString()`，内部走 `getPinyinWithoutTone()`，**实际输出不带声调**。这是一处文档与实现不一致。如果需要声调，请直接使用 `PinyinResult` 的拼音列表接口读取 `Pinyin` 枚举。
:::

字母和数字默认原样保留，所以 `znlp` 不会被转换掉。

## PinyinResult：链式配置输出

`pinyin.dev.znlp.PinyinResult` 既是配置对象也是输出对象，`with*` 风格的 setter 返回自身，可以链式调用：

| 方法 | 默认 | 作用 |
| :--- | :--- | :--- |
| `fuzzy(boolean)` | `false` | 启用模糊音映射 |
| `keepPunctuation(boolean)` | `false` | 保留标点 |
| `keepNumber(boolean)` | **`true`** | 保留数字 |
| `keepAlpha(boolean)` | **`true`** | 保留英文字母 |
| `keepOthers(boolean)` | `false` | 保留其他字符 |

输出方法：

| 方法 | 返回 |
| :--- | :--- |
| `asString()` / `asString(String splitter)` | 空格（或自定义分隔符）连接的拼音串 |
| `asHeadString()` / `asHeadString(String splitter)` | 首字母串 |
| `asList()` | `List<String>`，不带声调 |
| `asHeadList()` | `List<String>`，首字母列表 |
| `getPinyinList()` | 底层拼音对象列表 |

```java
PinyinResult r = Znlp.pinyin("你好，世界2026")
        .keepPunctuation(true)
        .fuzzy(true);

System.out.println(r.asString());        // ni hao ， shi jie 2026
System.out.println(r.asHeadString());    // n h s j
System.out.println(r.asList());
```

### 模糊音映射

`fuzzy(true)` 时应用以下固定映射（不可配置）：

| 原 | 模糊 | 原 | 模糊 |
| :--- | :--- | :--- | :--- |
| `zh` | `z` | `eng` | `en` |
| `ch` | `c` | `ang` | `an` |
| `sh` | `s` | `ing` | `in` |
| | | `iang` | `ian` |
| | | `uang` | `uan` |

平翘舌、前后鼻音都会被归一，适合做搜索召回的拼音归一化。

## PinyinService：拼音音节切分

`pinyin.dev.znlp.PinyinService` 提供比门面更细的能力，其中**拼音切分**用的是感知机模型（`PinyinSplitPerceptron`），可以把连写的拼音串切回音节：

```java
PinyinService pinyin = Znlp.getService(PinyinService.class);

List<String> syllables = pinyin.split("nihaoshijie");   // [ni, hao, shi, jie]
List<String> fromChars = pinyin.split(charArray);       // char[] 重载

PinyinResult result = pinyin.convert("你好");            // 等价于 Znlp.pinyin
SimplePinyin[] cp = pinyin.charPinyin('中');             // 单字读音（多音字返回多个）
```

`split` 解决的是拼音分词歧义问题——比如 `xian` 可以切成 `xi'an` 或 `xian`，规则方法容易出错，所以这里用了统计模型。

### 自定义读音

多音字或专有名词的读音不符合预期时，用 `CustomPinyin` 覆盖：

```java
CustomPinyin custom = pinyin.getCustomPinyin();
custom.put("重庆", "chong qing");
custom.remove("重庆");
Map<String, String> all = custom.getMap();

pinyin.rebuild();   // 修改后必须调用，刷新内部引擎
```

:::caution[改完必须 rebuild]
`CustomPinyin.put` 只是写入映射表，不会影响已经构建好的转换引擎。修改后必须调用 `PinyinService.rebuild()`，否则新的读音不生效。
:::

## 枚举类型

`dev.znlp.pinyin.enums` 包下有 5 个枚举：

| 枚举 | 含义 |
| :--- | :--- |
| `Pinyin` | 完整拼音（含声调） |
| `SimplePinyin` | 不带声调的拼音 |
| `PinyinHead` | 拼音首字母 |
| `Shengmu` | 声母 |
| `Yunmu` | 韵母 |

需要声调信息时，从 `getPinyinList()` 拿 `Pinyin` 枚举，而不是用 `asString()`。

## 资源文件

| 资源 | 路径 | 用途 |
| :--- | :--- | :--- |
| 拼音词典 | `znlp/pinyin/pinyin.txt` | 汉字到拼音的映射 |
| 切分模型 | `znlp/pinyin/pinyin-split.model` | 拼音音节切分感知机模型 |

两者都打包在 `znlp-resources` jar 中。资源缺失时抛 `ResourceNotFoundException`，排查方式见[资源下载与离线部署](../deployment/resources/)。

## 相关

- 拼音匹配的高亮应用：[智能高亮](highlight/)
- 感知机模型的训练与复用：[感知机框架](perceptron/)
