package dev.znlp.perceptron.train;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.perceptron.*;
import dev.znlp.perceptron.feature.FeatureIdSeek;
import dev.znlp.perceptron.feature.FeatureSetBuilder;
import dev.znlp.perceptron.feature.ImmutableFeatureSetWithKeys;
import dev.znlp.perceptron.utils.FastStringBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * 感知机训练工具
 * <p>
 * 该类负责从训练文件中构建感知机模型，包括：
 * <ul>
 *   <li>特征集合构建：从训练样本中提取所有特征并构建FeatureSet</li>
 *   <li>训练样本加载：将训练文件中的文本转换为TrainSample格式</li>
 *   <li>模型训练：使用内置的训练算法进行模型训练</li>
 *   <li>训练过程评估：在训练过程中对模型进行评估（如果提供了评估文件）</li>
 * </ul>
 * <p>
 * 训练流程：
 * <ol>
 *   <li>读取训练文件：支持单个文件或目录（递归读取所有非隐藏文件）</li>
 *   <li>构建特征集合：遍历所有训练样本，提取特征并构建FeatureSet</li>
 *   <li>加载训练样本：将训练文本转换为TrainSample格式</li>
 *   <li>执行训练：使用内置的训练算法进行迭代训练</li>
 *   <li>返回模型：返回训练完成的感知机模型</li>
 * </ol>
 *
 * @param <E>             序列元素类型，一般为char或String
 * @param <InputSequence> 输入序列类型，可能是char[]或List
 * @author jimi
 */
