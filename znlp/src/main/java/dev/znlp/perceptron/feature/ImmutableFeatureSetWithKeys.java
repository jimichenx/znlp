package dev.znlp.perceptron.feature;

import dev.znlp.base.common.DataOutputWrapper;
import dev.znlp.base.dat.DoubleArrayTrie;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单特征集实现，不支持运行时扩展（不带extMap）。
 * 适用于推理场景，内存占用更小。
 *
 * @author jimichan
 */
public class ImmutableFeatureSetWithKeys implements FeatureIdSeek {

    private final DoubleArrayTrie dat;

    private final List<String> keys;

    public ImmutableFeatureSetWithKeys(
            DoubleArrayTrie dat,
            List<String> keys
    ) {
        this.dat = dat;
        this.keys = keys;
    }

    public int featureId(String feature) {
        return dat.indexOf(feature);
    }

    @Override
    public int featureId(char[] chars, int length) {
        if (chars == null || length <= 0) {
            return -1;
        }
        return dat.indexOf(chars, 0, length);
    }


    public int size() {
        return dat.size();
    }

    public List<String> getKeys() {
        return keys;
    }

    public ImmutableFeatureSet toImmutableFeatureSet() {
        return new ImmutableFeatureSet(dat);
    }

    /**
     * 将特征集保存到文件夹
     * 保存 feature.dat (DoubleArrayTrie 二进制格式) 和 feature.txt (keys 文本格式)
     *
     * @param dir 目标文件夹
     * @throws IOException 如果发生 I/O 错误
     */
    public void saveToDir(File dir) throws IOException {
        dir.mkdirs();

        // 保存 DoubleArrayTrie 到 feature.dat
        File datFile = new File(dir, "feature.dat");
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(datFile));
             DataOutputStream out = new DataOutputStream(bos)) {
            dat.write(out);
            out.flush();
        }

        // 保存 keys 到 feature.txt
        File textFile = new File(dir, "feature.txt");
        if (keys != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(textFile.toURI()))) {
                for (String line : keys) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    public void writeBinToDataOutput(DataOutput out, boolean datTxtMode) throws IOException {
        DataOutputWrapper wrapper = new DataOutputWrapper(out);

        wrapper.writeBoolean(datTxtMode);

        if (datTxtMode) {
            wrapper.writeStringList(keys);
        } else {
            // 仅写入 dat（二进制格式）
            dat.write(out);
        }
    }

    /**
     * 从文件夹读取特征集
     * 读取 feature.dat (DoubleArrayTrie 二进制格式) 和 feature.txt (keys 文本格式)
     *
     * @param dir 源文件夹
     * @return ImmutableFeatureSetWitKeys 实例
     * @throws IOException 如果发生 I/O 错误或文件不存在
     */
    public static ImmutableFeatureSetWithKeys readFromDir(File dir) throws IOException {
        File datFile = new File(dir, "feature.dat");
        File textFile = new File(dir, "feature.txt");

        if (!datFile.exists()) {
            throw new IOException("File not found: " + datFile.getAbsolutePath());
        }
        if (!textFile.exists()) {
            throw new IOException("File not found: " + textFile.getAbsolutePath());
        }

        // 读取 DoubleArrayTrie
        DoubleArrayTrie dat;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(datFile));
             DataInputStream in = new DataInputStream(bis)) {
            dat = DoubleArrayTrie.read(in);
        }

        // 读取 keys
        List<String> keys = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(textFile.toURI()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                keys.add(line);
            }
        }

        return new ImmutableFeatureSetWithKeys(dat, keys);
    }

    // public static ImmutableFeatureSetWitKeys readFromDataInput(DataInput in) throws IOException {
    //     DataInputWrapper wrapper = new DataInputWrapper(in);
    //     boolean datTxtMode = wrapper.readBoolean();

    //     DoubleArrayTrie dat;
    //     List<String> keys;

    //     if (datTxtMode) {
    //         // txt 模式：读取 keys，然后从 keys 构造 dat
    //         keys = wrapper.readStringList();
    //         dat = DoubleArrayTrie.builder().build(keys);
    //     } else {
    //         // dat 模式：直接读取 dat，keys 为 null
    //         dat = DoubleArrayTrie.read(in);
    //         keys = null;
    //     }

    //     return new ImmutableFeatureSetWitKeys(dat, keys);
    // }
}
