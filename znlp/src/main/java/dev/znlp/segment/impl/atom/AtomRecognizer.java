package dev.znlp.segment.impl.atom;

import dev.znlp.base.common.ZeroCopyString;
import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.base.dat.StringIntDATMap.DATMapLongMatcherInt;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 原子识别器
 * <p>
 * 负责识别文本中的时间、数字、数量词、单词等特殊模式。
 * 该类的设计使得识别逻辑可以独立进行单元测试。
 *
 * @author jimichan
 */
public class AtomRecognizer {

    /**
     * 识别结果回调接口
     */
    @FunctionalInterface
    public interface RecognitionCallback {
        /**
         * 当识别到一个结果时调用
         *
         * @param result 识别结果
         */
        void onRecognized(AtomRecognitionResult result);
    }

    private static final ThreadLocal<char[]> NORMALIZED = ThreadLocal.withInitial(() -> new char[64]);

    private final StringIntDATMap dat;
    private final Set<Character> chineseNumSet;
    private final Pattern emailPattern;
    private final Pattern xPattern;

    /**
     * 使用指定的模板数据构造识别器
     *
     * @param dat 模板数据（DAT映射）
     */
    public AtomRecognizer(StringIntDATMap dat) {
        this.dat = dat;

        // 初始化中文数字集合
        this.chineseNumSet = new HashSet<>();
        chineseNumSet.add('零');
        chineseNumSet.add('一');
        chineseNumSet.add('二');
        chineseNumSet.add('三');
        chineseNumSet.add('四');
        chineseNumSet.add('五');
        chineseNumSet.add('六');
        chineseNumSet.add('七');
        chineseNumSet.add('八');
        chineseNumSet.add('九');
        chineseNumSet.add('两');
        chineseNumSet.add('壹');
        chineseNumSet.add('贰');
        chineseNumSet.add('叁');
        chineseNumSet.add('肆');
        chineseNumSet.add('伍');
        chineseNumSet.add('陆');
        chineseNumSet.add('柒');
        chineseNumSet.add('捌');
        chineseNumSet.add('玖');
        chineseNumSet.add('拾');
        chineseNumSet.add('十');
        chineseNumSet.add('百');
        chineseNumSet.add('千');
        chineseNumSet.add('万');
        chineseNumSet.add('亿');

        // 编译正则表达式
        this.emailPattern = Pattern.compile("[NA]+@[NA]+NA+");
        this.xPattern = Pattern.compile("A+[N\\-][N\\-A]*");
    }

    /**
     * 识别文本中的原子模式（使用回调方式，避免创建List）
     *
     * @param chars    字符数组
     * @param callback 识别结果回调
     */
    public void recognize(char[] chars, RecognitionCallback callback) {
        recognize(chars, 0, chars == null ? 0 : chars.length, callback);
    }

    public void recognize(char[] chars, int origin, int length, RecognitionCallback callback) {
        if (chars == null || length <= 0) {
            return;
        }

        // 先扫描，判断是否包含数字、英文字母等必要元素
        boolean foundNum = false;
        boolean foundAlpha = false;
        boolean foundAt = false;

        int end = origin + length;
        for (int i = origin; i < end; i++) {
            char c = chars[i];
            if (c < '{') {
                if (c >= '0' && c <= '9') {
                    foundNum = true;
                } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                    foundAlpha = true;
                }
            } else {
                if (chineseNumSet.contains(c)) {
                    foundNum = true;
                }
            }

            if (foundAlpha || foundNum) {
                break;
            }
        }

        if (!foundNum && !foundAlpha) {
            // 没有数字和字母，直接返回
            return;
        }

        char[] normalizedChars = normalizeWindow(chars, origin, length);

        for (int i = origin; i < end; i++) {
            if (chars[i] == '@') {
                foundAt = true;
                break;
            }
        }

        DATMapLongMatcherInt match = dat.matchLong(normalizedChars, 0, length);

        int bigXEnd = -1;
        boolean foundBigX = false;

