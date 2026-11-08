# FastText 包代码导读

## 概述

FastText 是一个高效的文本分类和词向量学习工具，基于 Facebook 的 FastText 实现。本包提供了完整的 FastText 模型实现，支持词向量训练（CBOW、Skip-gram）和文本分类（Supervised）两种主要任务。

## 核心原理

### 1. FastText 基本思想

FastText 的核心思想是将文本表示为词向量的平均值，并通过子词（subword）特征来处理未登录词（OOV）问题。主要特点包括：

- **词向量平均**：将文本中所有词的向量取平均作为文本表示
- **子词特征**：使用字符级别的 n-gram 来捕获词的内部结构
- **哈希桶机制**：使用哈希函数将 n-gram 映射到固定大小的向量空间

### 2. 模型架构

FastText 模型包含三个主要组件：

1. **输入矩阵（wi）**：存储词向量和 n-gram 向量
2. **输出矩阵（wo）**：存储标签或词的输出向量
3. **损失函数（Loss）**：计算预测误差并更新参数

### 3. 训练流程

```
输入文本 → 词典处理 → 词ID + n-gram ID → 计算隐藏层向量 → 损失计算 → 反向传播 → 参数更新
```

## 代码结构

### 核心类

#### `FastText.java`
主入口类，提供模型的训练、预测和测试功能。

**主要方法：**
- `train()`: 训练 FastText 模型
- `trainSupervised()`: 训练有监督分类模型（复制 Args，不关闭字符 n-gram）
- `predict()`: 预测文本标签
- `test()`: 按官方公式计算 P@k / R@k
- `saveModel()` / `loadModel()`: Facebook 兼容的非量化 `.bin`

**关键属性：**
- `args`: 模型参数配置
- `dict`: 词典对象，管理词表和标签
- `model`: 模型对象，包含核心算法逻辑

#### `Model.java`
模型核心类，负责前向传播、预测和参数更新。

**主要方法：**
- `computeHidden()`: 计算隐藏层向量（词向量的平均值）
- `predict()`: 预测输入的标签
- `update()`: 更新模型参数

**关键属性：**
- `wi`: 输入矩阵，存储词向量和 n-gram 向量
- `wo`: 输出矩阵，存储标签或词的输出向量
- `loss`: 损失函数实例

**内部类：**
- `State`: 模型状态类，保存计算过程中的中间状态（隐藏层向量、输出向量、梯度向量等）

#### `Dictionary.java`
词典管理类，负责管理词表和标签。

**主要功能：**
- 词到ID的映射
- 词频统计
- 子词（n-gram）提取
- 标签管理

**关键属性：**
- `onehotMap`: 快速词映射，管理词到ID的映射
- `ntokens`: 总词数
- `nwords`: 词的数量
- `nlabels`: 标签的数量

### 参数配置

#### `Args.java`
模型参数配置类，管理训练和预测的各种参数。

**主要参数：**
- `dim`: 词向量维度（默认100）
- `ws`: 窗口大小（默认5）
- `epoch`: 训练轮数（默认5）
- `lr`: 学习率（默认0.05）
- `minCount`: 词的最小出现次数（默认5）
- `minn/maxn`: 子词的最小/最大长度（默认3/6）
- `bucket`: n-gram 哈希桶数量（默认2000000）
- `wordNgrams`: 词 n-gram 的最大长度（默认1）
- `neg`: 负采样数量（默认5）
- `loss`: 损失函数类型
- `model`: 模型类型

#### `ModelName.java`
模型类型枚举：
- `cbow`: 连续词袋模型，通过上下文词预测中心词
- `sg`: Skip-gram 模型，通过中心词预测上下文词
- `sup`: 有监督模型，用于文本分类

#### `LossName.java`
损失函数类型枚举：
- `hs`: 分层 Softmax，适用于大规模分类任务
- `ns`: 负采样，适用于词向量训练
- `softmax`: 标准 Softmax，适用于小规模分类任务
- `ova`: 一对一分类，将多分类转化为多个二分类

### 损失函数

#### `Loss.java`
损失函数抽象基类，定义了损失函数的基本接口。

**主要方法：**
- `forward()`: 前向传播，计算损失值并更新梯度
- `predict()`: 预测前 k 个最佳结果
- `computeOutput()`: 计算输出向量

**工具方法：**
- `sigmoid()`: Sigmoid 函数（使用查找表优化）
- `log()`: Log 函数（使用查找表优化）

#### 具体实现类

1. **`NegativeSamplingLoss.java`**
   - 负采样损失函数，适用于词向量训练
   - 通过采样负样本替代计算所有类别的概率
   - 使用负采样表根据词频分布高效采样

2. **`HierarchicalSoftmaxLoss.java`**
   - 分层 Softmax 损失函数
   - 通过构建霍夫曼树减少计算复杂度

