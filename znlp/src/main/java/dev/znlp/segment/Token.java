package dev.znlp.segment;

import java.io.Serializable;
import java.util.List;

/**
 * 一个词切分单元，用户可以直接访问此单词的全部属性
 * 改类类名以后不可以修改
 *
 * @author jimichan
 */
public final class Token implements Serializable, WordAndNature {

    static final long serialVersionUID = 1L;

    /**
     * 词语
     */
    public final String word;

    /**
     * 词性
     */
    private Nature nature;

    /**
     * 在文本中的起始位置
     */
    public int offset;

    private int posInc = 1;

    /**
     * 索引分词，切分子词
     */
    @SuppressWarnings("serial")
    private List<Token> subword;

    private String customFlag;


    public Token(String word) {
        this.word = word;
    }

    /**
     * 构造一个词
     *
     * @param word   词语
     * @param nature 词性
     */
    public Token(String word, Nature nature) {
        this.word = word;
        this.nature = nature;
    }

    /**
     * 构造一个单词
     *
     * @param word   词语
     * @param nature 词性
     * @param offset 文本偏移位置
     */
    public Token(String word, Nature nature, int offset) {
        this.word = word;
        this.nature = nature;
        this.offset = offset;
    }


    public Nature getNature() {
        return nature;
    }

    public String getNatureString() {
        if (nature == null) {
            return "";
        } else {
            return nature.name();
        }
    }

    public void setNature(Nature nature) {
        this.nature = nature;
    }

    public int getOffset() {
        return offset;
    }

    public List<Token> getSubword() {
        return subword;
    }


    public boolean hasSubword() {
        return subword != null && !subword.isEmpty();
    }

    @Override
    public String getWord() {
        return word;
    }

    @Override
    public String getNatureName() {
        return getNatureString();
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setSubword(List<Token> subword) {
        this.subword = subword;
    }

    public String getCustomFlag() {
        return customFlag;
    }

    public void setCustomFlag(String customFlag) {
        this.customFlag = customFlag;
    }

    /**
     * 长度
     *
     * @return 长度
     */
    public int length() {
        return word.length();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (nature == null) {
            sb.append(word);
        } else {
            sb.append(word).append("/").append(nature);
        }

        if (subword != null) {
            sb.append(subword);
        }

        return sb.toString();
    }

    public int getPosInc() {
        return posInc;
    }

    public Token setPosInc(int posInc) {
        this.posInc = posInc;
        return this;
    }
}
