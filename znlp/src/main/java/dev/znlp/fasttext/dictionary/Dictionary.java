package dev.znlp.fasttext.dictionary;

import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.ModelName;
import dev.znlp.fasttext.utils.LittleEndianData;
import dev.znlp.fasttext.utils.AutoDataInput;
import dev.znlp.fasttext.utils.HashUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 词典类，负责管理FastText模型的词表和标签。
 * 包含词的ID映射、频率统计、子词提取等功能，是FastText模型的重要组成部分。
 */
public class Dictionary {

    /**
     * 句尾标记符
     */
    public static final String EOS = "</s>";
    
    /**
     * 词头标记符
     */
    public static final String BOW = "<";
    
    /**
     * 词尾标记符
     */
    public static final String EOW = ">";

    /**
     * 模型参数配置
     */
    public final Args args;
    
    /**
     * 快速词映射，管理词到ID的映射和词的属性
     */
    public final FastWordMap onehotMap;
    
    /**
     * 总词数
     */
    public final long ntokens;
    
    /**
     * 词的数量
     */
    public final int nwords;
    
    /**
     * 标签的数量
     */
    public final int nlabels;

    /**
     * 词的丢弃概率数组，用于负采样
     */
    private float[] pdiscard = new float[0];
    
    /**
     * 剪枝索引大小
     */
    public long pruneidxSize = -1;
    
    /**
     * 剪枝索引映射
     */
    public Map<Integer, Integer> pruneidx = new HashMap<>();

    /**
     * 最大子词长度
     */
    private final int maxn;
    
    /**
     * 最小子词长度
     */
    private final int minn;
    
    /**
     * 哈希桶数量
     */
    private final int bucket;
    
    /**
     * 词n-gram的最大长度
     */
    private final int wordNgrams;

    /**
     * 构造Dictionary实例
     *
     * @param args 模型参数配置
     * @param onehotMap 快速词映射
     * @param ntokens 总词数
     * @param nwords 词的数量
     * @param nlabels 标签的数量
     */
    public Dictionary(Args args, FastWordMap onehotMap, long ntokens, int nwords, int nlabels) {
        this.args = args;
        this.onehotMap = onehotMap;
        this.ntokens = ntokens;
        this.nwords = nwords;
        this.nlabels = nlabels;

        this.maxn = args.maxn;
        this.minn = args.minn;
        this.bucket = args.bucket;
        this.wordNgrams = args.wordNgrams;
    }

    /**
     * 获取词典大小
     *
     * @return 词典大小
     */
    public int size() {
        return onehotMap.size();
    }

    /**
     * 检查词典是否被剪枝
     *
     * @return 是否被剪枝
     */
    public boolean isPruned() {
        return pruneidxSize >= 0;
    }

    /**
     * 计算词的子词特征。按 UTF-8 字符边界切分，与 Facebook FastText 一致。
     *
     * @param word 词（包含词头和词尾标记）
     * @param ngrams 输出的子词列表
     */
    private void computeSubwords(String word, IntArrayList ngrams) {
        byte[] bytes = word.getBytes(StandardCharsets.UTF_8);
        int wordLen = bytes.length;
        for (int i = 0; i < wordLen; i++) {
            if ((bytes[i] & 0xC0) == 0x80) {
                continue;
            }
            int j = i;
            int n = 1;
            while (j < wordLen && n <= maxn) {
                j++;
                while (j < wordLen && (bytes[j] & 0xC0) == 0x80) {
                    j++;
                }
                if (n >= minn && !(n == 1 && (i == 0 || j == wordLen))) {
                    int h = Integer.remainderUnsigned(HashUtils.fnv1aHash(bytes, i, j - i), bucket);
                    pushHash(ngrams, h);
                }
                n++;
            }
        }
    }

    /**
     * 获取指定类型的词频数组
     *
     * @param type 条目类型
     * @return 词频数组
     */
    public long[] getCounts(EntryType type) {
        long[] counts = (type == EntryType.label) ? new long[nlabels] : new long[nwords];
        int i = 0;
        for (Entry entry : onehotMap.wordList) {
            if (entry.type == type) {
                counts[i++] = entry.count;
            }
        }
        return counts;
    }

