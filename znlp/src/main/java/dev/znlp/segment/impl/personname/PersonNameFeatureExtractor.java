package dev.znlp.segment.impl.personname;

import dev.znlp.perceptron.CharWindow;
import dev.znlp.perceptron.FeatureExtractor;
import dev.znlp.perceptron.feature.FeatureConsumer;
import dev.znlp.perceptron.utils.FastStringBuilder;

/**
 * 人名识别特征提取器
 * <p>
 * 直接展开模板规则，避免模板解析和循环开销，提供极高的性能。
 * <p>
 * 对应的模板规则：
 * <pre>
 * %[0]2;          // 当前位置字符 + '2'
 * %[-1]1;         // 前1个字符 + '1'
 * %[-1]/%[0]5;    // 前1个字符 + '/' + 当前字符 + '5'
 * %[-2]/%[-1]4;   // 前2个字符 + '/' + 前1个字符 + '4'
 * %[1]3;          // 后1个字符 + '3'
 * %[0]/%[1]6;     // 当前字符 + '/' + 后1个字符 + '6'
 * %[1]/%[2]7;     // 后1个字符 + '/' + 后2个字符 + '7'
 * </pre>
 */
class PersonNameFeatureExtractor implements FeatureExtractor<char[]> {

    @Override
    public void compute(char[] sentence, int position,
                        FastStringBuilder buffer,
                        FeatureConsumer consumer) {
        int size = CharWindow.length(sentence);

        // 边界检查：确保position有效
        if (position < 0 || position >= size) {
            return;
        }

        // 特征1: %[0]2 - 当前位置字符 + '2'
        buffer.clear();
        buffer.append(CharWindow.at(sentence, position));
        buffer.append('2');
        consumer.acceptChars(buffer.buffer(), buffer.length());

        // 特征2: %[-1]1 - 前1个字符 + '1'
        int posMinus1 = position - 1;
        if (posMinus1 >= 0) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posMinus1));
            buffer.append('1');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征3: %[-1]/%[0]5 - 前1个字符 + '/' + 当前字符 + '5'
        if (posMinus1 >= 0) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posMinus1));
            buffer.append('/');
            buffer.append(CharWindow.at(sentence, position));
            buffer.append('5');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征4: %[-2]/%[-1]4 - 前2个字符 + '/' + 前1个字符 + '4'
        int posMinus2 = position - 2;
        if (posMinus2 >= 0 && posMinus1 >= 0) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posMinus2));
            buffer.append('/');
            buffer.append(CharWindow.at(sentence, posMinus1));
            buffer.append('4');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征5: %[1]3 - 后1个字符 + '3'
        int posPlus1 = position + 1;
        if (posPlus1 < size) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posPlus1));
            buffer.append('3');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征6: %[0]/%[1]6 - 当前字符 + '/' + 后1个字符 + '6'
        if (posPlus1 < size) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, position));
            buffer.append('/');
            buffer.append(CharWindow.at(sentence, posPlus1));
            buffer.append('6');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }

        // 特征7: %[1]/%[2]7 - 后1个字符 + '/' + 后2个字符 + '7'
        int posPlus2 = position + 2;
        if (posPlus1 < size && posPlus2 < size) {
            buffer.clear();
            buffer.append(CharWindow.at(sentence, posPlus1));
            buffer.append('/');
            buffer.append(CharWindow.at(sentence, posPlus2));
            buffer.append('7');
            consumer.acceptChars(buffer.buffer(), buffer.length());
        }
    }
}
