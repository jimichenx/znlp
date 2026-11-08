package dev.znlp.perceptron.utils;

import dev.znlp.base.common.DataInOutputUtils;
import dev.znlp.base.common.DataInputWrapper;
import dev.znlp.base.dat.DoubleArrayTrie;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.perceptron.PerceptronModel;
import dev.znlp.perceptron.feature.ImmutableFeatureSet;
import dev.znlp.perceptron.train.PerceptronTrainModel;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * 感知机模型读取器。
 * <p>
 * 负责从文件或classpath资源中读取感知机模型。
 * 支持压缩和非压缩格式，自动检测文件格式。
 * 就是模型保存的 bin 格式。
 * <p>
 * 文件格式：
 * <ol>
 *   <li>isCompressed (boolean) - 是否压缩</li>
 *   <li>labelCount (int) - 标签数量</li>
 *   <li>decodeQuickModel (boolean) - 是否使用快速解码</li>
 *   <li>datTextMode (boolean) - DAT的保存模式</li>
 *   <li>featureSet - 特征集（根据datTextMode决定格式）</li>
 *   <li>parameter (float[]) - 模型参数数组</li>
 * </ol>
 *
 * @author jimichan
 */
public class PerceptronModelLoadUtils {

    /**
     * 从文件读取感知机模型。
     * <p>
     * 读取由 {@link PerceptronTrainModel#saveModelBinFile(File, PerceptronTrainModel.FeatureFormat)} 保存的模型文件。
     * 支持压缩和非压缩格式，自动检测文件格式。
     *
     * @param inputFile 模型文件路径
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误或文件格式不正确
     */
    public static PerceptronModel read(File inputFile) throws IOException {
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Model file not found: " + inputFile.getAbsolutePath());
        }

