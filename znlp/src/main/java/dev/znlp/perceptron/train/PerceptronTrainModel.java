package dev.znlp.perceptron.train;

import dev.znlp.base.common.DataInputWrapper;
import dev.znlp.base.common.DataOutputWrapper;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.common.MaxHeapTopK;
import dev.znlp.base.dat.DoubleArrayTrie;
import dev.znlp.base.dat.DoubleArrayTrieBuilder;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.perceptron.PerceptronDecoder;
import dev.znlp.perceptron.decoder.QuickPerceptronDecoder;
import dev.znlp.perceptron.decoder.ViterbiPerceptronDecoder;
import dev.znlp.perceptron.feature.ImmutableFeatureSetWithKeys;
import dev.znlp.perceptron.PerceptronModel;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * 感知机模型实现。
 * <p>
 * 这个感知机面向向量进行计算，提供底层实现。FeatureSet中保存了所有特征，每个特征都有自己的数组下标，
 * 系统中存在N个特征。感知机处理的向量是一个长度为N的稀疏特征向量，只有部分位是1，其他为0，
 * 所以使用有限的int[]来保存为1的向量下标。
 * <p>
 * parameter是一个很长的float数组，逻辑上按以下结构组织：
 * [ [F0], [F1], [label_0, label_1, label_x], ..., [FN] ]
 * <p>
 * Fn表示一个特征在每个label的权重。假设Label有4个，特征总数是10000，那么逻辑上存在4个长度为1万的float向量，
 * 每个向量代表对应label的感知机参数权重，相当于存在四个感知机。
 * <p>
 * 一个Input包含多个特征，这些特征在每个感知机中对应的位置值之和，就是每个label的score。
 * 在不考虑转移概率和全局最优的情况下，只需选择当前最大得分即可；如果要考虑全局最优则需要viterbi解码。
 * <p>
 * decode方法中使用的int[]数组，最后一位留给转移特征使用。因此抽取特征向量时需要注意，最后一列应留给转移特征。
 * <p>
 * FeatureSet中Feature的key是按顺序排序的，假设实际数据的特征数量是1000，label是4，
 * 那么会在前面插入4+1个特殊特征。
 *
 * @author jimichan
 */
public class PerceptronTrainModel {
//    private static final double MAX_SCORE = Integer.MIN_VALUE;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PerceptronTrainModel.class);

    private final ImmutableFeatureSetWithKeys featureSet;
    private final int labelCount;
    private float[] parameter;
    private final boolean decodeQuickModel = false;
    private PerceptronDecoder decoder;


    public PerceptronTrainModel(ImmutableFeatureSetWithKeys featureSet, int labelCount, boolean decodeQuickModel) {
        this.featureSet = featureSet;
        this.labelCount = labelCount;
        this.parameter = new float[featureSet.size() * labelCount];
//        this.labelLimitInParameter = (labelCount + 1) * labelCount;

        resetDecoder();
    }

    PerceptronTrainModel(ImmutableFeatureSetWithKeys featureSet,
                         int labelCount, float[] parameter
            , boolean decodeQuickModel) {
        this.featureSet = featureSet;
        this.labelCount = labelCount;
        this.parameter = parameter;
        resetDecoder();
    }

    private void resetDecoder() {
        if (decodeQuickModel) {
            decoder = new QuickPerceptronDecoder(
                    labelCount, parameter
            );
        } else {
            decoder = new ViterbiPerceptronDecoder(labelCount, parameter);
        }
    }


    public PerceptronModel toModel() {
        return new PerceptronModel(
                featureSet.toImmutableFeatureSet(),
                labelCount, parameter, decodeQuickModel
        );
    }

    public float[] getParameter() {
        return this.parameter;
    }

    void setParameter(float[] parameter) {
        this.parameter = parameter;
        resetDecoder();
    }

    public float parameterAt(int index) {
        return parameter[index];
    }

    public int parameterSize() {
        return parameter.length;
    }

    public PerceptronDecoder getDecoder() {
        return decoder;
    }

