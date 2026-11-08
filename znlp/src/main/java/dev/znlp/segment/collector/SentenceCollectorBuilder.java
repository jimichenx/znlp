package dev.znlp.segment.collector;

import dev.znlp.base.dat.StringIntDATMap;
import dev.znlp.segment.pipeline.BestPathAlgorithm;
import dev.znlp.segment.impl.bigram.BiGramTableDictionary;
import dev.znlp.segment.impl.dict.CoreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * SentenceCollector 构建器
 */
public class SentenceCollectorBuilder {

    private final List<SubwordComputer> subwordComputer = new ArrayList<>();

    private final List<SubwordInfoSetup> setupList = new ArrayList<>();

    /**
     * 增加一个SubwordInfoSetup
     */
    public void addSubwordInfoSetup(SubwordInfoSetup setup) {
        setupList.add(setup);
    }

    /**
     * 安装默认的索引子词切分算法
     */
    public SentenceCollectorBuilder indexSubword() {
        return indexSubword(2);
    }

    /**
     * 安装默认的索引子词切分算法
     *
     * @param minWordLen 最小词长
     */
    public SentenceCollectorBuilder indexSubword(int minWordLen) {
        IndexSubwordComputer computer = new IndexSubwordComputer();
        computer.setMinWordLength(minWordLen);
        subwordComputer.add(computer);
        return this;
    }

    /**
     * 安装智能子词切分算法
     *
     * @param algorithm             最佳路径算法
     * @param coreDictionary        核心词典
     * @param biGramTableDictionary 二元语法表词典
     */
    public SentenceCollectorBuilder smartSubword(
            BestPathAlgorithm algorithm,
            CoreDictionary coreDictionary,
            BiGramTableDictionary biGramTableDictionary
    ) {
        SmartSubwordComputer p = new SmartSubwordComputer(algorithm, coreDictionary, biGramTableDictionary);
        subwordComputer.add(p);
        return this;
    }

    /**
     * 安装基于词典的切分算法
     */
    public SentenceCollectorBuilder ruleBaseSubword(List<SubwordRuleDict> dict) {
        RuleDictSubwordComputer p = new RuleDictSubwordComputer(dict);
        subwordComputer.add(p);
        return this;
    }

    public SentenceCollectorBuilder fillCoreDict(CoreDictionary dbcms) {
        addSubwordInfoSetup(new CoreDictSubwordInfoSetup(dbcms));
        return this;
    }

    public SentenceCollectorBuilder fillCustomDict(StringIntDATMap trie) {
        addSubwordInfoSetup(new CustomDictSubwordInfoSetup(trie));
        return this;
    }

    public SentenceCollector build() {
        return new SentenceCollector(subwordComputer, setupList);
    }

}

