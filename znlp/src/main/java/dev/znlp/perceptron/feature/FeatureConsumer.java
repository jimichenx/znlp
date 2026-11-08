package dev.znlp.perceptron.feature;

/**
 * 特征消费者接口，用于接收和处理提取的特征。
 * <p>
 * 该接口是一个函数式接口，用于在特征提取过程中接收和处理提取的特征。
 * 它提供了一种灵活的方式来处理特征，允许特征提取函数与特征处理逻辑解耦。
 * <p>
 * 在结构化感知机框架中，特征消费者通常用于：
 * 1. 将提取的特征添加到特征向量中
 * 2. 统计特征出现次数
 * 3. 验证特征的有效性
 * <p>
 * 该接口的设计允许使用Lambda表达式或方法引用，使代码更加简洁和易读。
 *
 * @author jimichan
 */
@FunctionalInterface
public interface FeatureConsumer {
    /**
     * 接收和处理特征。
     * <p>
     * 该方法是特征消费者的核心功能，用于处理提取的特征。
     *
     * @param feature 提取的特征名称
     */
    void accept(String feature);

    /**
     * 接收缓冲里当前已写入的特征。推理路径覆盖此方法，避免先生成字符串。
     * 缓冲在下次 {@code clear} 或 {@code append} 前有效，实现应立刻查号。
     */
    default void acceptChars(char[] chars, int length) {
        accept(new String(chars, 0, length));
    }
}