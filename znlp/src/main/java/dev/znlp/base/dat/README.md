# Double Array Trie (DAT) 模块

## 概述

本模块实现了高效的双数组 Trie（Double Array Trie）数据结构，是自然语言处理中的核心基础组件。双数组 Trie 是一种空间和时间效率都很高的 Trie 树实现，广泛应用于词典查找、前缀匹配、多模式匹配等场景。

## 核心特性

- **高效匹配**：O(m) 时间复杂度的字符串匹配，其中 m 为字符串长度
- **内存优化**：相比传统 Trie 树，显著减少内存占用
- **前缀搜索**：支持快速查找所有匹配的前缀
- **多模式匹配**：支持在文本中查找所有可能的匹配项
- **零拷贝优化**：提供虚拟拼接查询，避免创建临时字符串对象
- **序列化支持**：支持将构建好的 Trie 序列化到磁盘，便于持久化

## 核心类说明

### 1. DoubleArrayTrie

双数组 Trie 的核心实现类，提供字符串匹配和前缀搜索功能。

**主要功能：**
- **精确匹配**：`indexOf(CharSequence key)` - 查找字符串在字典中的索引
- **前缀搜索**：`commonPrefixSearch(CharSequence key)` - 查找所有匹配的前缀
- **多模式匹配**：`matcher(String text)` - 获取匹配器进行多模式匹配
- **虚拟拼接查询**：`indexOfConcat()` - 零拷贝的拼接字符串查询，适用于 BiGram 等场景

**使用示例：**
```java
// 构建双数组 Trie
List<String> keys = Arrays.asList("北京", "北京大学", "清华", "清华大学");
DoubleArrayTrie dat = DoubleArrayTrie.builder().build(keys);

// 精确匹配
int index = dat.indexOf("北京大学");  // 返回 1

// 前缀搜索
List<Integer> indices = dat.commonPrefixSearch("北京大学计算机系");
// 返回 [0, 1]，表示"北京"和"北京大学"都匹配

// 多模式匹配
DATMatcher matcher = dat.matcher("北京大学和清华大学");
while (matcher.next()) {
    int begin = matcher.getBegin();    // 匹配起始位置
    int length = matcher.getLength();  // 匹配长度
    int index = matcher.getIndex();    // 字典索引
}
```

### 2. DoubleArrayTrieBuilder

双数组 Trie 的构建器接口，负责将有序的字符串列表转换为双数组 Trie 结构。

**实现类：**
- `DoubleArrayTrieBuilder2`：唯一实现。初始数组按词表估算，建完收到实际占用的槽位。

**使用示例：**
```java
// 使用默认构建器（推荐使用 Builder2）
DoubleArrayTrie dat = DoubleArrayTrie.builder().build(keys);

// 指定初始容量
DoubleArrayTrie dat = DoubleArrayTrie.builder(8 * 1024 * 1024).build(keys);
```

**注意事项：**
- 输入的字符串列表必须**已按字典序排序且没有重复键**，否则 `build` 抛出 `IllegalArgumentException`
- 初始容量按词表估算。显式传入的容量只作为下限，建完会收到实际大小

### 3. DATMatcher

双数组 Trie 的匹配器，支持在文本中查找所有可能的匹配项。

**匹配模式：**
- **全匹配模式**：查找文本中所有可能的匹配（包括重叠匹配）
- **最长匹配模式**：通过 `DATLongMatcher` 实现，优先匹配最长的词

**使用示例：**
```java
DATMatcher matcher = dat.matcher("文本内容", 0);
while (matcher.next()) {
    String matched = text.substring(
        matcher.getBegin(), 
        matcher.getBegin() + matcher.getLength()
    );
    int dictIndex = matcher.getIndex();
    // 处理匹配结果
}
```

### 4. DoubleArrayTrieMap<T>

基于双数组 Trie 的键值对映射，将字符串键映射到任意类型的值。

**主要功能：**
- 精确查询：`get(CharSequence key)`
- 多模式匹配：`match(String text)` 返回 `MapMatcher`
- 值更新：`set(String key, T value)`

