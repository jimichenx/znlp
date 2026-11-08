package dev.znlp.fasttext;

/**
 * 分数和标签的配对类，用于存储预测结果中的分数和对应的标签。
 */
public class ScoreLabelPair {
    /**
     * 分数值
     */
    public float score;
    
    /**
     * 对应的标签
     */
    public String label;

    /**
     * 构造ScoreLabelPair实例
     *
     * @param score 分数值
     * @param label 对应的标签
     */
    public ScoreLabelPair(float score, String label) {
        this.score = score;
        this.label = label;
    }

    /**
     * 返回ScoreLabelPair的字符串表示
     *
     * @return 格式为"[label,score]"的字符串
     */
    @Override
    public String toString() {
        return "[" + label + "," + score + "]";
    }
}
