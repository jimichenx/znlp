package dev.znlp.segment;

import dev.znlp.segment.reader.*;

import java.io.Reader;
import java.util.List;

/**
 * 分词结果读取器接口。
 *
 * <p>SegmentReader 提供了一个统一的接口，用于从不同的输入源（字符串、Reader）
 * 读取分词结果，并支持对分词结果进行过滤处理（如停用词过滤、标点符号过滤）。</p>
 *
 * <h3>基本使用</h3>
 * <pre>{@code
 * // 1. 创建基础的分词读取器
 * WordSegmenter segmenter = WordSegmenter.create();
 * SegmentReader reader = SegmentReader.create(segmenter);
 *
 * // 2. 对文本进行分词
 * WordTermSequence result = reader.segment("这是一段测试文本");
 *
 * // 3. 遍历分词结果
 * for (WordTerm term : result) {
 *     System.out.println(term.word);
 * }
 * }</pre>
 *
 * <h3>使用过滤器</h3>
 * <pre>{@code
 * // 创建带过滤功能的分词读取器
 * SegmentReader reader = SegmentReader.builder(segmenter)
 *     .filterPunctuation(true)           // 过滤标点符号
 *     .filterStopWords()                 // 使用默认停用词表
 *     .build();
 *
 * WordTermSequence result = reader.segment("这是一段测试文本，包含标点！");
 * }</pre>
 *
 * <h3>自定义停用词表</h3>
 * <pre>{@code
 * // 使用自定义停用词表
 * StopWordDict customDict = new MemStopWordDict(Arrays.asList("的", "了", "是"));
 * SegmentReader reader = SegmentReader.builder(segmenter)
 *     .filterStopWords(customDict)
 *     .build();
 * }</pre>
 *
 * @author jimichan
 * @see WordSegmenter
 * @see TokenSequence
 * @since 2.1.0
 */
public interface WordSegmentReader {

    /**
     * 对 Reader 输入流进行分词处理。
     *
     * <p>此方法适用于处理大文本或流式输入，可以有效避免将整个文本加载到内存中。</p>
     *
     * @param reader 字符输入流，不能为 null
     * @return 分词结果序列，包含所有识别出的词项
     * @throws NullPointerException 如果 reader 为 null
     * @throws RuntimeException     如果读取过程中发生 IO 错误
     */
    TokenSequence segment(Reader reader);

    /**
     * 对字符串文本进行分词处理。
     *
     * <p>这是最常用的分词方法，适合处理短文本或已加载到内存的文本。</p>
     *
     * <pre>{@code
     * SegmentReader reader = SegmentReader.create(segmenter);
     * WordTermSequence result = reader.segment("自然语言处理技术");
     * }</pre>
     *
     * @param text 待分词的文本，不能为 null
     * @return 分词结果序列，包含所有识别出的词项
     * @throws NullPointerException 如果 text 为 null
     */
    TokenSequence segment(String text);

    /**
     * 创建一个基础的分词读取器。
     *
     * <p>此方法创建的读取器不包含任何过滤功能。如果需要添加过滤器（如停用词过滤、
     * 标点符号过滤），请使用 {@link #builder(WordSegmenter)} 方法。</p>
     *
     * @param wordSegmenter 底层分词器，不能为 null
     * @return 新创建的分词读取器实例
     * @throws NullPointerException 如果 wordSegmenter 为 null
     * @see #builder(WordSegmenter)
     */
    static WordSegmentReader create(WordSegmenter wordSegmenter) {
        return new DefaultWordSegmentReader(wordSegmenter);
    }