    /**
     * 单线程训练时调用.平均感知机
     */
    public void update(TrainSample data, double[] total, int[] timestamp, int current) {
        int length = data.getSize();
        int[] guessLabel = new int[length];
        decoder.decode(data.getFeatureSequence(), guessLabel);
        for (int i = 0; i < length; i++) {
            Labels labels = featureToLabel(data, guessLabel, i);
            updateParameter(labels.goldFeature, labels.predFeature, total, timestamp, current);
        }
    }

    /**
     * 多线程训练时调用.结构化感知机
     */
    public void update(TrainSample data) {
        int length = data.getSize();
        int[] guessLabel = new int[length];
        decoder.decode(data.getFeatureSequence(), guessLabel);
        for (int i = 0; i < length; i++) {
            Labels labels = featureToLabel(data, guessLabel, i);
            updateOnline(labels.goldFeature, labels.predFeature);
        }
    }

    private void updateOnline(int[] goldIndex, int[] predictIndex) {
        for (int i = 0; i < goldIndex.length; i++) {
            int xii = predictIndex[i];
            if (goldIndex[i] == xii) {
                continue;
            } else {
                parameter[goldIndex[i]]++;
                if (xii >= 0 && xii < parameter.length) {
                    parameter[xii]--;
                } else {
                    throw new IllegalArgumentException("更新参数时传入了非法的下标");
                }
            }
        }
    }

    private Labels featureToLabel(TrainSample data, int[] guessLabel, int i) {
        IntArrayList featureVector = data.getFeatureSequence().get(i);
        int[] goldFeature = new int[featureVector.size()];
        int[] predFeature = new int[featureVector.size()];
        int last = featureVector.size() - 1;
        for (int j = 0; j < last; j++) {
            goldFeature[j] = featureVector.get(j) * labelCount + data.getLabel()[i];
            predFeature[j] = featureVector.get(j) * labelCount + guessLabel[i];
        }
        goldFeature[last] = (i == 0 ? labelCount : data.getLabel()[i - 1]) * labelCount + data.getLabel()[i];
        predFeature[last] = (i == 0 ? labelCount : guessLabel[i - 1]) * labelCount + guessLabel[i];
        return new Labels(goldFeature, predFeature);
    }

    private boolean updateParameter(int[] goldIndex, int[] predictIndex, double[] total, int[] timestamp, int current) {
        if (Arrays.equals(goldIndex, predictIndex)) {
            return false;
        }
        for (int i = 0; i < goldIndex.length; i++) {
            if (goldIndex[i] == predictIndex[i]) {
                continue;
            } else {
                record(goldIndex[i], 1.0f, total, timestamp, current);
                if (predictIndex[i] >= 0 && predictIndex[i] < parameter.length) {
                    record(predictIndex[i], -1.0f, total, timestamp, current);
                } else {
                    throw new IllegalArgumentException("更新参数时传入了非法的下标");
                }
            }
        }
        return true;
    }

    private void record(int index, float value, double[] total, int[] timestamp, int current) {
        int passed = current - timestamp[index];
        total[index] += passed * parameter[index];
        parameter[index] += value;
        timestamp[index] = current;
    }

    public void average(double[] total, int[] timestamp, int current) {
        float cf = current;
        for (int i = 0; i < parameter.length; i++) {
            float pass = cf - timestamp[i];
            float totali = (float) total[i];
            parameter[i] = (totali + pass * parameter[i]) / cf;
        }
    }

