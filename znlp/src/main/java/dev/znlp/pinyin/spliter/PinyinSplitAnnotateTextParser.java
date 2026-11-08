package dev.znlp.pinyin.spliter;

import dev.znlp.perceptron.AnnotateTextParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼音分割的标注文本解析器
 * <p>
 * 将完整联系的拼音输入解析为字符-标签对
 * 例如：wanzhengdepinyin => wan﹍zheng﹍de﹍pin﹍yin =>
 * [w/B, a/M, n/E, z/B, h/M, e/M, n/E, d/B, e/E, p/B, i/M, n/E, y/B, i/M, n/E]
 * <p>
 * 标签说明：
 * B - Begin（单词开始）
 * M - Middle（单词中间）
 * E - End（单词结束）
 * S - Single（单字单词）
 */
class PinyinSplitAnnotateTextParser implements AnnotateTextParser<Character, char[]> {

    static final char SEPARATOR = '﹍';

    @Override
    public char[] parseToInputList(String text) {
        // Optimized single-pass filtering without regex or temporary objects
        int len = text.length();
        if (len == 0) {
            return new char[0];
        }

        // Count non-separator chars first to allocate exact size
        int resultLen = 0;
        for (int i = 0; i < len; i++) {
            if (text.charAt(i) != SEPARATOR) {
                resultLen++;
            }
        }

        // Single-pass copy to pre-sized array
        char[] result = new char[resultLen];
        int pos = 0;
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (ch != SEPARATOR) {
                result[pos++] = ch;
            }
        }

        return result;
    }

    @Override
    public char[] listToInput(List<Pair<Character>> list) {
        if (list == null || list.isEmpty()) {
            return new char[0];
        }
        int size = list.size();
        char[] result = new char[size];
        for (int i = 0; i < size; i++) {
            result[i] = list.get(i).first();
        }

        return result;
    }

    @Override
    public List<Pair<Character>> parse(String text) {
        List<Pair<Character>> result = new ArrayList<>();

        String[] words = text.split(String.valueOf(SEPARATOR));

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            switch (word.length()) {
                case 1:
                    result.add(new Pair<>(word.charAt(0), "S"));
                    break;
                case 2:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "E"));
                    break;
                case 3:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "M"));
                    result.add(new Pair<>(word.charAt(2), "E"));
                    break;
                case 4:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "M"));
                    result.add(new Pair<>(word.charAt(2), "M"));
                    result.add(new Pair<>(word.charAt(3), "E"));
                    break;
                case 5:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "M"));
                    result.add(new Pair<>(word.charAt(2), "M"));
                    result.add(new Pair<>(word.charAt(3), "M"));
                    result.add(new Pair<>(word.charAt(4), "E"));
                    break;
                case 6:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "M"));
                    result.add(new Pair<>(word.charAt(2), "M"));
                    result.add(new Pair<>(word.charAt(3), "M"));
                    result.add(new Pair<>(word.charAt(4), "M"));
                    result.add(new Pair<>(word.charAt(5), "E"));
                    break;
                case 7:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "M"));
                    result.add(new Pair<>(word.charAt(2), "M"));
                    result.add(new Pair<>(word.charAt(3), "M"));
                    result.add(new Pair<>(word.charAt(4), "M"));
                    result.add(new Pair<>(word.charAt(5), "M"));
                    result.add(new Pair<>(word.charAt(6), "E"));
                    break;
                case 8:
                    result.add(new Pair<>(word.charAt(0), "B"));
                    result.add(new Pair<>(word.charAt(1), "M"));
                    result.add(new Pair<>(word.charAt(2), "M"));
                    result.add(new Pair<>(word.charAt(3), "M"));
                    result.add(new Pair<>(word.charAt(4), "M"));
                    result.add(new Pair<>(word.charAt(5), "M"));
                    result.add(new Pair<>(word.charAt(6), "M"));
                    result.add(new Pair<>(word.charAt(7), "E"));
                    break;
                default:
                    // 长度大于8的情况
                    result.add(new Pair<>(word.charAt(0), "B"));
                    for (int i = 1; i < word.length() - 1; i++) {
                        result.add(new Pair<>(word.charAt(i), "M"));
                    }
                    result.add(new Pair<>(word.charAt(word.length() - 1), "E"));
                    break;
            }
        }

        return result;
    }

}