# BinTrie 二分字典树

## 概述

`bintrie` 包实现了一个高效的二分字典树（Trie树）数据结构，用于快速存储和检索字符串集合。该实现支持多种存储策略和匹配算法，适用于中文分词、关键词提取、实体识别等自然语言处理场景。

## 核心组件

### 1. 接口层

#### `Trie<T>`
字典树的核心接口，定义了基本的查询操作：
- `get(char[] key)` / `get(CharSequence key)` / `get(char[] key, int offset, int len)`: 通过不同形式查询值
- `containsKey(String key)`: 检查是否包含指定键

#### `BinTrieNode<T>`
Trie节点的接口，定义了节点的基本操作：
- `addChildNode(BinTrieNode<T> node)`: 添加子节点
- `findChild(char c)`: 查找指定字符的子节点
- `getStatus()`: 获取节点状态
- `getValue()`: 获取节点关联的值

#### `TrieTreeMatcher<T>`
文本匹配器接口，用于在文本中查找匹配的词语：
- `next()`: 获取下一个匹配的词语
- `getParams()`: 获取当前匹配的参数
- `getParam(int i)`: 获取指定索引的参数
- `getOffset()`: 获取匹配位置在文本中的偏移量

### 2. 实现类

#### `BinTrieTree<T>`
二分Trie树的核心实现类，同时实现了 `Trie` 和 `BinTrieNode` 接口。

**主要特性**：
- 支持两种根节点存储模式：
  - **数组模式**：使用65536大小的数组直接索引，查找O(1)，内存占用约512KB
  - **映射模式**：使用哈希映射存储首字符，内存占用小，适合首字符较少的情况
- 支持两种子节点实现：
  - **ArrayTrieNode**：数组存储，二分查找，适合子节点少的情况
  - **HashTrieNode**：哈希存储，O(1)查找，适合子节点多的情况

**主要方法**：
- `put(String word, T value)`: 插入词语及其关联值
- `get(...)`: 查询词语对应的值
- `prefixSearch(String key)`: 前缀搜索，返回所有以指定前缀开头的词语
- `remove(String key)`: 删除词语（标记为无效）
- `newForwardMatcher(String text)`: 创建前向最大匹配器
- `newAllMatcher(String text)`: 创建全匹配器
- `keys()`: 获取所有词语的迭代器
- `entry()`: 获取所有键值对的迭代器

#### `AbstractTrieNode<T>`
Trie节点的抽象基类，定义了节点的基本属性：
- `_char`: 节点代表的字符
- `status`: 节点状态（Status_Begin/Status_Continue/Status_End/Status_Null）
- `value`: 节点关联的值对象
- `level`: 节点在树中的层级深度

#### `ArrayTrieNode<T>`
基于数组的节点实现，使用有序数组存储子节点，通过二分查找定位：
- **查找**：O(log n) 时间复杂度
- **插入**：O(n) 时间复杂度（需要维护有序性）
- **适用场景**：子节点数量较少（通常少于20个）

#### `HashTrieNode<T>`
基于哈希表的节点实现，使用 `CharObjectHashMap` 存储子节点：
- **查找**：O(1) 平均时间复杂度
- **插入**：O(1) 平均时间复杂度
- **适用场景**：子节点数量较多（通常超过20个）
- **性能对比**：相比数组实现，内存增加约4.5%，插入速度提升约33%

### 3. 构建器

#### `BinTrieTreeBuilder`
枚举类，提供多种预配置的构建策略：

- **`_default`**: 默认配置，使用哈希节点，数组模式根节点（推荐）
- **`bigArray`**: 大数组模式，使用数组节点，数组模式根节点
- **`bigHash`**: 大哈希模式，使用数组节点，数组模式根节点
- **`miniArray`**: 小数组模式，使用数组节点，映射模式根节点（节省内存）
- **`miniHash`**: 小哈希模式，使用哈希节点，映射模式根节点（节省内存）

**构建方法**：
- `build()`: 构建空的Trie树
- `build(Map<String, T> map)`: 从Map构建
- `build(String split, Iterable<String> lines, Function<String[], T> transFun)`: 从文本行构建

### 4. 匹配器实现

#### `TrieTreeAllMatcher<T>`
全匹配器实现，找出文本中所有匹配的词语，包括重叠的匹配。

**匹配策略**：
- 从文本的每个位置开始，尝试匹配所有可能的词语
- 返回所有找到的匹配，包括重叠的匹配
- 匹配结果按照在文本中出现的顺序返回

**示例**：
如果词典中包含"中"、"中国"、"中国人"，对于文本"中国人"：
- 全匹配器会返回："中"、"中国"、"中国人"（3个匹配）

#### `TrieTreeForwardMaxMatcher<T>`
前向最大匹配器实现，采用前向最大匹配算法（FMM），找出每个起始位置的最长匹配。

