package dev.znlp.converter;

import dev.znlp.base.common.CacheFileUtils;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

/**
 * 繁简体转换词典加载器
 * <p>
 * 负责加载繁简体转换词典资源。该加载器支持从原始文本文件加载词典，
 * 并使用缓存机制提高后续加载速度。
 * </p>
 *
 * @author jimichan
 */
public class TransformInnerDictLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TransformInnerDictLoader.class);

    /**
     * 简体转繁体词典文件的资源路径
     */
    public static final String S2T_DICT_FILE_PATH = "znlp/ts-dict/s2t.txt";

    /**
     * 繁体转简体词典文件的资源路径
     */
    public static final String T2S_DICT_FILE_PATH = "znlp/ts-dict/t2s.txt";

    /**
     * 词典文件哈希值文件路径后缀
     */
    private static final String DICT_HASH_SUFFIX = ".hash";

    /**
     * 简体转繁体词典缓存文件前缀
     */
    private static final String S2T_CACHE_FILE_PREFIX = "S2T_Dict_";

    /**
     * 繁体转简体词典缓存文件前缀
     */
    private static final String T2S_CACHE_FILE_PREFIX = "T2S_Dict_";

    /**
     * 词典文件行分隔符
     */
    private static final String DICT_LINE_SEPARATOR = "=";

    /**
     * 词典文件每行的期望字段数量（源词 + 目标词）
     */
    private static final int EXPECTED_FIELD_COUNT = 2;

    private final ZnlpSystemConfig config;
    private final NlpResourceLoader loader;

    /**
     * 词典类型枚举
     */
    private enum DictType {
        S2T(S2T_DICT_FILE_PATH, S2T_CACHE_FILE_PREFIX),
        T2S(T2S_DICT_FILE_PATH, T2S_CACHE_FILE_PREFIX);

        private final String dictPath;
        private final String cachePrefix;

        DictType(String dictPath, String cachePrefix) {
            this.dictPath = dictPath;
            this.cachePrefix = cachePrefix;
        }

        public String getDictPath() {
            return dictPath;
        }

        public String getCachePrefix() {
            return cachePrefix;
        }
    }

    public TransformInnerDictLoader(ZnlpSystemConfig config, NlpResourceLoader loader) {
        this.config = config;
        this.loader = loader;
    }

    /**
     * 加载繁简体转换词典
     *
     * @return 包含s2t和t2s词典的TransformInnerDict实例
     */
    public TransformInnerDict load() {
        TransformDictionary s2t = loadDictionary(DictType.S2T);
        TransformDictionary t2s = loadDictionary(DictType.T2S);
        return new TransformInnerDict(s2t, t2s);
    }

    private TransformDictionary loadDictionary(DictType dictType) {
        try {
            // 加载源文件资源
            NlpResource dictResource = loader.loadResource(dictType.getDictPath());
            if (dictResource == null) {
                throw new RuntimeException("转换词典资源未找到: " + dictType.getDictPath());
            }

            // 尝试读取或计算 hash 值
            String hash = readHash(dictResource, dictType);

            // 如果 hash 存在，尝试从缓存加载
            if (isValidHash(hash)) {
                TransformDictionary cachedDict = tryLoadFromCache(hash, dictType);
                if (cachedDict != null) {
                    return cachedDict;
                }
            }

            // 从原始文件构建
            logger.info("从源文件构建{}转换词典", dictType);
            TreeMap<String, String> wordMap = buildDictionaryFromSource(dictResource);

            if (wordMap.isEmpty()) {
                throw new RuntimeException("转换词典文件为空或无效: " + dictType.getDictPath());
            }

            TransformDictionary dictionary = new TransformDictionaryImpl(wordMap);

            // 如果 hash 存在，保存到缓存
            if (isValidHash(hash)) {
                trySaveToCache(hash, wordMap, dictType);
            }

            return dictionary;
        } catch (Exception e) {
            throw new RuntimeException("创建" + dictType + "转换词典失败", e);
        }
    }

    private boolean isValidHash(String hash) {
        return hash != null && !hash.isEmpty();
    }

    private TransformDictionary tryLoadFromCache(String hash, DictType dictType) {
        File cacheFile = CacheFileUtils.getCacheFile(config, dictType.getCachePrefix() + hash);
        if (!cacheFile.exists()) {
            return null;
        }

        try {
            logger.info("从缓存加载{}转换词典: {}", dictType, cacheFile.getAbsolutePath());
            return loadFromCache(cacheFile);
        } catch (Exception e) {
            logger.warn("{}缓存加载失败，将重新构建: {}", dictType, e.getMessage());
            return null;
        }
    }

    private void trySaveToCache(String hash, TreeMap<String, String> wordMap, DictType dictType) {
        try {
            saveToCache(hash, wordMap, dictType);
        } catch (Exception e) {
            logger.warn("{}缓存保存失败: {}", dictType, e.getMessage());
        }
    }

    private TreeMap<String, String> buildDictionaryFromSource(NlpResource dictResource) {
        TreeMap<String, String> wordMap = new TreeMap<>();
        parseSourceFile(dictResource, wordMap);
        return wordMap;
    }

    private void parseSourceFile(NlpResource dictResource, TreeMap<String, String> wordMap) {
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

                String sourceWord = fields[0].trim();
                String targetWord = fields[1].trim();

                if (!sourceWord.isEmpty() && !targetWord.isEmpty()) {
                    wordMap.put(sourceWord, targetWord);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取转换词典文件时发生错误", e);
        }
    }

    private String readHash(NlpResource sourceResource, DictType dictType) {
        String hashFromFile = tryReadHashFromFile(dictType);
        if (hashFromFile != null) {
            return hashFromFile;
        }
        return calculateHashFromSource(sourceResource);
    }

    private String tryReadHashFromFile(DictType dictType) {
        try {
            NlpResource hashResource = loader.tryLoadResource(dictType.getDictPath() + DICT_HASH_SUFFIX);
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

    @SuppressWarnings("unchecked")
    private TransformDictionary loadFromCache(File cacheFile) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(cacheFile)))) {

            TreeMap<String, String> wordMap = (TreeMap<String, String>) ois.readObject();
            return new TransformDictionaryImpl(wordMap);
        } catch (ClassNotFoundException e) {
            throw new IOException("类加载失败", e);
        }
    }

    private void saveToCache(String hash, TreeMap<String, String> wordMap, DictType dictType) throws IOException {
        File cacheFile = CacheFileUtils.getCacheFile(config, dictType.getCachePrefix() + hash);
        ensureParentDirectoryExists(cacheFile);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(cacheFile)))) {

            oos.writeObject(wordMap);
            logger.info("{}转换词典已缓存到: {}", dictType, cacheFile.getAbsolutePath());
        }
    }

    private void ensureParentDirectoryExists(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                logger.warn("创建缓存目录失败: " + parentDir.getAbsolutePath());
            }
        }
    }
}
