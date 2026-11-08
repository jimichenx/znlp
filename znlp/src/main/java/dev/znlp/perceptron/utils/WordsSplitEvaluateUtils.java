package dev.znlp.perceptron.utils;

import java.util.List;

/**
 * 拼音切分评估工具类
 * 提供拼音切分正确性评估的静态方法
 */
public class WordsSplitEvaluateUtils {

    /**
     * 计算拼音切分正确的数量
     * 通过字符位置对齐来比较，正确处理错误切分，不会因为一个错误影响后续统计
     *
     * @param gold 正确答案的拼音列表
     * @param pred 预测结果的拼音列表
     * @return 正确匹配的拼音数量
     */
    public static int wordCorrect(List<String> gold, List<String> pred) {
        int goldIndex = 0;
        int predIndex = 0;
        int goldLen = 0;
        int predLen = 0;

        int correct = 0;
        while (goldIndex < gold.size() && predIndex < pred.size()) {
            if (goldLen == predLen) {
                // 字符位置对齐，比较当前拼音是否相同
                if (gold.get(goldIndex).equals(pred.get(predIndex))) {
                    correct++;
                }
                // 无论是否匹配，都继续前进
                goldLen += gold.get(goldIndex).length();
                predLen += pred.get(predIndex).length();
                goldIndex++;
                predIndex++;
            } else if (goldLen < predLen) {
                // 预测结果超前，跳过正确答案中的当前拼音
                goldLen += gold.get(goldIndex).length();
                goldIndex++;
            } else {
                // 正确答案超前，跳过预测结果中的当前拼音
                predLen += pred.get(predIndex).length();
                predIndex++;
            }
        }
        return correct;
    }
}

