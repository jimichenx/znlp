package dev.znlp.segment.impl.userdict;

import dev.znlp.base.dat.StringIntDATMap;

/**
 * 自定义词典。
 * <p>
 * {@link #getTrie()} 返回已经发布的双数组，这棵树不会再被修改。
 * 分词处理一句时取一次，并在这句内一直使用。
 *
 * @author jimichan
 */
public interface UserDictionary {

    /**
     * 当前已发布的词典。没有词时返回 {@code null}。
     */
    StringIntDATMap getTrie();

}
