package dev.znlp.segment.collector;

import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BestPathAlgorithm;
import dev.znlp.segment.impl.bigram.BiGramTableDictionary;
import dev.znlp.segment.impl.dict.CoreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 智能的子词二次切分算法。
 * <p>
 * 通过mini的Viterbi算法，选择一个最佳的切分方式。
 *
 * @author jimichan
 */

public class SmartSubwordComputer implements SubwordComputer {

    private final BestPathAlgorithm algorithm;

    private final CoreDictionary coreDictionary;
    private final BiGramTableDictionary biGramTableDictionary;

    /**
     * 外部程序控制是否进一步拆分.返回true表示不再拆分
     */
    private Function<String, Boolean> blackListCallback;

    public SmartSubwordComputer(BestPathAlgorithm algorithm,
                                CoreDictionary coreDictionary,
                                BiGramTableDictionary biGramTableDictionary) {
        this.algorithm = algorithm;
        this.coreDictionary = coreDictionary;
        this.biGramTableDictionary = biGramTableDictionary;
    }


    /**
     * 拆分结果保存到term中去
     *
     * @param term
     * @param wordnet
     * @param wordPath
     */
    @Override
    public boolean run(Token term, Wordnet wordnet, Wordpath wordPath) {

        //2个字的不拆
        //3.3.0版本开始变成2个字不拆。但是三字是否切分，需要看是否存在bigram搭配（要求严格点）
        if (term.length() <= 2) {
            return false;
        }


        // 人名不拆
        if (term.getNature() == Nature.nr) {
            return false;
        }

        //时间怎么拆

        //
        boolean result = false;

        // 创建子词图
        Wordnet subWordnet = wordnet.subWordnet(term.offset, term.length());
        Wordpath subPath = algorithm.select(subWordnet);

        List<Vertex> list = new ArrayList<>();
        java.util.Iterator<Vertex> iterator = subPath.iteratorVertex();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }

        if (list != null && !list.isEmpty()) {
            List<Token> subList = new ArrayList<>(list.size());
            int len = 0;
            for (Vertex v : list) {
                Token x = new Token(v.realWord(), v.nature, term.offset + v.offset());
                len += v.length();
                subList.add(x);
            }

            // [省 政府]/n
            // 如果是3字词，切分为两片。那么要求在biGramTableDictionary中包含一个pair

            if (len == term.length()) {

                if (blackListCallback != null) {
                    if (blackListCallback.apply(term.word)) {
                        return false;
                    }
                }

                if (len == 3 && subList.size() == 2) {
                    // 需要从原始wordnet中获取wordID
                    Vertex v1 = wordnet.getVertex(term.offset + list.get(0).offset(), list.get(0).length());
                    Vertex v2 = wordnet.getVertex(term.offset + list.get(1).offset(), list.get(1).length());
                    if (v1 != null && v2 != null &&
                            this.biGramTableDictionary.bigramFrequency(v1.wordID, v2.wordID) > 0) {
                        term.setSubword(subList);
                        result = true;
                    }
                } else {
                    term.setSubword(subList);
                    result = true;
                }

            }

        }
        return result;
    }

    public void setBlackListCallback(Function<String, Boolean> blackListCallback) {
        this.blackListCallback = blackListCallback;
    }

}

