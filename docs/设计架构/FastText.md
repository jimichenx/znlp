# FastText 文本分类与词向量

znlp 的 FastText 从 Facebook [fastText](https://github.com/facebookresearch/fastText)（对齐 v0.9.2 行为与 `.bin` 布局）迁到 Java：词/子词向量平均成文本表示，再接到分类或词向量损失。它**不进入 `Znlp` 门面**，由 `fasttext.dev.znlp.FastText` 直接训练、预测、存盘。

源码根包：`dev.znlp.fasttext`。包内 `README.md` 是类级导读；本文记设计约束与对齐点。

## 能做什么

| 任务 | `Args.model` | 常用损失 | 数据 |
| :--- | :--- | :--- | :--- |
| 文本分类 | `sup` | `softmax`（标签少）/ `hs`（标签多）/ `ova`（多标签） | 每行 `__label__xxx token token …` |
| Skip-gram 词向量 | `sg`（默认） | `ns` | 分好词的纯文本 |
| CBOW 词向量 | `cbow` | `ns` | 同上 |

分类输入应已分词。本模块不做中文切分。

## 调用入口

```java
Args args = new Args();
args.dim = 32;
args.epoch = 10;
args.lr = 0.1;
args.loss = LossName.softmax;
args.minCount = 1;
args.wordNgrams = 2;
args.minn = 3;   // 字符 n-gram，中文分类建议打开
args.maxn = 6;

FastText model = FastText.trainSupervised(new File("train.txt"), args);
List<ScoreLabelPair> top = model.predict(List.of("服务", "很好"), 2, 0.0f);
FastTextTestResult p1 = model.test(new File("test.txt"), 1, 0.0f, true);

model.saveModel(new File("model.bin"));
FastText loaded = FastText.loadModel(new File("model.bin"));
```

`trainSupervised` **只复制 `Args` 并把 `model` 设为 `sup`**，不改 lr / loss / minCount，也**不关闭** `minn`/`maxn`。这和官方 CLI 的 `supervised` 命令不同：官方会默认 `minn=maxn=0`。中文分类通常需要子词，所以这里保留调用方设置。

词向量走 `FastText.train(file, args)`，自行设 `model=sg|cbow`。

`k == Model.kUnlimitedPredictions`（-1）表示返回全部过阈值的标签。

可运行示例：`./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.FastTextExample`

## 流水线

```
语料文件
  │
  ▼
FileSampleLineIterable          <50MB 进内存；更大则按线程拆临时文件
  │
  ▼
DictionaryBuilder               扫描词/标签，频次裁剪，哈希表塌缩
  │
  ▼
wi / wo 矩阵                    wi: nwords+bucket × dim；wo: nlabels 或 nwords × dim
  │
  ▼
FastTextTrain（Hogwild）        每线程 State；无锁写共享矩阵
  │  sup: 平均词+子词+词 n-gram → 更新标签
  │  cbow / skipgram: 窗口内更新中心词或上下文
  ▼
FastText(args, dict, model)
```

预测（仅 `sup`）：

```
tokens + </s>
  → Dictionary.getLine（词 ID、OOV 子词、词 n-gram）
  → hidden = 输入行向量平均
  → Loss.predict → TopKScoreHeap
  → exp(score) + 标签名
```

## 表示：词、子词、哈希桶

隐藏层是输入特征行的平均，不是 RNN/Transformer。

一条分类样本的输入 ID 包括：

1. 词表内词的 ID（`maxn>0` 时再叠该词的字符 n-gram）
2. OOV 词只走 `BOW + token + EOW` 的字符 n-gram
3. `wordNgrams>1` 时的词级 n-gram（哈希进 bucket）

字符 n-gram 按 **UTF-8 字符边界**切（跳过 continuation byte），再对这段 UTF-8 字节做 FNV-1a。异或用有符号 `int8_t`，与官方「已发布模型按 signed char 哈希」一致。`HashUtils.fnv1aHash(String)` 固定 `UTF-8`，不跟平台默认编码走。

n-gram 不单独占词表：`h % bucket` 映射到 `wi` 的 `nwords + h` 行。默认 `bucket=2_000_000`，占内存的大头之一。

## 训练

多线程 Hogwild：共享 `wi`/`wo`，不加锁，和 C++ 一样。学习率线性衰减：`lr * (1 - progress)`。

| 模式 | 一次 update |
| :--- | :--- |
| `sup` | 整句特征平均 → 对一个标签（`ova` 则对全部标签） |
| `cbow` | 窗口内上下文（含子词）预测中心词 |
| `skipgram` | 中心词子词预测窗口内各上下文词 |

`<50MB` 的文件读进内存再按线程切；更大则 `splitMutiFiles` 出临时文件，训练结束删除。`LoopReader` 在 epoch 间循环同一数据源，中途停训会关掉 Reader。

监督数据没有 `__label__` 前缀时，`nlabels==0`，训练直接失败。负采样需要至少两个正计数目标，否则构造期抛错。

`Args` 上的 `preTrainedVectors`、autotune 字段**没有接到训练路径**。

## 损失与预测 Top-k

`LossFactory` 按 `args.loss` 建损失。分层 Softmax 的霍夫曼树、负采样表都从词典频次重建，因此 **`.bin` 不必序列化树**。

| 损失 | 场景 |
| :--- | :--- |
| `softmax` | 标签少，算全体类别 |
| `hs` | 标签多，沿树 O(log C) |
| `ns` | 词向量；分类也可用但不常见 |
| `ova` | 多标签，每个类别一个二分类 |

`predict` / `test` 的 top-k 走 `TopKScoreHeap`（容量 k 的最小堆）。标签很多或 `k=-1` 时，避免「每插入一次就全排序」。酒店评论那种两三类、k=1 的路径，收益可以忽略。

`test()` 按官方公式：

- P@k = nfound / (k × nexamples)
- R@k = nfound / nlabels

`nfound` 是 top-k 里命中的金标签数（累计，不是按名次分桶）。返回 `FastTextTestResult`。

对外分数是 `exp(stdLog(p))`，即 `p+1e-5` 量级，不是精确概率。

## 模型文件

`saveModel` / `loadModel` 对齐官方非量化 `.bin`（小端）：

```
magic 793712314
version 12
Args（dim…t，不含 lr/label/thread）
Dictionary（C 字符串词、count、type、pruneidx）
quant_input: false
DenseMatrix wi（int64 行、列 + float 行优先）
qout: false
DenseMatrix wo
```

量化 `.ftz` 会拒绝。version 11 的监督模型加载时把 `maxn` 置 0（官方兼容）。剪枝过的非量化模型同样拒绝。

加载后再用词典频次重建 HS 树 / 负采样表，不读树结构。

## 和官方 CLI 的差异（有意为之）

- `trainSupervised` 不默认关掉字符 n-gram。
- 不实现 quantize、autotune、预训练向量注入、`getWordVector` / 近邻 / analog。无监督训完只能自己读 `model.wi()`。
- Hogwild 无锁，`thread>1` 时结果不可复现；要对齐实验把 `thread=1` 并固定 `seed`。

## 源码地图

```
fasttext/
  FastText.java              训练 / 预测 / test / save / load
  FastTextTestResult.java    P@k、R@k
  Model.java                 hidden 平均、update、State
  ScoreIdPair / ScoreLabelPair
  args/                      Args、ModelName、LossName
  dictionary/                Dictionary、DictionaryBuilder、FastWordMap
  loss/                      Softmax / HS / NS / OVA、TopKScoreHeap
  train/                     FastTextTrain、FileSampleLineIterable、LoopReader
  utils/                     HashUtils、LittleEndianData、AutoDataInput
```

矩阵在 `dev.znlp.base.blas`（`DenseArrayMatrix`）。

## 如何改一处行为

- 换损失：只改 `Args.loss`，不要在 `trainSupervised` 里写死。
- 中文分类效果差：先确认输入已分词，再打开 `minn`/`maxn` 和 `wordNgrams=2`。
- 要加载别人的官方 `.bin`：哈希与子词必须保持 UTF-8 + signed byte；改 `HashUtils` / `computeSubwords` 会让旧模型对不齐。
- 加一种新损失：实现 `Loss`，在 `LossFactory` 注册，并保证 `predict` 仍写入 `List<ScoreIdPair>`（分数为 log 域）。
