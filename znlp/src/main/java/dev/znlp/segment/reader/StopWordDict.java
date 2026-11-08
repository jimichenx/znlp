package dev.znlp.segment.reader;

/**
 * 停用词接口
 *
 * @author jimichan
 */
public interface StopWordDict {
    /**
     * 判断当前输入word是否是停用词。true表示是停用词
     */
    boolean contains(String word);
}