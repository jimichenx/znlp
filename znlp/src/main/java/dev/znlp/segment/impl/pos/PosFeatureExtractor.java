package dev.znlp.segment.impl.pos;

import dev.znlp.base.common.Characters;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.utils.FastStringBuilder;

import java.util.List;

/**
 * 词性标注特征提取器
 *
 * @author jimichan
 */
public class PosFeatureExtractor implements FeatureExtractor<List<String>> {

    @Override
    public void compute(List<String> sentence, int position, FastStringBuilder buffer, FeatureConsumer consumer) {
        int size = sentence.size();

        String preWord = (position > 0) ? sentence.get(position - 1) : null;
        String curWord = sentence.get(position);
        String nextWord = (position < size - 1) ? sentence.get(position + 1) : null;

        // 检查后一个词是否为标点
        if (nextWord != null && nextWord.length() == 1) {
            char c = nextWord.charAt(0);
            if (Characters.isPunctuation(c) || c == ' ') {
                nextWord = null;
            }
        }

        // 检查前一个词是否为标点
        if (preWord != null && preWord.length() == 1) {
            char c = preWord.charAt(0);
            if (Characters.isPunctuation(c) || c == ' ') {
                preWord = null;
            }
        }

        // 特征1: 前一个词
        if (preWord != null) {
            buffer.clear();
            buffer.append(preWord);
            buffer.append('☺');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征2: 当前词 (为了增加权重发射两次)
        buffer.clear();
        buffer.append(curWord);
        consumer.acceptChars(buffer.buffer(), buffer.length());
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // 特征3: 后一个词
        if (nextWord != null) {
            buffer.clear();
            buffer.append(nextWord);
            buffer.append('♂');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征4: 前缀和后缀
        int length = curWord.length();
        if (length >= 2) {
            int last = length - 1;
            char c1 = curWord.charAt(0);
            char l1 = curWord.charAt(last);

            // 长度 >= 2
            buffer.clear();
            buffer.append(c1);
            buffer.append('★');
            consumer.acceptChars(buffer.buffer(), buffer.length());

            buffer.clear();
            buffer.append(l1);
            buffer.append('✆');
            consumer.acceptChars(buffer.buffer(), buffer.length());

            if (length >= 3) {
                char c2 = curWord.charAt(1);
                char l2 = curWord.charAt(last - 1);

                // 长度 >= 3
                buffer.clear();
                buffer.append(c1);
                buffer.append(c2);
                buffer.append('★');
                consumer.acceptChars(buffer.buffer(), buffer.length());

                buffer.clear();
                buffer.append(l1);
                buffer.append(l2);
                buffer.append('✆');
                consumer.acceptChars(buffer.buffer(), buffer.length());

                if (length >= 4) {
                    char c3 = curWord.charAt(2);
                    char l3 = curWord.charAt(last - 2);

                    // 长度 >= 4
                    buffer.clear();
                    buffer.append(c1);
                    buffer.append(c2);
                    buffer.append(c3);
                    buffer.append('★');
                    consumer.acceptChars(buffer.buffer(), buffer.length());

                    buffer.clear();
                    buffer.append(l1);
                    buffer.append(l2);
                    buffer.append(l3);
                    buffer.append('✆');
                    consumer.acceptChars(buffer.buffer(), buffer.length());
                }
            }
        }
    }
}
