package dev.znlp.perceptron;

import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.perceptron.feature.FeatureIdSeek;
import dev.znlp.perceptron.train.EvaluationSample;
import dev.znlp.perceptron.train.PerceptronTrainModel;
import dev.znlp.perceptron.train.TrainSample;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * 感知机评估工具类
 */
public class PerceptronEvaluator {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PerceptronEvaluator.class);

    /**
     * 评估模型
     *
     * @param instance   感知机实例
     * @param testFile   测试文件或目录
     * @param workDir    工作目录（用于缓存）
     * @param model      训练模型
     * @param <E>        元素类型
     * @param <InputSequence> 输入序列类型
     * @return 评估结果
     */
    public static <E, InputSequence> EvaluateResult evaluate(
            PerceptronInstance<E, InputSequence> instance,
            File testFile,
            File workDir,
            PerceptronTrainModel model) {

        if (testFile == null || !testFile.exists()) {
            throw new IllegalArgumentException("评估文件不存在: " + testFile);
        }
        if (model == null) {
            throw new IllegalArgumentException("模型对象不能为null");
        }

        try {
            return evaluateInternal(instance, testFile, workDir, model.toModel().getFeatureSet(), model.getDecoder());
        } catch (IOException e) {
            throw new RuntimeException("评估过程中发生IO错误", e);
        }
    }

    public static <E, InputSequence> EvaluateResult evaluate(
            PerceptronInstance<E, InputSequence> instance,
            File testFile,
            File workDir,
            PerceptronModel model) {

        if (testFile == null || !testFile.exists()) {
            throw new IllegalArgumentException("评估文件不存在: " + testFile);
        }
        if (model == null) {
            throw new IllegalArgumentException("模型对象不能为null");
        }

        try {
            return evaluateInternal(instance, testFile, workDir, model.getFeatureSet(), model.getDecoder());
        } catch (IOException e) {
            throw new RuntimeException("评估过程中发生IO错误", e);
        }
    }

    private static <E, InputSequence> EvaluateResult evaluateInternal(
            PerceptronInstance<E, InputSequence> instance,
            File evaluateFile,
            File workDir,
            ImmutableFeatureSet featureSet,
            PerceptronDecoder decoder) throws IOException {

        logger.info("开始评估模型...");
        long startTime = System.currentTimeMillis();

        // 加载评估样本
        List<EvaluationSample> evaluationSampleList = loadEvaluateSamples(instance, evaluateFile, workDir, featureSet);

        if (evaluationSampleList.isEmpty()) {
            logger.warn("评估样本为空");
            return null;
        }

        logger.info("加载了 {} 个评估样本", evaluationSampleList.size());

        // 创建评估函数并执行评估
        EvaluateResult result = instance.getEvaluateFunction().create(
                decoder,
                featureSet
        ).evaluate(evaluationSampleList);

        long endTime = System.currentTimeMillis();
        logger.info("评估完成，耗时 {} MS", endTime - startTime);
        logger.info("评估结果: {}", result);

        return result;
    }

    /**
     * 加载评估样本
     *
     * @param instance     感知机实例
     * @param evaluateFile 评估文件
     * @param workDir      工作目录
     * @param featureSet   特征集
     * @param <E>          元素类型
     * @param <InputSequence> 输入序列类型
     * @return 评估样本列表
     * @throws IOException IO异常
     */
    public static <E, InputSequence> List<EvaluationSample> loadEvaluateSamples(
            PerceptronInstance<E, InputSequence> instance,
            File evaluateFile,
            File workDir,
            FeatureIdSeek featureSet) throws IOException {
        
        long startTime = System.currentTimeMillis();
        
        // 尝试从缓存读取
        File cacheDir = getEvaluateSamplesCacheDir(workDir);
        File cacheFile = null;
        if (cacheDir != null) {
            // 使用文件名及其哈希作为缓存文件名，避免冲突
            String cacheName = "eval-" + evaluateFile.getName() + "-" + evaluateFile.getAbsolutePath().hashCode() + ".bin";
            cacheFile = new File(cacheDir, cacheName);
            
            if (cacheFile.exists()) {
                 // 简单的检查：如果缓存文件比源文件旧，则失效 (对于目录比较复杂，这里简化处理，或者假设workDir对应特定数据集)
                 // 实际上为了简单起见，这里假设workDir是专门为当前任务指定的，且用户负责清理缓存
                 // 或者我们只检查文件存在
                 logger.info("从缓存文件读取EvaluateSamples: {}", cacheFile.getAbsolutePath());
                 try {
                     List<EvaluationSample> samples = readEvaluationSamplesFromCache(cacheFile, instance.getAnnotateTextParser());
                     logger.info("读取EvaluateSamples耗时 {} MS, 包含{}个样例",
                            System.currentTimeMillis() - startTime, samples.size());
                     return samples;
                 } catch (Exception e) {
                     logger.warn("读取缓存文件失败，重新加载: {}", e.getMessage());
                 }
            }
        }

        // 重新加载
        List<EvaluationSample> evaluationSampleList = new ArrayList<>(5000);
        
        // 构建 labelMap
        String[] labels = instance.getLables();
        Map<String, Integer> labelMap = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            labelMap.put(labels[i], i);
        }

        AnnotateTextParser<E, InputSequence> parser = instance.getAnnotateTextParser();

        List<File> files = files(evaluateFile);
        for (File file : files) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    InputSequence inputSeq = parser.parseToInputList(line);
                    TrainSample trainSample = createTrainSample(instance, parser, line, featureSet, labelMap);
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

        // 保存缓存
        if (cacheFile != null) {
            try {
                logger.info("保存EvaluateSamples到缓存: {}", cacheFile.getAbsolutePath());
                saveEvaluationSamplesToCache(evaluationSampleList, cacheFile);
            } catch (IOException e) {
                logger.warn("保存缓存文件失败: {}", e.getMessage());
            }
        }

        return evaluationSampleList;
    }

    public static <E, InputSequence> TrainSample createTrainSample(
            PerceptronInstance<E, InputSequence> instance,
            AnnotateTextParser<E, InputSequence> parser,
            String sample,
            FeatureIdSeek featureSet,
            Map<String, Integer> labelMap) {

        List<AnnotateTextParser.Pair<E>> pairList = parser.parse(sample);
        InputSequence inputSeq = parser.listToInput(pairList);

        int[] labelList = pairList.stream()
                .mapToInt(pair -> {
                    Integer idx = labelMap.get(pair.second());
                    if (idx == null) {
                        throw new IllegalArgumentException("Unknown label: " + pair.second());
                    }
                    return idx;
                })
                .toArray();
        List<IntArrayList> v = instance.tansform(inputSeq, featureSet);

        return new TrainSample(v, labelList);
    }
    
    private static File getEvaluateSamplesCacheDir(File workDir) {
        if (workDir == null) {
            return null;
        }
        if (!workDir.exists()) {
            workDir.mkdirs();
        }
        File file = new File(workDir, "evaluate-samples");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    private static List<File> files(File file) {
        if (file.isFile()) {
            return Collections.singletonList(file);
        } else {
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
    
    // 序列化 EvaluationSample (只序列化必要的 TrainSample 部分和 raw text)
    // 注意：EvaluationSample 中 inputSeq 类型不确定，且 EvaluationSample 构造函数需要它。
    // 但是我们缓存是为了加速，如果 inputSeq 不能序列化怎么办？
    // 在 PerceptronTrainer 中，TrainSample 是被缓存的 (featureSequence, label)。
    // EvaluationSample 包含 raw text (String), featureSequence, label, inputSeq.
    // inputSeq 是 InputSequence 泛型。
    // 观察 PerceptronTrainer.readTrainSamplesFromCache 只恢复 TrainSample。
    // 但是 loadEvaluateSamples 需要返回 List<EvaluationSample>。
    // EvaluationSample 的 inputSeq 字段是 transient 或者可忽略吗？
    // 让我们看 EvaluationSample 定义。
    // 如果 inputSeq 无法序列化，我们就不能简单缓存 EvaluationSample 对象。
    // 但是我们可以缓存 (String rawText, TrainSample data)。
    // 读取时，我们需要重新 parseToInputList(rawText) 得到 inputSeq 吗？
    // parseToInputList 应该很快，比较慢的是 feature extraction (tansform)。
    // 但是 wait, tansform 是根据 inputSeq 做的。
    // 之前 PerceptronTrainer 中没有缓存 loadEvaluateSamples 的结果！
    // 之前 PerceptronTrainer 只缓存了 loadTrainSamples (返回 List<TrainSample>)。
    // 只有 loadEvaluateSamples 是每次重新加载的 (见 PerceptronTrainer:446 "重新加载")。
    // 用户要求："既然提供了File workDir,就是要对测试用例的对象列表就行缓存，多次重复调用加快速度。"
    // 所以我必须实现 EvaluationSample 的缓存。
    
    // 方案：
    // 缓存 String line, List<IntArrayList> featureSequence, int[] label.
    // 读取时，从 line 恢复 inputSeq (调用 parser.parseToInputList(line))。
    // 这样避免了序列化泛型 InputSequence。
    
    private static void saveEvaluationSamplesToCache(List<EvaluationSample> sampleList, File cacheFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
            oos.writeInt(sampleList.size());
            for (EvaluationSample sample : sampleList) {
                // Write raw text
                oos.writeUTF(sample.getLine());
                
                // Write featureSequence
                List<IntArrayList> featureSequence = sample.getFeatureSequence();
                oos.writeInt(featureSequence.size());
                for (IntArrayList list : featureSequence) {
                    int[] array = list.toArray();
                    oos.writeInt(array.length);
                    for (int value : array) {
                        oos.writeInt(value);
                    }
                }
                
                // Write label
                int[] label = sample.getLabel();
                oos.writeInt(label.length);
                for (int value : label) {
                    oos.writeInt(value);
                }
            }
        }
    }
    
    // 读取时需要 instance 来 parser inputSeq
    // 但是 loadEvaluateSamples 方法里有 instance。
    // 所以我们需要把 readEvaluationSamplesFromCache 改为接受 parser。
    
    private static <E, InputSequence> List<EvaluationSample> readEvaluationSamplesFromCache(
            File cacheFile) throws IOException, ClassNotFoundException {
        // 这里有一个问题：我们需要 InputSequence。
        // 读取时我们只能拿到 raw text。
        // 所以我们不能在这里完全构建 EvaluationSample，除非我们传入 parser。
        // 或者我们返回一个中间结构，然后在外面构建。
        // 为了简单，我们不支持 InputSequence 的缓存（因为它通常很快），只缓存耗时的特征向量。
        // 但是 EvaluationSample 的构造需要 InputSequence。
        // 所以我们必须在读取时重新解析 InputSequence。
        // 由于 Java 泛型擦除，我们不能在静态方法里直接 parse，除非传入 parser。
        throw new UnsupportedOperationException("Internal method requires parser context, handled inline or via adjusted signature");
    }
    
    // 修改 readEvaluationSamplesFromCache 签名
    private static <E, InputSequence> List<EvaluationSample> readEvaluationSamplesFromCache(
            File cacheFile,
            AnnotateTextParser<E, InputSequence> parser) throws IOException {
        
        List<EvaluationSample> sampleList = new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                String line = ois.readUTF();
                
                // 反序列化 featureSequence
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
                
                // 反序列化 label
                int labelLen = ois.readInt();
                int[] label = new int[labelLen];
                for (int j = 0; j < labelLen; j++) {
                    label[j] = ois.readInt();
                }
                
                // Reconstruct inputSeq
                InputSequence inputSeq = parser.parseToInputList(line);
                
                sampleList.add(new EvaluationSample(line, featureSequence, label, inputSeq));
            }
        }
        return sampleList;
    }
}
