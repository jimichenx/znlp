package dev.znlp.newwords.engine;

import dev.znlp.newwords.scorer.AdaptiveNewWordScorer;
import dev.znlp.base.common.Characters;
import dev.znlp.base.common.ParagraphReader;
import dev.znlp.base.common.ParagraphReaderSmart;
import dev.znlp.base.dat.DoubleArrayTrieMap;
import dev.znlp.newwords.counter.FrequentItemsCounterFactory;
import dev.znlp.newwords.counter.IFrequentItemsCounter;
import dev.znlp.newwords.entity.IntCount;
import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.entity.WordInfo;
import dev.znlp.newwords.scorer.NewWordScorer;
import dev.znlp.newwords.scorer.NewWordScorerFactory;
import dev.znlp.segment.impl.dict.CoreDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 新词发现引擎
 *
 * @author jimichan
 */
public class NewWordFindEngine implements INewWordFindEngine {
    private final int minGroup;
    private final int maxGroup;
    private final boolean excludeCoreDict;

    private int docCount = 0;
    private final CoreDictionary coreDictionary;

    /**
     * 新词打分器（包含过滤和打分功能）
     */
    private NewWordScorer scorer = NewWordScorerFactory.DEFAULT_SCORER;

    /**
     * 语料中总共有多少字
     */
    private int ziCountTotal = 0;

    /**
     * 片段的数量
     */
    private int partCount = 0;

    /**
     * 每个字母的Freq
     */
    private final int[] ziFreqArray = new int[65535];

    /**
     * 收集频率出现最多的Ngram片断
     */
    private final IFrequentItemsCounter topWordCounter;

    /**
     * 第一轮计算下来的，最有可能的片段
     */
    private final HashMap<String, WordInfo> candidateMap = new HashMap<>();

    /**
     * 过滤以这些字符开头的片段
     */
    private final int[] filterStartChar = new int[65535];
    private final int[] filterContainsChar = new int[65535];

    /**
     * 候选词典. 主要用来第二轮代替hashmap，判断存在
     */
    private DoubleArrayTrieMap<String> dict;

    private boolean verbose = true;

    /**
     * 使用配置对象创建新词发现引擎（推荐方式）
     *
     * @param config          引擎配置参数
     * @param excludeCoreDict 是否排除核心词典中已存在的词汇
     * @param coreDictionary  核心词典实例
     */
    public NewWordFindEngine(INewWordFindEngine.Config config,
                             boolean excludeCoreDict, CoreDictionary coreDictionary) {
        this(config.minGroup(), config.maxGroup(), config.minOccurCount(),
                excludeCoreDict, coreDictionary);
    }

    /**
     * 使用原始参数创建新词发现引擎
     *
     * @param minGroup        发现词组的最小字符长度
     * @param maxGroup        发现词组的最大字符长度
     * @param minOccurCount   词组在语料中的最小出现次数
     * @param excludeCoreDict 是否排除核心词典中已存在的词汇
     * @param coreDictionary  核心词典实例
     */
    public NewWordFindEngine(int minGroup, int maxGroup, int minOccurCount,
                             boolean excludeCoreDict, CoreDictionary coreDictionary) {
        this.minGroup = minGroup;
        this.maxGroup = maxGroup;
        this.excludeCoreDict = excludeCoreDict;
        this.coreDictionary = coreDictionary;
        this.topWordCounter = FrequentItemsCounterFactory.createStandard(2000000, minOccurCount);

        // 初始化过滤字符数组
        for (int i = 0; i < 65535; i++) {
            filterStartChar[i] = 0;
            filterContainsChar[i] = 0;
            char c = (char) i;

            if (Characters.isPunctuation(c)) {
                filterStartChar[i] = 1;
                filterContainsChar[i] = 1;
                continue;
            }
            if (Characters.isASCII(c)) {
                filterStartChar[i] = 1;
                filterContainsChar[i] = 1;
                continue;
            }
            if (Character.isWhitespace(c)) {
                filterStartChar[i] = 1;
                filterContainsChar[i] = 1;
                continue;
            }
        }

        // 过滤特定字符
        String filterChars = "˦�来将就这的了和与想我你他为或是对并以于由有个之在把等再从及";
        for (char ch : filterChars.toCharArray()) {
            filterStartChar[ch] = 1;
        }

        // 初始化空的 dict
        TreeMap<String, String> emptyMap = new TreeMap<>();
        emptyMap.put("a", "");
        dict = new DoubleArrayTrieMap<>(emptyMap);
    }

    /**
     * 返回最终的结果，使用当前设置的打分器（包含过滤和打分功能），按score排序
     * <p>调用前必须先通过 setScorer() 设置打分器</p>
     */
    @Override
    public ArrayList<NewWord> result() {
        if (scorer == null) {
            throw new IllegalStateException("必须先通过 setScorer() 设置打分器");
        }

        // 如果是自适应打分器，先进行阈值学习
        if (scorer instanceof AdaptiveNewWordScorer adaptiveScorer) {
            if (!adaptiveScorer.isThresholdsLearned()) {
                // 将所有候选词传递给打分器进行学习
                adaptiveScorer.learnThresholds(new ArrayList<>(candidateMap.values()));
            }
        }
        
        ArrayList<NewWord> resultList = new ArrayList<>(candidateMap.size());

        for (WordInfo info : candidateMap.values()) {
            if (scorer.accept(info)) {
                NewWord newWord = new NewWord(
                        info.getWord(), info.getWord().length(),
                        info.getCount(), info.getDoc(),
                        info.getMi(),
                        info.getMi_avg(),
                        info.getEntropy(),
                        info.getLe(),
                        info.getRe(),
                        info.getIdf(),
                        info.getLeftVariety(),
                        info.getRightVariety()
                );
                newWord.setScore(scorer.score(newWord));
                resultList.add(newWord);
            }
        }

        resultList.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        return resultList;
    }

