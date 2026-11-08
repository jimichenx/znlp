package dev.znlp.segment.collector;

/**
 * 子词规则词典接口
 */
public interface SubwordRuleDict {
    /**
     * 匹配词语并返回切分规则
     *
     * @param word 待匹配的词语
     * @return Int数组，每个int表示对应词的长度
     * ABC => ABC 不切分 返回[3]
     * A/?C 通配切分 返回 [1,2]
     * 返回 null表示没有匹配
     */
    int[] match(String word);
}

