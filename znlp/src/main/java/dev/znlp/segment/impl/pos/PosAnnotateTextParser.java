package dev.znlp.segment.impl.pos;

import dev.znlp.base.common.CharNormUtils;
import dev.znlp.perceptron.AnnotateTextParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 词性标注语料解析器
 * <p>
 * 解析格式：word/tag word/tag
 *
 * @author jimichan
 */
public class PosAnnotateTextParser implements AnnotateTextParser<String, List<String>> {

    @Override
    public List<Pair<String>> parse(String text) {
        List<Pair<String>> result = new ArrayList<>();
        // Split by whitespace
        String[] parts = text.trim().split("\\s+");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            // Split by last slash to separate word and tag
            int lastSlash = part.lastIndexOf('/');
            if (lastSlash > 0 && lastSlash < part.length() - 1) {
                String word = part.substring(0, lastSlash);
                String tag = part.substring(lastSlash + 1);
                word = CharNormUtils.convert(word);
                // 注意：PerceptronTrainer 约定 Pair.first 是输入元素(Word)，Pair.second 是标签(Label)
                result.add(new Pair<>(word, tag));
            }
        }
        return result;
    }

    @Override
    public List<String> parseToInputList(String text) {
        List<Pair<String>> pairs = parse(text);
        return listToInput(pairs);
    }

    @Override
    public List<String> listToInput(List<Pair<String>> list) {
        List<String> input = new ArrayList<>(list.size());
        for (Pair<String> pair : list) {
            input.add(pair.first());
        }
        return input;
    }
}
