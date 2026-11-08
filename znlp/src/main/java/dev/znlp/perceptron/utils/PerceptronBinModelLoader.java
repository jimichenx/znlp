package dev.znlp.perceptron.utils;

import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.PerceptronModel;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 读取裸二进制格式的感知机模型（{@code parameter.bin} + {@code feature.dat}）。
 * <p>
 * 与 {@link PerceptronModelLoadUtils} 读取的 gzip {@code .model} 不同，本格式拆成两个文件：
 * <ul>
 *     <li>{@code parameter.bin}：{@code labelCount(int)} + {@code pSize(int)} + 大端序 float 数组</li>
 *     <li>{@code feature.dat}：DoubleArrayTrie 二进制</li>
 * </ul>
 * 当前 {@code znlp/cws-model} 使用的仍是这种格式。
 */
public final class PerceptronBinModelLoader {

    private static final String FEATURE_DAT = "feature.dat";
    private static final String PARAMETER_BIN = "parameter.bin";
    private static final String PARAMETER_DAT = "parameter.dat";

    private PerceptronBinModelLoader() {
    }

    /**
     * 从模型目录加载。优先 {@code parameter.bin}，其次 {@code parameter.dat}。
     */
    public static PerceptronModel load(File dir) throws IOException {
        if (dir == null || !dir.isDirectory()) {
            throw new IOException("Model directory not found: " + dir);
        }
        File parameterFile = resolveParameterFile(dir);
        File featureFile = new File(dir, FEATURE_DAT);
        if (!featureFile.isFile()) {
            throw new IOException("Feature file not found: " + featureFile.getAbsolutePath());
        }
        try (InputStream parameterIn = new BufferedInputStream(new FileInputStream(parameterFile));
             InputStream featureIn = new BufferedInputStream(new FileInputStream(featureFile))) {
            return load(parameterIn, featureIn);
        }
    }

    /**
     * 通过 {@link NlpResourceLoader} 加载目录前缀下的裸二进制模型。
     *
     * @param dirPrefix 例如 {@code znlp/cws-model}
     */
    public static PerceptronModel load(NlpResourceLoader loader, String dirPrefix) throws IOException {
        if (loader == null) {
            throw new IllegalArgumentException("NlpResourceLoader cannot be null");
        }
        if (dirPrefix == null || dirPrefix.isBlank()) {
            throw new IllegalArgumentException("dirPrefix cannot be null or empty");
        }
        String prefix = trimSlash(dirPrefix);
        NlpResource parameterResource = loader.tryLoadResource(prefix + "/" + PARAMETER_BIN);
        if (parameterResource == null) {
            parameterResource = loader.loadResource(prefix + "/" + PARAMETER_DAT);
        }
        NlpResource featureResource = loader.loadResource(prefix + "/" + FEATURE_DAT);
        try (InputStream parameterIn = parameterResource.inputStream();
             InputStream featureIn = featureResource.inputStream()) {
            return load(parameterIn, featureIn);
        }
    }

    /**
     * 从两个输入流读取裸二进制格式模型。调用方负责关闭流。
     */
    public static PerceptronModel load(InputStream parameterBin, InputStream featureDat) throws IOException {
        float[] parameter;
        int labelCount;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(parameterBin))) {
            labelCount = in.readInt();
            int pSize = in.readInt();
            if (labelCount <= 0 || pSize <= 0) {
                throw new IOException("Invalid parameter.bin header: labelCount="
                        + labelCount + ", pSize=" + pSize);
            }
            parameter = readFloats(in, pSize);
        }

        ImmutableFeatureSet featureSet;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(featureDat))) {
            featureSet = ImmutableFeatureSet.read(in);
        }

        return new PerceptronModel(featureSet, labelCount, parameter, false);
    }

    private static float[] readFloats(DataInputStream in, int pSize) throws IOException {
        float[] parameter = new float[pSize];
        byte[] buffer = new byte[1024 * 1024 * 4];
        ByteBuffer wrap = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN);
        int point = 0;
        while (point < pSize) {
            int remainingBytes = (pSize - point) * 4;
            int n = Math.min(buffer.length, remainingBytes);
            in.readFully(buffer, 0, n);
            if (n % 4 != 0) {
                throw new IOException("parameter.bin size is not aligned to 4 bytes: " + n);
            }
            wrap.clear();
            wrap.limit(n);
            int count = n / 4;
            for (int i = 0; i < count; i++) {
                parameter[point++] = wrap.getFloat();
            }
        }
        return parameter;
    }

    private static File resolveParameterFile(File dir) throws IOException {
        File bin = new File(dir, PARAMETER_BIN);
        if (bin.isFile()) {
            return bin;
        }
        File dat = new File(dir, PARAMETER_DAT);
        if (dat.isFile()) {
            return dat;
        }
        throw new IOException("Parameter file not found in " + dir.getAbsolutePath()
                + " (tried " + PARAMETER_BIN + " and " + PARAMETER_DAT + ")");
    }

    private static String trimSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