**匹配策略**：
- 从文本的当前位置开始，尝试匹配尽可能长的词语
- 当找到匹配时，返回最长的匹配结果
- 匹配完成后，从匹配结束的位置继续下一次匹配
- 不会返回重叠的匹配，每个字符最多被匹配一次

**示例**：
如果词典中包含"中"、"中国"、"中国人"，对于文本"中国人"：
- 前向最大匹配器会返回："中国人"（1个匹配，最长）

## 使用示例

### 基本使用

```java
// 1. 创建Trie树
BinTrieTree<String> tree = BinTrieTreeBuilder._default.build();

// 2. 插入词语
tree.put("中国", "CN");
tree.put("中国人", "CNP");
tree.put("美国", "US");

// 3. 查询
String value = tree.get("中国");  // 返回 "CN"
boolean exists = tree.containsKey("中国人");  // 返回 true

// 4. 前缀搜索
Set<Entry<String, String>> results = tree.prefixSearch("中");
// 返回: [("中国", "CN"), ("中国人", "CNP")]
```

### 从Map构建

```java
Map<String, String> map = new HashMap<>();
map.put("中国", "CN");
map.put("美国", "US");
BinTrieTree<String> tree = BinTrieTreeBuilder._default.build(map);
```

### 从文本行构建

```java
List<String> lines = Arrays.asList(
    "中国 CN Beijing",
    "美国 US Washington"
);
BinTrieTree<String[]> tree = BinTrieTreeBuilder._default.build(
    " ", lines,
    arr -> arr  // 将分割后的数组作为值
);
```

### 文本匹配

#### 前向最大匹配

```java
BinTrieTree<String> tree = BinTrieTreeBuilder._default.build();
tree.put("中国", "CN");
tree.put("中国人", "CNP");

TrieTreeMatcher<String> matcher = tree.newForwardMatcher("我是中国人");
String word;
while ((word = matcher.next()) != null) {
    System.out.println("匹配: " + word);
    System.out.println("参数: " + matcher.getParams());
    System.out.println("位置: " + matcher.getOffset());
}
```

#### 全匹配

```java
TrieTreeMatcher<String> matcher = tree.newAllMatcher("中国人");
String word;
while ((word = matcher.next()) != null) {
    System.out.println("匹配: " + word);
}
// 输出: "中"、"中国"、"中国人"
```

### 遍历所有词语

```java
// 遍历所有词语
Iterator<String> keys = tree.keys();
while (keys.hasNext()) {
    String key = keys.next();
    System.out.println(key);
}

// 遍历所有键值对
for (Entry<String, String> entry : tree.entry()) {
    System.out.println(entry.getKey() + " -> " + entry.getValue());
}
```

## 性能特点

### 时间复杂度
- **查询**：O(m)，其中m是词语长度
- **插入**：O(m)，其中m是词语长度
- **前缀搜索**：O(m + k)，其中m是前缀长度，k是匹配的词语数量
- **匹配**：O(n*m)，其中n是文本长度，m是平均词语长度

### 空间复杂度
- O(n*m)，其中n是词语数量，m是平均词语长度

### 存储模式选择建议

| 场景 | 推荐配置 | 说明 |
|------|---------|------|
| 通用场景 | `_default` | 性能与内存平衡 |
| 大型词典（>10万词） | `bigArray` 或 `bigHash` | 使用数组模式根节点，查找更快 |
| 小型词典（<1万词） | `miniArray` 或 `miniHash` | 使用映射模式根节点，节省内存 |
| 子节点多（>20个） | `*Hash` 系列 | 使用哈希节点，查找更快 |
| 子节点少（<20个） | `*Array` 系列 | 使用数组节点，内存更省 |

## 节点状态说明

节点状态用于标识节点在Trie树中的位置和作用：

- **`Status_Begin` (1)**: 中间节点，不是词的结尾
- **`Status_Continue` (2)**: 既是中间节点也是词的结尾（如"中国"中的"国"节点，既是"中国"的结尾，也是"中国人"的中间节点）
- **`Status_End` (3)**: 词的结尾节点
- **`Status_Null` (-1)**: 无效节点（用于删除操作）

## 线程安全性

**该类不是线程安全的**。如果需要在多线程环境中使用，需要外部同步。建议在多线程环境中使用只读操作，或在构建完成后不再修改。

## 注意事项

1. **大小写处理**：所有插入和查询的字符串会自动转换为小写
2. **删除操作**：`remove()` 方法只是标记节点为无效，不会真正释放内存。如需真正释放内存，建议重新构建Trie树
3. **内存占用**：数组模式根节点会占用约512KB内存（65536个引用），即使只使用少量字符
4. **匹配器复用**：匹配器不是线程安全的，不应在多线程间共享

## 算法来源

匹配器算法改造自 Ansj 开源的分词项目。

## 相关类

- `common.base.dev.znlp.CharObjectHashMap`: 字符到对象的哈希映射实现
- `common.base.dev.znlp.CharObjectMap`: 字符到对象的映射接口
- `common.base.dev.znlp.AbstractIterator`: 抽象迭代器基类