    /**
     * 模型压缩。通过移除不重要的特征来减小模型体积。
     * <p>
     * 特征的重要性（score）通过该特征在所有标签下的权重绝对值之和来计算。
     * </p>
     * <p>
     * 压缩逻辑：
     * 1. 首先移除评分低于 {@code threshold} 的特征。
     * 2. 如果移除后总共移除的特征比例不足 {@code ratio}，则继续按评分从小到大移除特征，直到达到 {@code ratio} 指定的压缩比例。
     * </p>
     *
     * @param ratio     目标压缩率，取值范围为 [0, 1)。
     *                  表示预期移除的特征比例。例如：取值 0.9 表示移除 90% 的特征，保留 10%。
     * @param threshold 评分阈值。评分低于此值的特征将被直接移除。
     * @return 压缩后的新 {@link PerceptronTrainModel} 实例。
     */
    public PerceptronTrainModel compress(double ratio, double threshold) {
        if (ratio < 0 || ratio >= 1) {
            throw new IllegalArgumentException("压缩比必须介于 0 和 1 之间");
        }

        if (featureSet.getKeys() == null) {
            throw new IllegalStateException("FeatureSet keys不能为空");
        }

        int k = ratio == 0.0 ? 0 : (int) (ratio * featureSet.size());

        List<String> featureList = featureSet.getKeys();

        Set<Integer> filterSet = new HashSet<>();
        for (int id = 0; id < featureList.size(); id++) {
            float s = score(id);
            if (s < threshold) {
                filterSet.add(id);
            }
        }

        logger.info("threshold filterd {}", filterSet.size());

        if (k > 0 && filterSet.size() < k) {
            MaxHeapTopK heap = new MaxHeapTopK(k - filterSet.size());

            logger.info("let's filter top min {}", (k - filterSet.size()));

            for (int id = 0; id < featureList.size(); id++) {
                float s = score(id);
                if (s >= threshold) {
                    heap.push(id, s);
                }
            }

            Set<Integer> topMinResultIdSet = new HashSet<>();
            for (MaxHeapTopK.Pair<Integer, Float> pair : heap.result()) {
                topMinResultIdSet.add(pair.first());
            }

            filterSet.addAll(topMinResultIdSet);
        }

        logger.info("remove {} feature,real compress {}",
                filterSet.size(), String.format("%.3f", filterSet.size() * 1.0f / featureList.size()));

        int newSize = featureList.size() - filterSet.size();
        List<String> newFeatureList = new ArrayList<>(newSize);
        float[] newParameter = new float[labelCount * newSize];

        int cc = 0;
        for (int index = 0; index < featureList.size(); index++) {
            if (index <= labelCount || !filterSet.contains(index)) {
                newFeatureList.add(featureList.get(index));
                System.arraycopy(parameter, index * labelCount, newParameter, cc * labelCount, labelCount);
                cc++;
            }
        }

        DoubleArrayTrieBuilder builder = DoubleArrayTrie.builder();
        DoubleArrayTrie newDat = builder.build(newFeatureList);
        return new PerceptronTrainModel(
                new ImmutableFeatureSetWithKeys(newDat, newFeatureList),
                labelCount, newParameter, decodeQuickModel);
    }

    private float score(int id) {
        float s = 0.0f;
        for (int i = id * labelCount; i < id * labelCount + labelCount; i++) {
            s += Math.abs(parameter[i]);
        }
        return s;
    }


    record Labels(int[] goldFeature, int[] predFeature) {}

    public static enum FeatureFormat {
        FeatureText, FeatureBin
    }

    /**
     * 保存为 PerceptronModel 格式的二进制文件（供推理使用）
     * 保存为单个 model.bin 文件，包含：
     * 1. labelCount (int)
     * 2. decodeQuickModel (boolean)
     * 3. datTextMode (boolean) - DAT的保存模式，是txt还是bin
     * 4. featureSet (DoubleArrayTrie 二进制格式)
     * 5. parameter (float[] 二进制格式)
     *
     * @param outputFile  目标文件
     * @param format DAT的保存模式，是txt还是bin
     * @throws IOException 如果发生 I/O 错误
     */
    public void saveModelBinFile(File outputFile, FeatureFormat format) throws IOException {
        outputFile.getParentFile().mkdirs();

        File modelFile = outputFile;
        try (OutputStream fileOut = new FileOutputStream(modelFile);
             OutputStream bufferedOut = new BufferedOutputStream(fileOut)) {

            try (GZIPOutputStream gzipOut = new GZIPOutputStream(bufferedOut);
                 DataOutputStream out = new DataOutputStream(gzipOut)) {

                DataOutputWrapper wrapper = new DataOutputWrapper(out);

                boolean datTextMode = format == FeatureFormat.FeatureText;

                // 0. 写入文件名
                out.writeUTF(outputFile.getName());

                // 1. 写入 labelCount
                out.writeInt(labelCount);
                out.writeBoolean(decodeQuickModel);
                out.writeBoolean(datTextMode);

                // 2. 写入 featureSet (DoubleArrayTrie)
                featureSet.writeBinToDataOutput(out, datTextMode);

                // 3. 写入 parameter
                wrapper.writeFloatArray(parameter);
            }
        }
    }

