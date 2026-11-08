package dev.znlp.converter;

import dev.znlp.base.dat.DoubleArrayTrieMap;

import java.util.TreeMap;

/**
 * 繁简体转换词典。用双数组的最长匹配做替换：同一起点取最长词，然后从该词之后继续。
 *
 * @author jimichan
 */
public class TransformDictionaryImpl implements TransformDictionary {

    private final DoubleArrayTrieMap<String> words;

    /**
     * @param wordMap 词语映射，键为待匹配的词，值为替换结果
     */
    public TransformDictionaryImpl(TreeMap<String, String> wordMap) {
        this.words = new DoubleArrayTrieMap<>(wordMap);
    }

    @Override
    public String transform(String text) {
        return transform(text.toCharArray());
    }

    @Override
    public String transform(char[] charArray) {
        DoubleArrayTrieMap<String>.MapLongMatcher matcher = words.matchLong(charArray);
        StringBuilder sb = new StringBuilder(charArray.length);
        int offset = 0;
        while (matcher.next()) {
            int begin = matcher.getBegin();
            while (offset < begin) {
                sb.append(charArray[offset]);
                offset++;
            }
            sb.append(matcher.getValue());
            offset = begin + matcher.getLength();
        }
        while (offset < charArray.length) {
            sb.append(charArray[offset]);
            offset++;
        }
        return sb.toString();
    }
}
