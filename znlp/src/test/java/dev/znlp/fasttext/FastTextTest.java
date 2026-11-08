package dev.znlp.fasttext;

import org.junit.jupiter.api.Test;
import dev.znlp.base.blas.DenseArrayMatrix;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.LossName;
import dev.znlp.fasttext.args.ModelName;
import dev.znlp.fasttext.dictionary.Dictionary;
import dev.znlp.fasttext.dictionary.FastWordMap;
import dev.znlp.fasttext.loss.NegativeSamplingLoss;
import dev.znlp.fasttext.utils.HashUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FastTextTest {

    @Test
    void utf8HashMatchesSignedInt8Fnv() {
        assertEquals(1335831723, HashUtils.fnv1aHash("hello"));
        assertEquals(718964643, HashUtils.fnv1aHash("你好"));
        byte[] utf8 = "你好".getBytes(StandardCharsets.UTF_8);
        assertEquals(HashUtils.fnv1aHash("你好"), HashUtils.fnv1aHash(utf8));
    }

    @Test
    void subwordsFollowUtf8CharacterBoundaries() {
        Args args = new Args();
        args.minn = 3;
        args.maxn = 6;
        args.bucket = 2000000;
        FastWordMap map = new FastWordMap(args.label, 8, 8);
        Dictionary dict = new Dictionary(args, map, 0, 0, 0);
        IntArrayList ngrams = dict.getSubwords("hello");
        assertFalse(ngrams.isEmpty());
        int expected = Integer.remainderUnsigned(HashUtils.fnv1aHash("<he"), args.bucket);
        assertEquals(expected, ngrams.get(0));
    }

    @Test
    void hashTableKeepsEmptySlotForTinyVocab() {
        FastWordMap map = new FastWordMap("__label__", 8, 8);
        map.add("only", 1);
        map.wordHash2WordId = new int[FastWordMap.hashTableSize(map.size())];
        map.initWordHash2WordId();
        assertTrue(FastWordMap.hashTableSize(1) > 1);
        assertTrue(FastWordMap.hashTableSize(2) > 2);
        assertEquals(-1, map.getId("missing"));
        assertEquals(0, map.getId("only"));
    }

    @Test
    void negativeSamplingRejectsSingleTarget() {
        DenseArrayMatrix wo = new DenseArrayMatrix(1, 4);
        assertThrows(IllegalArgumentException.class,
                () -> new NegativeSamplingLoss(wo, 5, new long[]{10}));
        assertThrows(IllegalArgumentException.class,
                () -> new NegativeSamplingLoss(wo, 5, new long[]{10, 0}));
    }

    @Test
    void trainSupervisedDoesNotMutateArgsOrDisableSubwords() throws Exception {
        Path train = Files.createTempFile("ft-train", ".txt");
        try {
            Files.writeString(train, """
                    __label__pos 服务 很好
                    __label__neg 服务 很差
                    __label__pos 房间 干净
                    __label__neg 房间 脏
                    """.repeat(20), StandardCharsets.UTF_8);

            Args args = new Args();
            args.dim = 8;
            args.epoch = 1;
            args.thread = 1;
            args.bucket = 100;
            args.lr = 0.2;
            args.loss = LossName.hs;
            args.minCount = 1;
            args.minn = 3;
            args.maxn = 6;
            args.wordNgrams = 2;

            FastText model = FastText.trainSupervised(train.toFile(), args);

            assertEquals(0.2, args.lr);
            assertEquals(LossName.hs, args.loss);
            assertEquals(ModelName.sg, args.model);
            assertEquals(3, args.minn);
            assertEquals(6, args.maxn);

            assertEquals(ModelName.sup, model.args().model);
            assertEquals(LossName.hs, model.args().loss);
            assertEquals(3, model.args().minn);
            assertEquals(6, model.args().maxn);
            assertEquals(0.2, model.args().lr);
        } finally {
            Files.deleteIfExists(train);
        }
    }

    @Test
    void unlimitedPredictSaveLoadAndPrecisionAtK() throws Exception {
        Path train = Files.createTempFile("ft-train", ".txt");
        Path modelFile = Files.createTempFile("ft-model", ".bin");
        try {
            Files.writeString(train, """
                    __label__pos 服务 很好 房间 干净
                    __label__neg 服务 很差 房间 脏
                    __label__pos 态度 热情 体验 满意
                    __label__neg 态度 冷淡 体验 糟糕
                    """.repeat(30), StandardCharsets.UTF_8);

            Args args = new Args();
            args.dim = 16;
            args.epoch = 8;
            args.thread = 1;
            args.bucket = 200;
            args.lr = 0.1;
            args.loss = LossName.softmax;
            args.minCount = 1;
            args.minn = 3;
            args.maxn = 6;
            args.wordNgrams = 2;
            args.seed = 1;

            FastText model = FastText.trainSupervised(train.toFile(), args);

            List<ScoreLabelPair> unlimited = model.predict(List.of("服务", "很好"), Model.kUnlimitedPredictions, 0.0f);
            assertEquals(2, unlimited.size());
            assertTrue(unlimited.get(0).score >= unlimited.get(1).score);

            List<ScoreLabelPair> top1 = model.predict(List.of("服务", "很好"), 1, 0.0f);
            assertEquals(1, top1.size());

            FastTextTestResult metrics = model.test(train.toFile(), 1, 0.0f, false);
            assertEquals(1, metrics.k());
            assertTrue(metrics.nexamples() > 0);
            assertEquals(metrics.nexamples(), metrics.nlabels());
            assertEquals(metrics.nfound() / (double) metrics.nexamples(), metrics.precision(), 1e-9);
            assertTrue(metrics.precision() >= 0.0 && metrics.precision() <= 1.0);
            assertEquals(metrics.precision(), metrics.recall(), 1e-9);

            model.saveModel(modelFile.toFile());
            FastText loaded = FastText.loadModel(modelFile.toFile());
            List<ScoreLabelPair> afterLoad = loaded.predict(List.of("服务", "很好"), 1, 0.0f);
            assertEquals(top1.get(0).label, afterLoad.get(0).label);
            assertEquals(top1.get(0).score, afterLoad.get(0).score, 1e-5);

            assertThrows(IllegalArgumentException.class, () -> model.test(train.toFile(), 0, 0.0f, false));
        } finally {
            Files.deleteIfExists(train);
            Files.deleteIfExists(modelFile);
        }
    }

    @Test
    void emptyLabelsThrow() throws Exception {
        Path train = Files.createTempFile("ft-nolabel", ".txt");
        try {
            Files.writeString(train, "只有 词 没有 标签\n", StandardCharsets.UTF_8);
            Args args = new Args();
            args.dim = 8;
            args.epoch = 1;
            args.thread = 1;
            args.minCount = 1;
            args.bucket = 50;
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> FastText.trainSupervised(train.toFile(), args));
            assertTrue(ex.getMessage().contains("Empty label"));
        } finally {
            Files.deleteIfExists(train);
        }
    }

    @Test
    void skipgramTrainsOnTinyCorpus() throws Exception {
        Path corpus = Files.createTempFile("ft-sg", ".txt");
        try {
            Files.writeString(corpus, """
                    自然 语言 处理 技术
                    深度 学习 模型 训练
                    中文 分词 算法 优化
                    """.repeat(10), StandardCharsets.UTF_8);

            Args args = new Args();
            args.model = ModelName.sg;
            args.dim = 8;
            args.epoch = 1;
            args.thread = 1;
            args.minCount = 1;
            args.bucket = 50;
            args.lr = 0.1;

            FastText model = FastText.train(corpus.toFile(), args);
            assertNotNull(model);
            assertTrue(model.args().dim > 0);
        } finally {
            Files.deleteIfExists(corpus);
        }
    }

    @Test
    void badMagicRejectedOnLoad() throws Exception {
        Path bad = Files.createTempFile("ft-bad", ".bin");
        try {
            Files.writeString(bad, "not-fasttext", StandardCharsets.UTF_8);
            IOException ex = assertThrows(IOException.class, () -> FastText.loadModel(bad.toFile()));
            assertTrue(ex.getMessage().contains("bad magic") || ex.getMessage().contains("wrong file format"));
        } finally {
            Files.deleteIfExists(bad);
        }
    }
}
