# znlp-examples

可直接运行的示例与**模型训练脚本**。这里没有单元测试，也不参与 CI——训练动辄需要数 GB 语料和内存，只在本机按需执行。

所有脚本以仓库根目录为工作目录（Gradle 已配置），所以脚本里的 `data/...` 相对路径在任意终端下都成立。

## 运行一个类

```bash
./gradlew :znlp-examples:run -PmainClass=examples.dev.znlp.CwsPerceptronExample

# 需要传参时
./gradlew :znlp-examples:run \
  -PmainClass=train.examples.dev.znlp.NERPerceptronTrainer \
  -PappArgs="/path/to/ner-data"
```

`run` 任务默认堆上限 2G；模型资源来自 `:znlp-resources`（classpath），因此不依赖本机 `data/` 的示例可以直接跑。

## 目录

### `demo` —— 用法示例

| 类 | 说明 | 需要 `data/` |
| :--- | :--- | :---: |
| `CwsPerceptronExample` | 直接用感知机模型分词，不走流水线 | 否 |
| `DefaultSegmenterExample` | 默认分词器与词性标注 | 否 |
| `DucklingExample` | 时间/金额等结构化实体解析 | 否 |
| `FastTextExample` | FastText 分类推理 | 否 |
| `demo/PosExample` | 词性感知机独立调用 | 否 |
| `demo/PersonNamePerceptronLoadDemo` | 从 classpath 加载人名模型并识别 | 否 |
| `demo/PinyinSplitPerceptronLoadDemo` | 拼音串切分模型 | 否 |
| `demo/CustomDictionaryPluginExample` | 自定义词典插件接入 | 否 |
| `demo/NewWordDiscoveryDemo` | 从文本语料发现新词，导出 CSV | 是（`data/newword_test`） |
| `demo/WordnetStringDemo` | 打印词图（Wordnet）结构，排查切分问题 | 否 |
| `demo/ConfigLoaderDemo` / `demo/EmptyPropertyDemo` | 配置加载与空值处理 | 否 |
| `demo/ZeroCopyStringUsageExample` | 零拷贝字符串用法 | 否 |
| `demo/ProjectPathLocatorUsageExample` | 仓库路径定位工具 | 否 |
| `demo/ManualPerformanceDemo` | 新词计数器吞吐/内存手测 | 否 |

### `train` —— 训练与数据准备

| 类 | 作用 |
| :--- | :--- |
| `PersonNamePerceptronTrainer` | 人名识别感知机训练 + 压缩 + 写入资源 |
| `PinyinSplitPerceptronTrainer` | 拼音切分感知机训练 + 写入资源 |
| `NERPerceptronTrainer` | 命名实体识别感知机训练 |
| `PosDataPreparer` | 词性语料预处理（切分训练/测试集） |
| `PosPerceptronTrainer` | 词性标注感知机：训练 / 压缩评估 / 导出 |
| `DucklingModelTrainer` | Duckling 规则 + 朴素贝叶斯模型训练 |
| `EvaluateModelExample` | 加载已保存模型做离线评估 |
| `TrainingData` | Duckling 训练数据与固定测试时钟 |

## 重新训练

整体流程：**准备语料 → 训练 → 压缩 → 写入 `znlp-resources` → 跑测试确认切分未意外变化**。

| 模型 | 训练脚本 | 输入数据 | 产物 |
| :--- | :--- | :--- | :--- |
| 人名 | `PersonNamePerceptronTrainer` | `data/source/train/name-nr/train`（`/test` 可选） | `data/work/name-nr/model`；`znlp-resources/.../personname/nr-name.model` |
| 拼音切分 | `PinyinSplitPerceptronTrainer` | `data/source/train/pinyin-split/yuliao_train.txt`（`yuliao_test.txt` 可选） | `data/work/pinyin-split/model`；`.../pinyin/pinyin-split.model` |
| NER | `NERPerceptronTrainer` | 默认 `../znlp-data/data_train/ner/{train,test}`，也可用第一个参数指定 | `.../znlp/ner/` |
| 词性 | `PosDataPreparer` → `PosPerceptronTrainer` | `data/source/train/corpus/{cncorpus,pku}` → `data/work/pos.data/{train,test}` | `data/work/pos-model`；`.../znlp/pos-model/pos.model` |
| Duckling | `DucklingModelTrainer` | 代码内 `Grammars` 规则与样本 | `.../znlp/duckling.model` |

### 词性模型（三段式）

`PosPerceptronTrainer` 用第一个参数选择阶段，默认 `save`：

```bash
# 1) 语料 → data/work/pos.data（90% 训练 / 10% 测试，按 5 万行分片）
./gradlew :znlp-examples:run -PmainClass=train.examples.dev.znlp.PosDataPreparer

# 2) 训练（5 轮迭代，默认单线程，避免大语料 OOM）
./gradlew :znlp-examples:run -PmainClass=train.examples.dev.znlp.PosPerceptronTrainer -PappArgs=train

# 3) 压缩率扫描并评估
./gradlew :znlp-examples:run -PmainClass=train.examples.dev.znlp.PosPerceptronTrainer -PappArgs=compress

# 4) 压缩后写入 resources
./gradlew :znlp-examples:run -PmainClass=train.examples.dev.znlp.PosPerceptronTrainer -PappArgs=save
```

迭代轮数与线程数在各自 `main` 里以常量形式给出，需要调整时直接改代码：人名 23 轮、拼音切分 5 轮、NER 10 轮 4 线程、词性 5 轮 1 线程。多线程能提速，但内存占用会明显上升。

### NER 数据

NER 语料不在本仓库，默认读取与仓库同级的 `znlp-data/data_train/ner/`，目录下需有 `train/` 与 `test/`。换位置时直接用参数传入：

```bash
./gradlew :znlp-examples:run \
  -PmainClass=train.examples.dev.znlp.NERPerceptronTrainer \
  -PappArgs="/abs/path/to/ner"
```

## 训练后必须验证

模型文件直接服务于 `SegmentRecipe.perceptron()` 路径，训练结果会改变线上切分，所以每次重训后：

```bash
./gradlew :znlp:test
```

- 分词快照测试（`SegmentSnapshotTest`）会立刻暴露切分变化；失败信息给出实际输出，确属改进再更新 `znlp/src/test/resources/dev/znlp/segment/segment-snapshot.txt`。
- 想确认模型本身能正常解码，跑重资源用例：`./gradlew :znlp:test -PincludeTags=heavy`

## 注意

- `data/`（语料与中间产物）不入库，见 `.gitignore`；模型文件写入 `znlp-resources` 后**会进 git**，提交前留意体积（当前资源目录约 245MB）。
- 训练脚本会覆盖 `znlp-resources` 中的正式模型，建议先提交或在独立分支上跑。
- 语料缺失时脚本会直接报错退出并提示路径，不再静默跳过。
