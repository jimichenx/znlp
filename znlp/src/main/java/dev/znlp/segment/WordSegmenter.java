package dev.znlp.segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分词器接口，用于将文本切分为词语序列。
 * <p>
 * 实现类应该是无状态的，因此可以安全地在多线程环境中共享使用。
 * 对于相同的分词算法配置，只需要创建一个实例即可。
 * <p>
 * 基本使用示例：
 * <pre>
 * WordSegmenter segmenter = ...; // 获取分词器实例
 * segmenter.segment("你好世界", term -> {
 *     System.out.println(term.word + "/" + term.getNature());
 * });
 * </pre>
 *
 * @author jimichan
 * @since 2.1.0
 */
public interface WordSegmenter {

    /**
     * 对文本进行分词，通过回调函数处理每个分词结果。
     * <p>
     * 这是分词器的核心方法，所有实现类必须实现此方法。
     * 使用回调模式可以避免一次性加载所有分词结果到内存，适合处理大文本。
     *
     * @param text     需要分词的字符数组，不能为 null
     * @param consumer 分词结果的消费者，用于处理每个 WordTerm，不能为 null
     * @throws NullPointerException 如果 text 或 consumer 为 null
     */
    void segment(char[] text, Consumer<Token> consumer);


    /**
     * 对字符串进行分词的便捷方法。
     * <p>
     * 内部会将字符串转换为字符数组后调用 {@link #segment(char[], Consumer)}。
     *
     * @param text     需要分词的文本字符串，不能为 null
     * @param consumer 分词结果的消费者，不能为 null
     * @throws NullPointerException 如果 text 或 consumer 为 null
     */
    default void segment(String text, Consumer<Token> consumer) {
        if (text == null) {
            throw new NullPointerException("text cannot be null");
        }
        if (consumer == null) {
            throw new NullPointerException("consumer cannot be null");
        }
        segment(text.toCharArray(), consumer);
    }

    /**
     * 对文本进行分词并返回所有分词结果的便捷方法。
     * <p>
     * 注意：此方法会将所有分词结果加载到内存中，不适用于超大文本（建议文本长度不超过 10000 字符）。
     * 对于大文本，请使用 {@link #segment(String, Consumer)} 方法进行流式处理。
     *
     * @param text 需要分词的文本
     * @return 包含所有分词结果的不可变列表，如果文本为空则返回空列表
     */
    default List<Token> segment(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        // 预估初始容量：假设平均词长为 2 个字符，最小容量为 16
        int initialCapacity = Math.max(text.length() / 2, 16);
        List<Token> result = new ArrayList<>(initialCapacity);
        segment(text.toCharArray(), result::add);

        return Collections.unmodifiableList(result);
    }

    /**
     * 对文本进行分词并返回词语字符串列表的便捷方法。
     * <p>
     * 此方法只返回词语文本，不包含词性等其他信息。
     *
     * @param text 需要分词的文本
     * @return 包含所有词语的不可变列表
     */
    default List<String> segmentToStrings(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        segment(text.toCharArray(), term -> result.add(term.word));

        return Collections.unmodifiableList(result);
    }

}