3. **`SoftmaxLoss.java`**
   - 标准 Softmax 损失函数
   - 计算所有类别的概率分布

4. **`OneVsAllLoss.java`**
   - 一对一分类损失函数
   - 将多分类问题转化为多个二分类问题

5. **`BinaryLogisticLoss.java`**
   - 二分类逻辑回归损失函数
   - 作为其他损失函数的基础类

### 训练模块

#### `FastTextTrain.java`
训练管理类，负责管理 FastText 模型的训练过程。

**主要功能：**
- 多线程训练支持
- 动态学习率调整
- 训练进度监控
- 损失计算和报告

**训练模式：**
- `supervised()`: 有监督训练（文本分类）
- `cbow()`: CBOW 模式训练（词向量）
- `skipgram()`: Skip-gram 模式训练（词向量）

#### `SampleLine.java`
训练样本类，表示一行训练数据。

#### `FileSampleLineIterable.java`
文件样本迭代器，从文件读取训练样本。

### 词典模块

#### `DictionaryBuilder.java`
词典构建器，从训练数据构建词典。

#### `Entry.java`
词典条目类，表示词典中的一个词或标签。

#### `EntryType.java`
条目类型枚举：
- `word`: 词
- `label`: 标签

#### `FastWordMap.java`
快速词映射，高效管理词到ID的映射。

### 工具类

#### `HashUtils.java`
哈希工具类，提供 n-gram 哈希功能。

#### `AutoDataInput.java`
自动数据输入类，用于模型序列化和反序列化。

#### `Pair.java`
通用配对类。

#### `ScoreIdPair.java` / `ScoreLabelPair.java`
预测结果类，包含分数和ID/标签的配对。

## 使用示例

### 1. 训练有监督分类模型

```java
Args args = new Args();
args.dim = 100;
args.epoch = 5;
args.lr = 0.1;

FastText model = FastText.trainSupervised(new File("train.txt"), args);
model.saveModel(new File("model.bin"));
FastText loaded = FastText.loadModel(new File("model.bin"));
```

### 2. 预测文本标签

```java
List<String> tokens = Arrays.asList("这是", "一段", "测试", "文本");
List<ScoreLabelPair> predictions = model.predict(tokens, 5, 0.0f);

for (ScoreLabelPair pair : predictions) {
    System.out.println(pair.label + ": " + pair.score);
}
```

### 3. 测试模型性能

```java
model.test(new File("test.txt"), 1, 0.0f, true);
```

### 4. 训练词向量模型

```java
Args args = new Args();
args.model = ModelName.sg;  // 或 ModelName.cbow
args.loss = LossName.ns;
args.dim = 100;
args.epoch = 5;

FastText model = FastText.train(new File("corpus.txt"), args);
```

## 关键算法细节

### 1. 隐藏层向量计算

隐藏层向量是输入词向量的平均值：

```
hidden = (v1 + v2 + ... + vn) / n
```

其中 `v1, v2, ..., vn` 是输入词的向量表示。

### 2. 子词特征提取

对于词 "hello"，如果 `minn=3, maxn=6`，会提取以下子词：
- `<he`, `hel`, `hell`, `hello`
- `ell`, `ello`, `ello>`
- `llo`, `llo>`
- `lo>`

这些子词通过哈希函数映射到固定大小的向量空间。

### 3. 负采样

负采样通过以下步骤构建负采样表：
1. 计算每个词的采样概率：`P(w) = count(w)^0.5 / sum`
2. 根据概率构建负采样表
3. 训练时从表中随机采样负样本

### 4. 动态学习率

学习率在训练过程中线性衰减：

```
lr(t) = lr(0) * (1 - progress)
```

其中 `progress` 是训练进度（0.0 到 1.0）。

## 性能优化

1. **查找表优化**：Sigmoid 和 Log 函数使用查找表加速计算
2. **多线程训练**：支持多线程并行训练，提高训练速度
3. **哈希桶机制**：使用哈希函数将 n-gram 映射到固定大小空间，避免存储所有 n-gram
4. **负采样**：通过采样负样本替代计算所有类别，显著提高训练效率

## 注意事项

1. **内存管理**：大规模数据集训练时需要注意内存使用，可以通过调整 `bucket` 参数控制 n-gram 哈希桶大小
2. **参数调优**：不同任务需要调整不同的参数，如文本分类通常使用 `softmax` 损失，词向量训练使用 `ns` 损失
3. **数据格式**：有监督训练数据格式为：`__label__<label> <text>`，每行一个样本
4. **线程安全**：多线程训练时，参数更新需要适当的同步机制

## 扩展阅读

- Facebook FastText 论文：["Bag of Tricks for Efficient Text Classification"](https://arxiv.org/abs/1607.01759)
- Word2Vec 原理：CBOW 和 Skip-gram 模型的基础
- 负采样和分层 Softmax：两种加速训练的技术
