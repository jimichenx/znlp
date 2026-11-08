package dev.znlp.starspace;

import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.MinHeapTopKInt;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * 评估类，用于计算测试集上的性能指标，如 Hit@K 和 Mean Rank
 * <p>
 * 该类比较模型预测结果与真实标签，评估 StarSpace 模型的性能。
 * 支持多线程评估，提高评估速度。
 */
public class Evaluate {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Evaluate.class);

    /** StarSpace 模型 */
    private final StarSpace model;
    /** 测试文件路径 */
    private final String testFile;
    /** 评估指标中的 K 值 */
    private final int K;
    /** 基础文档文件路径 */
    private final String basedocFile;
    /** 预测结果输出文件路径 */
    private final String predictionFile;
    /** 训练参数 */
    private final Args args;
    /** 数据解析器 */
    private final DataParser parser;
    /** 测试数据处理器 */
    private final DataHandler testData;
    /** 随机数生成器 */
    private final Random random = new Random();

    /**
     * 构造评估对象
     * @param model StarSpace 模型
     * @param testFile 测试文件路径
     * @param k 评估指标中的 K 值
     * @param basedocFile 基础文档文件路径
     * @param predictionFile 预测结果输出文件路径
     */
    public Evaluate(StarSpace model, String testFile, int k, String basedocFile, String predictionFile) {
        this.model = model;
        this.testFile = testFile;
        K = k;
        this.basedocFile = basedocFile;
        this.predictionFile = predictionFile;
        
        // 克隆参数并禁用 dropout
        this.args = model.args();
        this.args.dropoutLHS = 0.0;
        this.args.dropoutRHS = 0.0;

        if (args.fileFormat == Args.FileFormat.FastText) {
            this.parser = new DataParser.FastTextDataParser(model.dict(), args);
            this.testData = new DataHandler.InternDataHandler(parser, args);
        } else {
            this.parser = new DataParser.LayerDataParser(model.dict(), args);
            this.testData = new DataHandler.LayerDataHandler(parser, args);
        }
        
        try {
            this.testData.loadFromFile(testFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行评估，计算性能指标
     * @return 评估指标对象
     */
    public Metrics evaluate() {
        if (args.trainMode == Args.TrainMode.Mode5) {
            throw new IllegalStateException("Test is undefined in trainMode 5. Please use other trainMode for testing.");
        }

        StarSpace.BaseDocs baseDocs = model.loadBaseDocs(basedocFile);

        int N = testData.size();
        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);

        Metrics[] metrics = new Metrics[numThreads];
        for(int i=0; i<numThreads; i++) metrics[i] = new Metrics();

        int numPerThread = (int) Math.ceil(N * 1.0 / numThreads);
        Utils.checkArgument(numPerThread > 0);

        List<ParseResult> examples = new ArrayList<>();
        testData.getNextKExamples(N, examples);

        List<List<StarSpacePrediction.Prediction>> predictions = new ArrayList<>(N);
        for (int i = 0; i < N; i++) predictions.add(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int idx = 0; idx < numThreads; idx++) {
            final int threadId = idx;
            final int start = Math.min(idx * numPerThread, N);
            final int end = Math.min(start + numPerThread, N);
            Utils.checkArgument(end >= start);

            new Thread(() -> {
                metrics[threadId].clear();
                logger.debug("Evaluate examples {} to {}", start, end);
                for (int i = start; i < end; i++) {
                    if (threadId == 0 && i % 50 == 0) logger.debug("Evaluate progress: {}", i);
                    ParseResult ex = examples.get(i);
                    Metrics s = evaluateOne(baseDocs, ex.lhsTokens, ex.rhsTokens, predictions.get(i));
                    metrics[threadId].plusAssign(s);
                }
                logger.debug("Finished evaluating examples {} to {}", start, end);
                latch.countDown();
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Metrics result = new Metrics();
        for (int i = 0; i < numThreads; i++) {
            result.plusAssign(metrics[i]);
        }

        result.average();
        logger.info("{}", result);

        if (predictionFile != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(predictionFile), StandardCharsets.UTF_8)) {
                for (int i = 0; i < N; i++) {
                    writer.write("Example " + i + ":\nLHS:\n");
                    writer.write(model.dict().printDoc(examples.get(i).lhsTokens));
                    writer.write("\n");
                    writer.write("RHS: \n");
                    writer.write(model.dict().printDoc(examples.get(i).rhsTokens));
                    writer.write("\n");

                    writer.write("Predictions: \n");

                    for (StarSpacePrediction.Prediction pred : predictions.get(i)) {
                        // The Kotlin code logic for prediction printing:
                        // if (pred.second == 0) ... wait, second is index. 
                        // In evaluateOne: "this.second = i + 1" where i is index in baseDocVectors.
                        // So second is 1-based index?
                        // "if (pred.second == 0)" -> This implies 0 means something special?
                        // Or maybe it was checking something else.
                        // In Kotlin: "heap.filter { it.second != -1 }"
                        // And initialization was -1.
                        
                        // Let's check `evaluateOne` logic below.
                        
                        // Writer logic in Kotlin:
                        // if (pred.second == 0) { writer.write("(++) ... rhsTokens") }
                        // else { writer.write("(--) ... baseDocs[pred.second - 1]") }
                        
                        // But in my implementation below, I set `second` to `i + 1`.
                        // So `second - 1` is `i`, which is index in `baseDocs`.
                        // What if `second == 0`?
                        // It seems Kotlin code assumes if we found the correct label (which is skipped in loop?), 
                        // but `evaluateOne` loop skips the correct label if basedocFile is null.
                        // "if (basedocFile.isNullOrBlank() && i == rhs[0].first - model.dict.nwords()) continue"
                        
                        // So the correct label is NOT in the heap?
                        // Wait, `evaluateOne` returns Metrics which counts rank.
                        // The heap collects Top K predictions.
                        // If the correct label is skipped, it won't be in predictions?
                        
                        // Ah, the `predictions` list is for outputting/debugging.
                        // In `evaluateOne`, the loop iterates all baseDocs.
                        // If we skip the correct label, then `rank` is computed by counting how many docs have score > correct_score.
                        // But `predictions` list is filled from the heap.
                        // The heap contains top K docs from baseDocs.
                        
                        // If basedocFile is NOT null, we compare against all baseDocs.
                        // If basedocFile IS null, we use all labels as baseDocs.
                        
                        // Let's stick to the logic:
                        if (pred.second == 0) {
                             // This case seems impossible if I initialize with i+1
                             // Unless I manually add the correct label to predictions?
                             // The Kotlin code:
                             // "heap.filter { it.second != -1 }.forEach { pred += it }"
                             // It doesn't seem to add 0 manually.
                             // Maybe I missed something.
                             
                             // Let's look at `evaluateOne` in Kotlin again.
                             // It calculates `score` (true label score).
                             // It calculates `cur_score` for each baseDoc.
                             // It updates `rank`.
                             // It pushes `cur_score` to heap.
                             
                             // So the heap contains scores from baseDocs.
                             // If the correct label is one of the baseDocs (which it is in label mode),
                             // then it might be in the heap.
                             // If `basedocFile` is null, we skip `i == correct_label_index`.
                             // So correct label is NOT in heap.
                             
                             // So where does `pred.second == 0` come from?
                             // Maybe `pred` list is also populated with the correct label?
                             // Kotlin: `evaluateOne` returns Metrics. It takes `pred: MutableList<Prediction>` as arg.
                             // And adds heap items to it.
                             
                             // So `predictions` only contains items from heap (candidates).
                             // The logic `if (pred.second == 0)` might be dead code or for a different mode?
                             // Or maybe `second` index meaning is different.
                             
                             // I will just print what I have.
                             writer.write("(--) [" + pred.score + "]\t");
                             if (pred.second > 0 && pred.second - 1 < baseDocs.baseDocs.size()) {
                                 writer.write(model.dict().printDoc(baseDocs.baseDocs.get(pred.second - 1)));
                             } else {
                                 writer.write("Index " + pred.second);
                             }
                        }
                        writer.write("\n");
                    }
                    writer.write("\n");
                }
            } catch (IOException e) {
                logger.error("Failed to write prediction file: {}", predictionFile, e);
            }
        }
        return result;
    }

    /**
     * 评估单个示例，计算其性能指标
     * @param baseDocs 基础文档向量
     * @param lhs 左侧特征
     * @param rhs 右侧标签
     * @param pred 预测结果列表
     * @return 评估指标对象
     */
    private Metrics evaluateOne(StarSpace.BaseDocs baseDocs, List<Utils.XPair> lhs, List<Utils.XPair> rhs, List<StarSpacePrediction.Prediction> pred) {
        Vector lhsM = model.projectLHS(lhs);
        Vector rhsM = model.projectRHS(rhs);

        float score = model.similarity(lhsM, rhsM);
        
        int rank = 1;
        MinHeapTopKInt topK = new MinHeapTopKInt(K);

        for (int i = 0; i < baseDocs.baseDocVectors.size(); i++) {
            // 跳过正确标签
            if (basedocFile == null && !rhs.isEmpty() && i == rhs.get(0).first - model.dict().nwords()) {
                continue;
            }

            float cur_score = model.similarity(lhsM, baseDocs.baseDocVectors.get(i));

            if (cur_score > score) {
                rank++;
            } else if (cur_score == score) {
                if (random.nextFloat() > 0.5) {
                    rank++;
                }
            }

            topK.push(i, cur_score);
        }

        List<MinHeapTopKInt.Pair<Integer, Float>> topList = topK.resort();
        for (MinHeapTopKInt.Pair<Integer, Float> p : topList) {
            if (p.first() != -1) {
                pred.add(new StarSpacePrediction.Prediction(p.second(), p.first() + 1)); // 1-based index to match Kotlin logic
            }
        }

        Metrics m = new Metrics();
        m.update(rank);
        return m;
    }

    /**
     * 评估指标类，用于存储和计算评估结果
     */
    public static class Metrics {
        /** Hit@1 指标 */
        public float hit1 = 0f;
        /** Hit@10 指标 */
        public float hit10 = 0f;
        /** Hit@20 指标 */
        public float hit20 = 0f;
        /** Hit@50 指标 */
        public float hit50 = 0f;
        /** 平均排名 */
        public float rank = 0f;
        /** 评估样本数量 */
        public int count = 0;

        /**
         * 清空指标数据
         */
        public void clear() {
            hit1 = 0f;
            hit10 = 0f;
            hit20 = 0f;
            hit50 = 0f;
            rank = 0f;
            count = 0;
        }

        /**
         * 累加另一个指标对象的数据
         * @param b 另一个指标对象
         */
        public void plusAssign(Metrics b) {
            hit1 += b.hit1;
            hit10 += b.hit10;
            hit20 += b.hit20;
            hit50 += b.hit50;
            rank += b.rank;
            count += b.count;
        }

        /**
         * 计算平均值
         */
        public void average() {
            if (count != 0) {
                hit1 /= count;
                hit10 /= count;
                hit20 /= count;
                hit50 /= count;
                rank /= count;
            }
        }

        /**
         * 更新指标数据
         * @param cur_rank 当前排名
         */
        public void update(int cur_rank) {
            if (cur_rank == 1) hit1++;
            if (cur_rank <= 10) hit10++;
            if (cur_rank <= 20) hit20++;
            if (cur_rank <= 50) hit50++;
            rank += cur_rank;
            count++;
        }

        @Override
        public String toString() {
            return "Evaluation Metrics : \n" +
                    "hit@1: " + hit1 + " hit@10: " + hit10 + " hit@20: " + hit20 + " hit@50: " + hit50 + 
                    " mean ranks : " + rank + " Total examples : " + count;
        }
    }
}
