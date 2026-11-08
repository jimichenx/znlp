package dev.znlp.segment.impl.correction;

import dev.znlp.base.common.CharSourceLineReader;
import dev.znlp.base.dat.DoubleArrayTrieMap;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.resources.UseLines;
import dev.znlp.config.ZnlpSystemConfig;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * 人工纠错词典
 * <p>
 * 格式 第几套/房
 *
 * @author jimichan
 */
public class DefaultCorrectionDictionary implements CorrectionDictionary {

    static InternalLogger logger = InternalLoggerFactory.getInstance(DefaultCorrectionDictionary.class);

    private DoubleArrayTrieMap<CorrectionWord> doubleArrayTrie;

    /**
     * 纠错词典资源路径。
     * <p>
     * 目前固定加载内置词典；如需按需扩展，可在此处增加路径。
     */
    private static final List<String> CORRECTION_DICT_PATHS = Collections.singletonList("correction-dict/correction_dict.txt");

    @Override
    public DoubleArrayTrieMap<CorrectionWord> getTrie() {
        return doubleArrayTrie;
    }

    public DefaultCorrectionDictionary(ZnlpSystemConfig config, NlpResourceLoader loader) {
        try {
            loadFromRealData(loader, CORRECTION_DICT_PATHS);
        } catch (Exception e) {
            logger.warn("加载纠错词典失败: " + e.getMessage(), e);
        }
    }

    private void loadFromRealData(NlpResourceLoader loader, List<String> resourceUrls) throws Exception {
        TreeMap<String, CorrectionWord> map = new TreeMap<>();

        for (String url : resourceUrls) {
            NlpResource resource = loader.tryLoadResource(url);
            if (resource == null) {
                logger.warn("miss resource " + url);
                continue;
            }

            try (CharSourceLineReader reader = UseLines.lineReader(resource.inputStream())) {
                for (String line : reader) {
                    CorrectionWord correctionWord = CorrectionWord.parse(line);
                    map.put(correctionWord.path(), correctionWord);
                }
            }
        }

        if (map.isEmpty()) {
            logger.debug("纠错词典为空");
            return;
        }

        this.doubleArrayTrie = new DoubleArrayTrieMap<>(map);
        logger.info("加载纠错词典完成，共 " + map.size() + " 个词条");
    }
}
