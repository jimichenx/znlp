package dev.znlp.base.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PKU 格式解析器，支持复合词（[ ... ]pos）解析。
 *
 * 提供：
 * - parseToWords(line): 将一行解析为 PkuWord 列表（保留复合词结构）
 * - parseToFlatWords(line): 将一行解析为扁平的 PkuWord 列表（展开复合词的子项）
 * - parseToFlatString(line): 将一行解析并以 "word/pos word/pos ..." 的形式返回字符串（过滤掉空 pos）
 */
public class PkuParser {

    /**
     * 将一行解析为带子词结构的 PkuWord 列表（保留复合词）。
     */
    public static List<PkuWord> parseToWords(String line) {
        if (line == null || line.isEmpty()) {
            return Collections.emptyList();
        }
        List<PkuWord> result = new ArrayList<>();
        String[] words = line.split(" ");
        PkuWord bigWord = null;
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            int x = word.lastIndexOf(']');
            if (word.startsWith("[") && !word.equals("[/w")) {
                // start of big word
                bigWord = new PkuWord("", "");
                String rw = word.substring(1);
                bigWord.getSubWord().add(simpleWord(rw));

                // [xxx]pos single token
                if (x > 0 && x < word.length()) {
                    bigWord = null;
                    int lsp = rw.lastIndexOf("/");
                    // siWord = rw.substring(0, lsp) / word.substring(lsp+2, x)
                    String w = rw.substring(0, lsp);
                    String pos1 = "";
                    if (lsp + 2 <= word.length()) {
                        // original kotlin used word.substring(lsp + 2, x) -- careful: lsp was index in rw, not word
                        pos1 = word.substring(lsp + 2, x);
                    }
                    String pos2 = "";
                    if (x + 1 < word.length()) {
                        pos2 = word.substring(x + 1);
                    }
                    PkuWord siWord = new PkuWord(w, pos1);
                    PkuWord siWord2 = new PkuWord(w, pos2);
                    siWord2.getSubWord().add(siWord);
                    result.add(siWord2);
                }

            } else if (bigWord != null && x > 0 && x < word.length()) {
                // end of big word
                String rw = word.substring(0, x);
                bigWord.getSubWord().add(simpleWord(rw));
                String ppos = word.substring(x + 1);
                if (ppos.startsWith("/")) {
                    ppos = ppos.substring(1);
                }
                bigWord.setPos(ppos);
                result.add(bigWord);
                bigWord = null;

            } else {
                if (bigWord != null) {
                    bigWord.getSubWord().add(simpleWord(word));
                } else {
                    result.add(simpleWord(word));
                }
            }
        }
        return result;
    }

    /**
     * 将一行解析为扁平的 PkuWord 列表（展开复合词中的子词）。
     */
    public static List<PkuWord> parseToFlatWords(String line) {
        List<PkuWord> words = parseToWords(line);
        List<PkuWord> flat = new ArrayList<>();
        for (PkuWord w : words) {
            if (w.hasSub()) {
                flat.addAll(w.getSubWord());
            } else {
                flat.add(w);
            }
        }
        return flat;
    }

    /**
     * 将一行解析并返回扁平的 "word/pos ..." 字符串（过滤空 pos）
     */
    public static String parseToFlatString(String line) {
        List<PkuWord> flat = parseToFlatWords(line);
        List<String> out = new ArrayList<>();
        for (PkuWord w : flat) {
            String pos = w.getPos() == null ? "" : w.getPos().trim();
            String word = w.getWord() == null ? "" : w.getWord().trim();
            if (!pos.isEmpty()) {
                out.add(word + "/" + pos);
            }
        }
        return String.join(" ", out);
    }

    private static PkuWord simpleWord(String token) {
        if (token == null) return new PkuWord("", "");
        int sp = token.lastIndexOf('/');
        if (sp < 0) {
            return new PkuWord(token, "");
        }
        if (sp == 0) {
            return new PkuWord("", token.substring(1));
        }
        return new PkuWord(token.substring(0, sp), token.substring(sp + 1));
    }
}