    /**
     * 创建分词读取器构建器。
     *
     * <p>使用构建器模式可以方便地配置各种过滤选项，构建符合需求的分词读取器。</p>
     *
     * <pre>{@code
     * SegmentReader reader = SegmentReader.builder(segmenter)
     *     .filterPunctuation(true)
     *     .filterStopWords()
     *     .build();
     * }</pre>
     *
     * @param wordSegmenter 底层分词器，不能为 null
     * @return 构建器实例，用于配置和创建分词读取器
     * @throws NullPointerException 如果 wordSegmenter 为 null
     * @see Builder
     */
    static Builder builder(WordSegmenter wordSegmenter) {
        return new Builder(wordSegmenter);
    }

    /**
     * 分词读取器构建器。
     *
     * <p>提供流式 API 来配置分词读取器的各种过滤选项。构建器支持链式调用，
     * 可以方便地组合多个过滤器。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 示例1: 只过滤标点符号
     * SegmentReader reader1 = SegmentReader.builder(segmenter)
     *     .filterPunctuation(true)
     *     .build();
     *
     * // 示例2: 使用默认停用词表
     * SegmentReader reader2 = SegmentReader.builder(segmenter)
     *     .filterStopWords()
     *     .build();
     *
     * // 示例3: 组合多个过滤器
     * SegmentReader reader3 = SegmentReader.builder(segmenter)
     *     .filterPunctuation(true)
     *     .filterStopWords()
     *     .build();
     *
     * // 示例4: 使用自定义停用词表
     * StopWordDict customDict = new MemStopWordDict(Arrays.asList("的", "了"));
     * SegmentReader reader4 = SegmentReader.builder(segmenter)
     *     .filterStopWords(customDict)
     *     .build();
     * }</pre>
     */
    class Builder {
        private final WordSegmenter wordSegmenter;
        private boolean punctuation = false;
        private List<StopWordDict> stopWordDicts = null;

        /**
         * 构造函数。
         *
         * @param wordSegmenter 底层分词器，不能为 null
         */
        public Builder(WordSegmenter wordSegmenter) {
            this.wordSegmenter = wordSegmenter;
        }

        /**
         * 配置标点符号过滤。
         *
         * <p>启用后，分词结果中将不包含标点符号。</p>
         *
         * @param enable true 表示启用标点符号过滤，false 表示禁用
         * @return 当前构建器实例，支持链式调用
         */
        public Builder filterPunctuation(boolean enable) {
            this.punctuation = enable;
            return this;
        }

        /**
         * 启用停用词过滤，使用系统默认的停用词表。
         *
         * <p>默认停用词表包含常见的中文停用词，如"的"、"了"、"是"等。
         * 启用后，这些词将从分词结果中过滤掉。</p>
         *
         * <pre>{@code
         * SegmentReader reader = SegmentReader.builder(segmenter)
         *     .filterStopWords()  // 使用默认停用词表
         *     .build();
         * }</pre>
         *
         * @return 当前构建器实例，支持链式调用
         * @see SystemStopWordDict
         */
        public Builder filterStopWords() {
            this.stopWordDicts = null; // 表示使用默认词典
            return this;
        }

        /**
         * 启用停用词过滤，使用指定的停用词表。
         *
         * <p>可以多次调用此方法添加多个停用词表，所有词表将被合并使用。</p>
         *
         * <pre>{@code
         * // 创建自定义停用词表
         * StopWordDict customDict = new MemStopWordDict(Arrays.asList("的", "了", "是"));
         *
         * SegmentReader reader = SegmentReader.builder(segmenter)
         *     .filterStopWords(customDict)
         *     .build();
         * }</pre>
         *
         * @param stopWordDict 停用词表，不能为 null
         * @return 当前构建器实例，支持链式调用
         * @throws NullPointerException 如果 stopWordDict 为 null
         * @see StopWordDict
         */
        public Builder filterStopWords(StopWordDict stopWordDict) {
            if (this.stopWordDicts == null) {
                this.stopWordDicts = new java.util.ArrayList<>();
            }
            this.stopWordDicts.add(stopWordDict);
            return this;
        }

