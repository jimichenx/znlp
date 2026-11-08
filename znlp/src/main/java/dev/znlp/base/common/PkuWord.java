package dev.znlp.base.common;

import java.util.ArrayList;
import java.util.List;

/**
 * PkuWord 支持复合词（subWord 列表）。
 */
public class PkuWord {
    private String word;
    private String pos;
    private final List<PkuWord> subWord = new ArrayList<>();

    public PkuWord(String word, String pos) {
        this.word = word;
        this.pos = pos;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public List<PkuWord> getSubWord() {
        return subWord;
    }

    public boolean hasSub() {
        return !subWord.isEmpty();
    }

    @Override
    public String toString() {
        if (hasSub()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < subWord.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(subWord.get(i).toString());
            }
            sb.append("]").append(pos);
            return sb.toString();
        } else {
            return word + "/" + pos;
        }
    }
}

