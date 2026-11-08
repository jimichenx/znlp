# StarSpace 模型介绍与使用指南

## 1. 模型简介
StarSpace 是一个通用的神经网络模型，用于学习各种实体（Entity）的向量表示（Embedding）。这些实体可以是单词、句子、文档、标签、图节点，甚至是用户和商品。

StarSpace 的核心思想是将不同类型的实体映射到同一个向量空间中，使得相互关联的实体在空间中的距离更近（相似度更高），而不相关的实体距离更远。

## 2. 基本原理

StarSpace 模型主要由以下几个部分组成：

1.  **Embeddings（嵌入层）**：
    模型包含一个字典（Dictionary），每个条目（单词或标签）都对应一个 $D$ 维向量。
    -   **LHS (Left-Hand Side) 实体**：通常代表输入，如文档、查询、上下文。LHS 的向量表示通常是其包含的所有单词向量的聚合（如求和或平均）。
    -   **RHS (Right-Hand Side) 实体**：通常代表目标，如标签、文档、后续单词。

2.  **相似度函数 (Similarity Function)**：
    用于计算 LHS 实体向量 $V_{LHS}$ 和 RHS 实体向量 $V_{RHS}$ 之间的相似度。常用的有：
    -   **Cosine Similarity (余弦相似度)**
    -   **Dot Product (点积)**

3.  **损失函数 (Loss Function)**：
    模型通过 Ranking Loss（排序损失）进行训练。对于一个正样本对 $(LHS, RHS^+)$ 和一组负样本 $RHS^-$，模型试图最小化以下损失：
    $$
    Loss = \sum_{RHS^-} \max(0, margin - sim(LHS, RHS^+) + sim(LHS, RHS^-))
    $$
    即要求正样本的相似度比负样本的相似度至少高出一个 `margin`。

## 3. 模型使用

### 3.1 数据格式
StarSpace 使用简单的文本格式。每一行代表一个训练样本。
-   **FastText 格式**：标准的分类数据格式。单词以空格分隔，标签以 `__label__` 开头。
    ```text
    这 是 一个 很好的 酒店 __label__positive
    ```
-   **LabelDoc 格式**：使用制表符 `\t` 分隔输入特征和标签特征。
    ```text
    word1 word2 ... \t label_feat1 label_feat2 ...
    ```

### 3.2 训练模型
使用 `ModelTrainer` 类进行训练。

```java
// 1. 配置参数
Args args = new Args();
args.trainMode = Args.TrainMode.Mode0; // 选择训练模式
args.lr = 0.01;                        // 学习率
args.epoch = 5;                        // 迭代轮数
args.dim = 100;                        // 向量维度

// 2. 初始化训练器
ModelTrainer trainer = new ModelTrainer(args, "train_data.txt", null);

// 3. 开始训练
StarSpace model = trainer.train();

// 4. 保存模型
model.saveModel("model_dir");
```

### 3.3 评估与预测
使用 `Evaluate` 类进行评估，使用 `StarSpacePrediction` 进行单条预测。

```java
// 评估
Evaluate evaluate = new Evaluate(model, "test_data.txt", 1, null, null);
evaluate.evaluate(); // 输出 Hit@K 等指标

// 预测
StarSpacePrediction predictor = model.prediction();
List<StarSpacePrediction.Prediction> results = predictor.predictOne("输入 文本");
for (StarSpacePrediction.Prediction p : results) {
    System.out.println("Label ID: " + p.second + ", Score: " + p.score);
}
```

## 4. 训练模式 (TrainMode) 详解与场景示例

StarSpace 的强大之处在于通过不同的 `TrainMode` 支持多种任务。

### Mode 0: 文本分类 / 情感分析
*   **描述**：标准的监督学习。LHS 是输入文本（词袋），RHS 是标签。
*   **数据格式**：`word1 word2 ... __label__class`
*   **场景**：
    -   新闻分类（体育、财经、科技...）
    -   情感分析（好评、差评）
    -   意图识别

### Mode 1: 基于内容的推荐 / 标签推荐
*   **描述**：从 RHS 特征集中随机选取一个作为 Label，其余作为 LHS 输入。
*   **数据格式**：`__label__music __label__rock __label__beatles`
*   **场景**：
    -   给定用户的一组兴趣标签，预测下一个可能感兴趣的标签。
    -   Collaborative Filtering：用户历史行为作为 LHS，预测下一个行为。

### Mode 2: 数据增强 / 上下文生成
*   **描述**：从 RHS 特征集中随机选取一个作为 LHS 输入，其余作为 RHS Label。
*   **数据格式**：同 Mode 1。
*   **场景**：
    -   给定一个标签，生成/检索相关的其他标签或属性。

### Mode 3: 图嵌入 / 链接预测
*   **描述**：从 RHS 特征集中随机选取两个，一个作为 LHS，一个作为 RHS。这意味着 RHS 中的所有特征在语义上是相互关联的。
*   **数据格式**：`node1 node2 node3 ...` (表示这些节点在图中相连或属于同一社群)
*   **场景**：
    -   社交网络分析：预测好友关系。
    -   知识图谱补全。

### Mode 4: 句子匹配 / 问答系统
*   **描述**：输入包含两个部分（通常用制表符或特定结构分隔）。第一个部分作为 LHS，第二个部分作为 RHS。
*   **数据格式**：`sentence1 \t sentence2`
*   **场景**：
    -   语义匹配：判断两句话是否意思相近。
    -   问答系统：LHS 是问题，RHS 是答案。
    -   跨语言检索：LHS 是中文句子，RHS 是对应的英文句子。

### Mode 5: 无监督词向量 (Word Embeddings)
*   **描述**：仅使用 LHS 数据。类似于 Word2Vec 或 FastText 的无监督训练。通过上下文预测中心词（或反之）。
*   **数据格式**：纯文本 `word1 word2 word3 ...`
*   **场景**：
    -   在没有标注数据的情况下，学习领域特定的词向量。
    -   作为下游任务的预训练特征。
