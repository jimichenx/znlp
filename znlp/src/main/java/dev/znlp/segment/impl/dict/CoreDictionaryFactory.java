package dev.znlp.segment.impl.dict;

import dev.znlp.base.common.CacheFileUtils;
import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.segment.DictionaryAbsWords;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

/**
 * 核心词典工厂类
 * <p>
 * 负责创建和加载核心词典实例。该工厂类支持从原始文本文件加载词典，
 * 并使用缓存机制提高后续加载速度。
 * </p>
 * <p>
 * 加载流程：
 * <ol>
 *   <li>首先尝试从缓存文件加载（如果存在且有效）</li>
 *   <li>如果缓存不存在或无效，则从原始文本文件加载</li>
 *   <li>加载完成后，将词典序列化为DAT格式并保存到缓存文件</li>
 * </ol>
 * </p>
 * <p>
 * 缓存机制通过文件内容的哈希值（MD5）来标识，当源文件变更时会自动重新构建缓存。
 * </p>
 *
 * @author jimichan
 */
public class CoreDictionaryFactory implements ServiceFactory<CoreDictionary> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CoreDictionaryFactory.class);

    /**
     * 核心词典文件的资源路径
     */
    private static final String CORE_DICT_FILE_PATH = "znlp/core-dict/CoreDict.txt";

    /**
     * 核心词典文件的哈希值文件路径（可选）
     */
    private static final String CORE_DICT_HASH_PATH = "znlp/core-dict/CoreDict.txt.hash";

    /**
     * 缓存文件名前缀
     */
    private static final String CACHE_FILE_PREFIX = "CoreDict_";

    /**
     * 词典文件行分隔符
     */
    private static final String DICT_LINE_SEPARATOR = " ";

    /**
     * 词典文件每行的期望字段数量（词语 + 词频）
     */
    private static final int EXPECTED_FIELD_COUNT = 2;

    /**
     * 系统配置，用于获取缓存目录等配置信息
     */
    private final ZnlpSystemConfig config;

    /**
     * 资源加载器，用于加载词典文件资源
     */
    private final NlpResourceLoader loader;

    /**
     * 构造函数，注入所需的依赖
     *
     * @param config 系统配置对象，用于获取缓存目录等配置
     * @param loader 资源加载器，用于加载词典文件资源
     */
    public CoreDictionaryFactory(ZnlpSystemConfig config, NlpResourceLoader loader) {
        this.config = config;
        this.loader = loader;
    }

    /**
     * 创建核心词典实例
     * <p>
     * 该方法会优先从缓存文件加载词典，如果缓存不存在或无效，则从原始文本文件构建。
     * 构建过程包括：
     * <ol>
     *   <li>读取原始文本文件，解析词语和词频</li>
     *   <li>补齐必要的标签词语（如果缺失）</li>
     *   <li>构建双数组Trie结构</li>
     *   <li>保存到缓存文件（如果可能）</li>
     * </ol>
     * </p>
     *
     * @return 核心词典实例
     * @throws RuntimeException 如果无法加载或构建词典
     */
    @Override
    public CoreDictionary create() {
        try {
            // 加载源文件资源
            NlpResource dictResource = loader.loadResource(CORE_DICT_FILE_PATH);
            if (dictResource == null) {
                throw new RuntimeException("核心词典资源未找到: " + CORE_DICT_FILE_PATH);
            }

            // 尝试读取或计算 hash 值
            String hash = readHash(dictResource);

            // 如果 hash 存在，尝试从缓存加载
            if (isValidHash(hash)) {
                CoreDictionary cachedDict = tryLoadFromCache(hash);
                if (cachedDict != null) {
                    return cachedDict;
                }
            }

            // 从原始文件构建
            logger.debug("从源文件构建核心词典");
            DictBuildResult buildResult = buildDictionaryFromSource(dictResource);

            // 如果 hash 存在，保存到缓存
            if (isValidHash(hash)) {
                trySaveToCache(hash, buildResult.totalFreq, buildResult.trie);
            }

            return new CoreDictionaryImpl(buildResult.totalFreq, buildResult.trie);
        } catch (Exception e) {
            throw new RuntimeException("创建核心词典失败", e);
        }
    }

    /**
     * 验证哈希值是否有效
     *
     * @param hash 待验证的哈希值
     * @return 如果哈希值非空且非空白字符串则返回true，否则返回false
     */
    private boolean isValidHash(String hash) {
        return hash != null && !hash.isEmpty();
    }

    /**
     * 尝试从缓存加载核心词典
     *
     * @param hash 源文件的哈希值
     * @return 成功加载则返回核心词典实例，失败则返回null
     */
    private CoreDictionary tryLoadFromCache(String hash) {
        File cacheFile = CacheFileUtils.getCacheFile(config, CACHE_FILE_PREFIX + hash);
        if (!cacheFile.exists()) {
            return null;
        }

        try {
            logger.debug("从缓存加载核心词典: " + cacheFile.getAbsolutePath());
            return loadFromCache(cacheFile);
        } catch (Exception e) {
            logger.warn("缓存加载失败，将重新构建: " + e.getMessage());
            return null;
        }
    }

    /**
     * 尝试将词典保存到缓存
     *
     * @param hash      源文件的哈希值
     * @param totalFreq 词频总和
     * @param trie      双数组Trie结构
     */
    private void trySaveToCache(String hash, int totalFreq, StringIntDATMap trie) {
        try {
            saveToCache(hash, totalFreq, trie);
        } catch (Exception e) {
            logger.warn("缓存保存失败: " + e.getMessage());
        }
    }

    /**
     * 从源文件构建词典数据结构
     *
     * @param dictResource 词典源文件资源
     * @return 构建结果，包含词频总和和Trie结构
     * @throws RuntimeException 如果构建过程出错
     */
    private DictBuildResult buildDictionaryFromSource(NlpResource dictResource) {
        TreeMap<String, Integer> wordFreqMap = new TreeMap<>();
        int totalFreq = parseSourceFile(dictResource, wordFreqMap);

        // 补齐标签词，确保ID顺序正确
        ensureAllLabelsPresent(wordFreqMap);

        if (wordFreqMap.isEmpty()) {
            throw new RuntimeException("核心词典文件为空或无效");
        }

        StringIntDATMap trie = new StringIntDATMap(wordFreqMap);
        return new DictBuildResult(totalFreq, trie);
    }

    /**
     * 解析源文件，提取词语和词频
     *
     * @param dictResource 词典源文件资源
     * @param wordFreqMap  用于存储词语和词频的映射
     * @return 所有词频的总和
     * @throws RuntimeException 如果读取文件时发生IO错误
     */
    private int parseSourceFile(NlpResource dictResource, TreeMap<String, Integer> wordFreqMap) {
        int totalFreq = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dictResource.inputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split(DICT_LINE_SEPARATOR);
                if (fields.length != EXPECTED_FIELD_COUNT) {
                    logger.warn(String.format("跳过格式错误的行 %d: %s", lineNumber, line));
                    continue;
                }

                String word = fields[0];
                int frequency = parseFrequency(fields[1], lineNumber);
                if (frequency > 0) {
                    wordFreqMap.put(word, frequency);
                    totalFreq += frequency;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取核心词典文件时发生错误", e);
        }
        return totalFreq;
    }

    /**
     * 解析词频字符串为整数
     *
     * @param freqStr    词频字符串
     * @param lineNumber 当前行号（用于错误提示）
     * @return 解析后的词频，解析失败返回0
     */
    private int parseFrequency(String freqStr, int lineNumber) {
        try {
            return Integer.parseInt(freqStr);
        } catch (NumberFormatException e) {
            logger.warn(String.format("第 %d 行词频解析失败: %s", lineNumber, freqStr));
            return 0;
        }
    }

    /**
     * 确保所有标签词都存在于词典中
     *
     * @param wordFreqMap 词频映射
     */
    private void ensureAllLabelsPresent(TreeMap<String, Integer> wordFreqMap) {
        for (String label : DictionaryAbsWords.allLabel()) {
            wordFreqMap.putIfAbsent(label, 1);
        }
    }

    /**
         * 词典构建结果的内部数据结构
         */
        private record DictBuildResult(int totalFreq, StringIntDATMap trie) {
    }

    /**
     * 读取或计算源文件的哈希值
     * <p>
     * 首先尝试从哈希文件中读取哈希值，如果哈希文件不存在，
     * 则从源文件计算MD5哈希值。哈希值用于标识缓存文件。
     * </p>
     *
     * @param sourceResource 源文件资源，用于计算MD5（当哈希文件不存在时）
     * @return 文件的哈希值，如果无法获取则返回null
     */
    private String readHash(NlpResource sourceResource) {
        // 首先尝试读取 hash 文件
        String hashFromFile = tryReadHashFromFile();
        if (hashFromFile != null) {
            return hashFromFile;
        }

        // 如果读取不到 hash 文件，从源文件计算 MD5
        return calculateHashFromSource(sourceResource);
    }

    /**
     * 尝试从哈希文件中读取预计算的哈希值
     *
     * @return 哈希值，如果文件不存在或读取失败则返回null
     */
    private String tryReadHashFromFile() {
        try {
            NlpResource hashResource = loader.tryLoadResource(CORE_DICT_HASH_PATH);
            if (hashResource == null) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(hashResource.inputStream(), StandardCharsets.UTF_8))) {
                String hash = reader.readLine();
                if (hash != null && !hash.trim().isEmpty()) {
                    logger.debug("从哈希文件读取到哈希值: " + hash.trim());
                    return hash.trim();
                }
            }
        } catch (Exception e) {
            logger.debug("读取哈希文件失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从源文件计算MD5哈希值
     *
     * @param sourceResource 源文件资源
     * @return 计算得到的哈希值，失败则返回null
     */
    private String calculateHashFromSource(NlpResource sourceResource) {
        try {
            logger.debug("哈希文件未找到，从源文件计算MD5");
            String hash = sourceResource.hash();
            logger.debug("计算得到MD5哈希值: " + hash);
            return hash;
        } catch (Exception e) {
            logger.warn("从源文件计算MD5哈希值失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从缓存文件加载核心词典
     * <p>
     * 缓存文件为二进制格式，包含词频总和和序列化的双数组Trie结构。
     * </p>
     *
     * @param cacheFile 缓存文件路径
     * @return 从缓存加载的核心词典实例
     * @throws IOException 如果读取缓存文件时发生IO错误
     */
    private CoreDictionary loadFromCache(File cacheFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(cacheFile)))) {

            int totalFreq = dis.readInt();
            StringIntDATMap trie = StringIntDATMap.read(dis);

            return new CoreDictionaryImpl(totalFreq, trie);
        }
    }

    /**
     * 将核心词典保存到缓存文件
     * <p>
     * 缓存文件为二进制格式，包含词频总和和序列化的双数组Trie结构。
     * 缓存文件名基于源文件的哈希值生成，确保源文件变更时能自动失效。
     * </p>
     *
     * @param hash      源文件的哈希值，用于生成缓存文件名
     * @param totalFreq 词典中所有词语的词频总和
     * @param trie      基于双数组Trie结构的词典映射
     * @throws IOException 如果写入缓存文件时发生IO错误
     */
    private void saveToCache(String hash, int totalFreq, StringIntDATMap trie) throws IOException {
        File cacheFile = CacheFileUtils.getCacheFile(config, CACHE_FILE_PREFIX + hash);
        ensureParentDirectoryExists(cacheFile);

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(cacheFile)))) {

            dos.writeInt(totalFreq);
            trie.write(dos);

            logger.info("核心词典已缓存到: " + cacheFile.getAbsolutePath());
        }
    }

    /**
     * 确保文件的父目录存在
     *
     * @param file 目标文件
     */
    private void ensureParentDirectoryExists(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                logger.warn("创建缓存目录失败: " + parentDir.getAbsolutePath());
            }
        }
    }
}
