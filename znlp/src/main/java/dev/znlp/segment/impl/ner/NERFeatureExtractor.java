package dev.znlp.segment.impl.ner;

import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.utils.FastStringBuilder;
import dev.znlp.segment.WordAndNature;

import java.util.List;

/**
 * NER Feature Extractor
 *
 * @author jimichan
 */
public class NERFeatureExtractor implements FeatureExtractor<List<WordAndNature>> {

    private static final String B = "_B_";
    private static final String E = "_E_";

    @Override
    public void compute(List<WordAndNature> sentence, int position, FastStringBuilder buffer, FeatureConsumer consumer) {
        int size = sentence.size();

        String pre3Word = B;
        String pre2Word = B;
        String pre2Pos = B;

        if (position >= 2) {
            WordAndNature x = sentence.get(position - 2);
            pre2Word = x.getWord();
            pre2Pos = x.getNatureName();

            if (position > 2) {
                pre3Word = sentence.get(position - 3).getWord();
            }
        }

        String preWord = B;
        String prePos = B;
        if (position >= 1) {
            WordAndNature x = sentence.get(position - 1);
            preWord = x.getWord();
            prePos = x.getNatureName();
        }

        WordAndNature cur = sentence.get(position);
        String curWord = cur.getWord();
        String curPos = cur.getNatureName();

        String nextWord = E;
        String nextPos = E;
        if (position <= size - 2) {
            WordAndNature x = sentence.get(position + 1);
            nextWord = x.getWord();
            nextPos = x.getNatureName();
        }

        String next2Word = E;
        String next2Pos = E;
        if (position <= size - 3) {
            WordAndNature x = sentence.get(position + 2);
            next2Word = x.getWord();
            next2Pos = x.getNatureName();
        }

        // Feature 1: pre2Word
        buffer.clear();
        buffer.append(pre2Word);
        buffer.append('1');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature 2: preWord
        buffer.clear();
        buffer.append(preWord);
        buffer.append('2');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature 3: curWord
        buffer.clear();
        buffer.append(curWord);
        buffer.append('3');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature 4: nextWord
        buffer.clear();
        buffer.append(nextWord);
        buffer.append('4');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature 5: next2Word
        buffer.clear();
        buffer.append(next2Word);
        buffer.append('5');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature A: pre2Pos
        buffer.clear();
        buffer.append(pre2Pos);
        buffer.append('A');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature B: prePos
        buffer.clear();
        buffer.append(prePos);
        buffer.append('B');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature C: curPos
        buffer.clear();
        buffer.append(curPos);
        buffer.append('C');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature D: nextPos
        buffer.clear();
        buffer.append(nextPos);
        buffer.append('D');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature E: next2Pos
        buffer.clear();
        buffer.append(next2Pos);
        buffer.append('E');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature F: pre2Pos + prePos
        buffer.clear();
        buffer.append(pre2Pos);
        buffer.append(prePos);
        buffer.append('F');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature G: prePos + curPos
        buffer.clear();
        buffer.append(prePos);
        buffer.append(curPos);
        buffer.append('G');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature H: curPos + nextPos
        buffer.clear();
        buffer.append(curPos);
        buffer.append(nextPos);
        buffer.append('H');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature I: nextPos + next2Pos
        buffer.clear();
        buffer.append(nextPos);
        buffer.append(next2Pos);
        buffer.append('I');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // Feature J: pre3Word
        buffer.clear();
        buffer.append(pre3Word);
        buffer.append('J');
        consumer.acceptChars(buffer.buffer(), buffer.length());
    }
}