**使用示例：**
```java
TreeMap<String, String> map = new TreeMap<>();
map.put("北京", "BEIJING");
map.put("上海", "SHANGHAI");

DoubleArrayTrieMap<String> datMap = new DoubleArrayTrieMap<>(map);

// 精确查询
String value = datMap.get("北京");  // 返回 "BEIJING"

// 多模式匹配
MapMatcher matcher = datMap.match("北京和上海");
while (matcher.next()) {
    String key = text.substring(matcher.getBegin(), 
                                matcher.getBegin() + matcher.getLength());
    String value = matcher.getValue();
}
```

### 5. StringIntDATMap

专门用于字符串到整数的映射，使用原始 int 数组存储值，减少内存开销。

**主要功能：**
- 与 `DoubleArrayTrieMap` 类似，但值类型固定为 `int`
- 支持虚拟拼接查询：`getConcat()` - 适用于 BiGram 等场景
- 支持序列化：`write()` / `read()`

**使用示例：**
```java
TreeMap<String, Integer> map = new TreeMap<>();
map.put("北京", 1);
map.put("上海", 2);

StringIntDATMap datMap = new StringIntDATMap(map);

// 精确查询
int value = datMap.get("北京");  // 返回 1

// 虚拟拼接查询（零拷贝）
int value = datMap.getConcat("北", '京', "市");  // 查询 "北|京|市"
```

## 架构设计

### 类层次结构

```
DoubleArrayTrieBuilder (接口)
    ├── AbstractDoubleArrayTrieBuilder (抽象基类)
    │   └── DoubleArrayTrieBuilder2
    │
DoubleArrayTrie (核心类)
    │
    ├── DATMatcher (匹配器)
    │   └── AbstractDATMatcher (抽象基类)
    │       ├── DATMatcher (全匹配)
    │       └── DATLongMatcher (最长匹配)
    │
    ├── DoubleArrayTrieMap<T> (泛型映射)
    │   └── MapMatcher (映射匹配器)
    │
    └── StringIntDATMap (整数映射)
        ├── DATMapMatcherInt (整数映射匹配器)
        └── DATMapLongMatcherInt (整数映射长匹配器)
```

### 数据结构

双数组 Trie 使用两个数组 `base[]` 和 `check[]` 来表示 Trie 树：

- **base[]**：存储状态转移的基础值
- **check[]**：验证状态转移的有效性

状态转移公式：`next_state = base[current_state] + char_code + 1`

如果 `check[next_state] == current_state`，则转移有效。

## 性能特点

1. **查询效率**：O(m) 时间复杂度，其中 m 为查询字符串长度
2. **内存效率**：相比传统 Trie 树，内存占用减少约 50-70%
3. **构建**：按词表估算初始数组，建完收到实际槽位。匹配器对失败转移做下标检查，数组末尾不再固定多留 65536 槽
4. **零拷贝优化**：虚拟拼接查询避免创建临时字符串，特别适合高频查询场景

## 使用场景

1. **词典查找**：快速查找词语是否在词典中
2. **前缀匹配**：中文分词中的前缀词典匹配
3. **多模式匹配**：在文本中查找多个关键词
4. **BiGram/NGram 查询**：通过虚拟拼接查询优化组合键查询
5. **序列化存储**：将构建好的 Trie 持久化到磁盘

## 注意事项

1. **输入排序**：构建 Trie 时，输入的字符串列表必须已按字典序排序，且不能有重复键
2. **内存管理**：小词表不再先分配 4M 个槽。数组长度等于构建时用到的最大下标
3. **线程安全**：构建完成后，Trie 对象是只读的，可以安全地在多线程环境中使用
4. **匹配器使用**：`next()` 返回 `false` 后不应继续调用，除非重置状态

## 参考实现

本实现参考了以下开源项目：
- [HanLP](https://github.com/hankcs/HanLP)
- [ansj_seg](https://github.com/NLPchina/ansj_seg)
- Darts (Double-ARray Trie System) by Taku Kudo

## 版本历史

- **Builder2**：当前唯一的构建实现