    /**
     * 第一轮扫描。该方法可以被调用多次,建议每次传入一篇文章.
     * 第一轮主要完成基本统计
     *
     * @param document
     */
    public void firstScan(String document) throws IOException {
        ParagraphReader reader = new ParagraphReaderSmart(document);
        String line = reader.next();

        while (line != null) {
            char[] charArray = line.toCharArray();
            int len = charArray.length;

            ziCountTotal += len;

            // 字频
            for (char ch : line.toCharArray()) {
                ziFreqArray[ch]++;
            }

            // NGram 循环
            for (int i = 0; i < len; i++) {
                int firstChar = charArray[i];
                if (filterStartChar[firstChar] == 1) {
                    continue;
                }

                for (int s = minGroup; s <= maxGroup; s++) {
                    int endIndex = i + s;
                    if (endIndex <= len) {
                        // 最后一个字也要过滤
                        if (filterStartChar[charArray[endIndex - 1]] == 1) {
                            continue;
                        }

                        boolean toSkip = false;

                        if (s <= 5) {
                            for (int j = i; j < endIndex; j++) {
                                if (filterStartChar[charArray[j]] == 1) {
                                    toSkip = true;
                                    break;
                                }
                            }
                        }

                        if (toSkip) {
                            continue;
                        }

                        for (int j = i; j < endIndex; j++) {
                            if (filterContainsChar[charArray[j]] == 1) {
                                toSkip = true;
                                break;
                            }
                        }

                        if (toSkip) {
                            continue;
                        }

                        String word = line.substring(i, endIndex);

                        if (excludeCoreDict && coreDictionary != null && coreDictionary.contains(word)) {
                            continue;
                        }

                        topWordCounter.put(word);
                        partCount++;
                    }
                }
            }

            line = reader.next();
        }
    }

    public void finishFirst() {
        topWordCounter.clean();

        TreeMap<String, String> treeMap = new TreeMap<>();

        for (Map.Entry<String, IntCount> entry : topWordCounter.getData().entrySet()) {
            if (excludeCoreDict && coreDictionary != null && coreDictionary.contains(entry.getKey())) {
                continue;
            }

            treeMap.put(entry.getKey(), "");
            candidateMap.put(entry.getKey(), new WordInfo(entry.getKey()));
        }

        dict = new DoubleArrayTrieMap<>(treeMap);
    }

    /**
     * 第二轮扫描。该方法被调用多次,建议每次传入一篇文章.
     * 根据第一轮的基本统计，采用互信息和信息熵的方式计算新词
     */
    public void secondScan(String document) throws IOException {
        docCount++;
        ParagraphReader reader = new ParagraphReaderSmart(document);
        String line = reader.next();

        while (line != null) {
            int len = line.length();

            // NGram 循环
            for (int i = 0; i < len; i++) {
                int firstChar = line.charAt(i);
                if (filterStartChar[firstChar] == 1) {
                    continue;
                }

                for (int s = minGroup; s <= maxGroup; s++) {
                    int toIndex = i + s;
                    if (toIndex <= len) {
                        if (dict.get(line, i, toIndex - i) == null) {
                            continue;
                        }

                        String word = line.substring(i, toIndex);

                        WordInfo info = candidateMap.get(word);
                        if (info != null) {
                            info.incrementCount();
                            char left = (i >= 1) ? line.charAt(i - 1) : '^';
                            char right = (toIndex < len) ? line.charAt(toIndex) : '$';

                            info.addToLeft(left, 1);
                            info.addToRight(right, 1);
                            info.getDocSet().add(docCount);
                        }
                    }
                }
            }

            line = reader.next();
        }
    }

    public void endSecond() {
        double dc = docCount;
        double zc = ziCountTotal;

        for (WordInfo info : candidateMap.values()) {
            // 计算互信息
            // 在一起的概率
            float fenzi = info.getCount() / (float) ziCountTotal;
            float fenmu = 1f;
            for (char ch : info.getWord().toCharArray()) {
                fenmu *= ziFreqArray[ch] / (float) ziCountTotal;
            }

            float mi = (float) (Math.log(fenzi / fenmu) / Math.log(2));
            info.setMi(mi);
            info.setMi_avg(mi / info.getWord().length());

            // 计算左右最小熵
            info.entropy();
            info.tfIdf(dc, zc);
        }

        // 自动设置打分器的统计信息
        if (scorer != null) {
            scorer.setStatistics(ziCountTotal, docCount, ziFreqArray);
        }
    }

    // Getters and Setters
    public int getDocCount() {
        return docCount;
    }

    public int getZiCountTotal() {
        return ziCountTotal;
    }

    public int getPartCount() {
        return partCount;
    }

    public HashMap<String, WordInfo> getCandidateMap() {
        return candidateMap;
    }

    public int[] getZiFreqArray() {
        return ziFreqArray;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * 获取当前打分器
     */
    @Override
    public NewWordScorer getScorer() {
        return scorer;
    }

    /**
     * 设置打分器
     */
    @Override
    public void setScorer(NewWordScorer scorer) {
        this.scorer = scorer;
    }

}
