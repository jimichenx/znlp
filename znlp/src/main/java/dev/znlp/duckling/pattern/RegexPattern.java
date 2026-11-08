package dev.znlp.duckling.pattern;

import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.TextPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则 pattern，对整句 {@code matcher.find()}。长文本上条数宜少，能进词典的不要写正则。
 */
public class RegexPattern extends TextDucklingPattern {

    private final Pattern pattern;

    public RegexPattern(int id, Pattern pattern) {
        super(id);
        this.pattern = pattern;
    }

    @Override
    public List<DkNode<TextToken>> predicate(String text, Map<String, List<DkNode<TextToken>>> dictMatch) {
        List<DkNode<TextToken>> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end() - 1; // Inclusive end
            if (end < start) continue; // Empty match

            TextPosition fullRange = new TextPosition(start, end);
            List<TextPosition> groups = new ArrayList<>();

            for (int i = 0; i <= matcher.groupCount(); i++) {
                int gStart = matcher.start(i);
                int gEnd = matcher.end(i) - 1;
                if (gStart != -1) {
                    groups.add(new TextPosition(gStart, gEnd));
                } else {
                    groups.add(null);
                }
            }

            results.add(new DkNode<>(
                    "Regex[" + pattern + "]",
                    fullRange,
                    Collections.emptyList(),
                    new TextToken(matcher.group(), groups),
                    true
            ));
        }
        return results;
    }

    @Override
    public String toString() {
        return "RegexPattern(" + pattern + ") (" + id + ")";
    }
}
