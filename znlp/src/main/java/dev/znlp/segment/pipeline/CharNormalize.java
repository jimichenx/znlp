
package dev.znlp.segment.pipeline;

/**
 * 字符规范化接口
 * <p>
 * 分词之前可以对char进行转换。一般完成大小写、半全角、归一化转换的需求.
 *
 * @author jimichan
 */
public interface CharNormalize {

    /**
     * 对char数组里面的字符进行规范化操作，常见的有最小化和宽体字符处理
     *
     * @param text
     */
    void normal(char[] text);

}
