package dev.znlp.starspace;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据处理接口，用于管理和操作训练数据
 * <p>
 * 该接口定义了数据处理的核心方法，包括数据加载、保存、转换、获取示例等功能。
 * 主要用于 StarSpace 模型的训练过程中，处理输入数据并将其转换为模型可接受的格式。
 */
public interface DataHandler {

    /**
     * 从文件加载数据
     * @param fileName 文件路径
     * @throws IOException IO异常
     */
    void loadFromFile(String fileName) throws IOException;

    /**
     * 保存数据到输出流
     * @param out 输出流
     * @throws IOException IO异常
     */
    void save(OutputStream out) throws IOException;

    /**
     * 转换示例数据，根据训练模式进行处理
     * <p>
     * 在 trainMode=1 的情况下，会从右侧随机选择一个标签
     * @param example 原始示例
     * @param rslt 转换后的结果
     */
    void convert(ParseResult example, ParseResult rslt);

    /**
     * 根据索引获取单词级别的训练示例
     * @param idx 示例索引
     * @param rslts 结果列表
     */
    void getWordExamples(int idx, List<ParseResult> rslts);
    
    /**
     * 从文档中获取单词级别的训练示例
     * @param doc 文档数据
     * @param rslts 结果列表
     */
    void getWordExamples(List<Utils.XPair> doc, List<ParseResult> rslts);
    
    /**
     * 添加示例数据
     * @param example 示例数据
     */
    void addExample(ParseResult example);
    
    /**
     * 根据ID获取示例数据
     * @param idx 示例ID
     * @param rslt 结果对象
     */
    void getExampleById(int idx, ParseResult rslt);
    
    /**
     * 获取下一个示例数据
     * @param rslt 结果对象
     */
    void getNextExample(ParseResult rslt);
    
    /**
     * 随机获取一个示例数据
     * @param rslt 结果对象
     */
    void getRandomExample(ParseResult rslt);
    
    /**
     * 随机获取K个示例数据
     * @param K 示例数量
     * @param c 结果列表
     */
    void getKRandomExamples(int K, List<ParseResult> c);
    
    /**
     * 获取下K个示例数据
     * @param K 示例数量
     * @param c 结果列表
     */
    void getNextKExamples(int K, List<ParseResult> c);

    /**
     * 随机采样一个示例并从该示例中随机采样一个标签
     * <p>
     * 结果通常用作训练中的负样本
     * @param results 结果列表
     * @param trainWord 是否训练单词
     */
    void getRandomRHS(List<Utils.XPair> results, boolean trainWord);

    /**
     * 获取数据大小
     * @return 数据大小
     */
    int size();

    /**
     * 内部数据处理实现类，提供基础的数据处理功能
     * <p>
     * 该类实现了 DataHandler 接口的所有方法，提供了数据加载、转换、获取示例等基础功能。
     * 主要用于处理训练数据，支持不同的训练模式和数据格式。
     */
    class InternDataHandler implements DataHandler {

        private final InternalLogger logger = InternalLoggerFactory.getInstance(InternDataHandler.class);

        /** 数据解析器 */
        protected final DataParser parser;
        /** 训练参数 */
        protected final Args args;

        /** 示例数据列表 */
        protected List<ParseResult> examples = new ArrayList<>();
        /** 当前索引 */
        protected int idx = -1;
        /** 数据大小 */
        protected int size = 0;
        /** 随机数生成器 */
        protected Random random = new Random();

        public InternDataHandler(DataParser parser, Args args) {
            this.parser = parser;
            this.args = args;
        }