    /**
     * 从分词列表中获取词ID列表和标签ID列表
     *
     * @param tokens 分词列表
     * @param words 输出的词ID列表
     * @param labels 输出的标签ID列表
     * @return 处理的词数
     */
    public int getLine(Iterable<String> tokens, IntArrayList words, IntArrayList labels) {
        IntArrayList word_hashes = new IntArrayList();
        int ntokens = 0;

        words.clear();
        labels.clear();

        for (String token : tokens) {
            int h = HashUtils.fnv1aHash(token);
            int wid = onehotMap.getId(token, h);
            EntryType type = (wid < 0) ? onehotMap.getType(token) : onehotMap.getType(wid);

            ntokens++;

            if (type == EntryType.word) {
                addSubwords(words, token, wid);
                word_hashes.add(h);
            } else if (type == EntryType.label && wid >= 0) {
                labels.add(wid - nwords);
            }
        }

        addWordNgrams(words, word_hashes, wordNgrams);

        return ntokens;
    }
    
    /**
     * 从分词列表中获取词ID列表（用于词向量训练）
     *
     * @param tokens 分词列表
     * @param words 输出的词ID列表
     * @param rng 随机数生成器
     * @return 处理的词数
     */
    public int getLine(List<String> tokens, IntArrayList words, Random rng) {
        int ntokens = 0;
        words.clear();
        for (String token : tokens) {
            int wid = onehotMap.getId(token);
            if (wid < 0) continue;

            ntokens++;

            if (onehotMap.getType(wid) == EntryType.word && !discard(wid, rng.nextFloat())) {
                words.add(wid);
            }
            if (ntokens > 1024 || token.equals(EOS)) { // MAX_LINE_SIZE
                break;
            }
        }
        return ntokens;
    }

    /**
     * 添加词n-gram特征到词ID列表
     *
     * @param line 词ID列表
     * @param hashes 词哈希列表
     * @param n n-gram的最大长度
     */
    private void addWordNgrams(IntArrayList line, IntArrayList hashes, int n) {
        int hashSize = hashes.size();
        for (int i = 0; i < hashSize; i++) {
            long h = Integer.toUnsignedLong(hashes.get(i));
            long coeff = 116049371L;
            long bucketLong = Integer.toUnsignedLong(bucket);

            for (int j = i + 1; j < Math.min(hashSize, i + n); j++) {
                h = h * coeff + Integer.toUnsignedLong(hashes.get(j));
                pushHash(line, (int)Long.remainderUnsigned(h, bucketLong));
            }
        }
    }

    /**
     * 添加词的子词特征到词ID列表
     *
     * @param line 词ID列表
     * @param token 词
     * @param wid 词ID
     */
    private void addSubwords(IntArrayList line, String token, int wid) {
        if (wid < 0) { // out of vocab
            if (!EOS.equals(token)) {
                computeSubwords(BOW + token + EOW, line);
            }
        } else {
            if (maxn <= 0) { // in vocab w/o subwords
                line.add(wid);
            } else { // in vocab w/ subwords
                IntArrayList ngrams = getSubwords(wid);
                line.addAll(ngrams);
            }
        }
    }

    /**
     * 获取指定词ID的子词列表
     *
     * @param id 词ID
     * @return 子词列表
     */
    public IntArrayList getSubwords(int id) {
        return onehotMap.get(id).subwords;
    }

    /**
     * 获取指定词的子词列表
     *
     * @param word 词
     * @return 子词列表
     */
    public IntArrayList getSubwords(String word) {
        int i = onehotMap.getId(word);
        if (i >= 0) {
            return onehotMap.get(i).subwords;
        }

        IntArrayList ngrams = new IntArrayList();
        if (!word.equals(EOS)) {
            computeSubwords(BOW + word + EOW, ngrams);
        }
        return ngrams;
    }

    /**
     * 检查是否丢弃指定词
     *
     * @param id 词ID
     * @param rand 随机数
     * @return 是否丢弃
     */
    private boolean discard(int id, float rand) {
        return args.model != ModelName.sup && rand > pdiscard[id];
    }

    /**
     * 根据标签ID获取标签名称
     *
     * @param lid 标签ID
     * @return 标签名称
     */
    public String getLabel(int lid) {
        return onehotMap.get(lid + nwords).word;
    }
    
