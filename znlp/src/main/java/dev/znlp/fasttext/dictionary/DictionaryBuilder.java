package dev.znlp.fasttext.dictionary;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.train.SampleLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 词典构建器类，负责从训练数据中构建FastText模型的词典。
 * 包含词的添加、词频过滤、词典生成等功能，是词典构建过程的核心类。
 */
public class DictionaryBuilder {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DictionaryBuilder.class);

    /**
     * 最大词汇表大小常量
     */
    private static final int MAX_VOCAB_SIZE = 30000000;

    /**
     * 快速词映射，用于存储词和其属性
     */
    public final FastWordMap wordIdMap;
    
    /**
     * 总词数
     */
    public long ntokens = 0;
    
    /**
     * 词的数量
     */
    public int nwords = 0;
    
    /**
     * 标签的数量
     */
    public int nlabels = 0;

    /**
     * 构造DictionaryBuilder实例
     *
     * @param label 标签前缀
     * @param vocabSize 词汇表大小
     * @param initWordListSize 初始词列表大小
     */
    public DictionaryBuilder(String label, int vocabSize, Integer initWordListSize) {
        this.wordIdMap = new FastWordMap(label, vocabSize, initWordListSize != null ? initWordListSize : 1000);
    }

    /**
     * 获取当前词典大小
     *
     * @return 当前词典大小
     */
    public int size() {
        return wordIdMap.size();
    }

    /**
     * 添加一个词到词典，默认计数为1
     *
     * @param word 要添加的词
     */
    public void add(String word) {
        add(word, 1);
    }

    /**
     * 添加一个词到词典，并指定计数
     *
     * @param word 要添加的词
     * @param count 词的计数
     */
    public void add(String word, int count) {
        wordIdMap.add(word, count);
        ntokens += count;
    }

    /**
     * 将当前构建的词典转换为Dictionary实例
     *
     * @param args 模型参数配置
     * @return Dictionary实例
     */
    public Dictionary toDictionary(Args args) {
        return new Dictionary(args, wordIdMap, ntokens, nwords, nlabels);
    }

    /**
     * 根据词频阈值过滤词典
     *
     * @param t 词的最小频率阈值
     * @param minLabelCount 标签的最小频率阈值
     */
    public void threshold(long t, long minLabelCount) {
        List<Entry> wordList = new ArrayList<>();
        for (Entry entry : wordIdMap.wordList) {
            boolean keep = (entry.type == EntryType.word && entry.count >= t) ||
                           (entry.type == EntryType.label && entry.count >= minLabelCount);
            if (keep) {
                wordList.add(entry);
            }
        }

        wordList.sort((o1, o2) -> {
            if (o1.type != o2.type) {
                return Integer.compare(o1.type.value, o2.type.value);
            } else {
                return Long.compare(o2.count, o1.count); // Descending count
            }
        });

        nwords = 0;
        nlabels = 0;

        wordIdMap.wordList.clear();
        wordIdMap.wordList.addAll(wordList);

        Arrays.fill(wordIdMap.wordHash2WordId, -1);

        for (int i = 0; i < wordList.size(); i++) {
            Entry entry = wordList.get(i);
            int h = wordIdMap.find(entry.word);
            wordIdMap.wordHash2WordId[h] = i;
            if (entry.type == EntryType.word) {
                nwords++;
            } else if (entry.type == EntryType.label) {
                nlabels++;
            }
        }
    }
    
    /**
     * 折叠词哈希到ID的映射数组，优化内存使用
     */
    public void collapseWordHash2Id() {
        wordIdMap.wordHash2WordId = new int[FastWordMap.hashTableSize(size())];
        wordIdMap.initWordHash2WordId();
    }

    /**
     * 从文件中构建词典
     *
     * @param args 模型参数配置
     * @param sources 训练数据迭代器列表
     * @param maxVocabSize 最大词汇表大小
     * @return 构建好的Dictionary实例
     */
    public static Dictionary buildFromFile(Args args, List<Iterable<SampleLine>> sources, int maxVocabSize) {
        DictionaryBuilder builder = new DictionaryBuilder(args.label, maxVocabSize, 5000);
        double mmm = 0.75 * maxVocabSize;
        long lastMinThreshold = 1;

        logger.info("Read file build dictionary ...");

        long[] thresholdCount = {0}; // Mutable wrapper

        for (Iterable<SampleLine> source : sources) {
            for (SampleLine sample : source) {
                for (String token : sample.words()) {
                    builder.add(token);
                    if (builder.ntokens % 1000000 == 0 && logger.isDebugEnabled()) {
                        logger.debug("Read {}M words", builder.ntokens / 1000000);
                    }

                    if (builder.size() > mmm) {
                        long minThreshold = lastMinThreshold;
                        thresholdCount[0]++;
                        while (builder.size() > (mmm * 0.75f)) {
                            // int before = builder.size();
                            builder.threshold(minThreshold, minThreshold);
                            lastMinThreshold = minThreshold;
                            minThreshold++;
                        }
                    }
                }
                builder.add(Dictionary.EOS);
            }
        }

        // System level pruning
        builder.threshold(args.minCount, args.minCountLabel);
        
        builder.collapseWordHash2Id();

        logger.info("Read {} words", builder.ntokens);
        logger.info("Number of words: {}", builder.nwords);
        logger.info("Number of labels: {}", builder.nlabels);

        if (builder.size() == 0) {
            throw new RuntimeException("Empty vocabulary. Try a smaller -minCount value.");
        }

        return builder.toDictionary(args);
    }
}
