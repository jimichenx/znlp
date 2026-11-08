package dev.znlp.segment.impl.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 纠错词条
 * 例如：第几套/房
 *
 * @author jimichan
 */
public record CorrectionWord(String raw, String path, int[] words) {

    @Override
    public String toString() {
        return "CorrectionWord{" +
                "path='" + path + '\'' +
                ", raw='" + raw + '\'' +
                ", words=" + Arrays.toString(words) +
                '}';
    }

    /**
     * 解析纠错规则
     * 例如：第几套/房
     *
     * @param line 规则字符串
     * @return CorrectionWord
     */
    public static CorrectionWord parse(String line) {
        String raw = line.trim();
        String[] parts = raw.split("/");

        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }

        StringBuilder pathBuilder = new StringBuilder();
        int[] words = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            String word = list.get(i);
            pathBuilder.append(word);
            words[i] = word.length();
        }

        String path = pathBuilder.toString();
        return new CorrectionWord(raw, path, words);
    }
}
