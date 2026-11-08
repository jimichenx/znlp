package dev.znlp.segment.impl.bigram;

import dev.znlp.base.common.CSRSparseMatrix;
import dev.znlp.base.common.CacheFileUtils;
import dev.znlp.base.common.TreeBasedTable;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceFactory;
import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.segment.impl.dict.CoreDictionary;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 二元语法表词典工厂类
 * <p>
 * 负责创建和加载二元语法表词典实例。该工厂类支持从原始文本文件加载词典，
 * 并使用缓存机制提高后续加载速度。
 * </p>
 * <p>
 * 加载流程：
 * <ol>
 *   <li>首先尝试从缓存文件加载（如果存在且有效）</li>
 *   <li>如果缓存不存在或无效，则从原始文本文件加载</li>
 *   <li>加载完成后，将词典序列化为CSR稀疏矩阵格式并保存到缓存文件</li>
 * </ol>
 * </p>
 * <p>
 * 缓存机制通过文件内容的哈希值（MD5）来标识，当源文件变更时会自动重新构建缓存。
 * </p>
 *
 * @author jimichan
 */
public class BiGramTableDictionaryFactory implements ServiceFactory<BiGramTableDictionary> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BiGramTableDictionaryFactory.class);

    /**
     * 二元语法表文件的资源路径
     */
    private static final String BIGRAM_DICT_FILE_PATH = "znlp/core-dict/CoreDict.bigram.txt";

    /**
     * 二元语法表文件的哈希值文件路径（可选）
     */
    private static final String BIGRAM_DICT_HASH_PATH = "znlp/core-dict/CoreDict.bigram.txt.hash";

    /**
     * 缓存文件名前缀
     */
    private static final String CACHE_FILE_PREFIX = "BiGramTable_v2_";

    /**
     * 系统配置，用于获取缓存目录等配置信息
     */
    private final ZnlpSystemConfig config;

    /**
     * 资源加载器，用于加载词典文件资源
     */
    private final NlpResourceLoader loader;

    /**
     * 核心词典，用于获取词语ID
     */
    private final CoreDictionary coreDictionary;

    /**
     * 构造函数，注入所需的依赖
     *
     * @param config         系统配置对象，用于获取缓存目录等配置
     * @param loader         资源加载器，用于加载词典文件资源
     * @param coreDictionary 核心词典，用于获取词语ID
     */
    public BiGramTableDictionaryFactory(ZnlpSystemConfig config, NlpResourceLoader loader, CoreDictionary coreDictionary) {
        this.config = config;
        this.loader = loader;
        this.coreDictionary = coreDictionary;
    }

    /**
     * 创建二元语法表词典实例
     * <p>
     * 该方法会优先从缓存文件加载词典，如果缓存不存在或无效，则从原始文本文件构建。
     * 构建过程包括：
     * <ol>
     *   <li>读取原始文本文件，解析词语对和频率</li>
     *   <li>构建CSR稀疏矩阵结构</li>
     *   <li>保存到缓存文件（如果可能）</li>
     * </ol>
     * </p>
     *
     * @return 二元语法表词典实例
     * @throws RuntimeException 如果无法加载或构建词典
     */
    @Override
    public BiGramTableDictionary create() {
        try {
            // 加载源文件资源
            NlpResource dictResource = loader.loadResource(BIGRAM_DICT_FILE_PATH);
            if (dictResource == null) {
                throw new RuntimeException("二元语法表资源未找到: " + BIGRAM_DICT_FILE_PATH);
            }

            // 尝试读取或计算 hash 值
            String hash = readHash(dictResource);

            // 如果 hash 存在，尝试从缓存加载
            if (isValidHash(hash)) {
                BiGramTableDictionary cachedDict = tryLoadFromCache(hash);
                if (cachedDict != null) {
                    return cachedDict;
                }
            }

            // 从原始文件构建
            logger.debug("从源文件构建二元语法表词典");
            CSRSparseMatrix matrix = buildDictionaryFromSource(dictResource);

            // 如果 hash 存在，保存到缓存
            if (isValidHash(hash)) {
                trySaveToCache(hash, matrix);
            }

            return new BiGramTableDictionaryImpl(matrix, coreDictionary);
        } catch (Exception e) {
            throw new RuntimeException("创建二元语法表词典失败", e);
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
     * 尝试从缓存加载二元语法表词典
     *
     * @param hash 源文件的哈希值
     * @return 成功加载则返回二元语法表词典实例，失败则返回null
     */
    private BiGramTableDictionary tryLoadFromCache(String hash) {
        File cacheFile = CacheFileUtils.getCacheFile(config, CACHE_FILE_PREFIX + hash);
        if (!cacheFile.exists()) {
            return null;
        }

        try {
            logger.debug("从缓存加载二元语法表词典: " + cacheFile.getName());
            return loadFromCache(cacheFile);
        } catch (Exception e) {
            logger.warn("缓存加载失败，将重新构建: " + e.getMessage());
            return null;
        }
    }

    /**
     * 尝试将词典保存到缓存
     *
     * @param hash 源文件的哈希值
     * @param trie 双数组Trie结构
     */
    private void trySaveToCache(String hash, CSRSparseMatrix matrix) {
        try {
            saveToCache(hash, matrix);
        } catch (Exception e) {
            logger.warn("缓存保存失败: " + e.getMessage());
        }
    }

    /**
     * 从源文件构建词典数据结构
     *
     * @param dictResource 词典源文件资源
     * @return 构建的CSR稀疏矩阵结构
     * @throws RuntimeException 如果构建过程出错
     */
    private CSRSparseMatrix buildDictionaryFromSource(NlpResource dictResource) {
        TreeBasedTable<Integer, Integer, Integer> table = new TreeBasedTable<>();
        parseSourceFile(dictResource, table);

        if (table.isEmpty()) {
            throw new RuntimeException("二元语法表文件为空或无效");
        }

        return new CSRSparseMatrix(table, coreDictionary.size());
    }

    /**
     * 解析源文件，提取词语对和频率
     * <p>
     * 文件格式说明：
     * <ul>
     *   <li>非缩进行：第一个词（wordA）</li>
     *   <li>缩进行（以\t开头）：\t频率 wordB1 wordB2 wordB3 ...（多个wordB共享同一频率）</li>
     * </ul>
     * 例如：
     * <pre>
     * 0Begin
     *     6 慰问信 陪伴 年复一年
     *     17 挂 翻 编后
     * </pre>
     * 表示：0Begin@慰问信=6, 0Begin@陪伴=6, 0Begin@挂=17, 等等
     * </p>
     *
     * @param dictResource  词典源文件资源
     * @param table         用于存储词语对和频率的表格
     * @throws RuntimeException 如果读取文件时发生IO错误
     */
    private void parseSourceFile(NlpResource dictResource, TreeBasedTable<Integer, Integer, Integer> table) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dictResource.inputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            String firstWord = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.startsWith("\t")) {
                    // 缩进行：包含频率和多个第二个词
                    if (firstWord == null) {
                        logger.warn(String.format("跳过格式错误的行 %d（缺少第一个词）: %s", lineNumber, line));
                        continue;
                    }

                    parseBigramLine(line, firstWord, lineNumber, table);
                } else {
                    // 非缩进行：第一个词。不能用 String.trim()：抽象词以 \u0001 开头
                    // （如 \u00010Begin），trim() 会去掉该字符，导致整段二元接续丢失。
                    firstWord = rstripBigramKey(line);
                    if (firstWord.isEmpty()) {
                        firstWord = null;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取二元语法表文件时发生错误", e);
        }
    }

    /**
     * 解析二元语法行（缩进行）
     * <p>
     * 格式：\t频率 wordB1 wordB2 wordB3 ...
     * </p>
     *
     * @param line       缩进行内容
     * @param firstWord  第一个词
     * @param lineNumber 当前行号（用于错误提示）
     * @param table      用于存储词语对和频率的表格
     */
    private void parseBigramLine(String line, String firstWord, int lineNumber,
                                 TreeBasedTable<Integer, Integer, Integer> table) {
        // 去除开头的制表符
        String content = line.substring(1);

        // 找到第一个空格，分隔频率和词语列表
        int firstSpaceIndex = content.indexOf(' ');
        if (firstSpaceIndex == -1) {
            logger.warn(String.format("跳过格式错误的行 %d（无法找到空格分隔符）: %s", lineNumber, line));
            return;
        }

        // 解析频率
        String freqStr = content.substring(0, firstSpaceIndex);
        int frequency = parseFrequency(freqStr, lineNumber);
        if (frequency <= 0) {
            return;
        }

        // 获取第一个词的ID
        int idA = coreDictionary.getWordID(firstWord);
        if (idA == -1) {
            return;
        }

        // 解析词语列表（空格分隔）
        String wordsStr = content.substring(firstSpaceIndex + 1);
        String[] words = wordsStr.split(" ");

        // 为每个第二个词创建词语对
        for (String secondWord : words) {
            if (!secondWord.isEmpty()) {
                int idB = coreDictionary.getWordID(secondWord);
                if (idB >= 0) {
                    table.put(idA, idB, frequency);
                }
            }
        }
    }

    /**
     * 去掉行尾空白，但保留行首的 {@code \u0001} 抽象词标记。
     */
    private static String rstripBigramKey(String line) {
        int end = line.length();
        while (end > 0) {
            char c = line.charAt(end - 1);
            if (c == ' ' || c == '\t' || c == '\r') {
                end--;
            } else {
                break;
            }
        }
        return end == line.length() ? line : line.substring(0, end);
    }

    /**
     * 解析频率字符串为整数
     *
     * @param freqStr    频率字符串
     * @param lineNumber 当前行号（用于错误提示）
     * @return 解析后的频率，解析失败返回0
     */
    private int parseFrequency(String freqStr, int lineNumber) {
        try {
            return Integer.parseInt(freqStr);
        } catch (NumberFormatException e) {
            logger.warn(String.format("第 %d 行频率解析失败: %s", lineNumber, freqStr));
            return 0;
        }
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
            NlpResource hashResource = loader.tryLoadResource(BIGRAM_DICT_HASH_PATH);
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
     * 从缓存文件加载二元语法表词典
     * <p>
     * 缓存文件为二进制格式，包含序列化的CSR稀疏矩阵结构。
     * </p>
     *
     * @param cacheFile 缓存文件路径
     * @return 从缓存加载的二元语法表词典实例
     * @throws IOException 如果读取缓存文件时发生IO错误
     */
    private BiGramTableDictionary loadFromCache(File cacheFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(cacheFile)))) {

            CSRSparseMatrix matrix = CSRSparseMatrix.readExternal(dis);

            return new BiGramTableDictionaryImpl(matrix, coreDictionary);
        }
    }

    /**
     * 将二元语法表词典保存到缓存文件
     * <p>
     * 缓存文件为二进制格式，包含序列化的CSR稀疏矩阵结构。
     * 缓存文件名基于源文件的哈希值生成，确保源文件变更时能自动失效。
     * </p>
     *
     * @param hash    源文件的哈希值，用于生成缓存文件名
     * @param matrix  基于CSR稀疏矩阵结构的词典
     * @throws IOException 如果写入缓存文件时发生IO错误
     */
    private void saveToCache(String hash, CSRSparseMatrix matrix) throws IOException {
        File cacheFile = CacheFileUtils.getCacheFile(config, CACHE_FILE_PREFIX + hash);
        ensureParentDirectoryExists(cacheFile);

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(cacheFile)))) {

            matrix.writeExternal(dos);

            logger.info("二元语法表词典已缓存到: " + cacheFile.getName());
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