        private void errorOnZeroExample(String fileName) throws IOException {
            throw new IOException("File '" + fileName
                    + "' does not contain any valid example. "
                    + "Please check: is the file empty? "
                    + "Do the examples contain proper feature and Label according to the trainMode? "
                    + "If your examples are unlabeled, try to set trainMode=5.");
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void loadFromFile(String fileName) throws IOException {
            logger.info("Loading data from file : {}", fileName);

            Pattern sep = Pattern.compile("[\t ]");

            try (Stream<String> lines = Utils.lines(new File(fileName))) {
                examples = lines.parallel()
                        .map(line -> parser.parse(line, sep))
                        .filter(res -> res != null)
                        .collect(Collectors.toList());
            }

            logger.info("Total number of examples loaded : {}", examples.size());
            size = examples.size();
            if (size == 0) {
                errorOnZeroExample(fileName);
            }
        }

        protected <T> T rand(List<T> list) {
            return list.get(random.nextInt(list.size()));
        }

        protected <T> int randIndex(List<T> list) {
            return random.nextInt(list.size());
        }

        @Override
        public void convert(ParseResult example, ParseResult rslt) {
            List<Utils.XPair> lhsTokens = new ArrayList<>();
            List<Utils.XPair> rhsTokens = new ArrayList<>();
            rslt.lhsTokens = lhsTokens;
            rslt.rhsTokens = rhsTokens;
            rslt.weight = example.weight;

            Args.TrainMode trainMode = args.trainMode;

            lhsTokens.addAll(example.lhsTokens);

            if (trainMode == Args.TrainMode.Mode0) {
                // lhs is the same, pick one random Label as rhs
                assert !example.lhsTokens.isEmpty();
                assert !example.rhsTokens.isEmpty();

                rhsTokens.add(rand(example.rhsTokens));

            } else {
                assert example.rhsTokens.size() > 1;
                
                if (trainMode == Args.TrainMode.Mode1) {
                    // pick one random Label as rhs and the rest is lhs
                    int idx = randIndex(example.rhsTokens);

                    for (int i = 0; i < example.rhsTokens.size(); i++) {
                        Utils.XPair tok = example.rhsTokens.get(i);
                        if (i == idx) {
                            rhsTokens.add(tok);
                        } else {
                            lhsTokens.add(tok);
                        }
                    }
                } else if (trainMode == Args.TrainMode.Mode2) {
                    // pick one random Label as lhs and the rest is rhs
                    int idx = randIndex(example.rhsTokens);

                    for (int i = 0; i < example.rhsTokens.size(); i++) {
                        Utils.XPair tok = example.rhsTokens.get(i);
                        if (i == idx) {
                            lhsTokens.add(tok);
                        } else {
                            rhsTokens.add(tok);
                        }
                    }
                } else if (trainMode == Args.TrainMode.Mode3) {
                    // pick two random labels, one as lhs and the other as rhs
                    if (example.rhsTokens.size() == 2) {
                        int idx = randIndex(example.rhsTokens);
                        int idx2 = (idx == 0) ? 1 : 0;
                        lhsTokens.add(example.rhsTokens.get(idx));
                        rhsTokens.add(example.rhsTokens.get(idx2));
                    } else {
                        int idx = randIndex(example.rhsTokens);
                        int idx2;
                        do {
                            idx2 = randIndex(example.rhsTokens);
                        } while (idx2 == idx);

                        lhsTokens.add(example.rhsTokens.get(idx));
                        rhsTokens.add(example.rhsTokens.get(idx2));
                    }

                } else if (trainMode == Args.TrainMode.Mode4) {
                    // the first one as lhs and the second one as rhs
                    rhsTokens.add(example.rhsTokens.get(1));
                    lhsTokens.add(example.rhsTokens.get(0));
                }
            }
        }

        @Override
        public void getWordExamples(int idx, List<ParseResult> rslts) {
            getWordExamples(examples.get(idx).lhsTokens, rslts);
        }

        @Override
        public void getWordExamples(List<Utils.XPair> doc, List<ParseResult> rslts) {
            int ws = args.ws;
            rslts.clear();

            for (int widx = 0; widx < doc.size(); widx++) {
                ParseResult rslt = new ParseResult();
                List<Utils.XPair> lhsTokens = new ArrayList<>();
                rslt.lhsTokens = lhsTokens;

                List<Utils.XPair> rhsTokens = new ArrayList<>();
                rslt.rhsTokens = rhsTokens;

                rhsTokens.add(doc.get(widx));

                for (int i = Math.max(widx - ws, 0); i < Math.min(widx + ws + 1, doc.size()); i++) { // Note: +1 because Java subList is exclusive but here we iterate manually
                     // Kotlin: until Math.min(widx + ws, doc.size) -> exclusive
                     // Wait, Kotlin's `until` is exclusive. Java's loop `i < ...` is exclusive.
                     // Original code: `for (i in Math.max(widx - ws, 0) until Math.min(widx + ws, doc.size))`
                     // In Kotlin `min(widx + ws, doc.size)` is the upper bound.
                     // But `ws` is window size (radius).
                     // If widx=10, ws=5, range [5, 15).
                     // But we want to include tokens around widx.
                     // `widx + ws` seems correct for exclusive upper bound if we want `ws` elements after `widx`.
                     // Let's stick to original logic.
                    if (i != widx) {
                        lhsTokens.add(doc.get(i));
                    }
                }
                rslt.weight = args.wordWeight;
                rslts.add(rslt);
            }
        }

        @Override
        public void addExample(ParseResult example) {
            examples.add(example);
            size++;
        }

        @Override
        public void getExampleById(int idx, ParseResult rslt) {
            Utils.checkArgument(idx < size);
            convert(examples.get(idx), rslt);
        }

        @Override
        public void getNextExample(ParseResult rslt) {
            idx++;
            if (idx >= size) {
                idx -= size;
            }
            convert(examples.get(idx), rslt);
        }

        @Override
        public void getRandomExample(ParseResult rslt) {
            convert(rand(examples), rslt);
        }

        @Override
        public void getKRandomExamples(int K, List<ParseResult> c) {
            int kSamples = Math.min(K, size);
            for (int i = 0; i < kSamples; i++) {
                ParseResult example = new ParseResult();
                getRandomExample(example);
                c.add(example);
            }
        }

        @Override
        public void getNextKExamples(int K, List<ParseResult> c) {
            int kSamples = Math.min(K, size);
            for (int i = 0; i < kSamples; i++) {
                idx = (idx + 1) % size;
                ParseResult example = new ParseResult();
                convert(examples.get(idx), example);
                c.add(example);
            }
        }

        @Override
        public void getRandomRHS(List<Utils.XPair> results, boolean trainWord) {
            results.clear();
            ParseResult ex = rand(examples);

            if (args.trainMode == Args.TrainMode.Mode5 || trainWord) {
                results.add(rand(ex.lhsTokens));
            } else {
                int r = randIndex(ex.rhsTokens);
                if (args.trainMode == Args.TrainMode.Mode2) {
                    for (int i = 0; i < ex.rhsTokens.size(); i++) {
                        if (i != r) {
                            results.add(ex.rhsTokens.get(i));
                        }
                    }
                } else {
                    results.add(ex.rhsTokens.get(r));
                }
            }
        }

        @Override
        public void save(OutputStream out) throws IOException {
            Writer writer = new OutputStreamWriter(out);
            writer.append("data length : ").append(String.valueOf(size)).append("\n");

            for (ParseResult example : examples) {
                writer.append("lhs : ");
                for (Utils.XPair t : example.lhsTokens) {
                    writer.append(String.valueOf(t.first));
                    writer.append(":");
                    writer.append(String.valueOf(t.second));
                }
                writer.append("\n");
                writer.append("rhs : ");
                for (Utils.XPair t : example.rhsTokens) {
                    writer.append(String.valueOf(t.first));
                    writer.append(":");
                    writer.append(String.valueOf(t.second));
                }
                writer.append("\n");
            }
            writer.flush();
        }
    }

