package dev.znlp.pinyin;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResource;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.pinyin.enums.Pinyin;
import dev.znlp.pinyin.enums.SimplePinyin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 拼音词典加载器
 * 负责从资源文件加载拼音数据并构建索引
 *
 * @author jimichan
 */
final class PinyinDictLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PinyinDictLoader.class);

    private final NlpResourceLoader resourceLoader;

    public PinyinDictLoader(NlpResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 加载拼音资源并构建完整的词典数据
     *
     * @return 拼音词典数据容器
     */
    PinyinDictData load() {
        Map<String, Pinyin[]> pinyinTable = loadPinyinTable();
        SimplePinyin[][] charPinyin = buildCharPinyinIndex(pinyinTable);

        return new PinyinDictData(pinyinTable, charPinyin);
    }

    /**
     * 加载拼音映射表
     *
     * @return 拼音映射表
     */
    private Map<String, Pinyin[]> loadPinyinTable() {
        logger.debug("开始加载内置拼音表资源");

        List<NlpResource> resources = loadResources();
        Map<String, Pinyin[]> pinyinMap = new TreeMap<>();

        for (NlpResource resource : resources) {
            parsePinyinResource(resource, pinyinMap);
        }

        return pinyinMap;
    }

    /**
     * 加载拼音资源文件列表
     *
     * @return 资源列表
     */
    private List<NlpResource> loadResources() {
        List<NlpResource> list = new ArrayList<>();

        // 加载主拼音资源
        NlpResource mainResource = resourceLoader.loadResource("znlp/pinyin/pinyin.txt");
        if (mainResource != null) {
            list.add(mainResource);
        }

        // 尝试加载扩展拼音资源
        try {
            NlpResource extResource = resourceLoader.loadResource("znlp/pinyin/pinyin-ext.txt");
            if (extResource != null) {
                list.add(extResource);
            }
        } catch (Exception e) {
            // 扩展资源不存在,忽略
        }

        return list;
    }

    /**
     * 解析单个拼音资源文件
     *
     * @param resource  资源对象
     * @param targetMap 目标映射表
     */
    private void parsePinyinResource(NlpResource resource, Map<String, Pinyin[]> targetMap) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.inputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parsePinyinLine(line, targetMap);
            }
        } catch (Exception e) {
            logger.error("Failed to load pinyin dictionary", e);
        }
    }

    /**
     * 解析拼音行
     * 格式示例：
     * 降龙伏虎=xiang2,long2,fu2,hu3
     * 单=dan1,shan4,chan2
     *
     * @param line      行内容
     * @param targetMap 目标映射表
     */
    private void parsePinyinLine(String line, Map<String, Pinyin[]> targetMap) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        String[] param = line.split("=");
        if (param.length != 2) {
            return;
        }

        String key = param[0];
        try {
            Pinyin[] pinyins = Tex2PinyinComputer.parse(param[1]);
            if (pinyins != null) {
                targetMap.put(key, pinyins);
            }
        } catch (Exception e) {
            logger.error("parse " + param[1], e);
        }
    }

    /**
     * 构建单字拼音索引
     *
     * @param pinyinTable 拼音映射表
     * @return 字符码到拼音的索引数组
     */
    private SimplePinyin[][] buildCharPinyinIndex(Map<String, Pinyin[]> pinyinTable) {
        int maxCharCode = findMaxCharCode(pinyinTable);
        SimplePinyin[][] charIndex = new SimplePinyin[maxCharCode + 1][];
        fillCharIndex(charIndex, pinyinTable);
        return charIndex;
    }

    /**
     * 查找拼音表中单字的最大字符码
     *
     * @param pinyinTable 拼音映射表
     * @return 最大字符码
     */
    private int findMaxCharCode(Map<String, Pinyin[]> pinyinTable) {
        int max = -1;
        for (String key : pinyinTable.keySet()) {
            if (key.length() == 1) {
                int code = key.charAt(0);
                if (code > max) {
                    max = code;
                }
            }
        }
        return max;
    }

    /**
     * 填充字符索引数组
     *
     * @param charIndex   字符索引数组
     * @param pinyinTable 拼音映射表
     */
    private void fillCharIndex(SimplePinyin[][] charIndex, Map<String, Pinyin[]> pinyinTable) {
        for (Map.Entry<String, Pinyin[]> entry : pinyinTable.entrySet()) {
            String key = entry.getKey();
            Pinyin[] value = entry.getValue();

            if (key.length() == 1) {
                int code = key.charAt(0);
                charIndex[code] = toSimplePinyinArray(value);
            }
        }
    }

    /**
     * 将 Pinyin 数组转换为 SimplePinyin 数组
     *
     * @param pinyins Pinyin 数组
     * @return SimplePinyin 数组
     */
    private SimplePinyin[] toSimplePinyinArray(Pinyin[] pinyins) {
        SimplePinyin[] simples = new SimplePinyin[pinyins.length];
        for (int i = 0; i < pinyins.length; i++) {
            simples[i] = pinyins[i].getSimple();
        }
        return simples;
    }

    /**
         * 拼音词典数据容器
         * 用于封装加载后的拼音词典数据
         */
        public record PinyinDictData(Map<String, Pinyin[]> pinyinTable, SimplePinyin[][] charPinyin) {
    }
}
