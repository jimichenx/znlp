package dev.znlp.fasttext;

/**
 * 分数和ID的配对类，用于存储预测结果中的分数和对应的ID。
 *
 * @param score 分数值
 * @param id    对应的ID
 */
public record ScoreIdPair(float score, int id) {
    /**
     * 构造ScoreIdPair实例
     *
     * @param score 分数值
     * @param id    对应的ID
     */
    public ScoreIdPair {
    }

    /**
     * 返回ScoreIdPair的字符串表示
     *
     * @return 格式为"[id,score]"的字符串
     */
    @Override
    public String toString() {
        return "[" + id + "," + score + "]";
    }
}