        // 使用公共方法从文件读取
        try (FileInputStream fileIn = new FileInputStream(inputFile)) {
            return readFromInputStream(fileIn,null);
        }
    }

    /**
     * 从NlpResourceLoader加载的资源读取感知机模型。
     * <p>
     * 使用NlpResourceLoader从多个源（本地文件系统、Jar包、Classpath）加载模型文件，
     * 支持压缩和非压缩格式，自动检测文件格式。
     *
     * @param nlpResourceLoader 资源加载器
     * @param resourcePath 资源路径（例如："models/pinyin-split.bin"）
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误、资源不存在或文件格式不正确
     */
    public static PerceptronModel read(NlpResourceLoader nlpResourceLoader, String resourcePath) throws IOException {
        if (nlpResourceLoader == null) {
            throw new IllegalArgumentException("NlpResourceLoader cannot be null");
        }
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }

        // 使用NlpResourceLoader加载资源
        NlpResource resource = nlpResourceLoader.loadResource(resourcePath);

        // 使用公共方法从输入流读取
        return readFromInputStream(resource.inputStream(),nlpResourceLoader.getCacheDir());
    }

    /**
     * 从classpath资源读取感知机模型。
     * <p>
     * 从类路径中加载模型文件，支持压缩和非压缩格式，自动检测文件格式。
     * <p>
     * 资源路径可以是绝对路径（以"/"开头）或相对路径。
     * 例如："/models/pinyin-split.bin" 或 "models/pinyin-split.bin"
     *
     * @param resourcePath classpath资源路径（例如："/models/pinyin-split.bin"）
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误、资源不存在或文件格式不正确
     */
    public static PerceptronModel readFromClasspath(String resourcePath) throws IOException {
        // 使用与 ImmutableFeatureSet 相同的 ClassLoader 选择策略
        // 优先使用 ContextClassLoader，更可靠，特别是在测试和容器环境中
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = PerceptronModelLoadUtils.class.getClassLoader();
        }
        if (classLoader == null) {
            throw new IOException("Cannot get ClassLoader for loading resource: " + resourcePath);
        }

        // 尝试两种路径格式：原始路径和以"/"开头的路径
        // 因为 getResourceAsStream() 对这两种格式的处理不同
        String pathWithoutSlash = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        String pathWithSlash = "/" + pathWithoutSlash;

        // 先尝试原始路径（不以"/"开头），这在大多数情况下更可靠
        InputStream testStream = classLoader.getResourceAsStream(pathWithoutSlash);
        String normalizedPath = pathWithoutSlash;

        // 如果原始路径找不到，尝试以"/"开头的路径
        if (testStream == null) {
            testStream = classLoader.getResourceAsStream(pathWithSlash);
            normalizedPath = pathWithSlash;
        }

        // 先检测是否为压缩格式
        String format = String.format(
                "Resource not found in classpath: %s (tried: %s and %s, classLoader: %s)",
                resourcePath, pathWithoutSlash, pathWithSlash, classLoader.getClass().getName()
        );
        if (testStream == null) {
            // 提供更详细的错误信息，帮助诊断问题
            String errorMsg = format;
            throw new IOException(errorMsg);
        }

        // 关闭测试流
        testStream.close();

        // 重新获取输入流进行实际读取（使用找到资源的路径）
        InputStream inputStream = classLoader.getResourceAsStream(normalizedPath);
        if (inputStream == null) {
            // 提供更详细的错误信息，帮助诊断问题
            String errorMsg = format;
            throw new IOException(errorMsg);
        }

        // 使用公共方法从输入流读取
        return readFromInputStream(inputStream,null);
    }

    /**
     * 从输入流读取感知机模型。
     * <p>
     * 内部辅助方法，用于从输入流读取模型数据。
     * 自动检测压缩格式，支持压缩和非压缩格式。
     *
     * @param inputStream 输入流
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误或数据格式不正确
     */
    private static PerceptronModel readFromInputStream(InputStream inputStream,File cacheDir) throws IOException {
        try (BufferedInputStream bufferedIn = new BufferedInputStream(inputStream)) {
            GZIPInputStream gzipIn = new GZIPInputStream(bufferedIn);
            DataInput dataInput = new DataInputStream(gzipIn);

            return readFromDataInput(dataInput,cacheDir);
        }
    }


    /**
     * 从 DataInput 读取感知机模型。
     * <p>
     * 内部方法，用于从数据输入流读取模型数据。
     *
     * @param dataInput 数据输入流
     * @return 读取的感知机模型实例
     * @throws IOException 如果发生 I/O 错误或数据格式不正确
     */
    static PerceptronModel readFromDataInput(DataInput dataInput,File cacheDir) throws IOException {
        DataInputWrapper wrapper = new DataInputWrapper(dataInput);

        // 1. 读取文件名
        String fileName = wrapper.readUTF();

        // 2. 读取 labelCount
        int labelCount = wrapper.readInt();

        // 3. 读取 decodeQuickModel
        boolean decodeQuickModel = wrapper.readBoolean();

        // 4. 读取 datTextMode
        boolean datTextMode = wrapper.readBoolean();

        // 5. 读取 featureSet
        ImmutableFeatureSet featureSet = readFeatureSet(dataInput, datTextMode, cacheDir, fileName);

        // 6. 读取 parameter
        float[] parameter = wrapper.readFloatArray();

        return new PerceptronModel(featureSet, labelCount, parameter, decodeQuickModel);
    }

    /**
     * 根据 fileName 在 cacheDir 下计算 featureSet DAT 的缓存文件路径。
     * 将路径中的分隔符和非法字符替换为 '_'，并加上 ".dat" 后缀。
     */
    private static File cacheFileForFeatureSet(File cacheDir, String fileName) {
        String safe = fileName.replaceAll("[/\\\\:.*?\"<>|]", "_");
        if (safe.isEmpty()) {
            safe = "model";
        }
        return new File(cacheDir, safe + "_cache_featureSet.dat");
    }

    /**
     * 从 DataInput 读取特征集。
     * <p>
     * 根据 datTextMode 决定读取方式：
     * <ul>
     *   <li>如果 datTextMode 为 true：若 cacheDir != null 且缓存存在则跳过流中的 list、从缓存读 DAT；否则读取字符串列表构建 DAT，并可选写入缓存</li>
     *   <li>如果 datTextMode 为 false：直接读取 DoubleArrayTrie 二进制格式</li>
     * </ul>
     *
     * @param dataInput   数据输入流
     * @param datTextMode DAT保存模式
     * @param cacheDir    缓存目录，为 null 时不使用缓存
     * @param fileName    模型文件名，用于计算缓存文件名
     * @return 读取的特征集实例
     * @throws IOException 如果发生 I/O 错误
     */
    private static ImmutableFeatureSet readFeatureSet(DataInput dataInput, boolean datTextMode,
                                                      File cacheDir, String fileName) throws IOException {
        DataInputWrapper wrapper = new DataInputWrapper(dataInput);
        boolean innerDatTextMode = wrapper.readBoolean();

        DoubleArrayTrie dat;
        if (innerDatTextMode) {
            File cacheFile = (cacheDir != null) ? cacheFileForFeatureSet(cacheDir, fileName) : null;
            if (cacheFile != null && cacheFile.isFile()) {
                // 缓存存在：跳过流中的 list，不从内存构建；直接从缓存读 DAT 二进制
                DataInOutputUtils.skipStringList(dataInput);
                try (DataInputStream cacheIn = new DataInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
                    dat = DoubleArrayTrie.read(cacheIn);
                }
            } else {
                // 无缓存或缓存不存在：读取 list，构建 DAT；若有 cacheDir 则写入缓存
                List<String> keys = wrapper.readStringList();
                if (keys == null) {
                    throw new IOException("Keys list cannot be null in txt mode");
                }
                dat = DoubleArrayTrie.builder().build(keys);
                if (cacheFile != null) {
                    cacheDir.mkdirs();
                    try (DataOutputStream cacheOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
                        dat.write(cacheOut);
                    }
                }
            }
        } else {
            // dat 模式：直接读取 dat（二进制格式）
            dat = DoubleArrayTrie.read(dataInput);
        }

        return new ImmutableFeatureSet(dat);
    }
}

