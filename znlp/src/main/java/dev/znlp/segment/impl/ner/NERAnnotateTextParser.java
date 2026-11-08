package dev.znlp.segment.impl.ner;

import dev.znlp.base.common.CharNormUtils;
import dev.znlp.base.common.PkuParser;
import dev.znlp.base.common.PkuWord;
import dev.znlp.perceptron.AnnotateTextParser;
import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordAndNature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NER Annotate Text Parser
 * <p>
 * Parses PKU format (e.g. "[胜利/nz 海上/s 油田/n]nt") into Token sequence and NER tags.
 * <p>
 * Updated to only handle 'ns' and 'nt', and map single entities to 'S'.
 *
 * @author jimichan
 */
public class NERAnnotateTextParser implements AnnotateTextParser<WordAndNature, List<WordAndNature>> {

    private final Set<String> targetPos;

    public NERAnnotateTextParser(Set<String> targetPos) {
        this.targetPos = new HashSet<>(targetPos);
    }

    @Override
    public List<Pair<WordAndNature>> parse(String text) {
        List<PkuWord> pkuWords = PkuParser.parseToWords(text);
        List<Pair<WordAndNature>> result = new ArrayList<>();

        for (PkuWord word : pkuWords) {
            String pos = word.getPos();
            if (word.hasSub()) {
                // Compound entity, e.g., [A/a B/b]c
                // pos is 'c' (the entity type)
                List<PkuWord> subWords = word.getSubWord();
                if (targetPos.contains(pos)) {
                    if (subWords.size() == 1) {
                        PkuWord sub = subWords.get(0);
                        addPair(result, sub, "S"); // Changed from S-pos to S
                    } else {
                        for (int i = 0; i < subWords.size(); i++) {
                            PkuWord sub = subWords.get(i);
                            String label;
                            if (i == 0) {
                                label = "B-" + pos;
                            } else if (i == subWords.size() - 1) {
                                label = "E-" + pos;
                            } else {
                                label = "M-" + pos;
                            }
                            addPair(result, sub, label);
                        }
                    }
                } else {
                    // Not a target entity type, treat as O
                    for (PkuWord sub : subWords) {
                        addPair(result, sub, "O");
                    }
                }
            } else {
                // Single word
                if (targetPos.contains(pos)) {
                    addPair(result, word, "S"); // Changed from S-pos to S
                } else {
                    addPair(result, word, "O");
                }
            }
        }
        return result;
    }

    private void addPair(List<Pair<WordAndNature>> result, PkuWord pkuWord, String label) {
        String wordStr = CharNormUtils.convert(pkuWord.getWord());
        Nature nature = Nature.parse(pkuWord.getPos());
        Token token = new Token(wordStr, nature);
        result.add(new Pair<>(token, label));
    }

    @Override
    public List<WordAndNature> parseToInputList(String text) {
        List<Pair<WordAndNature>> pairs = parse(text);
        return listToInput(pairs);
    }

    @Override
    public List<WordAndNature> listToInput(List<Pair<WordAndNature>> list) {
        List<WordAndNature> input = new ArrayList<>(list.size());
        for (Pair<WordAndNature> pair : list) {
            input.add(pair.first());
        }
        return input;
    }
}