        /**
         * 启用停用词过滤，使用多个停用词表。
         *
         * <p>所有提供的停用词表将被合并使用。可以组合使用系统词表和自定义词表。</p>
         *
         * <pre>{@code
         * List<StopWordDict> dicts = Arrays.asList(
         *     new SystemStopWordDict(),
         *     new MemStopWordDict(Arrays.asList("自定义", "停用词"))
         * );
         *
         * SegmentReader reader = SegmentReader.builder(segmenter)
         *     .filterStopWords(dicts)
         *     .build();
         * }</pre>
         *
         * @param stopWordDicts 停用词表列表，不能为 null 或包含 null 元素
         * @return 当前构建器实例，支持链式调用
         * @throws NullPointerException 如果 stopWordDicts 为 null 或包含 null 元素
         */
        public Builder filterStopWords(List<StopWordDict> stopWordDicts) {
            if (this.stopWordDicts == null) {
                this.stopWordDicts = new java.util.ArrayList<>();
            }
            this.stopWordDicts.addAll(stopWordDicts);
            return this;
        }

        /**
         * 构建分词读取器实例。
         *
         * <p>根据之前配置的选项创建一个新的 SegmentReader 实例。
         * 过滤器将按照以下顺序应用：</p>
         * <ol>
         *   <li>基础分词</li>
         *   <li>标点符号过滤（如果启用）</li>
         *   <li>停用词过滤（如果启用）</li>
         * </ol>
         *
         * @return 配置好的分词读取器实例
         */
        public WordSegmentReader build() {
            WordSegmentReader reader = new DefaultWordSegmentReader(wordSegmenter);
            if (punctuation) {
                reader = new PunctuationFilter(reader);
            }
            if (stopWordDicts != null) {
                if (stopWordDicts.isEmpty()) {
                    reader = new StopwordFilter(reader); // 使用默认词典
                } else {
                    reader = new StopwordFilter(reader, stopWordDicts);
                }
            }
            return reader;
        }
    }

    /**
     * 创建带过滤功能的分词读取器（快捷方法）。
     *
     * <p>这是一个便捷方法，用于快速创建带有常用过滤选项的分词读取器。
     * 如果需要更灵活的配置，建议使用 {@link #builder(WordSegmenter)} 方法。</p>
     *
     * <pre>{@code
     * // 同时启用标点符号过滤和停用词过滤
     * SegmentReader reader = SegmentReader.createWithFilters(
     *     segmenter,
     *     true,  // 过滤标点符号
     *     true   // 过滤停用词（使用默认词表）
     * );
     * }</pre>
     *
     * @param wordSegmenter     底层分词器，不能为 null
     * @param filterPunctuation 是否过滤标点符号
     * @param filterStopWords   是否过滤停用词（使用默认词表）
     * @return 配置好的分词读取器实例
     * @throws NullPointerException 如果 wordSegmenter 为 null
     */
    static WordSegmentReader createWithFilters(WordSegmenter wordSegmenter, boolean filterPunctuation, boolean filterStopWords) {
        Builder builder = builder(wordSegmenter)
                .filterPunctuation(filterPunctuation);
        if (filterStopWords) {
            builder.filterStopWords(); // 使用默认停用词过滤
        }
        return builder.build();
    }

    /**
     * 创建带过滤功能的分词读取器（使用自定义停用词表）。
     *
     * <p>此方法允许指定自定义的停用词表。</p>
     *
     * <pre>{@code
     * StopWordDict customDict = new MemStopWordDict(Arrays.asList("的", "了"));
     * SegmentReader reader = SegmentReader.createWithFilters(
     *     segmenter,
     *     true,        // 过滤标点符号
     *     customDict   // 使用自定义停用词表
     * );
     * }</pre>
     *
     * @param wordSegmenter     底层分词器，不能为 null
     * @param filterPunctuation 是否过滤标点符号
     * @param stopWordDict      停用词表，不能为 null
     * @return 配置好的分词读取器实例
     * @throws NullPointerException 如果 wordSegmenter 或 stopWordDict 为 null
     */
    static WordSegmentReader createWithFilters(WordSegmenter wordSegmenter, boolean filterPunctuation, StopWordDict stopWordDict) {
        return builder(wordSegmenter)
                .filterPunctuation(filterPunctuation)
                .filterStopWords(stopWordDict)
                .build();
    }

