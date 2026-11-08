package dev.znlp.segment.impl.userdict;

import dev.znlp.base.common.CharSourceLineReader;
import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.resources.UseLines;
import dev.znlp.config.ZnlpSystemConfig;

import java.util.TreeMap;

/**
 * 用户自定义词典，持有 6 类资源对应的双数组 Trie。
 *
 * @author jimichan
 */
public class ResourceUserDictionary {

    static InternalLogger logger = InternalLoggerFactory.getInstance(ResourceUserDictionary.class);

    /** 通用自定义词典 */
    private StringIntDATMap customDictDat;
    /** 上海地名 */
    private StringIntDATMap shanghaiPlaceNameDat;
    /** 人名词典 */
    private StringIntDATMap personNameDat;
    /** 全国地名大全 */
    private StringIntDATMap nationwidePlaceNameDat;
    /** 机构名词典 */
    private StringIntDATMap organizationNameDat;
    /** 现代汉语补充词库 */
    private StringIntDATMap modernChineseSupplementDat;

    private final NlpResourceLoader loader;

    /** 资源路径：通用自定义词典 */
    public static final String PATH_CUSTOM_DICT = "znlp/custom-dict/CustomDictionary.txt";
    /** 资源路径：上海地名 */
    public static final String PATH_SHANGHAI_PLACE = "znlp/custom-dict/上海地名.txt";
    /** 资源路径：人名词典 */
    public static final String PATH_PERSON_NAME = "znlp/custom-dict/人名词典.txt";
    /** 资源路径：全国地名大全 */
    public static final String PATH_NATIONWIDE_PLACE = "znlp/custom-dict/全国地名大全.txt";
    /** 资源路径：机构名词典 */
    public static final String PATH_ORGANIZATION = "znlp/custom-dict/机构名词典.txt";
    /** 资源路径：现代汉语补充词库 */
    public static final String PATH_MODERN_CHINESE_SUPPLEMENT = "znlp/custom-dict/现代汉语补充词库.txt";

    public ResourceUserDictionary(ZnlpSystemConfig config, NlpResourceLoader loader) {
        this.loader = loader;
    }

    /**
     * 从指定资源路径加载词条并构建 StringIntDATMap。
     *
     * @param loader       资源加载器
     * @param resourcePath 资源路径
     * @return 构建好的 DAT，资源缺失或为空时返回 null
     */
    private StringIntDATMap loadDatFromResource(NlpResourceLoader loader, String resourcePath) {
        try {
            NlpResource resource = loader.tryLoadResource(resourcePath);
            if (resource == null) {
                logger.debug("未找到资源: " + resourcePath);
                return null;
            }

            TreeMap<String, Integer> map = new TreeMap<>();
            try (CharSourceLineReader reader = UseLines.lineReader(resource.inputStream())) {
                for (String line : reader) {
                    String[] params = line.split("\\s");
                    if (params.length > 0) {
                        String word = params[0].trim();
                        if (!word.isEmpty()) {
                            map.put(word, 1000);
                        }
                    }
                }
            }

            if (map.isEmpty()) {
                logger.debug("词典为空: " + resourcePath);
                return null;
            }

            StringIntDATMap dat = new StringIntDATMap(map);
            logger.info("加载词典完成: " + resourcePath + ", 词条数: " + map.size());
            return dat;
        } catch (Exception e) {
            logger.warn("加载词典失败: " + resourcePath + ", " + e.getMessage(), e);
            return null;
        }
    }

    public StringIntDATMap getCustomDictDat() {
        if (customDictDat == null) {
            synchronized (this) {
                if (customDictDat == null) {
                    customDictDat = loadDatFromResource(loader, PATH_CUSTOM_DICT);
                }
            }
        }
        return customDictDat;
    }

    public StringIntDATMap getShanghaiPlaceNameDat() {
        if (shanghaiPlaceNameDat == null) {
            synchronized (this) {
                if (shanghaiPlaceNameDat == null) {
                    shanghaiPlaceNameDat = loadDatFromResource(loader, PATH_SHANGHAI_PLACE);
                }
            }
        }
        return shanghaiPlaceNameDat;
    }

    public StringIntDATMap getPersonNameDat() {
        if (personNameDat == null) {
            synchronized (this) {
                if (personNameDat == null) {
                    personNameDat = loadDatFromResource(loader, PATH_PERSON_NAME);
                }
            }
        }
        return personNameDat;
    }

    public StringIntDATMap getNationwidePlaceNameDat() {
        if (nationwidePlaceNameDat == null) {
            synchronized (this) {
                if (nationwidePlaceNameDat == null) {
                    nationwidePlaceNameDat = loadDatFromResource(loader, PATH_NATIONWIDE_PLACE);
                }
            }
        }
        return nationwidePlaceNameDat;
    }

    public StringIntDATMap getOrganizationNameDat() {
        if (organizationNameDat == null) {
            synchronized (this) {
                if (organizationNameDat == null) {
                    organizationNameDat = loadDatFromResource(loader, PATH_ORGANIZATION);
                }
            }
        }
        return organizationNameDat;
    }

    public StringIntDATMap getModernChineseSupplementDat() {
        if (modernChineseSupplementDat == null) {
            synchronized (this) {
                if (modernChineseSupplementDat == null) {
                    modernChineseSupplementDat = loadDatFromResource(loader, PATH_MODERN_CHINESE_SUPPLEMENT);
                }
            }
        }
        return modernChineseSupplementDat;
    }

    /**
     * 返回通用自定义词典的 Trie，用于兼容仅使用单一词典的调用方。
     */
    public StringIntDATMap getTrie() {
        return getCustomDictDat();
    }
}