    /**
     * 保存完整的训练模型到指定目录（包括 featureSet 的 keys）
     * 采用多文件保存方式：
     * 1. meta.txt - 元信息（labelCount, decodeQuickModel）
     * 2. feature.dat - DoubleArrayTrie 二进制格式
     * 3. feature.txt - featureSet 的 keys 文本格式
     * 4. parameter.dat - 模型参数数组
     *
     * @param outputDir 目标文件夹
     * @throws IOException 如果发生 I/O 错误
     */
    public void saveModelToDir(File outputDir) throws IOException {
        if (featureSet.getKeys() == null) {
            throw new IllegalStateException("FeatureSet keys 不能为空");
        }

        outputDir.mkdirs();

        // 1. 保存元信息
        File metaFile = new File(outputDir, "meta.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile))) {
            writer.write("labelCount=" + labelCount);
            writer.newLine();
            writer.write("decodeQuickModel=" + decodeQuickModel);
            writer.newLine();
        }

        // 2. 保存 featureSet（包括 DAT 和 keys）
        featureSet.saveToDir(outputDir);

        // 3. 保存 parameter
        File parameterFile = new File(outputDir, "parameter.dat");
        try (FileOutputStream fos = new FileOutputStream(parameterFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream out = new DataOutputStream(bos)) {
            DataOutputWrapper wrapper = new DataOutputWrapper(out);
            wrapper.writeFloatArray(parameter);
        }
    }

    /**
     * 从指定目录读取完整的训练模型
     * 读取多文件格式：
     * 1. meta.txt - 元信息（labelCount, decodeQuickModel）
     * 2. feature.dat - DoubleArrayTrie 二进制格式
     * 3. feature.txt - featureSet 的 keys 文本格式
     * 4. parameter.dat - 模型参数数组
     *
     * @param inputDir 源文件夹
     * @return PerceptronTrainModel 实例
     * @throws IOException 如果发生 I/O 错误或文件不存在
     */
    public static PerceptronTrainModel readModelFromDir(File inputDir) throws IOException {
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new IOException("Directory not found or not a directory: " + inputDir.getAbsolutePath());
        }

        // 1. 读取元信息
        File metaFile = new File(inputDir, "meta.txt");
        if (!metaFile.exists()) {
            throw new IOException("Meta file not found: " + metaFile.getAbsolutePath());
        }

        int labelCount = 0;
        boolean decodeQuickModel = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if ("labelCount".equals(key)) {
                        labelCount = Integer.parseInt(value);
                    } else if ("decodeQuickModel".equals(key)) {
                        decodeQuickModel = Boolean.parseBoolean(value);
                    }
                }
            }
        }

        // 2. 读取 featureSet
        ImmutableFeatureSetWithKeys featureSet = ImmutableFeatureSetWithKeys.readFromDir(inputDir);

        // 3. 读取 parameter
        File parameterFile = new File(inputDir, "parameter.dat");
        if (!parameterFile.exists()) {
            throw new IOException("Parameter file not found: " + parameterFile.getAbsolutePath());
        }

        float[] parameter;
        try (FileInputStream fis = new FileInputStream(parameterFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream in = new DataInputStream(bis)) {
            DataInputWrapper wrapper = new DataInputWrapper(in);
            parameter = wrapper.readFloatArray();
        }

        return new PerceptronTrainModel(featureSet, labelCount, parameter, decodeQuickModel);
    }
}
