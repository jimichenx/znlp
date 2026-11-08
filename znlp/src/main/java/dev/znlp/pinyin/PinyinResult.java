package dev.znlp.pinyin;

import dev.znlp.pinyin.enums.Pinyin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 拼音转换结果类
 * <p>
 * 负责存储和处理拼音转换的结果，支持多种格式化输出方式，
 * 包括字符串、列表、首字母等形式，并提供模糊拼音和保留特殊字符的功能
 *
 * @author jimichan
 */
public final class PinyinResult {

    /**
     * 拼音列表
     */
    private final List<Pinyin> pinyinList;

    /**
     * 原始文本
     */
    private final String originalText;

    /**
     * 是否保留标点符号
     */
    private boolean keepPunctuation = false;

    /**
     * 是否保留数字
     */
    private boolean keepNumber = true;

    /**
     * 是否保留字母
     */
    private boolean keepAlpha = true;

    /**
     * 是否保留其他字符
     */
    private boolean keepOthers = false;

    /**
     * 是否启用模糊拼音
     */
    private boolean useFuzzyPinyin;

    /**
     * 模糊拼音模式的正则表达式
     */
    private static final Pattern FUZZY_PATTERN = Pattern.compile("(^zh|^ch|^sh|iang$|ang$|ing$|eng$|uang$)");

    /**
     * 模糊拼音映射表
     */
    private static final Map<String, String> FUZZY_MAP = buildFuzzyMap();

    /**
     * 完整模糊拼音对应表（预计算）
     */
    private static final Map<String, String> FULL_FUZZY_PINYIN_MAP = buildFullFuzzyPinyinMap();

    /**
     * 构造方法
     *
     * @param pinyinList   拼音列表
     * @param originalText 原始文本
     */
    PinyinResult(List<Pinyin> pinyinList, String originalText) {
        this.pinyinList = pinyinList;
        this.originalText = originalText;
    }

    /**
     * 获取原始拼音列表
     *
     * @return 拼音列表
     */
    public List<Pinyin> getPinyinList() {
        return pinyinList;
    }

    /**
     * 设置是否启用模糊拼音
     *
     * @param fuzzy 是否启用模糊拼音
     * @return 当前对象，支持链式调用
     */
    public PinyinResult fuzzy(boolean fuzzy) {
        this.useFuzzyPinyin = fuzzy;
        return this;
    }

    /**
     * 设置是否保留标点符号
     *
     * @param keep 是否保留标点符号
     * @return 当前对象，支持链式调用
     */
    public PinyinResult keepPunctuation(boolean keep) {
        this.keepPunctuation = keep;
        return this;
    }

    /**
     * 设置是否保留数字
     *
     * @param keep 是否保留数字
     * @return 当前对象，支持链式调用
     */
    public PinyinResult keepNumber(boolean keep) {
        this.keepNumber = keep;
        return this;
    }

    /**
     * 设置是否保留字母
     *
     * @param keep 是否保留字母
     * @return 当前对象，支持链式调用
     */
    public PinyinResult keepAlpha(boolean keep) {
        this.keepAlpha = keep;
        return this;
    }

    /**
     * 设置是否保留其他字符
     *
     * @param keep 是否保留其他字符
     * @return 当前对象，支持链式调用
     */
    public PinyinResult keepOthers(boolean keep) {
        this.keepOthers = keep;
        return this;
    }

    /**
     * 输出为字符串（默认使用空格分隔）
     *
     * @return 拼音字符串
     */
    public String asString() {
        return asString(" ");
    }

    /**
     * 输出为字符串（使用指定分隔符）
     *
     * @param splitter 分隔符
     * @return 拼音字符串
     */
    public String asString(String splitter) {
        return joinSkipNull(asList(), splitter);
    }

    /**
     * 输出为首字母字符串（默认使用空格分隔）
     *
     * @return 首字母字符串
     */
    public String asHeadString() {
        return asHeadString(" ");
    }

    /**
     * 输出为首字母字符串（使用指定分隔符）
     *
     * @param splitter 分隔符
     * @return 首字母字符串
     */
    public String asHeadString(String splitter) {
        return joinSkipNull(asHeadList(), splitter);
    }

