package dev.znlp.segment.impl.cws;

import dev.znlp.perceptron.CharWindow;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.utils.FastStringBuilder;

/**
 * CWS 感知机特征提取器，与 {@code PerceptronSegmentDefinition} 模板一致。
 * <pre>
 * %[0]2;
 * %[-1]1;
 * %[-1]/%[0]5;
 * %[-2]/%[-1]4;
 * %[1]3;
 * %[0]/%[1]6;
 * %[1]/%[2]7;
 * </pre>
 */
class CwsFeatureExtractor implements FeatureExtractor<char[]> {

    @Override
    public void compute(char[] sentence, int position,
                        FastStringBuilder buffer,
                        FeatureConsumer consumer) {
        int size = CharWindow.length(sentence);
        if (position < 0 || position >= size) {
            return;
        }

        buffer.clear();
        buffer.append(CharWindow.at(sentence, position));
        buffer.append('2');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        int posMinus1 = position - 1;
        if (posMinus1 >= 0) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posMinus1));
            buffer.append('1');
            consumer.acceptChars(buffer.buffer(), buffer.length());

            buffer.clear();
            buffer.append(CharWindow.at(sentence, posMinus1));
            buffer.append('/');
            buffer.append(CharWindow.at(sentence, position));
            buffer.append('5');
            consumer.acceptChars(buffer.buffer(), buffer.length());

            int posMinus2 = position - 2;
            if (posMinus2 >= 0) {
                buffer.clear();
                buffer.append(CharWindow.at(sentence, posMinus2));
                buffer.append('/');
                buffer.append(CharWindow.at(sentence, posMinus1));
                buffer.append('4');
                consumer.acceptChars(buffer.buffer(), buffer.length());
            }
        }

        int posPlus1 = position + 1;
        if (posPlus1 < size) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posPlus1));
            buffer.append('3');
            consumer.acceptChars(buffer.buffer(), buffer.length());

            buffer.clear();
            buffer.append(CharWindow.at(sentence, position));
            buffer.append('/');
            buffer.append(CharWindow.at(sentence, posPlus1));
            buffer.append('6');
            consumer.acceptChars(buffer.buffer(), buffer.length());

            int posPlus2 = position + 2;
            if (posPlus2 < size) {
                buffer.clear();
                buffer.append(CharWindow.at(sentence, posPlus1));
                buffer.append('/');
                buffer.append(CharWindow.at(sentence, posPlus2));
                buffer.append('7');
                consumer.acceptChars(buffer.buffer(), buffer.length());
            }
        }
    }
}
