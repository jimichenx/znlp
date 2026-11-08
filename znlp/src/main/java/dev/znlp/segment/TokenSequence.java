package dev.znlp.segment;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * WordTerm序列的轻量级包装类。
 *
 * <p>此类封装了一个 {@link Token} 的迭代器，提供了便捷的遍历和流式处理方法。
 * 主要用于表示分词结果序列，支持 for-each 循环和 Stream API。</p>
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>这是一个轻量级的包装类，主要目的是提供更好的 API 语义</li>
 *   <li>不负责创建迭代器，只负责包装已有的迭代器</li>
 *   <li>支持一次性遍历（迭代器模式）</li>
 *   <li>提供 Stream API 支持，便于函数式编程</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 通过 SegmentReader 获取分词结果
 * SegmentReader reader = SegmentReader.create(wordSegmenter);
 * WordTermSequence sequence = reader.segment("自然语言处理");
 *
 * // 方式1: 使用 for-each 循环
 * for (WordTerm term : sequence) {
 *     System.out.println(term.word);
 * }
 *
 * // 方式2: 使用 Stream API
 * List<String> words = sequence.stream()
 *     .map(term -> term.word)
 *     .collect(Collectors.toList());
 *
 * // 方式3: 使用迭代器
 * Iterator<WordTerm> iterator = sequence.iterator();
 * while (iterator.hasNext()) {
 *     WordTerm term = iterator.next();
 *     System.out.println(term.word);
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li><strong>一次性遍历：</strong>底层迭代器只能遍历一次，多次调用 {@link #iterator()}
 *       或 {@link #stream()} 会返回同一个迭代器</li>
 *   <li><strong>线程安全：</strong>此类不是线程安全的，不应在多线程环境中共享</li>
 * </ul>
 *
 * @author jimichan
 * @see Token
 * @see WordSegmentReader
 */
public final class TokenSequence implements Iterable<Token> {

    private final Iterator<Token> source;

    /**
     * 构造一个 WordTermSequence。
     *
     * @param source 底层的 WordTerm 迭代器，不能为 null
     * @throws NullPointerException 如果 source 为 null
     */
    public TokenSequence(Iterator<Token> source) {
        if (source == null) {
            throw new NullPointerException("source iterator cannot be null");
        }
        this.source = source;
    }

    /**
     * 从 Iterable 构造 WordTermSequence。
     *
     * <p>此构造函数会立即调用 {@code source.iterator()} 获取迭代器。</p>
     *
     * @param source WordTerm 的可迭代对象，不能为 null
     * @throws NullPointerException 如果 source 为 null
     */
    public TokenSequence(Iterable<Token> source) {
        this(source.iterator());
    }

    /**
     * 返回底层的迭代器。
     *
     * <p><strong>注意：</strong>多次调用此方法会返回同一个迭代器实例，
     * 因此只能遍历一次。</p>
     *
     * @return WordTerm 迭代器
     */
    @Override
    public Iterator<Token> iterator() {
        return source;
    }

    /**
     * 将 WordTermSequence 转换为 Stream。
     *
     * <p>此方法提供了流式处理的能力，可以使用 Stream API 进行过滤、映射、收集等操作。</p>
     *
     * <p><strong>注意：</strong>返回的 Stream 基于底层迭代器，只能使用一次。
     * 多次调用此方法会返回基于同一迭代器的 Stream，可能导致不可预期的行为。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 过滤并收集词语
     * List<String> nouns = sequence.stream()
     *     .filter(term -> term.getNature() == Nature.n)
     *     .map(term -> term.word)
     *     .collect(Collectors.toList());
     *
     * // 统计词数
     * long count = sequence.stream().count();
     *
     * // 查找特定词
     * Optional<WordTerm> found = sequence.stream()
     *     .filter(term -> term.word.equals("中国"))
     *     .findFirst();
     * }</pre>
     *
     * @return WordTerm 的 Stream，顺序与迭代器一致
     */
    public Stream<Token> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(source, Spliterator.ORDERED),
                false
        );
    }

    /**
     * 将 WordTerm 序列转换为 List。
     *
     * <p>此方法会遍历底层迭代器并将所有 WordTerm 收集到一个 ArrayList 中。</p>
     *
     * <p><strong>注意：</strong>调用此方法会消耗底层迭代器，之后无法再次遍历。</p>
     *
     * @return 包含所有 WordTerm 的 List
     */
    public List<Token> toList() {
        List<Token> result = new ArrayList<>();
        source.forEachRemaining(result::add);
        return result;
    }

    /**
     * 将 WordTerm 序列转换为词语字符串的 List。
     *
     * <p>此方法会遍历底层迭代器，提取每个 WordTerm 的词语字符串，并收集到一个 ArrayList 中。</p>
     *
     * <p><strong>注意：</strong>调用此方法会消耗底层迭代器，之后无法再次遍历。</p>
     *
     * @return 包含所有词语字符串的 List
     */
    public List<String> toWordList() {
        List<String> result = new ArrayList<>();
        source.forEachRemaining(term -> result.add(term.word));
        return result;
    }
}