    /**
     * 输出为拼音字符串列表
     *
     * @return 拼音字符串列表
     */
    public List<String> asList() {
        List<String> list = new ArrayList<>(pinyinList.size());
        int index = 0;

        for (Pinyin pinyin : pinyinList) {
            if (pinyin == Pinyin.none5) {
                char character = originalText.charAt(index);
                String target = processNonPinyinCharacter(character);
                list.add(target);
            } else {
                String pinyinWithoutTone = pinyin.getPinyinWithoutTone();
                if (useFuzzyPinyin) {
                    list.add(FULL_FUZZY_PINYIN_MAP.getOrDefault(pinyinWithoutTone, pinyinWithoutTone));
                } else {
                    list.add(pinyinWithoutTone);
                }
            }
            index++;
        }

        return list;
    }

    /**
     * 输出为拼音首字母列表
     *
     * @return 拼音首字母列表
     */
    public List<Character> asHeadList() {
        List<Character> list = new ArrayList<>(pinyinList.size());
        int index = 0;

        for (Pinyin pinyin : pinyinList) {
            if (pinyin == Pinyin.none5) {
                char character = originalText.charAt(index);
                if (shouldKeepNonPinyinCharacter(character)) {
                    list.add(character);
                } else {
                    list.add(null);
                }
            } else {
                list.add(pinyin.getFirstChar());
            }
            index++;
        }

        return list;
    }

    /**
     * 处理非拼音字符的保留逻辑
     *
     * @param character 字符
     * @return 保留的字符字符串，不需要保留时返回null
     */
    private String processNonPinyinCharacter(char character) {
        if (keepPunctuation && Character.isWhitespace(character)) {
            return String.valueOf(character);
        } else if (keepNumber && (character >= '0' && character <= '9')) {
            return String.valueOf(character);
        } else if (keepAlpha && ((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z'))) {
            return String.valueOf(character);
        } else if (keepOthers) {
            return String.valueOf(character);
        }

        return null;
    }

    /**
     * 判断是否应该保留非拼音字符
     *
     * @param character 字符
     * @return 是否应该保留
     */
    private boolean shouldKeepNonPinyinCharacter(char character) {
        if (keepPunctuation && Character.isWhitespace(character)) {
            return true;
        } else if (keepNumber && (character >= '0' && character <= '9')) {
            return true;
        } else if (keepAlpha && ((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z'))) {
            return true;
        } else return keepOthers;
    }

    /**
     * 连接列表，跳过null值
     *
     * @param list 列表
     * @param splitter 分隔符
     * @return 连接后的字符串
     */
    private String joinSkipNull(List<?> list, String splitter) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for (Object item : list) {
            if (item != null) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    if (!splitter.isEmpty()) {
                        sb.append(splitter);
                    }
                }
                sb.append(item);
            }
        }

        return sb.toString();
    }

    /**
     * 构建模糊拼音映射表
     *
     * @return 模糊拼音映射表
     */
    private static Map<String, String> buildFuzzyMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("zh", "z");
        map.put("ch", "c");
        map.put("sh", "s");
        map.put("eng", "en");
        map.put("ang", "an");
        map.put("ing", "in");
        map.put("iang", "ian");
        map.put("uang", "uan");
        return map;
    }

    /**
     * 构建完整的模糊拼音对应表（预计算所有可能的拼音转换）
     *
     * @return 完整模糊拼音对应表
     */
    private static Map<String, String> buildFullFuzzyPinyinMap() {
        Map<String, String> map = new HashMap<>();

        for (Pinyin value : Pinyin.values()) {
            String shengmu = value.getShengmu().toString();
            String yunmu = value.getYunmu().toString();

            // 声母模糊转换
            if (shengmu.equals("zh")) {
                shengmu = "z";
            } else if (shengmu.equals("ch")) {
                shengmu = "c";
            } else if (shengmu.equals("sh")) {
                shengmu = "s";
            }

            // 韵母模糊转换
            if (yunmu.equals("eng")) {
                yunmu = "en";
            } else if (yunmu.equals("ang")) {
                yunmu = "an";
            } else if (yunmu.equals("ing")) {
                yunmu = "in";
            } else if (yunmu.equals("iang")) {
                yunmu = "ian";
            } else if (yunmu.equals("uang")) {
                yunmu = "uan";
            }

            map.put(value.getPinyinWithoutTone(), shengmu + yunmu);
        }

        return map;
    }

    @Override
    public String toString() {
        return asString();
    }
}