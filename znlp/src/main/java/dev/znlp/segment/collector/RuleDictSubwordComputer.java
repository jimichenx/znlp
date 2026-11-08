package dev.znlp.segment.collector;

import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.VertexRow;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则词典
 * ABC => A/BC 切分
 * ABC => ABC 不切分
 * A?C => A/?C 通配切分
 */
public class RuleDictSubwordComputer implements SubwordComputer {

    private final List<SubwordRuleDict> dictList;

    public RuleDictSubwordComputer(List<SubwordRuleDict> dictList) {
        this.dictList = dictList;
    }

    @Override
    public boolean run(Token term, Wordnet wordnet, Wordpath wordPath) {
        for (SubwordRuleDict dict : dictList) {
            int[] mt = dict.match(term.word);
            if (mt != null) {
                String word = term.word;
                int offset = 0;

                if (mt.length == 1) {
                    // 不切分
                    term.setSubword(null);
                    continue;
                }

                List<Token> sublist = new ArrayList<>(mt.length);
                for (int len : mt) {
                    VertexRow row = wordnet.row(term.offset + offset);

                    Vertex vertex = row.get(len);
                    Nature nature = vertex != null ? vertex.nature : null;

                    sublist.add(new Token(word.substring(offset, offset + len), nature, offset + term.offset));
                    offset += len;
                }
                term.setSubword(sublist);
                return true;
            }
        }
        return false;
    }
}

