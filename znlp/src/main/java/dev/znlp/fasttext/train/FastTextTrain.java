package dev.znlp.fasttext.train;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.fasttext.FastText;
import dev.znlp.fasttext.Model;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.LossName;
import dev.znlp.fasttext.args.ModelName;
import dev.znlp.fasttext.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FastText模型训练类，负责管理FastText模型的训练过程。
 * 支持多线程训练、动态学习率调整、损失计算和训练进度监控，
 * 实现了CBOW、skipgram和监督学习三种训练模式。
 */
public class FastTextTrain {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FastTextTrain.class);

    /**
     * 模型参数配置
     */
    private final Args args;
    
    /**
     * FastText实例
     */
    private final FastText fastText;
    
    /**
     * 词典对象
     */
    private final Dictionary dict;

    /**
     * 已处理的词数计数器
     */
    private final AtomicLong tokenCount = new AtomicLong(0);
    
    /**
     * 共享的损失值
     */
    private final ShareDouble loss = new ShareDouble(-1.0);
    
    /**
     * 训练开始时间
     */
    private long startTime;
    
    /**
     * 训练过程中发生的异常
     */
    private volatile Exception trainException = null;

    /**
     * 要处理的总词数
     */
    private final long wantProcessTotalTokens;

    /**
     * 构造FastTextTrain实例
     *
     * @param args 模型参数配置
     * @param fastText FastText实例
     */
    public FastTextTrain(Args args, FastText fastText) {
        this.args = args;
        this.fastText = fastText;
        this.dict = fastText.dict();
        this.wantProcessTotalTokens = (long) args.epoch * dict.ntokens;
    }

    /**
     * 检查是否继续训练
     *
     * @return 是否继续训练
     */
    private boolean keepTraining() {
        return tokenCount.get() < wantProcessTotalTokens && trainException == null;
    }

    /**
     * 获取训练进度
     *
     * @return 训练进度，范围为0.0到1.0
     */
    private float progress() {
        return (float) (tokenCount.get()) / wantProcessTotalTokens;
    }

    /**
     * 启动多线程训练
     *
     * @param sources 训练数据迭代器列表，每个迭代器对应一个线程的训练数据
     */
    public void startThreads(List<Iterable<SampleLine>> sources) {
        startTime = System.currentTimeMillis();
        int threadCount = sources.size();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            threads.add(new Thread(new TrainThread(i, sources.get(i))));
        }

        for (Thread t : threads) {
            t.start();
        }

        while (keepTraining()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
            if (loss.get() >= 0) {
                float progress = progress();
                printInfo(progress, loss, false);
            }
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("等待训练线程结束被中断", e);
            }
        }

        if (trainException != null) {
            throw new RuntimeException(trainException);
        }

        printInfo(1.0f, loss, true);
        logger.info("Train use time {} ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 打印训练信息
     *
     * @param progress 训练进度
     * @param loss 损失值
     * @param stop 是否停止训练
     */
    private void printInfo(float progress, ShareDouble loss, boolean stop) {
        double t = (System.currentTimeMillis() - startTime) / 1000.0;
        double lr = args.lr * (1.0 - progress);
        double wst = 0.0;
        long eta = 720 * 3600; // Default 1 month

        if (progress > 0 && t >= 0) {
            eta = (long) (t * (1.0f - progress) / progress);
            wst = (double) tokenCount.get() / t / args.thread;
        }

        long etah = eta / 3600;
        long etam = eta % 3600 / 60;
        long etas = eta % 3600 % 60;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Progress: %2.2f%% words/sec/thread: %8.0f", progress * 100, wst));
        if (!stop) {
            sb.append(String.format(" lr: %2.5f", lr));
        }
        sb.append(String.format(" arg.loss: %2.5f", loss.get()));
        if (!stop) {
            sb.append(" ETA: " + etah + "h " + etam + "m " + etas + "s");
        }
        if (stop) {
            logger.info("{}", sb);
        } else if (logger.isDebugEnabled()) {
            logger.debug("{}", sb);
        }
    }

    /**
     * 训练线程类，负责单个线程的训练逻辑
     */
    private class TrainThread implements Runnable {
        /**
         * 线程ID
         */
        private final int threadId;
        
        /**
         * 训练数据迭代器
         */
        private final Iterable<SampleLine> parts;
        
        /**
         * 模型状态
         */
        private final Model.State state;
        
        /**
         * 本地词数计数器
         */
        private long localTokenCount = 0;

        /**
         * 构造TrainThread实例
         *
         * @param threadId 线程ID
         * @param parts 训练数据迭代器
         */
        public TrainThread(int threadId, Iterable<SampleLine> parts) {
            this.threadId = threadId;
            this.parts = parts;
            this.state = new Model.State(args.dim, fastText.getOutput().getRow(), args.seed + threadId);
        }

        /**
         * 线程运行方法，执行训练逻辑
         */
        @Override
        public void run() {
            LoopReader reader = new LoopReader(parts);
            int emptyCount = 0;
            IntArrayList line = new IntArrayList();
            IntArrayList labels = new IntArrayList();

            try {
                while (keepTraining()) {
                    float progress = progress();
                    float lr = (float) (args.lr * (1.0 - progress));

                    if (reader.hasNext()) {
                        SampleLine sample = reader.next();
                        if (sample.words().isEmpty()) {
                            emptyCount++;
                            if (emptyCount > 1000) break;
                            continue;
                        } else {
                            emptyCount = 0;
                            List<String> tokens = sample.words();
                            
                            if (args.model == ModelName.sup) {
                                localTokenCount += dict.getLine(tokens, line, labels);
                                supervised(state, fastText.model(), lr, line, labels);
                            } else if (args.model == ModelName.cbow) {
                                localTokenCount += dict.getLine(tokens, line, state.rng);
                                cbow(state, fastText.model(), lr, line);
                            } else if (args.model == ModelName.sg) {
                                localTokenCount += dict.getLine(tokens, line, state.rng);
                                skipgram(state, fastText.model(), lr, line);
                            }

                            if (localTokenCount > args.lrUpdateRate) {
                                tokenCount.addAndGet(localTokenCount);
                                localTokenCount = 0;
                                if (threadId == 0) {
                                    loss.set(state.getLoss());
                                }
                            }
                        }
                    } else {
                        break; // Should not happen with LoopReader unless empty
                    }
                }
            } catch (Exception e) {
                trainException = e;
            } finally {
                reader.close();
            }
        }

        /**
         * 执行有监督训练
         *
         * @param state 模型状态
         * @param model 模型对象
         * @param lr 学习率
         * @param line 词ID列表
         * @param labels 标签ID列表
         */
        private void supervised(Model.State state, Model model, float lr, IntArrayList line, IntArrayList labels) {
            if (labels.size() == 0 || line.size() == 0) return;
            if (args.loss == LossName.ova) {
                model.update(line, labels, Model.kAllLabelsAsTarget, lr, state);
            } else {
                int i = state.rng.nextInt(labels.size());
                model.update(line, labels, i, lr, state);
            }
        }

        /**
         * 执行CBOW训练
         *
         * @param state 模型状态
         * @param model 模型对象
         * @param lr 学习率
         * @param line 词ID列表
         */
        private void cbow(Model.State state, Model model, float lr, IntArrayList line) {
            IntArrayList bow = new IntArrayList();
            int ws = Math.max(args.ws, 1);
            for (int w = 0; w < line.size(); w++) {
                int boundary = state.rng.nextInt(ws) + 1;
                bow.clear();
                for (int c = -boundary; c <= boundary; c++) {
                    if (c != 0 && w + c >= 0 && w + c < line.size()) {
                        IntArrayList ngrams = dict.getSubwords(line.get(w + c));
                        bow.addAll(ngrams);
                    }
                }
                model.update(bow, line, w, lr, state);
            }
        }

        /**
         * 执行skipgram训练
         *
         * @param state 模型状态
         * @param model 模型对象
         * @param lr 学习率
         * @param line 词ID列表
         */
        private void skipgram(Model.State state, Model model, float lr, IntArrayList line) {
            int ws = Math.max(args.ws, 1);
            for (int w = 0; w < line.size(); w++) {
                int boundary = state.rng.nextInt(ws) + 1;
                IntArrayList ngrams = dict.getSubwords(line.get(w));
                for (int c = -boundary; c <= boundary; c++) {
                    if (c != 0 && w + c >= 0 && w + c < line.size()) {
                        model.update(ngrams, line, w + c, lr, state);
                    }
                }
            }
        }
    }

    /**
     * 共享双精度值类，用于在多线程间共享损失值
     */
    private static class ShareDouble {
        /**
         * 共享的双精度值
         */
        private volatile double value;

        /**
         * 构造ShareDouble实例
         *
         * @param value 初始值
         */
        public ShareDouble(double value) {
            this.value = value;
        }

        /**
         * 获取共享值
         *
         * @return 共享值
         */
        public double get() {
            return value;
        }

        /**
         * 设置共享值
         *
         * @param value 新值
         */
        public void set(double value) {
            this.value = value;
        }
    }
}
