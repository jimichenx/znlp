package dev.znlp.fasttext;

/**
 * FastText 测试集评估结果，指标与官方 FastText 一致：
 * P@k = nfound / (k * nexamples)，R@k = nfound / nlabels。
 *
 * @param k         评估用的 top-k
 * @param nexamples 参与评估的样本数
 * @param nlabels   金标签总数
 * @param nfound    top-k 预测中命中的金标签数
 */
public record FastTextTestResult(int k, long nexamples, long nlabels, long nfound) {

    public double precision() {
        return nexamples == 0 ? Double.NaN : (double) nfound / ((long) k * nexamples);
    }

    public double recall() {
        return nlabels == 0 ? Double.NaN : (double) nfound / nlabels;
    }
}
