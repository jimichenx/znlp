package dev.znlp.segment.impl.cws;

import dev.znlp.perceptron.AnnotateTextParser;

import java.util.ArrayList;
import java.util.List;

/**
 * CWS 标注解析器。词之间用 {@code ﹍} 分隔，标签为 BMES。
 */
class CwsAnnotateTextParser implements AnnotateTextParser<Character, char[]> {

    static final char SEPARATOR = '﹍';

    @Override
    public char[] parseToInputList(String text) {
        int len = text.length();
        if (len == 0) {
            return new char[0];
        }
        int resultLen = 0;
        for (int i = 0; i < len; i++) {
            if (text.charAt(i) != SEPARATOR) {
                resultLen++;
            }
        }
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
            int n = word.length();
            if (n == 1) {
                result.add(new Pair<>(word.charAt(0), "S"));
                continue;
            }
            result.add(new Pair<>(word.charAt(0), "B"));
            for (int i = 1; i < n - 1; i++) {
                result.add(new Pair<>(word.charAt(i), "M"));
            }
            result.add(new Pair<>(word.charAt(n - 1), "E"));
        }
        return result;
    }
}
