package dev.znlp.segment.impl.correction;

import dev.znlp.base.dat.DoubleArrayTrieMap;

/**
 * 分词纠错词典。
 * <p>
 * {@link #getTrie()} 返回已经发布的双数组，这棵树不会再被修改。
 * 分词处理一句时取一次，并在这句内一直使用。
 *
 * @author jimichan
 */
public interface CorrectionDictionary {

    /**
     * 当前已发布的词典。没有规则时返回 {@code null}。
     */
    DoubleArrayTrieMap<CorrectionWord> getTrie();

}
