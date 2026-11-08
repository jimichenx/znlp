package dev.znlp.starspace;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 词典类，用于将字符串标记（单词/标签）映射到整数 ID
 * <p>
 * 该类负责词汇表的构建、计数阈值过滤、N-gram 哈希等功能。
 * 主要用于将输入文本转换为模型可处理的整数 ID 表示，支持从文件构建词典。
 */
public class Dictionary {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Dictionary.class);

    /** 哈希常数 */
    private static final int HASH_C = 116049371;
    /** 最大词汇表大小 */
    private static final int MAX_VOCAB_SIZE = 30000000;

    /** 训练参数 */
    private final Args args;
    /** 标签前缀 */
    private final String label;
    /** 是否使用权重 */
    private final boolean useWeight;
    /** 是否归一化文本 */
    private final boolean normalizeText;
    /** N-gram 大小 */
    private final int ngrams;
    /** 哈希桶大小 */
    private final int bucket;

    /** 词条列表 */
    private List<Entry> entryList = new ArrayList<>(50000);
    /** 哈希到索引的映射 */
    private final int[] hashToIndex = new int[MAX_VOCAB_SIZE];

    /** 词典大小 */
    private int size = 0;
    /** 单词数量 */
    private int nwords = 0;
    /** 标签数量 */
    private int nlabels = 0;
    /** 标记总数 */
    private long ntokens = 0;

    /** 空白字符模式 */
    private final Pattern whitePattern = Pattern.compile("\\s");

    /**
     * 构造词典对象
     * @param args 训练参数
     */
    public Dictionary(Args args) {
        this.args = args;
        this.label = args.label;
        this.useWeight = args.useWeight;
        this.normalizeText = args.normalizeText;
        this.ngrams = args.ngrams;
        this.bucket = args.bucket;
        Arrays.fill(hashToIndex, -1);
    }

    /**
     * 获取词典大小
     * @return 词典大小
     */
    public int size() {
        return size;
    }

    /**
     * 获取单词数量
     * @return 单词数量
     */
    public int nwords() {
        return nwords;
    }

    /**
     * 获取标签数量
     * @return 标签数量
     */
    public int nlabels() {
        return nlabels;
    }

    /**
     * 获取标记总数
     * @return 标记总数
     */
    public long ntokens() {
        return ntokens;
    }

    /**
     * 计算字符串的哈希值
     * @param str 输入字符串
     * @return 哈希值
     */
    private long hash(String str) {
        int h = (int) 2166136261L;
        for (byte strByte : str.getBytes()) {
            h = (h ^ strByte) * 16777619;
        }
        return h & 0xffffffffL;
    }

    /**
     * 查找单词在词典中的位置
     * @param word 单词
     * @return 位置索引
     */
    private int find(String word) {
        int h = (int) (hash(word) % MAX_VOCAB_SIZE);
        while (hashToIndex[h] != -1 && !entryList.get(hashToIndex[h]).symbol.equals(word)) {
            h = (h + 1) % MAX_VOCAB_SIZE;
        }
        return h;
    }

    /**
     * 获取符号的 ID
     * @param symbol 符号字符串
     * @return 符号的 ID，不存在则返回 -1
     */
    public int getId(String symbol) {
        int h = find(symbol);
        return hashToIndex[h];
    }

    /**
     * 根据 ID 获取符号
     * @param id 符号 ID
     * @return 符号字符串
     */
    public String getSymbol(int id) {
        Utils.checkArgument(id >= 0);
        Utils.checkArgument(id < size);
        return entryList.get(id).symbol;
    }

    /**
     * 根据标签 ID 获取标签
     * @param lid 标签 ID
     * @return 标签字符串
     */
    public String getLabel(int lid) {
        Utils.checkArgument(lid >= 0);
        Utils.checkArgument(lid < nlabels);
        return entryList.get(lid + nwords).symbol;
    }

    /**
     * 根据 ID 获取词条类型
     * @param id 词条 ID
     * @return 词条类型
     */
    public EntryType getType(int id) {
        Utils.checkArgument(id >= 0);
        Utils.checkArgument(id < size);
        return entryList.get(id).type;
    }

    /**
     * 根据字符串获取类型
     * @param w 输入字符串
     * @return 词条类型
     */
    public EntryType getType(String w) {
        return w.startsWith(label) ? EntryType.Label : EntryType.Word;
    }

    /**
     * 插入符号到词典
     * @param symbol 符号字符串
     */
    private void insert(String symbol) {
        int h = find(symbol);
        ntokens++;
        if (hashToIndex[h] == -1) {
            Entry e = new Entry(symbol, 1, getType(symbol));
            entryList.add(e);
            hashToIndex[h] = size++;
        } else {
            entryList.get(hashToIndex[h]).count++;
        }
    }

    /**
     * 从文件读取数据并构建词典
     * @param file 文件路径
     * @throws IOException IO异常
     */
    public void readFromFile(String file) throws IOException {
        logger.info("Build dict from input file : {}", file);
        
        long minThreshold = 1;
        AtomicLong linesRead = new AtomicLong(0);
        double over = 0.75 * MAX_VOCAB_SIZE;

        try (Stream<String> lines = Utils.lines(new File(file))) {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line == null || line.trim().isEmpty()) continue;
                
                linesRead.incrementAndGet();
                List<String> tokens = parseForDict(line);

                for (String token : tokens) {
                    insert(token);
                    if (ntokens % 1000000 == 0 && logger.isDebugEnabled()) {
                        logger.debug("Read {}M words", ntokens / 1000000);
                    }
                    if (size > over) {
                        minThreshold++;
                        threshold(minThreshold, minThreshold);
                    }
                }
            }
        }

        threshold(args.minCount, args.minCountLabel);

        logger.info("Read {}M words", ntokens / 1000000);
        logger.info("Number of words in dictionary: {}", nwords);
        logger.info("Number of labels in dictionary: {}", nlabels);

        if (linesRead.get() == 0) {
            throw new IOException("Empty file: " + file);
        }

        if (size == 0) {
            throw new IOException("Empty vocabulary. Try a smaller -minCount value.");
        }
    }

    /**
     * 根据阈值过滤词典
     * @param t 单词的最小出现次数
     * @param tl 标签的最小出现次数
     */
    private void threshold(long t, long tl) {
        List<Entry> newEntryList = new ArrayList<>(entryList.size());
        for (Entry entry : entryList) {
            if (entry.type == EntryType.Word && entry.count < t) continue;
            if (entry.type == EntryType.Label && entry.count < tl) continue;
            newEntryList.add(entry);
        }
        
        newEntryList.sort((o1, o2) -> {
            int typeCmp = o1.type.compareTo(o2.type);
            if (typeCmp != 0) return typeCmp;
            return Long.compare(o2.count, o1.count); // Descending count
        });

        entryList = newEntryList;
        computeCounts();
    }

    /**
     * 解析文本行用于构建词典
     * @param line 文本行
     * @return 标记列表
     */
    private List<String> parseForDict(String line) {
        String[] parts = whitePattern.split(line);
        List<String> tokens = new ArrayList<>();
        
        for (String part : parts) {
            String t = part.trim();
            if (t.isEmpty()) continue;

            if (useWeight) {
                int i = t.indexOf(':');
                if (i > 0) {
                    t = t.substring(0, i);
                }
            }

            if (normalizeText) {
                t = Utils.NormalizeText.normalize(t);
            }
            
            if (t.startsWith("__weight__")) continue;

            tokens.add(t);
        }
        return tokens;
    }

    /**
     * 打印词典内容
     */
    public void print() {
        for (Entry entry : entryList) {
            if (entry.count > 1) {
                System.out.println();
            }
            System.out.println(entry);
        }
        System.out.println("length " + size);
        System.out.println("nwords = " + nwords);
        System.out.println("nlabels = " + nlabels);
        System.out.println("ntokens = " + ntokens);
    }

    /**
     * 计算词典统计信息
     */
    private void computeCounts() {
        size = 0;
        nwords = 0;
        nlabels = 0;

        Arrays.fill(hashToIndex, -1);

        for (int i = 0; i < entryList.size(); i++) {
            Entry entry = entryList.get(i);
            int h = find(entry.symbol);
            hashToIndex[h] = size++;
            if (entry.type == EntryType.Word) {
                nwords++;
            } else if (entry.type == EntryType.Label) {
                nlabels++;
            }
        }
    }

    /**
     * 从 TSV 模型文件加载词典
     * @param modelfile 模型文件路径
     * @throws IOException IO异常
     */
    public void loadDictFromTsvModel(String modelfile) throws IOException {
        logger.info("Loading dict from model file : {}", modelfile);

        try (Stream<String> lines = Utils.lines(new File(modelfile))) {
            lines.filter(l -> !l.trim().isEmpty()).forEach(line -> {
                int i = line.indexOf('\t');
                if (i > 0) {
                    String symbol = line.substring(0, i);
                    insert(symbol);
                }
            });
        }

        computeCounts();

        logger.info("Number of words in dictionary: {}", nwords);
        logger.info("Number of labels in dictionary: {}", nlabels);
    }

    /**
     * 打印文档内容
     * @param tokens 标记列表
     * @return 文档字符串
     */
    public String printDoc(List<Utils.XPair> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Utils.XPair pair : tokens) {
            if (pair.first < size) {
                sb.append(getSymbol(pair.first)).append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * 解析文档字符串
     * @param doc 文档字符串
     * @return 标记对列表
     */
    public List<Utils.XPair> parseDoc(String doc) {
        return parseDoc(Arrays.asList(whitePattern.split(doc)));
    }
    
    /**
     * 解析文档标记列表
     * @param tokens 标记列表
     * @return 标记对列表
     */
    public List<Utils.XPair> parseDoc(List<String> tokens) {
        List<Utils.XPair> rslts = new ArrayList<>();
        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            
            float weight = 1.0f;
            if (useWeight) {
                int pos = t.indexOf(':');
                if (pos > 0) {
                    try {
                        weight = Float.parseFloat(t.substring(pos + 1));
                        t = t.substring(0, pos);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }

            if (normalizeText) {
                t = Utils.NormalizeText.normalize(t);
            }
            
            int wid = getId(t);
            if (wid < 0) {
                continue;
            }

            EntryType type = getType(wid);
            if (type == EntryType.Word) {
                rslts.add(new Utils.XPair(wid, weight));
            }
        }

        if (ngrams > 1) {
            addNgrams(tokens, rslts);
        }

        return rslts;
    }

    /**
     * 添加 N-gram 特征
     * @param tokens 标记列表
     * @param line 特征行
     */
    public void addNgrams(List<String> tokens, List<Utils.XPair> line) {
        if (ngrams <= 1) {
            return;
        }

        List<Long> hashes = new ArrayList<>();
        for (String token : tokens) {
             String t = token.trim();
             if (useWeight) {
                 int pos = t.indexOf(':');
                 if (pos > 0) t = t.substring(0, pos);
             }
             if (normalizeText) t = Utils.NormalizeText.normalize(t);
             
             if (getType(t) == EntryType.Word) {
                 hashes.add(hash(t));
             }
        }

        int nword_nlabels = nwords + nlabels;
        for (int i = 0; i < hashes.size(); i++) {
            long h = hashes.get(i);
            for (int j = i + 1; j < hashes.size() && j < i + ngrams; j++) {
                h = h * HASH_C + hashes.get(j);
                h = Math.abs(h);
                long id = h % bucket;
                if (id < 0) id += bucket;
                
                line.add(Utils.XPair.createPair((int)(nword_nlabels + id), 1.0f));
            }
        }
    }

    /**
     * 保存词典到输出流
     * @param ofs 输出流
     * @throws IOException IO异常
     */
    public void save(OutputStream ofs) throws IOException {
        DataOutputStream dout = new DataOutputStream(ofs);
        dout.writeInt(size);
        dout.writeInt(nwords);
        dout.writeInt(nlabels);
        dout.writeLong(ntokens);

        for (int i = 0; i < size; i++) {
            Entry e = entryList.get(i);
            dout.writeUTF(e.symbol);
            dout.writeLong(e.count);
            dout.writeInt(e.type.ordinal());
        }
        dout.flush();
    }

    /**
     * 从输入流加载词典
     * @param ifs 输入流
     * @return 词典对象
     * @throws IOException IO异常
     */
    public Dictionary load(InputStream ifs) throws IOException {
        DataInputStream in = new DataInputStream(ifs);
        size = in.readInt();
        nwords = in.readInt();
        nlabels = in.readInt();
        ntokens = in.readLong();

        Arrays.fill(hashToIndex, -1);
        entryList = new ArrayList<>(size);
        EntryType[] values = EntryType.values();

        for (int i = 0; i < size; i++) {
            Entry e = new Entry(
                in.readUTF(),
                in.readLong(),
                values[in.readInt()]
            );
            entryList.add(e);
            hashToIndex[find(e.symbol)] = i;
        }
        return this;
    }

    /**
     * 词典词条类
     * <p>
     * 表示词典中的一个词条，包含符号、计数和类型信息。
     */
    public static class Entry {
        /** 符号字符串 */
        public final String symbol;
        /** 出现次数 */
        public long count;
        /** 词条类型 */
        public final EntryType type;

        /**
         * 构造词条对象
         * @param symbol 符号字符串
         * @param count 出现次数
         * @param type 词条类型
         */
        public Entry(String symbol, long count, EntryType type) {
            this.symbol = symbol;
            this.count = count;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Entry(symbol=" + symbol + ", count=" + count + ", type=" + type + ")";
        }
    }

    /**
     * 词条类型枚举
     */
    public enum EntryType {
        /** 单词类型 */
        Word, 
        /** 标签类型 */
        Label
    }
}
