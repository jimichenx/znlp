package dev.znlp.segment.impl.bigram;

import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.VertexRow;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;
import dev.znlp.segment.pipeline.BestPathAlgorithm;
import dev.znlp.segment.impl.dict.CoreDictionary;

/**
 * 基于核心词典的bi词之前的共出现的次数，采用viterbi选择出一个概率最大的path
 * 是权重越小越好 距离越短
 *
 * @author jimichan
 */
public final class ViterbiBestPathAlgorithm implements BestPathAlgorithm {

    private final BiGramTableDictionary coreBiGramTableDictionary;

    /**
     * 平滑参数
     */
    public final double dSmoothingPara = 0.1;
    /**
     * Smoothing 平滑因子
     */
    public final double dTemp;

    /**
     * 来自Hanlp里面的算法
     *
     * @param from
     * @param to
     * @return
     */
    private final double partA = (1 - dSmoothingPara);
    private final double partB;
    private final double PARTA_PARTB;
    private final double PARTTA_Dtemp;
    private final double PartZ;
    private final double value1;
    private final double value2;
    private final double value3;
    private final double value4;
    private final double value5;

    public ViterbiBestPathAlgorithm(BiGramTableDictionary coreBiGramTableDictionary,
                                    CoreDictionary coreDictionary) {
        this.coreBiGramTableDictionary = coreBiGramTableDictionary;
        dTemp = (double) 1 / coreDictionary.totalFreq() + 0.00001;
        partB = (1 - dTemp);
        PARTA_PARTB = partA * partB;
        PARTTA_Dtemp = partA * dTemp;
        PartZ = dSmoothingPara / coreDictionary.totalFreq();

        double[] values = new double[21];
        for (int i = 0; i < 21; i++) {
            values[i] = Math.abs(-Math.log(PartZ * i + PARTTA_Dtemp));
        }

        value1 = values[1];
        value2 = values[2];
        value3 = values[3];
        value4 = values[4];
        value5 = values[5];
    }


    /**
     * 在原因的path基础上。多个识别器做了修改。
     * 1. 合成词
     * 2. 截断+合成
     *
     * @param wordnet
     * @return Wordpath
     */
    @Override
    public Wordpath select(Wordnet wordnet) {

        // 从第二个字符节点开始，一直到最后一个字符
        final int charSize = wordnet.getCharSizeLength();

        // 第一行的From肯定来自Start节点

        VertexRow firstRow = wordnet.getRow(0);
        Vertex begin = wordnet.getBeginRow().getFirst();
        for (int i = 0; i < firstRow.size(); i++) {
            updateFrom(firstRow.vertexAt(i), begin);
        }

        for (int i = 0; i < charSize; i++) {

            final VertexRow row = wordnet.row(i);
            int rowSize = row.size();
            if (rowSize == 0) {
                continue;
            }

            for (int a = 0; a < rowSize; a++) {
                Vertex node = row.vertexAt(a);

                if (node.from == null) {
                    continue;
                }

                final VertexRow toRow = wordnet.row(i + node.length);
                int toSize = toRow.size();
                for (int b = 0; b < toSize; b++) {
                    updateFrom(toRow.vertexAt(b), node);
                }
            }

        }

        return buildPath(wordnet);
    }


    private void updateFrom(Vertex the, Vertex from) {

        //是权重越小越好 距离越短
        double weight = from.weight + calculateWeight(from, the);
        if (the.from == null || the.weight > weight) {
            the.from = from;
            the.weight = weight;
        }
    }


    private double calculateWeight(Vertex from, Vertex to) {
        int frequency = from.freq;
        if (frequency == 0) {
            // 防止发生除零错误
            frequency = 1;
        }

        // 用户词典、纠错词典等强制成词不走打分：选路之后由 SpanConstraint 直接合并
        // （见 Wordpath#force / canForce），并记录优先级供后续约束避让。
        int nTwoWordsFreq = coreBiGramTableDictionary.bigramFrequency(from.wordID, to.wordID);

        double value = 0;

        if (nTwoWordsFreq > 0) {
            value = -Math
                    .log(PartZ * frequency
                            +
                            PARTA_PARTB * nTwoWordsFreq / frequency

                            + PARTTA_Dtemp
                    );

        } else {
            if (frequency == 1) {
                value = value1;
            } else if (frequency == 2) {
                value = value2;
            } else if (frequency == 3) {
                value = value3;
            } else if (frequency == 4) {
                value = value4;
            } else if (frequency == 5) {
                value = value5;
            } else {
                value = -Math
                        .log(PartZ * frequency + PARTTA_Dtemp
                        );

            }
        }

        if (value < 0) {
            value = -value;
        }


        return value;
    }


    /**
     * 从后到前。根据权重获取最优路径
     *
     * @param wordnet
     * @return
     */
    private Wordpath buildPath(Wordnet wordnet) {
        //从后到前，获得完整的路径
        Wordpath wordPath = new Wordpath(wordnet);

        Vertex last = null;

        Vertex point = wordnet.getEndRow().first();

        while (point != null) {
            last = point;
            wordPath.combine(point);
            point = point.from;
        }

        // 最后一个point必定指向start节点

        if (last != wordnet.getBeginRow().first()) {
            throw new IllegalStateException("非完整路径,有可能wordnet初始化的时候就路径不完整");
        }
//        Preconditions.checkState(last == wordnet.getBeginRow().first(), "非完整路径,有可能wordnet初始化的时候就路径不完整");

        return wordPath;
    }

}