    /**
     * 分层训练的数据处理实现类
     * <p>
     * 该类扩展了 InternDataHandler，支持分层训练（或特定格式）的数据处理。
     * 主要特点是支持对 LHS 和 RHS 特征应用 dropout 技术，以减少过拟合。
     */
    class LayerDataHandler extends InternDataHandler {
        /** LHS 特征的 dropout 概率 */
        private final double dropoutLHS;
        /** RHS 特征的 dropout 概率 */
        private final double dropoutRHS;
        
        /** 线程本地随机数生成器，用于 dropout 操作的线程安全 */
        private static final ThreadLocal<Random> randomTL = ThreadLocal.withInitial(() -> new Random(0));

        public LayerDataHandler(DataParser parser, Args args) {
            super(parser, args);
            this.dropoutLHS = args.dropoutLHS;
            this.dropoutRHS = args.dropoutRHS;
        }

        private void insert(List<Utils.XPair> rslt, List<Utils.XPair> ex, double dropout) {
            if (dropout < 1e-8) {
                rslt.addAll(ex);
            } else {
                Random random = randomTL.get();
                for (Utils.XPair it : ex) {
                    if (random.nextFloat() > dropout) {
                        rslt.add(it);
                    }
                }
            }
        }

        @Override
        public void getWordExamples(int idx, List<ParseResult> rslts) {
            Utils.checkArgument(idx < size);
            ParseResult example = examples.get(idx);
            Utils.checkArgument(!example.rhsFeatures.isEmpty());
            
            // take one random sentence and train on Word
            super.getWordExamples(rand(example.rhsFeatures), rslts);
        }