public class PerceptronTrainer<E, InputSequence> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PerceptronTrainer.class);

    private final Map<String, Integer> labelMap;
    private final String[] labels;
    private final int featureMaxSize;
    private final AnnotateTextParser<E, InputSequence> annotateTextParser;
    private final FeatureExtractor<InputSequence> featureFunction;
    private final EvaluateFunctionFactory evaluateFunctionFactory;

    private final File workingDir;


    private final PerceptronInstance<E, InputSequence> instance;


    public PerceptronTrainer(
            PerceptronInstance<E, InputSequence> instance,
            File workingDir
    ) {
        this.instance = instance;
        this.labels = instance.getLables();
        this.featureMaxSize = instance.getFeatureMaxSize();
        this.annotateTextParser = instance.getAnnotateTextParser();
        this.featureFunction = instance.getFeatureFun();
        this.evaluateFunctionFactory = instance.getEvaluateFunction();

        this.labelMap = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            labelMap.put(labels[i], i);
        }

        this.workingDir = workingDir;
    }

    /**
     * 训练感知机模型
     * <p>
     * 从训练文件中读取样本，构建特征集合，加载训练样本，然后使用训练算法进行模型训练。
     * 如果提供了评估文件，会在每轮迭代后对模型进行评估。
     *
     * @param trainFile    训练文件或目录
     * @param evaluateFile 评估文件或目录（可选，为null时不进行评估）
     * @param iter         最大迭代次数
     * @param threadNum    训练线程数（1表示单线程平均感知机，>1表示多线程并行训练）
     * @param quickDecode  是否使用快速解码模式
     * @return 训练完成的感知机模型
     * @throws Exception 如果文件读取或训练过程中发生错误
     */
    public PerceptronTrainModel train(File trainFile, File evaluateFile, int iter, int threadNum, boolean quickDecode) throws Exception {
        List<File> trainFiles = files(trainFile);

        //构建FeatureSet
        ImmutableFeatureSetWithKeys featureSet = buildFeatureSet(trainFiles);

        // 加载训练样本
        List<TrainSample> sampleList = loadTrainSamples(trainFiles, featureSet);

        // 加载评估样本
        List<EvaluationSample> evaluationSampleList = evaluateFile != null
                ? PerceptronEvaluator.loadEvaluateSamples(instance, evaluateFile, workingDir, featureSet)
                : new ArrayList<>();

        logger.info("Start train ...");

        // 创建评估回调对象
        TrainingEvaluation evaluationCallback = new TrainingEvaluation(
                evaluateFunctionFactory,
                evaluationSampleList
        );

        return trainInner(featureSet, labels.length, sampleList, evaluationCallback, iter, quickDecode, threadNum);
    }

    /**
     * 构建特征集，支持从缓存读取或重新构建
     *
     * @param trainFiles 训练文件列表
     * @return 特征集
     * @throws IOException 如果文件读取过程中发生错误
     */
    private ImmutableFeatureSetWithKeys buildFeatureSet(List<File> trainFiles) throws IOException {
        logger.info("开始构建FeatureSet");
        long t1 = System.currentTimeMillis();

        ImmutableFeatureSetWithKeys featureSet;

        // 检查缓存文件是否存在
        File featureCacheDir = getFeatureCacheDir();

        if (featureCacheDir != null && new File(featureCacheDir, "feature.dat").exists()
                && new File(featureCacheDir, "feature.txt").exists()) {
            // 从缓存读取
            logger.info("从缓存文件读取FeatureSet: {}", featureCacheDir.getAbsolutePath());
            try {
                featureSet = ImmutableFeatureSetWithKeys.readFromDir(featureCacheDir);
                logger.info("读取FeatureSet耗时 {} MS, 包含{}个特征",
                        System.currentTimeMillis() - t1, featureSet.size());
                return featureSet;
            } catch (IOException e) {
                logger.warn("读取缓存文件失败，重新构建: {}", e.getMessage());
            }
        }

        // 构建 FeatureSet
        final FeatureSetBuilder featureSetBuilder = new FeatureSetBuilder(labels.length);
        final FastStringBuilder buffer = new FastStringBuilder(1024);
        for (File file : trainFiles) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    InputSequence inputSeq = annotateTextParser.parseToInputList(line);

                    if (inputSeq instanceof char[]) {
                        int len = ((char[]) inputSeq).length;
                        for (int i = 0; i < len; i++) {
                            final int pos = i;
                            featureFunction.compute(inputSeq, pos, buffer, (f) -> {
                                featureSetBuilder.put(f);
                                buffer.clear();
                            });
                        }

                    } else if (inputSeq instanceof List) {
                        int len = ((List) inputSeq).size();
                        for (int i = 0; i < len; i++) {
                            final int pos = i;
                            featureFunction.compute(inputSeq, pos, buffer, (f) -> {
                                featureSetBuilder.put(f);
                                buffer.clear();
                            });
                        }
                    }
                }
            }
        }
        featureSet = featureSetBuilder.build();

        // 保存到缓存文件
        if (featureCacheDir != null) {
            try {
                logger.info("保存FeatureSet到缓存: {}", featureCacheDir.getAbsolutePath());
                featureSet.saveToDir(featureCacheDir);
            } catch (IOException e) {
                logger.warn("保存缓存文件失败: {}", e.getMessage());
            }
        }

        logger.info("构建FeatureSet耗时 {} MS, 包含{}个特征",
                System.currentTimeMillis() - t1, featureSet.size());
        return featureSet;
    }

    /**
     * 获取特征集缓存目录
     *
     * @return 缓存目录，如果 workingDir 为 null 则返回 null
     */
    private File getFeatureCacheDir() {
        if (workingDir == null) {
            return null;
        }
        File workingDirFile = workingDir;
        if (!workingDirFile.exists()) {
            workingDirFile.mkdirs();
        }
        File file = new File(workingDirFile, "feature");
        file.mkdir();

        return file;
    }

    /**
     * 获取训练样本缓存目录
     *
     * @return 缓存目录，如果 workingDir 为 null 则返回 null
     */
    private File getTrainSamplesCacheDir() {
        if (workingDir == null) {
            return null;
        }
        File workingDirFile = (workingDir);
        if (!workingDirFile.exists()) {
            workingDirFile.mkdirs();
        }
        File file = new File(workingDirFile, "train-samples");
        file.mkdir();
        return file;
    }

    /**
     * 获取评估样本缓存目录
     *
     * @return 缓存目录，如果 workingDir 为 null 则返回 null
     */
    private File getEvaluateSamplesCacheDir() {
        if (workingDir == null) {
            return null;
        }
        File workingDirFile = (workingDir);
        if (!workingDirFile.exists()) {
            workingDirFile.mkdirs();
        }
        File file = new File(workingDirFile, "evaluate-samples");
        file.mkdir();
        return file;
    }

    private List<File> files(File file) {
        if (file.isFile()) {
            return Collections.singletonList(file);
        } else {
            // is dir
            List<File> result = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(file.toPath())) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().startsWith("."))
                        .forEach(p -> result.add(p.toFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }

    /**
     * 加载训练样本
     *
     * @param trainFiles 训练文件列表
     * @param featureSet 特征集
     * @return 训练样本列表
     * @throws IOException 如果文件读取过程中发生错误
     */
    private List<TrainSample> loadTrainSamples(List<File> trainFiles, ImmutableFeatureSetWithKeys featureSet) throws IOException {
        long startTime = System.currentTimeMillis();
        File cacheDir = getTrainSamplesCacheDir();
        File cacheFile = cacheDir != null ? new File(cacheDir, "train-samples.bin") : null;

        // 尝试从缓存读取
        if (cacheFile != null && cacheFile.exists()) {
            logger.info("从缓存文件读取TrainSamples: {}", cacheFile.getAbsolutePath());
            try {
                List<TrainSample> sampleList = readTrainSamplesFromCache(cacheFile);
                logger.info("读取TrainSamples耗时 {} MS, 包含{}个样例",
                        System.currentTimeMillis() - startTime, sampleList.size());
                return sampleList;
            } catch (IOException | ClassNotFoundException e) {
                logger.warn("读取缓存文件失败，重新加载: {}", e.getMessage());
            }
        }

        // 重新加载
        List<TrainSample> sampleList = new ArrayList<>();
        for (File file : trainFiles) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    sampleList.add(sampleText2TrainSample(line, featureSet));
                    if (sampleList.size() % 2000 == 0 && logger.isDebugEnabled()) {
                        logger.debug("Load TrainSamples {}", sampleList.size());
                    }
                }
            }
        }

        logger.info("加载TrainSample耗时 {} MS, 包含{}个样例",
                System.currentTimeMillis() - startTime, sampleList.size());

        // 保存到缓存
        if (cacheFile != null) {
            try {
                logger.info("保存TrainSamples到缓存: {}", cacheFile.getAbsolutePath());
                saveTrainSamplesToCache(sampleList, cacheFile);
            } catch (IOException e) {
                logger.warn("保存缓存文件失败: {}", e.getMessage());
            }
        }

        return sampleList;
    }

    /**
     * 加载评估样本
     *
     * @param evaluateFile 评估文件或目录
     * @param featureSet   特征集
     * @return 评估样本列表
     * @throws IOException 如果文件读取过程中发生错误
     */
    private List<EvaluationSample> loadEvaluateSamples(File evaluateFile, FeatureIdSeek featureSet) throws IOException {
        long startTime = System.currentTimeMillis();

        // 重新加载
        List<EvaluationSample> evaluationSampleList = new ArrayList<>(5000);
        for (File file : files(evaluateFile)) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    InputSequence inputSeq = annotateTextParser.parseToInputList(line);
                    TrainSample trainSample = sampleText2TrainSample(line, featureSet);
                    evaluationSampleList.add(new EvaluationSample(
                            line,
                            trainSample.getFeatureSequence(),
                            trainSample.getLabel(),
                            inputSeq));

                    if (evaluationSampleList.size() % 2000 == 0 && logger.isDebugEnabled()) {
                        logger.debug("Load Evaluate sample {}", evaluationSampleList.size());
                    }
                }
            }
        }

        logger.info("加载Evaluate TrainSample耗时 {} MS, 包含{}个样例",
                System.currentTimeMillis() - startTime, evaluationSampleList.size());

        return evaluationSampleList;
    }

    /**
     * 将训练样本列表保存到缓存文件
     *
     * @param sampleList 训练样本列表
     * @param cacheFile  缓存文件
     * @throws IOException 如果保存过程中发生错误
     */
    private void saveTrainSamplesToCache(List<TrainSample> sampleList, File cacheFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
            oos.writeInt(sampleList.size());
            for (TrainSample sample : sampleList) {
                // 序列化 featureSequence: List<IntArrayList>
                List<IntArrayList> featureSequence = sample.getFeatureSequence();
                oos.writeInt(featureSequence.size());
                for (IntArrayList list : featureSequence) {
                    int[] array = list.toArray();
                    oos.writeInt(array.length);
                    for (int value : array) {
                        oos.writeInt(value);
                    }
                }
                // 序列化 label: int[]
                int[] label = sample.getLabel();
                oos.writeInt(label.length);
                for (int value : label) {
                    oos.writeInt(value);
                }
            }
        }
    }

    /**
     * 从缓存文件读取训练样本列表
     *
     * @param cacheFile 缓存文件
     * @return 训练样本列表
     * @throws IOException            如果读取过程中发生错误
     * @throws ClassNotFoundException 如果类未找到
     */
    private List<TrainSample> readTrainSamplesFromCache(File cacheFile) throws IOException, ClassNotFoundException {
        List<TrainSample> sampleList = new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                // 反序列化 featureSequence: List<IntArrayList>
                int featureSeqSize = ois.readInt();
                List<IntArrayList> featureSequence = new ArrayList<>(featureSeqSize);
                for (int j = 0; j < featureSeqSize; j++) {
                    int arrayLen = ois.readInt();
                    IntArrayList list = new IntArrayList(arrayLen);
                    for (int k = 0; k < arrayLen; k++) {
                        list.add(ois.readInt());
                    }
                    featureSequence.add(list);
                }
                // 反序列化 label: int[]
                int labelLen = ois.readInt();
                int[] label = new int[labelLen];
                for (int j = 0; j < labelLen; j++) {
                    label[j] = ois.readInt();
                }
                sampleList.add(new TrainSample(featureSequence, label));
            }
        }
        return sampleList;
    }


    TrainSample sampleText2TrainSample(String sample, FeatureIdSeek featureSet) {
        return PerceptronEvaluator.createTrainSample(instance, annotateTextParser, sample, featureSet, labelMap);
    }


    // ========== 从 PerceptronTrainingAlgorithm 合并的方法 ==========

    /**
     * 默认多线程训练。
     *
     * @param featureSet         特征集
     * @param labelCount         标签数量
     * @param trainSource        训练样本
     * @param evaluationCallback 评估回调函数
     * @param maxIter            最大迭代次数
     * @param decodeQuickModel   是否使用快速解码模式
     * @param threadNumber       线程数。threadNumber=1 时平均感知机
     * @return 训练好的感知机模型
     */
    private PerceptronTrainModel trainInner(ImmutableFeatureSetWithKeys featureSet,
                                            int labelCount,
                                            List<TrainSample> trainSource,
                                            TrainingEvaluation evaluationCallback,
                                            int maxIter,
                                            boolean decodeQuickModel,
                                            int threadNumber) {
        if (threadNumber == 1) {
            return trainOneThread(featureSet, labelCount, trainSource, evaluationCallback, maxIter, decodeQuickModel);
        } else {
            return trainParallel(featureSet, labelCount, trainSource, evaluationCallback, maxIter, decodeQuickModel, threadNumber);
        }
    }

    /**
     * 单线程训练
     */
    private PerceptronTrainModel trainOneThread(ImmutableFeatureSetWithKeys featureSet,
                                                int labelCount,
                                                List<TrainSample> trainSource,
                                                TrainingEvaluation evaluationCallback,
                                                int maxIter,
                                                boolean decodeQuickModel) {
        PerceptronTrainModel model = new PerceptronTrainModel(featureSet, labelCount, decodeQuickModel);

        //应该是权重的总和 最后要平均？
        double[] total = new double[model.parameterSize()];
        //时间戳 每个正确预测的存活时间
        int[] timestamp = new int[model.parameterSize()];
        int current = 0; //第N次更新

        for (int k = 1; k <= maxIter; k++) {
            long t1 = System.currentTimeMillis();
            logger.info("#ITER {}/{}", k, maxIter);

            int per = 0;
            for (TrainSample sample : trainSource) {
                current++;
                model.update(sample, total, timestamp, current);
                per++;

                if (per % 5000 == 0 && logger.isDebugEnabled()) {
                    logger.debug("Process {}%", String.format("%.2f", per * 100.0 / trainSource.size()));
                }
            }
            long t2 = System.currentTimeMillis();

            logger.info("train use {} ms", t2 - t1);

            // 备份参数
            float[] back = Arrays.copyOf(model.getParameter(), model.parameterSize());
            model.average(total, timestamp, current);
            // 运行评估
            EvaluateResult result = evaluationCallback.evaluate(k, model);
            if (result != null) {
                logger.info("Evaluate Iter {} {}", k, result);
            }
            model.setParameter(back);
        }

        model.average(total, timestamp, current);
        return model;
    }

    /**
     * 多线程版本
     */
    private PerceptronTrainModel trainParallel(ImmutableFeatureSetWithKeys featureSet,
                                               int labelCount,
                                               List<TrainSample> trainSource,
                                               TrainingEvaluation evaluationCallback,
                                               int maxIter,
                                               boolean decodeQuickModel,
                                               int threadNumber) {
        PerceptronTrainModel[] modelArray = new PerceptronTrainModel[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            modelArray[i] = new PerceptronTrainModel(featureSet, labelCount, decodeQuickModel);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadNumber);

        // 分割训练数据
        int chunkSize = (trainSource.size() + threadNumber - 1) / threadNumber;
        List<List<TrainSample>> parts = new ArrayList<>();
        for (int i = 0; i < trainSource.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, trainSource.size());
            parts.add(trainSource.subList(i, end));
        }

        for (int k = 1; k <= maxIter; k++) {
            logger.info("#ITER {}/{}", k, maxIter);
            long t1 = System.currentTimeMillis();
            CountDownLatch countDownLatch = new CountDownLatch(threadNumber);

            for (int s = 0; s < threadNumber; s++) {
                final int threadIndex = s;
                final List<TrainSample> part = s < parts.size() ? parts.get(s) : new ArrayList<>();
                executor.submit(() -> {
                    try {
                        int count = 0;

                        for (TrainSample d : part) {
                            modelArray[threadIndex].update(d);
                            count++;

                            if (threadIndex == 0 && count % 5000 == 0 && logger.isDebugEnabled()) {
                                logger.debug("Process {}%", String.format("%.2f", count * 100.0 / part.size()));
                            }
                        }

                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            //把第二个开始的模型的参数全部和第一个平均
            float[] first = modelArray[0].getParameter();
            for (int i = 1; i < modelArray.length; i++) {
                float[] the = modelArray[i].getParameter();
                for (int j = 0; j < first.length; j++) {
                    first[j] += the[j];
                }
            }

            for (int j = 0; j < first.length; j++) {
                first[j] = first[j] / modelArray.length;
            }

            long t2 = System.currentTimeMillis();

            logger.info("use {} ms", t2 - t1);
            EvaluateResult result = evaluationCallback.evaluate(k, modelArray[0]);
            if (result != null) {
                logger.info("Evaluate Iter {} {}", k, result);
            }
        }

        executor.shutdownNow();

        return modelArray[0];
    }
}
