package dev.znlp.fasttext.args;

import dev.znlp.fasttext.utils.AutoDataInput;
import dev.znlp.fasttext.utils.LittleEndianData;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;

/**
 * FastText模型的参数配置类，管理模型训练和预测的各种参数。
 * 包含词向量训练、分类模型训练、模型结构等方面的参数设置。
 */
public class Args {

    /**
     * 词的最小出现次数，低于此阈值的词将被忽略
     */
    public int minCount = 5;
    
    /**
     * 标签的最小出现次数，低于此阈值的标签将被忽略
     */
    public int minCountLabel = 0;
    
    /**
     * 词n-gram的最大长度
     */
    public int wordNgrams = 1;
    
    /**
     * 用于存储n-gram的哈希桶数量
     */
    public int bucket = 2000000;
    
    /**
     * 最小子词长度
     */
    public int minn = 3;
    
    /**
     * 最大子词长度
     */
    public int maxn = 6;
    
    /**
     * 负采样阈值
     */
    public double t = 1e-4;
    
    /**
     * 学习率更新速率
     */
    public int lrUpdateRate = 100;
    
    /**
     * 词向量维度
     */
    public int dim = 100;
    
    /**
     * 窗口大小
     */
    public int ws = 5;
    
    /**
     * 训练轮数
     */
    public int epoch = 5;
    
    /**
     * 负采样数量
     */
    public int neg = 5;
    
    /**
     * 损失函数类型
     */
    public LossName loss = LossName.ns;
    
    /**
     * 模型类型
     */
    public ModelName model = ModelName.sg;
    
    /**
     * 最大词汇表大小
     */
    public int maxVocabSize = 500000;
    
    /**
     * 随机数种子
     */
    public int seed = 0;
    
    /**
     * 训练线程数
     */
    public int thread = Math.max(Runtime.getRuntime().availableProcessors() - 2, 2);
    
    /**
     * 标签前缀
     */
    public String label = "__label__";
    
    /**
     * 学习率
     */
    public double lr = 0.05;
    
    /**
     * 预训练向量文件
     */
    public File preTrainedVectors = null;
    
    /**
     * 是否保留预训练向量
     */
    public boolean keepPreTrainedVector = false;

    // Autotune (simplified port)
    /**
     * 自动调参验证文件
     */
    public File autotuneValidationFile = null;
    
    /**
     * 自动调参评估指标
     */
    public String autotuneMetric = "f1";
    
    /**
     * 自动调参预测数量
     */
    public int autotunePredictions = 1;
    
    /**
     * 自动调参持续时间（秒）
     */
    public int autotuneDuration = 60 * 5;
    
    /**
     * 自动调参模型大小限制
     */
    public String autotuneModelSize = "";

    /**
     * 手动设置的参数集合
     */
    private final Set<String> manualArgs = new HashSet<>();

    /**
     * 检查是否启用了自动调参
     *
     * @return 是否启用自动调参
     */
    public boolean hasAutotune() {
        return autotuneValidationFile != null && autotuneValidationFile.exists();
    }

    /**
     * 将参数标记为手动设置
     *
     * @param argName 参数名称
     */
    public void setManual(String argName) {
        manualArgs.add(argName);
    }

    /**
     * 检查参数是否为手动设置
     *
     * @param argName 参数名称
     * @return 是否为手动设置
     */
    public boolean isManual(String argName) {
        return manualArgs.contains(argName);
    }

    /**
     * 复制一份参数，调用方对象不会被训练入口改写。
     */
    public Args copy() {
        Args copy = new Args();
        copy.minCount = minCount;
        copy.minCountLabel = minCountLabel;
        copy.wordNgrams = wordNgrams;
        copy.bucket = bucket;
        copy.minn = minn;
        copy.maxn = maxn;
        copy.t = t;
        copy.lrUpdateRate = lrUpdateRate;
        copy.dim = dim;
        copy.ws = ws;
        copy.epoch = epoch;
        copy.neg = neg;
        copy.loss = loss;
        copy.model = model;
        copy.maxVocabSize = maxVocabSize;
        copy.seed = seed;
        copy.thread = thread;
        copy.label = label;
        copy.lr = lr;
        copy.preTrainedVectors = preTrainedVectors;
        copy.keepPreTrainedVector = keepPreTrainedVector;
        copy.autotuneValidationFile = autotuneValidationFile;
        copy.autotuneMetric = autotuneMetric;
        copy.autotunePredictions = autotunePredictions;
        copy.autotuneDuration = autotuneDuration;
        copy.autotuneModelSize = autotuneModelSize;
        copy.manualArgs.addAll(manualArgs);
        return copy;
    }

    /**
     * 按 Facebook FastText 小端格式写入参数。
     */
    public void save(OutputStream out) throws IOException {
        LittleEndianData.writeInt(out, dim);
        LittleEndianData.writeInt(out, ws);
        LittleEndianData.writeInt(out, epoch);
        LittleEndianData.writeInt(out, minCount);
        LittleEndianData.writeInt(out, neg);
        LittleEndianData.writeInt(out, wordNgrams);
        LittleEndianData.writeInt(out, loss.value);
        LittleEndianData.writeInt(out, model.value);
        LittleEndianData.writeInt(out, bucket);
        LittleEndianData.writeInt(out, minn);
        LittleEndianData.writeInt(out, maxn);
        LittleEndianData.writeInt(out, lrUpdateRate);
        LittleEndianData.writeDouble(out, t);
    }

    /**
     * 保存参数到文件通道（小端，与官方 .bin 一致）
     *
     * @param ofs 文件通道
     * @throws IOException IO异常
     */
    public void save(FileChannel ofs) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(13 * 4 + 8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(dim);
        buffer.putInt(ws);
        buffer.putInt(epoch);
        buffer.putInt(minCount);
        buffer.putInt(neg);
        buffer.putInt(wordNgrams);
        buffer.putInt(loss.value);
        buffer.putInt(model.value);
        buffer.putInt(bucket);
        buffer.putInt(minn);
        buffer.putInt(maxn);
        buffer.putInt(lrUpdateRate);
        buffer.putDouble(t);
        buffer.flip();
        ofs.write(buffer);
    }

    /**
     * 从自动数据输入加载参数
     *
     * @param input 自动数据输入
     * @return Args实例
     * @throws IOException IO异常
     */
    public static Args load(AutoDataInput input) throws IOException {
        Args args = new Args();
        args.dim = input.readInt();
        args.ws = input.readInt();
        args.epoch = input.readInt();
        args.minCount = input.readInt();
        args.neg = input.readInt();
        args.wordNgrams = input.readInt();
        args.loss = LossName.fromValue(input.readInt());
        args.model = ModelName.fromValue(input.readInt());
        args.bucket = input.readInt();
        args.minn = input.readInt();
        args.maxn = input.readInt();
        args.lrUpdateRate = input.readInt();
        args.t = input.readDouble();
        return args;
    }
}
