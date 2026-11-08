package dev.znlp.segment.reader;

import dev.znlp.config.ZnlpSystemConfig;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmentReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 过滤停用词
 * <p>
 * 可以指定一个或多个停用词词典
 *
 * @author jimichan
 */
public class StopwordFilter extends BaseFilterWordSegmentReader {

    private final List<StopWordDict> stopWords;

    public StopwordFilter(WordSegmentReader source, StopWordDict stopWord) {
        super(source);
        this.stopWords = Collections.singletonList(stopWord);
    }

    /**
     * 使用指定的停用词词典，和系统自带的通用词无关
     *
     * @param source
     * @param stopWords
     */
    public StopwordFilter(WordSegmentReader source, List<StopWordDict> stopWords) {
        super(source);
        ArrayList<StopWordDict> list = new ArrayList<>();
        for (StopWordDict stopWord : stopWords) {
            list.add(stopWord);
        }
        this.stopWords = list;
    }


    /**
     * 默认使用系统自带的停用词词典
     *
     * @param source
     */
    public StopwordFilter(WordSegmentReader source) {
        this(source, new SystemStopWordDict(new ZnlpSystemConfig()));
    }

    @Override
    public boolean test(Token term) {
        for (StopWordDict stopWord : stopWords) {
            if (stopWord.contains(term.word)) {
                return false;
            }
        }
        return true;
    }
}