        while (match.next()) {
            int type = match.getValue();
            int offset = match.getBegin();
            int span = match.getLength();

            // 安全地获取模板类型
            TemplateType templateType = getTemplateType(type);
            if (templateType == null) {
                continue; // 跳过未知类型
            }

            switch (templateType) {
                case TIME:
                    callback.onRecognized(new AtomRecognitionResult(offset, span, TemplateType.TIME));
                    break;

                case NUMBER:
                    if (span == 1 && chars[origin + offset] == '.') {
                        // 跳过单独的点
                    } else {
                        if (offset == bigXEnd) {
                            foundBigX = true;
                        }
                        callback.onRecognized(new AtomRecognitionResult(offset, span, TemplateType.NUMBER));
                    }
                    break;

                case MQ:
                    callback.onRecognized(new AtomRecognitionResult(offset, span, TemplateType.MQ));
                    break;

                case WORD:
                    callback.onRecognized(new AtomRecognitionResult(offset, span, TemplateType.WORD));
                    bigXEnd = offset + span;
                    break;

                case CONNECT:
                    if (offset == bigXEnd) {
                        foundBigX = true;
                    }
                    // 连接符不添加到结果中
                    break;
            }
        }

        // 处理email模式
        ZeroCopyString view = new ZeroCopyString(normalizedChars, 0, length);
        if (foundAt) {
            recognizeEmail(view, callback);
        }

        if (foundBigX) {
            recognizeXPattern(view, callback);
        }
    }

    /**
     * 识别文本中的原子模式（返回List，便利方法）
     * <p>
     * 内部使用回调方式实现，如果需要更好的性能，建议直接使用 {@link #recognize(char[], RecognitionCallback)} 方法
     *
     * @param chars 字符数组
     * @return 识别结果列表
     */
    public List<AtomRecognitionResult> recognize(char[] chars) {
        List<AtomRecognitionResult> results = new ArrayList<>();
        recognize(chars, results::add);
        return results;
    }

    /**
     * 归一化字符数组
     * <p>
     * 将数字、字母、中文数字等转换为特定字符：
     * - 数字和点 -> 'N'
     * - 字母和下划线 -> 'A'
     * - 中文数字 -> 'Z'
     *
     * @param chars 原始字符数组
     * @return 归一化后的字符数组
     */
    private char[] normalizeWindow(char[] chars, int origin, int length) {
        char[] normalized = NORMALIZED.get();
        if (normalized.length < length) {
            normalized = new char[length];
            NORMALIZED.set(normalized);
        }
        for (int i = 0; i < length; i++) {
            char c = chars[origin + i];
            if (c < '{') {
                if (c >= '0' && c <= '9' || c == '.') {
                    normalized[i] = 'N';
                } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_') {
                    normalized[i] = 'A';
                } else {
                    normalized[i] = c;
                }
            } else if (chineseNumSet.contains(c)) {
                normalized[i] = 'Z';
            } else {
                normalized[i] = c;
            }
        }
        return normalized;
    }

    public char[] normalizeChars(char[] chars) {
        char[] normalized = Arrays.copyOf(chars, chars.length);

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c < '{') {
                if (c >= '0' && c <= '9') {
                    normalized[i] = 'N';
                } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_') {
                    normalized[i] = 'A';
                } else if (c == '.') {
                    // 把点也归一化为数字N的一种形式
                    normalized[i] = 'N';
                }
            } else {
                if (chineseNumSet.contains(c)) {
                    normalized[i] = 'Z';
                }
            }
        }

        return normalized;
    }

    /**
     * 识别email模式
     *
     * @param normalizedChars 归一化后的字符数组
     * @param callback        识别结果回调
     */
    private void recognizeEmail(CharSequence normalizedChars, RecognitionCallback callback) {
        Matcher emailMatcher = emailPattern.matcher(normalizedChars);

        while (emailMatcher.find()) {
            callback.onRecognized(new AtomRecognitionResult(
                    emailMatcher.start(),
                    emailMatcher.end() - emailMatcher.start(),
                    TemplateType.WORD
            ));
        }
    }

    /**
     * 识别复杂的x模式
     *
     * @param normalizedChars 归一化后的字符数组
     * @param callback        识别结果回调
     */
    private void recognizeXPattern(CharSequence normalizedChars, RecognitionCallback callback) {
        Matcher xMatcher = xPattern.matcher(normalizedChars);

        while (xMatcher.find()) {
            callback.onRecognized(new AtomRecognitionResult(
                    xMatcher.start(),
                    xMatcher.end() - xMatcher.start(),
                    TemplateType.WORD
            ));
        }
    }

    /**
     * 检查字符是否为中文数字
     *
     * @param c 字符
     * @return 是否为中文数字
     */
    public boolean isChineseNumber(char c) {
        return chineseNumSet.contains(c);
    }

    /**
     * 根据类型值获取模板类型
     *
     * @param type 类型值
     * @return 模板类型，如果值无效则返回null
     */
    private TemplateType getTemplateType(int type) {
        if (type >= 0 && type < TemplateType.values().length) {
            return TemplateType.values()[type];
        }
        return null;
    }
}

