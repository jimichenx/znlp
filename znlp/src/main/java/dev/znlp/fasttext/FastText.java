package dev.znlp.fasttext;

import dev.znlp.base.blas.DenseArrayMatrix;
import dev.znlp.base.blas.Matrix;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.ModelName;
import dev.znlp.fasttext.dictionary.Dictionary;
import dev.znlp.fasttext.dictionary.DictionaryBuilder;
import dev.znlp.fasttext.loss.Loss;
import dev.znlp.fasttext.loss.LossFactory;
import dev.znlp.fasttext.train.FastTextTrain;
import dev.znlp.fasttext.train.FileSampleLineIterable;
import dev.znlp.fasttext.train.SampleLine;
import dev.znlp.fasttext.utils.AutoDataInput;
import dev.znlp.fasttext.utils.LittleEndianData;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * FastText 核心类，提供训练、预测、测试以及官方兼容的 {@code .bin} 保存/加载。
 *
 * @param args  模型参数配置
 * @param dict  词典
 * @param model 模型
 */
public record FastText(Args args, Dictionary dict, Model model) {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FastText.class);

    /** Facebook FastText 文件魔数 */
    public static final int FILEFORMAT_MAGIC = 793712314;
    /** 与官方 v0.9.2 对齐的格式版本 */
    public static final int FILEFORMAT_VERSION = 12;

    public FastText {
    }

    public Matrix getOutput() {
        return model.wo();
    }

    /**
     * 预测给定文本的标签。
     *
     * @param tokens    分词列表
     * @param k         返回前 k 个结果；{@link Model#kUnlimitedPredictions} 表示全部
     * @param threshold 分数阈值
     * @return 分数与标签对
     */
    public List<ScoreLabelPair> predict(Iterable<String> tokens, int k, float threshold) {
        IntArrayList words = new IntArrayList();
        IntArrayList labels = new IntArrayList();
        dict.getLine(withEos(tokens), words, labels);

        if (words.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoreIdPair> result = predict(k, words, threshold);
        List<ScoreLabelPair> labelPairs = new ArrayList<>(result.size());
        for (ScoreIdPair pair : result) {
            labelPairs.add(new ScoreLabelPair((float) Math.exp(pair.score()), dict.getLabel(pair.id())));
        }
        return labelPairs;
    }

    private static Iterable<String> withEos(Iterable<String> tokens) {
        return () -> new Iterator<>() {
            private final Iterator<String> it = tokens.iterator();
            private boolean eos;

            @Override
            public boolean hasNext() {
                return it.hasNext() || !eos;
            }

            @Override
            public String next() {
                if (it.hasNext()) {
                    return it.next();
                }
                eos = true;
                return Dictionary.EOS;
            }
        };
    }

    /**
     * 预测给定词 ID 列表的标签。
     */
    public List<ScoreIdPair> predict(int k, IntArrayList words, float threshold) {
        if (words.isEmpty()) {
            return Collections.emptyList();
        }
        if (args.model != ModelName.sup) {
            throw new RuntimeException("Model needs to be supervised for prediction!");
        }

        Model.State state = new Model.State(args.dim, dict.nlabels, 0);
        List<ScoreIdPair> predictions = new ArrayList<>();
        model.predict(words, k, threshold, predictions, state);
        return predictions;
    }

    public FastTextTestResult test(File file, int k, float threshold, boolean print) {
        return test(new FileSampleLineIterable(file), k, threshold, print);
    }

    /**
     * 按官方 FastText 计算 P@k / R@k。
     */
    public FastTextTestResult test(Iterable<SampleLine> file, int k, float threshold, boolean print) {
        int kk = (k == Model.kUnlimitedPredictions) ? dict.nlabels : k;
        if (kk < 1) {
            throw new IllegalArgumentException("k needs to be 1 or higher");
        }

        IntArrayList line = new IntArrayList();
        IntArrayList labels = new IntArrayList();
        long nexamples = 0;
        long nlabels = 0;
        long nfound = 0;

        for (SampleLine sample : file) {
            line.clear();
            labels.clear();
            dict.getLine(sample.words(), line, labels);

            if (!labels.isEmpty() && !line.isEmpty()) {
                List<ScoreIdPair> predictions = predict(kk, line, threshold);
                nexamples++;
                nlabels += labels.size();
                int limit = Math.min(kk, predictions.size());
                for (int i = 0; i < limit; i++) {
                    if (labels.indexOf(predictions.get(i).id()) != -1) {
                        nfound++;
                    }
                }
            }
        }

        FastTextTestResult result = new FastTextTestResult(kk, nexamples, nlabels, nfound);
        if (print) {
            logger.info("N\t{}", result.nexamples());
            logger.info("P@{}\t{}", kk, String.format("%.3f", result.precision()));
            logger.info("R@{}\t{}", kk, String.format("%.3f", result.recall()));
        }
        return result;
    }

    /**
     * 训练有监督分类模型。复制 {@code args}，只把 {@code model} 设为 supervised，
     * 不改学习率、损失、minCount，也不关闭字符 n-gram。
     */
    public static FastText trainSupervised(File file, Args args) {
        Args copy = args.copy();
        copy.model = ModelName.sup;
        return train(file, copy);
    }

    public static FastText train(File file, Args args) {
        FileSampleLineIterable parent = new FileSampleLineIterable(file);
        long size50M = 50L * 1024 * 1024;

        List<Iterable<SampleLine>> sources = new ArrayList<>();
        List<FileSampleLineIterable> splits = null;
        try {
            if (file.length() < size50M) {
                if (args.thread > 1) {
                    splits = parent.splitMutiFiles(args.thread);
                    for (FileSampleLineIterable split : splits) {
                        sources.add(split.toMemList());
                    }
                    splits = null;
                } else {
                    sources.add(parent.toMemList());
                }
            } else {
                splits = parent.splitMutiFiles(Math.max(args.thread, 1));
                sources.addAll(splits);
            }
            return train(sources, args);
        } finally {
            if (splits != null) {
                for (FileSampleLineIterable split : splits) {
                    split.deleteIfOwned();
                }
            }
        }
    }

    public static FastText train(List<Iterable<SampleLine>> sources, Args args) {
        Dictionary dict = DictionaryBuilder.buildFromFile(args, sources, args.maxVocabSize);
        if (args.model == ModelName.sup && dict.nlabels == 0) {
            throw new RuntimeException("Empty label set. Check that labels start with " + args.label);
        }

        DenseArrayMatrix input = new DenseArrayMatrix(dict.nwords + args.bucket, args.dim);
        input.uniform(1.0f / args.dim);
        dict.init();

        int outputSize = (args.model == ModelName.sup) ? dict.nlabels : dict.nwords;
        if (outputSize <= 0) {
            throw new RuntimeException("Empty output vocabulary.");
        }
        DenseArrayMatrix output = new DenseArrayMatrix(outputSize, args.dim);
        output.zero();

        FastText fastText = assemble(args, dict, input, output);
        new FastTextTrain(args, fastText).startThreads(sources);
        return fastText;
    }

    /**
     * 保存为 Facebook FastText 兼容的 {@code .bin}（非量化）。
     */
    public void saveModel(File file) throws IOException {
        if (!(model.wi() instanceof DenseArrayMatrix input) || !(model.wo() instanceof DenseArrayMatrix output)) {
            throw new IOException("Only dense (non-quantized) models can be saved");
        }
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            LittleEndianData.writeInt(out, FILEFORMAT_MAGIC);
            LittleEndianData.writeInt(out, FILEFORMAT_VERSION);
            args.save(out);
            dict.save(out);
            LittleEndianData.writeBoolean(out, false);
            LittleEndianData.writeLong(out, input.getRow());
            LittleEndianData.writeLong(out, input.getCol());
            LittleEndianData.writeFloats(out, input.getData());
            LittleEndianData.writeBoolean(out, false);
            LittleEndianData.writeLong(out, output.getRow());
            LittleEndianData.writeLong(out, output.getCol());
            LittleEndianData.writeFloats(out, output.getData());
        }
    }

    /**
     * 加载 Facebook FastText {@code .bin} 或本实现保存的非量化模型。量化 {@code .ftz} 不支持。
     */
    public static FastText loadModel(File file) throws IOException {
        try (AutoDataInput in = AutoDataInput.openLittleEndian(file)) {
            int magic = in.readInt();
            if (magic != FILEFORMAT_MAGIC) {
                throw new IOException(file + " has wrong file format (bad magic)");
            }
            int version = in.readInt();
            if (version < 11 || version > FILEFORMAT_VERSION) {
                throw new IOException(file + " has unsupported FastText version: " + version);
            }

            Args loadedArgs = Args.load(in);
            if (version == 11 && loadedArgs.model == ModelName.sup) {
                loadedArgs.maxn = 0;
            }
            Dictionary dict = Dictionary.loadModel(loadedArgs, in);

            boolean quantInput = in.readBoolean();
            if (quantInput) {
                throw new IOException("Quantized FastText models (.ftz) are not supported");
            }
            DenseArrayMatrix input = in.loadFloatMatrix();
            if (dict.isPruned()) {
                throw new IOException("Pruned non-quantized models are not supported");
            }

            boolean qout = in.readBoolean();
            if (qout && quantInput) {
                throw new IOException("Quantized FastText models (.ftz) are not supported");
            }
            DenseArrayMatrix output = in.loadFloatMatrix();
            return assemble(loadedArgs, dict, input, output);
        }
    }

    private static FastText assemble(Args args, Dictionary dict, Matrix input, Matrix output) {
        Loss loss = LossFactory.createLoss(args, output, args.model, dict);
        boolean normalizeGradient = (args.model == ModelName.sup);
        Model model = new Model(input, output, loss, normalizeGradient);
        return new FastText(args, dict, model);
    }
}
