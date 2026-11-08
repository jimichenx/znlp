package dev.znlp.segment.impl.personname;

import dev.znlp.perceptron.AnnotateTextParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 人名识别的标注文本解析器
 * <p>
 * 将标注格式的输入解析为字符-标签对
 * 例如："与X﹍令B﹍计M﹍划E﹍妻Y" =>
 * [与/X, 令/B, 计/M, 划/E, 妻/Y]
 * <p>
 * 标签说明：
 * B - Begin（人名开始）
 * M - Middle（人名中间）
 * E - End（人名结束）
 * O - Other（非人名）
 * X - 人名前的字符
 * Y - 人名后的字符
 * Z - 其他标记
 */
class PersonNameAnnotateTextParser implements AnnotateTextParser<Character, char[]> {

    static final char SEPARATOR = '﹍';

    @Override
    public char[] parseToInputList(String text) {
        // 优化的单遍过滤，不使用正则表达式或临时对象
        int len = text.length();
        if (len == 0) {
            return new char[0];
        }

        // 标注格式: 字B﹍字M﹍字E
        // 需要提取每个标注项的第一个字符
        List<Character> chars = new ArrayList<>();
        int i = 0;
        while (i < len) {
            char ch = text.charAt(i);
            if (ch == SEPARATOR) {
                i++;
                continue;
            }
            // 当前字符是实际字符，下一个是标签
            chars.add(ch);
            i += 2; // 跳过字符和标签
        }

        char[] result = new char[chars.size()];
        for (int j = 0; j < chars.size(); j++) {
            result[j] = chars.get(j);
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

        // 标注格式: 字B﹍字M﹍字E
        String[] items = text.split(String.valueOf(SEPARATOR));

        for (String item : items) {
            if (item.isEmpty()) {
                continue;
            }
            if (item.length() >= 2) {
                char zi = item.charAt(0);
                String label = String.valueOf(item.charAt(1));
                result.add(new Pair<>(zi, label));
            }
        }

        return result;
    }

    /**
     * 根据标签获取对应的ID
     */
    static int labelToId(String tag) {
        switch (tag) {
            case "B":
                return 0;
            case "M":
                return 1;
            case "E":
                return 2;
            case "O":
                return 3;
            case "X":
                return 4;
            case "Y":
                return 5;
            case "Z":
                return 6;
            default:
                return 3; // 默认为 O
        }
    }
}
