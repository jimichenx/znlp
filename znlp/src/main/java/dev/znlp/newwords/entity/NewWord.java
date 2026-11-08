package dev.znlp.newwords.entity;

/**
 * 新词发现结果
 */
public class NewWord {
    private final String word;
    private final int len;
    private final int freq;
    private final int docFreq;
    /**
     * 互信息。内聚程度
     */
    private final float mi;
    private final float avg_mi;
    /**
     * 左右最低熵.和两侧的黏连程度
     */
    private final float entropy;
    private final float le;
    private final float re;
    private final float idf;

    /**
     * 左右邻字种类数（Accessor Variety）
     */
    private final int leftVariety;
    private final int rightVariety;

    private float score = 0f;

    public NewWord(String word, int len, int freq, int docFreq,
                   float mi, float avg_mi, float entropy,
                   float le, float re, float idf,
                   int leftVariety, int rightVariety) {
        this.word = word;
        this.len = len;
        this.freq = freq;
        this.docFreq = docFreq;
        this.mi = mi;
        this.avg_mi = avg_mi;
        this.entropy = entropy;
        this.le = le;
        this.re = re;
        this.idf = idf;
        this.leftVariety = leftVariety;
        this.rightVariety = rightVariety;
    }

    /**
     * 兼容旧版本的构造函数（不包含leftVariety和rightVariety）
     */
    public NewWord(String word, int len, int freq, int docFreq,
                   float mi, float avg_mi, float entropy,
                   float le, float re, float idf) {
        this(word, len, freq, docFreq, mi, avg_mi, entropy, le, re, idf, 0, 0);
    }

    // Getters
    public String getWord() {
        return word;
    }

    public int getLen() {
        return len;
    }

    public int getFreq() {
        return freq;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public float getMi() {
        return mi;
    }

    public float getAvg_mi() {
        return avg_mi;
    }

    public float getEntropy() {
        return entropy;
    }

    public float getLe() {
        return le;
    }

    public float getRe() {
        return re;
    }

    public float getIdf() {
        return idf;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getLeftVariety() {
        return leftVariety;
    }

    public int getRightVariety() {
        return rightVariety;
    }
}