    /**
     * 根据词获取词ID
     *
     * @param word 词
     * @return 词ID
     */
    public int getWordId(String word) {
        return onehotMap.getId(word);
    }
    
    /**
     * 根据词ID获取词
     *
     * @param id 词ID
     * @return 词
     */
    public String getWord(int id) {
        return onehotMap.getWord(id);
    }

    /**
     * 初始化词典，包括初始化丢弃概率表和子词
     */
    public void init() {
        initTableDiscard();
        initNgrams();
    }

    /**
     * 初始化丢弃概率表
     */
    public void initTableDiscard() {
        pdiscard = new float[onehotMap.size()];
        double t = args.t;
        for (int i = 0; i < onehotMap.size(); i++) {
            float f = (float) (onehotMap.get(i).count * 1.0 / ntokens);
            pdiscard[i] = (float) (Math.sqrt(t / f) + t / f);
        }
    }

    /**
     * 初始化子词
     */
    public void initNgrams() {
        for (int id = 0; id < onehotMap.size(); id++) {
            Entry e = onehotMap.get(id);
            String word = BOW + e.word + EOW;

            if (maxn == 0) {
                e.subwords = new IntArrayList(1);
                e.subwords.add(id);
            } else {
                e.subwords = new IntArrayList();
                e.subwords.add(id);
                if (!e.word.equals(EOS)) {
                    computeSubwords(word, e.subwords);
                }
            }
        }
    }

    /**
     * 将哈希值添加到词ID列表
     *
     * @param hashes 词ID列表
     * @param id 哈希值
     */
    private void pushHash(IntArrayList hashes, int id) {
        if (pruneidxSize == 0 || id < 0) return;

        if (pruneidxSize > 0) {
            if (pruneidx.containsKey(id)) {
                id = pruneidx.get(id);
            } else {
                return;
            }
        }
        hashes.add(nwords + id);
    }

    /**
     * 按 Facebook FastText 小端格式写入词典。
     */
    public void save(OutputStream out) throws IOException {
        int size = onehotMap.size();
        LittleEndianData.writeInt(out, size);
        LittleEndianData.writeInt(out, nwords);
        LittleEndianData.writeInt(out, nlabels);
        LittleEndianData.writeLong(out, ntokens);
        LittleEndianData.writeLong(out, pruneidxSize);
        for (int i = 0; i < size; i++) {
            Entry e = onehotMap.get(i);
            LittleEndianData.writeCString(out, e.word);
            LittleEndianData.writeLong(out, e.count);
            out.write(e.type.value);
        }
        if (pruneidxSize > 0) {
            for (Map.Entry<Integer, Integer> pair : pruneidx.entrySet()) {
                LittleEndianData.writeInt(out, pair.getKey());
                LittleEndianData.writeInt(out, pair.getValue());
            }
        }
    }

    /**
     * 从模型文件中加载词典
     *
     * @param args 模型参数配置
     * @param buffer 自动数据输入
     * @return Dictionary实例
     * @throws IOException IO异常
     */
    public static Dictionary loadModel(Args args, AutoDataInput buffer) throws IOException {
        int size = buffer.readInt();
        int nwords = buffer.readInt();
        int nlabels = buffer.readInt();
        long ntokens = buffer.readLong();
        long pruneidxSize = buffer.readLong();

        List<Entry> wordList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Entry e = new Entry(buffer.readUTF(), buffer.readLong(), EntryType.fromValue(buffer.readUnsignedByte()));
            wordList.add(e);
        }

        Map<Integer, Integer> pruneidx = new HashMap<>();
        for (int i = 0; i < pruneidxSize; i++) {
            int first = buffer.readInt();
            int second = buffer.readInt();
            pruneidx.put(first, second);
        }

        FastWordMap onehotMap = new FastWordMap(
                new int[FastWordMap.hashTableSize(size)],
                wordList,
                args.label
        );

        Dictionary dict = new Dictionary(args, onehotMap, ntokens, nwords, nlabels);
        dict.pruneidxSize = pruneidxSize;
        dict.pruneidx = pruneidx;

        dict.initTableDiscard();
        dict.initNgrams();
        dict.onehotMap.initWordHash2WordId();

        return dict;
    }
}
