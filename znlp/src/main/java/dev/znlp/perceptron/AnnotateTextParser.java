package dev.znlp.perceptron;

import java.util.List;

/**
 * 标注文本解析器接口，用于解析标注的训练文本并转换为模型可用的格式。
 * <p>
 * 该接口是结构化感知机框架中连接原始标注文本和模型训练数据的关键组件。
 * 它负责将各种格式的标注文本转换为统一的内部表示，以便进行特征提取和模型训练。
 * <p>
 * <h3>核心功能：</h3>
 * <ul>
 *   <li>将标注文本解析为成对的标签序列</li>
 *   <li>将标注文本直接转换为输入序列</li>
 *   <li>将成对的标签序列转换为输入序列</li>
 * </ul>
 * <p>
 * <h3>应用场景：</h3>
 * <p>
 * 对于不同的NLP任务，标注文本的格式可能不同：
 * <ul>
 *   <li><b>分词任务</b>：标注文本可能是 "世界/你好"，需要转换为 "世/B 界/E 你/B 好/E" 的格式</li>
 *   <li><b>词性标注任务</b>：标注文本可能是 "世界/n 你好/a"，需要转换为包含词和词性的标签对</li>
 *   <li><b>命名实体识别任务</b>：标注文本可能是 "[世界/LOC]你好"，需要转换为实体和类型的标签对</li>
 * </ul>
 * <p>
 * 该接口的设计允许灵活适配不同的标注格式，使结构化感知机框架适用于各种NLP任务。
 *
 * @param <T> 标签数据类型，用于表示标注文本中的标签信息
 * @param <InputSequence> 输入序列类型，用于表示模型的输入数据
 * @author jimichan
 */
public interface AnnotateTextParser<T, InputSequence> {

    /**
     * 将标注文本解析为成对的标签序列。
     * <p>
     * 该方法是标注文本解析的核心功能，用于将原始标注文本转换为包含标签信息的成对序列。
     *
     * @param text 原始标注文本，如分词任务中的 "世界/你好"
     * @return 成对的标签序列，每个元素包含一个标签和对应的文本信息
     */
    List<Pair<T>> parse(String text);

    /**
     * 将标注文本直接转换为输入序列。
     * <p>
     * 该方法用于跳过中间的成对标签序列，直接将标注文本转换为模型可用的输入序列。
     *
     * @param text 原始标注文本
     * @return 转换后的输入序列，如char[]或String[]
     */
    InputSequence parseToInputList(String text);

    /**
     * 将成对的标签序列转换为输入序列。
     * <p>
     * 该方法用于将parse方法生成的成对标签序列转换为模型可用的输入序列。
     *
     * @param list 成对的标签序列，由parse方法生成
     * @return 转换后的输入序列，如char[]或String[]
     */
    InputSequence listToInput(List<Pair<T>> list);

    /**
     * 标签对类，用于表示标注文本中的标签和对应的文本信息。
     * <p>
     * 该内部类是一个简单的数据容器，用于存储标签和对应的文本信息。
     *
     * @param <A>    标签数据类型
     * @param first  标签信息，如分词任务中的B、E、M、S等标签
     * @param second 对应的文本信息，如分词任务中的单个字符
     * @author jimichan
     */
        record Pair<A>(A first, String second) {
        /**
         * 构造标签对。
         *
         * @param first  标签信息
         * @param second 对应的文本信息
         */
        public Pair {
        }

            /**
             * 返回标签对的字符串表示。
             *
             * @return 标签对的字符串表示，格式为 "(first,second)"
             */
            public String toString() {
                return "(" + first + "," + second + ")";
            }
        }
}