    /**
     * 创建带过滤功能的分词读取器（使用多个停用词表）。
     *
     * <p>此方法允许指定多个停用词表，所有词表将被合并使用。</p>
     *
     * <pre>{@code
     * List<StopWordDict> dicts = Arrays.asList(
     *     new SystemStopWordDict(),
     *     new MemStopWordDict(Arrays.asList("自定义词"))
     * );
     * SegmentReader reader = SegmentReader.createWithFilters(
     *     segmenter,
     *     true,   // 过滤标点符号
     *     dicts   // 使用多个停用词表
     * );
     * }</pre>
     *
     * @param wordSegmenter     底层分词器，不能为 null
     * @param filterPunctuation 是否过滤标点符号
     * @param stopWordDicts     停用词表列表，不能为 null
     * @return 配置好的分词读取器实例
     * @throws NullPointerException 如果 wordSegmenter 或 stopWordDicts 为 null
     */
    static WordSegmentReader createWithFilters(WordSegmenter wordSegmenter, boolean filterPunctuation, List<StopWordDict> stopWordDicts) {
        return builder(wordSegmenter)
                .filterPunctuation(filterPunctuation)
                .filterStopWords(stopWordDicts)
                .build();
    }

    /**
     * 动态禁用停用词过滤功能。
     *
     * <p>此方法会遍历过滤器链，找到并禁用所有停用词过滤器。
     * 禁用后，分词结果将包含停用词。</p>
     *
     * <p><strong>注意：</strong>此方法只是临时禁用过滤器，不会改变构建器的配置。
     * 如果需要永久禁用，建议重新构建分词读取器。</p>
     *
     * <pre>{@code
     * SegmentReader reader = SegmentReader.builder(segmenter)
     *     .filterStopWords()
     *     .build();
     *
     * // 临时禁用停用词过滤
     * reader.disableStopWordFilter();
     *
     * // 现在分词结果将包含停用词
     * WordTermSequence result = reader.segment("这是一段文本");
     * }</pre>
     */
    default void disableStopWordFilter() {
        WordSegmentReader reader = this;
        while (reader instanceof BaseFilterWordSegmentReader) {
            if (reader instanceof StopwordFilter) {
                ((StopwordFilter) reader).setEnable(false);
            }
            reader = ((BaseFilterWordSegmentReader) reader).getSource();
        }
    }

    /**
     * 动态禁用标点符号过滤功能。
     *
     * <p>此方法会遍历过滤器链，找到并禁用所有标点符号过滤器。
     * 禁用后，分词结果将包含标点符号。</p>
     *
     * <p><strong>注意：</strong>此方法只是临时禁用过滤器，不会改变构建器的配置。
     * 如果需要永久禁用，建议重新构建分词读取器。</p>
     *
     * <pre>{@code
     * SegmentReader reader = SegmentReader.builder(segmenter)
     *     .filterPunctuation(true)
     *     .build();
     *
     * // 临时禁用标点符号过滤
     * reader.disablePunctuationFilter();
     *
     * // 现在分词结果将包含标点符号
     * WordTermSequence result = reader.segment("你好，世界！");
     * }</pre>
     */
    default void disablePunctuationFilter() {
        WordSegmentReader reader = this;
        while (reader instanceof BaseFilterWordSegmentReader) {
            if (reader instanceof PunctuationFilter) {
                ((PunctuationFilter) reader).setEnable(false);
            }
            reader = ((BaseFilterWordSegmentReader) reader).getSource();
        }
    }
}
