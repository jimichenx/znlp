package dev.znlp.starspace;

import dev.znlp.base.blas.DenseMatrix;
import dev.znlp.base.blas.DenseVector;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 模型训练器类，负责管理训练循环、多线程训练和模型优化
 * <p>
 * 该类处理负采样、梯度更新、学习率衰减等训练相关的核心功能。
 * 支持多线程训练，提高训练速度和效率。
 */
public class ModelTrainer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ModelTrainer.class);

    /** 训练参数 */
    private final Args args;
    /** 词典 */
    private final Dictionary dict;
    /** 数据解析器 */
    private final DataParser parser;
    /** 训练数据处理器 */
    private final DataHandler trainData;
    /** 是否开启调试模式 */
    private final boolean isDebug;

    /** 训练模型 */
    private final TrainModel trainModel;
    /** 左侧嵌入矩阵 */
    private final SparseLinear lhsEmbeddings;
    /** 左侧嵌入矩阵（密集表示） */
    private final DenseMatrix lhsEmbeddingMatrix;
    /** 右侧嵌入矩阵 */
    private final SparseLinear rhsEmbeddings;
    /** 右侧嵌入矩阵（密集表示） */
    private final DenseMatrix rhsEmbeddingMatrix;
    /** 左侧更新数组 */
    private final float[] lhsUpdates;
    /** 右侧更新数组 */
    private final float[] rhsUpdates;

    /**
     * 构造模型训练器
     * @param args 训练参数
     * @param trainFile 训练文件路径
     * @param validationFile 验证文件路径
     * @throws IOException IO异常
     */
    public ModelTrainer(Args args, String trainFile, String validationFile) throws IOException {
        this.args = args;
        this.isDebug = args.debug;

        logger.info("Start to initialize starspace model.");
        
        // 构建词典
        this.dict = new Dictionary(args);
        this.dict.readFromFile(trainFile);

        // 初始化解析器
        if (args.fileFormat == Args.FileFormat.FastText) {
            this.parser = new DataParser.FastTextDataParser(dict, args);
        } else {
            this.parser = new DataParser.LayerDataParser(dict, args);
        }

        // 初始化训练数据
        if (args.fileFormat == Args.FileFormat.FastText) {
            this.trainData = new DataHandler.InternDataHandler(parser, args);
        } else {
            this.trainData = new DataHandler.LayerDataHandler(parser, args);
        }
        this.trainData.loadFromFile(trainFile);

        // 初始化模型
        this.trainModel = new TrainModel(dict, args);
        this.lhsEmbeddings = trainModel.lhsEmbeddings;
        this.lhsEmbeddingMatrix = trainModel.lhsMatrix;
        this.rhsEmbeddings = trainModel.rhsEmbeddings;
        this.rhsEmbeddingMatrix = trainModel.rhsMatrix;
        this.lhsUpdates = trainModel.lhsUpdates;
        this.rhsUpdates = trainModel.rhsUpdates;
    }

    /**
     * 执行训练过程
     * @return 训练好的 StarSpace 模型
     * @throws Exception 训练过程中的异常
     */
    public StarSpace train() throws Exception {
        float rate = (float) args.lr;
        float decrPerEpoch = (float) ((rate - 1e-9) / args.epoch);
        long t_start = System.currentTimeMillis();

        for (int i = 0; i < args.epoch; i++) {
            logger.info("Training epoch {}: {} {}", i, String.format("%.3f", rate), String.format("%.3f", decrPerEpoch));

            float err = trainEpoch(trainData, args.thread, t_start, i, rate, rate - decrPerEpoch);

            logger.info("Epoch {} train error : {}", i, String.format("%.8f", err));

            rate -= decrPerEpoch;
            long totSpent = System.currentTimeMillis() - t_start;
            if (totSpent > args.maxTrainTime * 1000L) {
                logger.warn("MaxTrainTime exceeded.");
                break;
            }
        }

        return new StarSpace(dict, args, lhsEmbeddingMatrix, rhsEmbeddingMatrix);
    }

    /**
     * 训练一个 epoch
     * @param trainData 训练数据
     * @param numThreads 线程数
     * @param t_start 开始时间
     * @param epochs_done 已完成的 epoch 数
     * @param startRate 起始学习率
     * @param finishRate 结束学习率
     * @return 平均损失
     * @throws InterruptedException 线程中断异常
     */
    private float trainEpoch(DataHandler trainData, int numThreads, long t_start, int epochs_done, float startRate, double finishRate) throws InterruptedException {
        Utils.checkArgument(startRate >= finishRate);
        Utils.checkArgument(startRate >= 0.0f);

        final ReentrantLock rateLocker = new ReentrantLock();
        final float[] sharedRate = {startRate};

        int numSamples = trainData.size();
        List<Integer> indices = new ArrayList<>(numSamples);
        for (int i = 0; i < numSamples; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices);

        int kDecrStep = 1000;
        float decrPerKSample = (float) ((startRate - finishRate) / (numSamples * 1.0 / kDecrStep));
        int negSearchLimit = Math.min(numSamples, args.negSearchLimit);

        numThreads = Math.max(numThreads, 2) - 1; // Why -1? Original code: Math.max(numThreads, 2) - 1
        numThreads = Math.min(numThreads, numSamples);

        float[] losses = new float[numThreads];
        long[] counts = new long[numThreads];
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicBoolean doneTraining = new AtomicBoolean(false);

        int numPerThread = (int) Math.ceil((numSamples * 1.0f / numThreads));
        Utils.checkArgument(numPerThread > 0);

        // Partition indices
        List<List<Integer>> partition = new ArrayList<>();
        for (int i = 0; i < numSamples; i += numPerThread) {
            partition.add(indices.subList(i, Math.min(i + numPerThread, numSamples)));
        }
        
        // Ensure partition size matches numThreads (it might be less if numSamples is small)
        if (partition.size() < numThreads) {
             numThreads = partition.size();
        }

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            final List<Integer> threadIndex = partition.get(t);
            
            new Thread(() -> {
                boolean amMaster = (threadId == 0);
                long t_epoch_start = System.currentTimeMillis();
                losses[threadId] = 0.0f;
                counts[threadId] = 0;

                int lastP = threadIndex.size() - 1;
                for (int ip = 0; ip < threadIndex.size(); ip++) {
                    int i = threadIndex.get(ip);
                    float thisLoss;

                    if (args.trainMode == Args.TrainMode.Mode5 || args.trainWord) {
                        List<ParseResult> exs = new ArrayList<>();
                        trainData.getWordExamples(i, exs);
                        for (ParseResult ex : exs) {
                            thisLoss = trainOneExample(trainData, ex, negSearchLimit, sharedRate[0], true);
                            Utils.checkArgument(thisLoss >= 0.0);
                            counts[threadId]++;
                            losses[threadId] += thisLoss;
                        }
                    }

                    if (args.trainMode != Args.TrainMode.Mode5) {
                        ParseResult ex = new ParseResult();
                        trainData.getExampleById(i, ex);
                        thisLoss = trainOneExample(trainData, ex, negSearchLimit, sharedRate[0], false);
                        Utils.checkArgument(thisLoss >= 0.0);
                        counts[threadId]++;
                        losses[threadId] += thisLoss;
                    }

                    // update rate racily.
                    if (i % kDecrStep == kDecrStep - 1) {
                        rateLocker.lock();
                        try {
                            sharedRate[0] -= decrPerKSample;
                        } finally {
                            rateLocker.unlock();
                        }
                    }

                    if (amMaster && (ip % 100 == 99 || ip == lastP)) {
                        long t_end = System.currentTimeMillis();
                        long t_epoch_spent = t_end - t_epoch_start;
                        long ex_done_this_epoch = ip;
                        
                        long ex_left = (long)threadIndex.size() * (args.epoch - epochs_done) - ex_done_this_epoch;
                        double ex_done = (long) epochs_done * threadIndex.size() + ex_done_this_epoch;
                        double time_per_ex = t_epoch_spent * 1.0 / (ex_done_this_epoch + 1); // Avoid div by zero
                        long eta = (long) (time_per_ex * ex_left);
                        
                        long tot_spent = t_end - t_start;
                        if (tot_spent > args.maxTrainTime * 1000L) {
                            break;
                        }

                        double epoch_progress = ex_done_this_epoch * 1.0 / threadIndex.size();
                        double progress = ex_done / (ex_done + ex_left);
                        
                        if (eta > args.maxTrainTime * 1000L - tot_spent) {
                            eta = args.maxTrainTime * 1000L - tot_spent;
                             progress = tot_spent * 1.0 / (eta + tot_spent);
                        }
                        
                        eta /= 1000;
                        long etah = eta / 3600;
                        long etam = (eta - etah * 3600) / 60;
                        long etas = eta - etah * 3600 - etam * 60;
                        
                        long toth = tot_spent / 1000 / 3600;
                        long totm = (tot_spent / 1000 - toth * 3600) / 60;
                        long tots = tot_spent / 1000 - toth * 3600 - totm * 60;

                        if (logger.isDebugEnabled()) {
                            StringBuilder log = new StringBuilder();
                            log.append(String.format("Epoch: %.1f%%", 100 * epoch_progress));
                            log.append(String.format(" lr: %.6f", sharedRate[0]));
                            log.append(String.format(" loss: %.6f", losses[threadId] / (counts[threadId] + 1e-9))); // avoid div zero

                            if (eta < 60) {
                                log.append(" eta: <1min ");
                            } else {
                                log.append(" eta: ").append(etah).append("h").append(etam).append("m");
                            }
                            log.append(" tot: ").append(toth).append("h").append(totm).append("m").append(tots).append("s ");
                            log.append(" (").append(String.format("%2.1f", 100 * progress)).append("%)");

                            logger.debug("{}", log);
                        }
                    }
                }
                latch.countDown();
            }).start();
        }

        // Norm truncation thread
        Thread truncator = new Thread(() -> {
            int i = 0;
            while (!doneTraining.get()) {
                int wIdx = i % lhsEmbeddings.numRows();
                trunc(lhsEmbeddingMatrix.get(wIdx), args.norm);
                i++;
                try {
                    Thread.sleep(1); // yield
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        truncator.start();

        latch.await();
        doneTraining.set(true);
        truncator.join();

        float totLoss = 0f;
        for (float loss : losses) {
            totLoss += loss;
        }

        long totCount = 0;
        for (long count : counts) {
            totCount += count;
        }
        return totLoss / totCount;
    }

    private float trainOneExample(DataHandler data, ParseResult example, int negSearchLimit, float rate, boolean trainWord) {
        if (example.rhsTokens.isEmpty() || example.lhsTokens.isEmpty()) {
            return 0f;
        }

        if (isDebug && logger.isDebugEnabled()) {
            logger.debug("vec: {}", example.lhsTokens);
            logger.debug("vec: {}", example.rhsTokens);
        }

        float wRate = example.weight * rate;

        if (args.loss == Args.LossFunction.SoftMax) {
            return trainNLL(data, example.lhsTokens, example.rhsTokens, negSearchLimit, wRate, trainWord);
        } else {
            return trainOne(data, example.lhsTokens, example.rhsTokens, negSearchLimit, wRate, trainWord);
        }
    }

    private float trainOne(DataHandler data, List<Utils.XPair> items, List<Utils.XPair> labels, int negSearchLimit, float rate0, boolean trainWord) {
        if (items.isEmpty()) return 0.0f;

        Vector lhs = trainModel.projectLHS(items);
        check(lhs);
        int cols = lhs.length();

        Vector rhsP = trainModel.projectRHS(labels);
        check(rhsP);

        float posSim = args.similarity(lhs, rhsP);
        
        // Triple loss helper
        // max(0, margin - pos + neg)
        
        float loss = 0.0f;
        List<Vector> negs = new ArrayList<>();
        List<List<Utils.XPair>> negLabelsBatch = new ArrayList<>();
        Vector negMean = new DenseVector(cols);

        int i = 0;
        while (i < negSearchLimit && negs.size() < args.maxNegSamples) {
            List<Utils.XPair> negLabels = new ArrayList<>();
            do {
                data.getRandomRHS(negLabels, trainWord);
            } while (deepEquals(negLabels, labels));

            Vector rhsN = trainModel.projectRHS(negLabels);
            check(rhsN);

            float negSim = args.similarity(lhs, rhsN);
            float thisLoss = (float) Math.max(0, args.margin - posSim + negSim);
            // Clamp huge loss
            if (thisLoss > 10e7) thisLoss = (float) 10e7;

            if (thisLoss > 0.0) {
                loss += thisLoss;
                negs.add(rhsN);
                negLabelsBatch.add(negLabels);
                negMean.plusAssign(rhsN);
            }
            i++;
        }
        
        if (negs.isEmpty()) return 0.0f;

        loss /= negSearchLimit;
        negMean.divAssign((float) negs.size());

        if (Float.isInfinite(loss)) throw new RuntimeException("Loss is infinite");

        if (rate0 == 0f) return loss;

        // Gradient update
        // dE/dw = t- - t+
        // dE/dt- = w
        // dE/dt+ = -w
        
        // negMean += -1.0 * rhsP
        negMean.plusAssign(-1.0f, rhsP); // negMean = negMean - rhsP

        float nRate = rate0 / negs.size();
        float[] negRate = new float[negs.size()];
        Arrays.fill(negRate, nRate);

        backward(items, labels, negLabelsBatch, negMean, lhs, rate0, -rate0, negRate);

        return loss;
    }

    private float trainNLL(DataHandler data, List<Utils.XPair> items, List<Utils.XPair> labels, int negSearchLimit, float rate0, boolean trainWord) {
        if (items.isEmpty()) return 0f;

        Vector lhs = trainModel.projectLHS(items);
        check(lhs);
        Vector rhsP = trainModel.projectRHS(labels);
        check(rhsP);

        int numClass = args.negSearchLimit + 1;
        float[] prob = new float[numClass];
        List<Vector> negClassVec = new ArrayList<>();
        List<List<Utils.XPair>> negLabelsBatch = new ArrayList<>();

        prob[0] = lhs.prod(rhsP);
        float max = prob[0];

        for (int i = 1; i < numClass; i++) {
            List<Utils.XPair> negLabels = new ArrayList<>();
            do {
                data.getRandomRHS(negLabels, trainWord);
            } while (deepEquals(negLabels, labels));

            Vector rhsN = trainModel.projectRHS(negLabels);
            check(rhsN);
            negClassVec.add(rhsN);
            negLabelsBatch.add(negLabels);

            prob[i] = lhs.prod(rhsN);
            max = Math.max(prob[i], max);
        }

        float base = 0f;
        for (int i = 0; i < numClass; i++) {
            prob[i] = (float) Math.exp(prob[i] - max);
            base += prob[i];
        }

        for (int i = 0; i < numClass; i++) {
            prob[i] /= base;
        }

        float loss = (float) -Math.log(prob[0]);

        // Gradients
        // rhsP *= (prob[0] - 1)
        rhsP.timesAssign(prob[0] - 1);
        
        for (int i = 0; i < numClass - 1; i++) {
            // rhsP += prob[i+1] * negClassVec[i]
            rhsP.plusAssign(prob[i + 1], negClassVec.get(i));
        }

        float[] negRate = new float[numClass - 1];
        for (int i = 0; i < negRate.length; i++) {
            negRate[i] = prob[i + 1] * rate0;
        }
        
        backward(items, labels, negLabelsBatch, rhsP, lhs, rate0, (prob[0] - 1) * rate0, negRate);

        return loss;
    }

    private void backward(List<Utils.XPair> items, List<Utils.XPair> labels, List<List<Utils.XPair>> negLabels,
                          Vector gradW, Vector lhs, float rate_lhs, float rate_rhsP, float[] rate_rhsN) {
        
        int cols = lhs.length();
        float n1 = 0f;
        float n2 = 0f;

        if (args.adagrad) {
            n1 = gradW.prod(gradW); // gradW is actually gradient for LHS (it is sum(neg) - pos)
            n2 = lhs.prod(lhs);
        }

        // Update LHS
        for (Utils.XPair w : items) {
            Vector row = lhsEmbeddingMatrix.get(w.first);
            update(cols, row, gradW, rate_lhs * w.second, n1, lhsUpdates, w.first);
        }

        // Update positive RHS
        for (Utils.XPair la : labels) {
            Vector row = rhsEmbeddingMatrix.get(la.first);
            update(cols, row, lhs, rate_rhsP * la.second, n2, rhsUpdates, la.first);
        }

        // Update negative RHS
        for (int i = 0; i < negLabels.size(); i++) {
            List<Utils.XPair> laList = negLabels.get(i);
            float rate = rate_rhsN[i];
            for (Utils.XPair la : laList) {
                Vector row = rhsEmbeddingMatrix.get(la.first);
                update(cols, row, lhs, rate * la.second, n2, rhsUpdates, la.first);
            }
        }
    }

    private void update(int cols, Vector dest, Vector src, float rate, float weight, float[] adagradWeight, int idx) {
        if (args.adagrad) {
            Utils.checkArgument(idx < adagradWeight.length);
            adagradWeight[idx] += weight / cols;
            float rate1 = rate / (float) Math.sqrt(adagradWeight[idx] + 1e-6);
            dest.minusAssign(rate1, src);
        } else {
            dest.minusAssign(rate, src);
        }
    }

    private boolean deepEquals(List<Utils.XPair> a, List<Utils.XPair> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) return false;
        }
        return true;
    }

    private void check(Vector vector) {
        vector.check();
    }

    private void trunc(Vector row, double maxNorm) {
        float norm = args.norm2(row);
        if (norm > maxNorm) {
            row.timesAssign((float) (maxNorm / norm));
        }
    }
}
