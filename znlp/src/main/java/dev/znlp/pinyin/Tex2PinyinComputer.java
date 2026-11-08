package dev.znlp.pinyin;

import dev.znlp.base.dat.DoubleArrayTrieMap;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.pinyin.enums.Pinyin;

import java.util.*;

/**
 * 文本转拼音计算机
 * <p>
 * 负责将中文文本转换为拼音，支持系统词典和自定义词典。
 * 用双数组收集每个起点的命中，留下更长的词，再按音节个数向前走。
 *
 * @author jimichan
 */
class Tex2PinyinComputer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Tex2PinyinComputer.class);

    /**
     * 拼音词表。值是该词的音节序列。
     */
    private DoubleArrayTrieMap<Pinyin[]> words;

    /**
     * 自定义拼音映射表
     */
    private Map<String, Pinyin[]> customPinyin;

    /**
     * 系统内置拼音词典
     */
    private final PinyinDict systemDict;

    /**
     * 构造方法
     *
     * @param systemDict   系统内置拼音词典
     * @param customPinyin 自定义拼音映射表
     */
    public Tex2PinyinComputer(PinyinDict systemDict, Map<String, Pinyin[]> customPinyin) {
        this.systemDict = systemDict;
        this.customPinyin = customPinyin;
        rebuildTrie();
    }

    /**
     * 构造方法，仅使用系统内置词典
     *
     * @param systemDict 系统内置拼音词典
     */
    public Tex2PinyinComputer(PinyinDict systemDict) {
        this.systemDict = systemDict;
        this.customPinyin = null;
        rebuildTrie();
    }

    /**
     * 设置自定义拼音映射表（字符串格式）
     *
     * @param customPinyinMap 自定义拼音映射表，格式：{文本: "拼音1,拼音2"}
     */
    public void setUpCustomPinyin(Map<String, String> customPinyinMap) {
        this.customPinyin = convertToPinyinArrayMap(customPinyinMap);
        rebuildTrie();
    }

    /**
     * 设置自定义拼音映射表（Pinyin数组格式）
     *
     * @param customPinyinMap 自定义拼音映射表
     */
    public void setCustomPinyin(Map<String, Pinyin[]> customPinyinMap) {
        this.customPinyin = customPinyinMap;
        rebuildTrie();
    }

    /**
     * 合并系统词典和自定义词典，并重建匹配树。自定义词覆盖同名系统词。
     */
    private void rebuildTrie() {
        long startTime = System.currentTimeMillis();

        TreeMap<String, Pinyin[]> mergedMap = new TreeMap<>();
        mergedMap.putAll(systemDict.getPinyinTable());
        if (customPinyin != null) {
            mergedMap.putAll(customPinyin);
        }
        this.words = new DoubleArrayTrieMap<>(mergedMap);

        long endTime = System.currentTimeMillis();
        logger.debug("拼音词典构建完成，耗时: {}ms", endTime - startTime);
    }

    /**
     * 将文本转换为拼音结果
     *
     * @param text 原始中文文本
     * @return 拼音转换结果对象
     */
    public PinyinResult text2Pinyin(String text) {
        return new PinyinResult(segmentLongestMatch(text.toCharArray()), text);
    }

    /**
     * 将文本转换为拼音结果（与text2Pinyin方法相同，提供兼容性接口）
     *
     * @param text 原始中文文本
     * @return 拼音转换结果对象
     */
    public PinyinResult convert(String text) {
        return this.text2Pinyin(text);
    }

    /**
     * 收集每个起点上的命中，只留下更长的键。比较的是键的字数和已经记下的音节个数，
     * 单字只保留第一个音节。游标再按音节个数前进。
     */
    private List<Pinyin> segmentLongestMatch(char[] charArray) {
        Pinyin[][] wordNet = new Pinyin[charArray.length][];
        DoubleArrayTrieMap<Pinyin[]>.MapMatcher matcher = words.match(charArray);
        while (matcher.next()) {
            int begin = matcher.getBegin();
            int length = matcher.getLength();
            if (wordNet[begin] == null || length > wordNet[begin].length) {
                Pinyin[] value = matcher.getValue();
                wordNet[begin] = length == 1 ? new Pinyin[]{value[0]} : value;
            }
        }

        List<Pinyin> pinyinList = new ArrayList<>(charArray.length);
        for (int offset = 0; offset < wordNet.length; ) {
            if (wordNet[offset] == null) {
                pinyinList.add(Pinyin.none5);
                offset++;
                continue;
            }
            Collections.addAll(pinyinList, wordNet[offset]);
            offset += wordNet[offset].length;
        }
        return pinyinList;
    }

    /**
     * 解析拼音字符串为Pinyin数组
     * 格式示例："yi1,ge4" → [Pinyin.yi1, Pinyin.ge4]
     *
     * @param text 拼音字符串，多个拼音用逗号分隔
     * @return Pinyin数组
     * @throws IllegalArgumentException 如果解析失败
     */
    public static Pinyin[] parse(String text) {
        String[] values = text.split(",");
        Pinyin[] pinyins = new Pinyin[values.length];

        for (int i = 0; i < values.length; i++) {
            try {
                pinyins[i] = Pinyin.valueOf(values[i].trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("解析拼音字符串失败: " + text, e);
            }
        }

        return pinyins;
    }

    /**
     * 将字符串格式的拼音映射转换为Pinyin数组格式的映射
     *
     * @param map 字符串格式的拼音映射
     * @return Pinyin数组格式的拼音映射
     */
    static Map<String, Pinyin[]> convertToPinyinArrayMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }

        HashMap<String, Pinyin[]> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.put(entry.getKey(), parse(entry.getValue()));
        }
        return result;
    }
}
