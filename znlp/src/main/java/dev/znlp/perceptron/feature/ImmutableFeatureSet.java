package dev.znlp.perceptron.feature;

import dev.znlp.base.dat.DoubleArrayTrie;

import java.io.*;

/**
 * 简单特征集实现，不支持运行时扩展（不带extMap）。
 * 适用于推理场景，内存占用更小。
 *
 * @author jimichan
 */
public class ImmutableFeatureSet implements FeatureIdSeek {

    private final DoubleArrayTrie dat;

    public ImmutableFeatureSet(DoubleArrayTrie dat) {
        this.dat = dat;
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

//    public int featureId(CharSequence feature) {
//        return dat.indexOf(feature);
//    }

    public int size() {
        return dat.size();
    }

    /**
     * 将特征集保存到文件（二进制格式）
     *
     * @param file 目标文件
     * @throws IOException 如果发生 I/O 错误
     */
    public void write(File file) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
             DataOutputStream out = new DataOutputStream(bos)) {
            dat.write(out);
            out.flush();
        }
    }

    public void write(DataOutput out) throws IOException {
        dat.write(out);
    }

    public static ImmutableFeatureSet read(DataInput in) throws IOException {
        DoubleArrayTrie dat = DoubleArrayTrie.read(in);
        return new ImmutableFeatureSet(dat);
    }

    /**
     * 从文件读取特征集（二进制格式）
     *
     * @param file 源文件
     * @return ImmutableFeatureSet 实例
     * @throws IOException 如果发生 I/O 错误
     */
    public static ImmutableFeatureSet read(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             DataInputStream in = new DataInputStream(bis)) {
            DoubleArrayTrie dat = DoubleArrayTrie.read(in);
            return new ImmutableFeatureSet(dat);
        }
    }

    /**
     * 从 classpath 资源读取特征集（二进制格式）
     *
     * @param resourcePath classpath 资源路径（例如："/model/feature.dat"）
     * @return ImmutableFeatureSet 实例
     * @throws IOException 如果发生 I/O 错误或资源不存在
     */
    public static ImmutableFeatureSet read(String resourcePath) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ImmutableFeatureSet.class.getClassLoader();
        }

        InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Resource not found in classpath: " + resourcePath);
        }

        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             DataInputStream in = new DataInputStream(bis)) {
            DoubleArrayTrie dat = DoubleArrayTrie.read(in);
            return new ImmutableFeatureSet(dat);
        }
    }

}
