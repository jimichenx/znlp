package dev.znlp.starspace;

import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.MinHeapTopKInt;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 预测辅助类，用于使用训练好的 StarSpace 模型执行预测（查找 top-K 相似标签）
 * <p>
 * 该类封装了预测相关的功能，包括加载基础文档、计算相似度、排序结果等。
 * 它是模型训练完成后用于实际预测的主要接口之一。
 */
public class StarSpacePrediction {

    /** StarSpace 模型 */
    private final StarSpace model;
    /** 基础文档 */
    private final StarSpace.BaseDocs baseDocs;

    /**
     * 构造预测器
     * @param model StarSpace 模型
     * @param basedoc 基础文档文件路径
     */
    public StarSpacePrediction(StarSpace model, String basedoc) {
        this.model = model;
        this.baseDocs = model.loadBaseDocs(basedoc);
    }

    /**
     * 预测单个文档（使用默认 K 值 5）
     * @param doc 文档文本
     * @return 预测结果列表
     */
    public List<Prediction> predictOne(String doc) {
        return predictOne(model.dict().parseDoc(doc), 5);
    }

    /**
     * 预测单个文档
     * @param doc 文档文本
     * @param k 返回的预测结果数量
     * @return 预测结果列表
     */
    public List<Prediction> predictOne(String doc, int k) {
        return predictOne(model.dict().parseDoc(doc), k);
    }

    /**
     * 预测单个输入
     * @param input 输入特征列表
     * @param k 返回的预测结果数量
     * @return 预测结果列表
     */
    public List<Prediction> predictOne(List<Utils.XPair> input, int k) {
        Vector lhsM = model.projectLHS(input);

        MinHeapTopKInt topMax = new MinHeapTopKInt(k);

        for (int i = 0; i < baseDocs.baseDocVectors.size(); i++) {
            float score = model.args().similarity(lhsM, baseDocs.baseDocVectors.get(i));
            topMax.push(i, score);
        }

        return topMax.resort().stream()
                .map(p -> new Prediction(p.second(), p.first())) // Pair(id, score) -> Prediction(score, second=id)
                .collect(Collectors.toList());
    }

    /**
     * 预测结果类
     */
    public static class Prediction {
        /** 预测分数 */
        public float score;
        /** 预测的 ID/索引 */
        public int second; // This is the ID/Index

        /**
         * 构造预测结果
         * @param score 预测分数
         * @param second 预测的 ID/索引
         */
        public Prediction(float score, int second) {
            this.score = score;
            this.second = second;
        }

        /**
         * 转换为字符串表示
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "Prediction(score=" + score + ", second=" + second + ")";
        }
    }
}
