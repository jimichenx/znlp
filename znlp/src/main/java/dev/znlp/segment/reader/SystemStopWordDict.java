package dev.znlp.segment.reader;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统停用词词典。同一组数据目录只加载一次，加载后只保留 DAT。
 *
 * @author jimichan
 */
public class SystemStopWordDict implements StopWordDict {

    public static final String StopWordDictPath = "znlp/stopword-dict/stopwords.txt";
    public static final String MergeStopWordDictPath = "znlp/stopword-dict/merge_stopwords.txt";

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SystemStopWordDict.class);
    private static final ConcurrentHashMap<String, DefaultStopWordDict> CACHE = new ConcurrentHashMap<>();

    private final DefaultStopWordDict stopDict;

    public SystemStopWordDict(ZnlpSystemConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.stopDict = CACHE.computeIfAbsent(cacheKey(config), key -> load(config));
    }

    @Override
    public boolean contains(String word) {
        return stopDict.contains(word);
    }

    DefaultStopWordDict loaded() {
        return stopDict;
    }

    private static String cacheKey(ZnlpSystemConfig config) {
        return String.join("\0", config.getDataDirs());
    }

    private static DefaultStopWordDict load(ZnlpSystemConfig config) {
        NlpResourceLoader resourceLoader = new NlpResourceLoader(config);
        Set<String> words = new HashSet<>();
        readLines(resourceLoader, StopWordDictPath, false, words);
        readLines(resourceLoader, MergeStopWordDictPath, true, words);

        DefaultStopWordDict dict = new DefaultStopWordDict();
        dict.add(words);
        dict.rebuild();
        dict.discardEditBuffer();
        return dict;
    }

    private static void readLines(NlpResourceLoader resourceLoader, String path, boolean merge, Set<String> words) {
        NlpResource resource = resourceLoader.tryLoadResource(path);
        if (resource == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.inputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!merge) {
                    words.add(line);
                    continue;
                }
                if (line.startsWith("+")) {
                    String word = line.substring(1).trim();
                    if (!word.isEmpty()) {
                        words.add(word);
                    }
                } else if (line.startsWith("-")) {
                    String word = line.substring(1).trim();
                    if (!word.isEmpty()) {
                        words.remove(word);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load stopword dictionary: " + path, e);
        }
    }
}
