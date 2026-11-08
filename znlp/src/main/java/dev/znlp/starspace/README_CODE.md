# StarSpace Java 代码导读

本文档旨在帮助开发者快速理解 `dev.znlp.starspace` 包下的代码结构和核心类设计。该实现是基于 Facebook StarSpace 算法的 Java 重写版本。

## 1. 包结构概览

代码主要位于 `dev.znlp.starspace` 包下，并未过度分包，以便于维护和理解。

```
dev.znlp.starspace
├── Args.java                // 配置参数类
├── DataHandler.java         // 数据加载与批处理接口及实现
├── DataParser.java          // 文本行解析接口及实现
├── Dictionary.java          // 词典管理（词 -> ID 映射）
├── Evaluate.java            // 模型评估逻辑
├── ModelTrainer.java        // 模型训练控制器
├── ParseResult.java         // 单条样本解析结果的数据结构
├── SparseLinear.java        // 稀疏线性层（矩阵包装器）
├── StarSpace.java           // 模型主体（包含核心矩阵和前向计算）
├── StarSpacePrediction.java // 预测逻辑封装
├── TrainModel.java          // 训练时的临时模型结构
└── Utils.java               // 通用工具类（文本归一化、TopK堆排序等）
```

## 2. 核心类解析

### 2.1 StarSpace.java (模型主体)
这是模型的核心类，主要负责：
-   **存储权重**：持有 `lhsEmbeddings` 和 `rhsEmbeddings` 两个 `SparseLinear` 对象（即 Embedding 矩阵）。
-   **前向计算**：
    -   `projectLHS(List<XPair> ws)`: 将输入的词 ID 列表转换为 LHS 向量（通常是向量平均或求和）。
    -   `projectRHS(List<XPair> ws)`: 将输入的词 ID 列表转换为 RHS 向量。
-   **相似度计算**：`similarity(Vector a, Vector b)` 计算两个向量的相似度（点积或余弦）。
-   **模型 IO**：`saveModel` 和 `loadModel` 用于模型的持久化和加载。

### 2.2 ModelTrainer.java (训练器)
负责管理整个训练流程：
-   **初始化**：构建词典 (`Dictionary`)，解析器 (`DataParser`) 和数据处理器 (`DataHandler`)。
-   **多线程训练**：`train()` 方法启动多个线程并行训练。
-   **训练循环 (`trainEpoch`)**：
    -   每个线程处理数据分片。
    -   调用 `trainOneExample` 处理单个样本。
    -   **负采样**：在 `trainOne` 或 `trainNLL` 中，为每个正样本随机采样 `negSearchLimit` 个负样本。
    -   **梯度更新**：计算 Loss（Hinge 或 Softmax），并反向传播更新 Embedding 矩阵 (`backward` 方法)。

### 2.3 Dictionary.java (词典)
管理词汇表：
-   维护 `String` (词/标签) 到 `int` (ID) 的映射。
-   支持 N-gram hashing (`addNgrams` 方法)，用于处理未登录词或丰富特征。
-   区分 `Word` 和 `Label` 类型。

### 2.4 DataHandler & DataParser (数据流)
这两个接口及其子类（`FastText...` 和 `Layer...`）负责数据的读取和预处理：
-   **DataParser**：将原始文本行解析为 `ParseResult` 对象，提取 LHS 和 RHS 的 token。
-   **DataHandler**：
    -   `loadFromFile`: 读取整个文件到内存（`InternDataHandler` 实现）。
    -   `convert`: 根据 `TrainMode`（如 Mode 0, Mode 1 等）动态构建正负样本对。例如在 Mode 1 下，它会从 RHS 特征中随机选一个作为 Label，剩下的作为 Input。

### 2.5 Args.java (配置)
定义了所有超参数，如学习率 (`lr`)、维度 (`dim`)、Epoch 数 (`epoch`)、训练模式 (`trainMode`) 等。该类实现了 `Serializable` 以便保存模型配置。

## 3. 关键流程导读

### 3.1 训练流程
1.  **准备数据**：`ModelTrainer` 加载文件，构建词典。
2.  **初始化权重**：`TrainModel` 初始化 LHS 和 RHS 矩阵（随机高斯分布）。
3.  **迭代 Epoch**：
    -   打乱数据索引。
    -   多线程并行处理样本。
    -   **正样本构建**：根据 `TrainMode` 将 `ParseResult` 转换为 LHS 和 RHS 正样本对。
    -   **前向传播**：`projectLHS` 和 `projectRHS` 得到向量。
    -   **负采样**：随机抽取不等于当前 RHS 的样本作为负样本。
    -   **计算 Loss & Gradient**：根据 Hinge Loss 公式计算误差，并计算梯度。
    -   **反向传播**：更新 LHS 和 RHS 矩阵中对应词的向量。
4.  **保存模型**：训练结束后生成二进制或文本格式的模型文件。

### 3.2 预测流程
1.  **加载模型**：`StarSpace.loadModel` 读取配置、词典和矩阵。
2.  **构建预测器**：`StarSpacePrediction` 封装模型和候选集（BaseDocs）。
3.  **预测**：
    -   将输入文本解析为 token 列表。
    -   `projectLHS` 得到查询向量。
    -   遍历所有候选标签（BaseDocs），计算相似度。
    -   使用 `MinHeapTopKInt` (最小堆实现的 Top K 最大值查询) 选出得分最高的 K 个结果。

## 4. 与原版实现的差异
-   **矩阵实现**：使用 `blas.base.dev.znlp.DenseMatrix` 替代了原版 Kotlin 中的 `ByteBufferMatrix`，但保持了相同的数学逻辑。
-   **语言特性**：利用 Java 8+ 的 Stream API 和 Lambda 表达式简化了部分数据处理逻辑，同时保留了核心算法的性能敏感部分的实现方式（如数组操作）。
