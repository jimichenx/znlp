package dev.znlp.base.common;

import java.io.IOException;

/**
 * 段落读取器接口，用于将文档分段处理。
 * <p>
 * 该接口提供了一种统一的方式来将大文档分割成多个段落，以便后续的分词、新词发现等NLP组件
 * 能够分段处理，避免一次性加载整个文档到内存中。
 * </p>
 *
 * <p>
 * 主要设计目的：
 * <ul>
 *   <li><strong>内存效率：</strong>支持流式处理，按段落读取，避免大文档占用过多内存</li>
 *   <li><strong>统一接口：</strong>提供统一的接口抽象，支持不同的输入源（String、Reader等）</li>
 *   <li><strong>智能分段：</strong>实现类可以在合适的位置（如标点符号处）进行分段，避免在词中间截断</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>{@code
 * ParagraphReader reader = new ParagraphReaderSmart("这是一段很长的文本...");
 * String paragraph;
 * while ((paragraph = reader.next()) != null) {
 *     // 处理段落
 *     processParagraph(paragraph);
 * }
 * }</pre>
 * </p>
 *
 * @author jimichan
 */
public interface ParagraphReader {

    /**
     * 读取下一个段落。
     * <p>
     * 每次调用返回文档中的下一个段落。当所有段落都已读取完毕时，返回 {@code null}。
     * </p>
     *
     * @return 下一个段落字符串，如果已到达文档末尾则返回 {@code null}
     * @throws IOException 如果发生I/O错误
     */
    String next() throws IOException;
}