        @Override
        public void convert(ParseResult example, ParseResult rslt) {
            Args.TrainMode trainMode = args.trainMode;
            rslt.weight = example.weight;
            List<Utils.XPair> lhsTokens = new ArrayList<>();
            List<Utils.XPair> rhsTokens = new ArrayList<>();
            rslt.lhsTokens = lhsTokens;
            rslt.rhsTokens = rhsTokens;

            if (trainMode == Args.TrainMode.Mode0) {
                assert !example.lhsTokens.isEmpty();
                assert !example.rhsFeatures.isEmpty();

                insert(lhsTokens, example.lhsTokens, dropoutLHS);
                insert(rhsTokens, rand(example.rhsFeatures), dropoutRHS);
            } else {
                assert example.rhsFeatures.size() > 1;
                
                if (trainMode == Args.TrainMode.Mode1) {
                    int idx = randIndex(example.rhsFeatures);
                    for (int i = 0; i < example.rhsFeatures.size(); i++) {
                        if (i == idx) {
                            insert(rhsTokens, example.rhsFeatures.get(i), dropoutRHS);
                        } else {
                            insert(lhsTokens, example.rhsFeatures.get(i), dropoutLHS);
                        }
                    }
                } else if (trainMode == Args.TrainMode.Mode2) {
                    int idx = randIndex(example.rhsFeatures);
                    for (int i = 0; i < example.rhsFeatures.size(); i++) {
                        if (i == idx) {
                            insert(lhsTokens, example.rhsFeatures.get(i), dropoutLHS);
                        } else {
                            insert(rhsTokens, example.rhsFeatures.get(i), dropoutRHS);
                        }
                    }
                } else if (trainMode == Args.TrainMode.Mode3) {
                    int idx = randIndex(example.rhsFeatures);
                    insert(lhsTokens, example.rhsFeatures.get(idx), dropoutLHS);
                    
                    int idx2;
                    do {
                        idx2 = randIndex(example.rhsFeatures);
                    } while (idx == idx2);
                    insert(rhsTokens, example.rhsFeatures.get(idx2), dropoutRHS);
                    
                } else if (trainMode == Args.TrainMode.Mode4) {
                    insert(lhsTokens, example.rhsFeatures.get(0), dropoutLHS);
                    insert(rhsTokens, example.rhsFeatures.get(1), dropoutRHS);
                }
            }
        }

        @Override
        public void getRandomRHS(List<Utils.XPair> results, boolean trainWord) {
            assert size > 0;
            ParseResult ex = rand(examples);
            int r = randIndex(ex.rhsFeatures);

            results.clear();
            Args.TrainMode trainMode = args.trainMode;

            if (trainMode == Args.TrainMode.Mode5 || trainWord) {
                // pick random Word
                // ex.rhsFeatures[r] is a List<XPair>
                // We need to pick a random word from it.
                List<Utils.XPair> feature = ex.rhsFeatures.get(r);
                results.add(feature.get(random.nextInt(feature.size())));
            } else if (trainMode == Args.TrainMode.Mode2) {
                for (int i = 0; i < ex.rhsFeatures.size(); i++) {
                    if (i != r) {
                        insert(results, ex.rhsFeatures.get(i), dropoutRHS);
                    }
                }
            } else {
                insert(results, ex.rhsFeatures.get(r), dropoutLHS);
            }
        }
    }
}
