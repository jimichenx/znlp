package dev.znlp.fasttext;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.test.TestTags;
import dev.znlp.base.blas.DenseVector;
import dev.znlp.base.blas.Vector;
import dev.znlp.fasttext.args.Args;
import dev.znlp.fasttext.args.LossName;
import dev.znlp.fasttext.loss.Loss;
import dev.znlp.fasttext.train.FileSampleLineIterable;
import dev.znlp.fasttext.train.SampleLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * FastText 性能基准。手动跑：
 * {@code ./gradlew :znlp:test --tests fasttext.dev.znlp.FastTextPerfBenchmark}
 */
@Tag(TestTags.BENCHMARK)
class FastTextPerfBenchmark {

    @Test
    void benchmark() throws Exception {
        System.out.println("========== FastText 性能基准 ==========");
        benchFindKBest();
        benchTrainLoad();
        benchPredictCopy();
        benchEndToEndPredict();
        System.out.println("========== 结束 ==========");
    }

    /** 当前实现：每次插入全排序。 */
    private static void findKBestSortEachInsert(int k, float threshold, List<ScoreIdPair> heap, Vector output) {
        heap.clear();
        for (int i = 0; i < output.length(); i++) {
            float val = output.get(i);
            if (val < threshold) {
                continue;
            }
            if (heap.size() == k && Loss.stdLog(val) < heap.get(heap.size() - 1).score()) {
                continue;
            }
            heap.add(new ScoreIdPair(Loss.stdLog(val), i));
            heap.sort((o1, o2) -> Float.compare(o2.score(), o1.score()));
            if (heap.size() > k) {
                heap.remove(heap.size() - 1);
            }
        }
    }

    /** 候选实现：有界最小堆。 */
    private static void findKBestHeap(int k, float threshold, List<ScoreIdPair> heap, Vector output) {
        heap.clear();
        PriorityQueue<ScoreIdPair> pq = new PriorityQueue<>(k + 1, Comparator.comparing(ScoreIdPair::score));
        for (int i = 0; i < output.length(); i++) {
            float val = output.get(i);
            if (val < threshold) {
                continue;
            }
            float score = Loss.stdLog(val);
            if (pq.size() == k && score < pq.peek().score()) {
                continue;
            }
            pq.offer(new ScoreIdPair(score, i));
            if (pq.size() > k) {
                pq.poll();
            }
        }
        heap.addAll(pq);
        heap.sort((o1, o2) -> Float.compare(o2.score(), o1.score()));
    }

    private void benchFindKBest() {
        System.out.println();
        System.out.println("-- findKBest 微基准（softmax 扫描标签）--");
        int[] labelCounts = {10, 200, 2000, 10000};
        int[] ks = {1, 5, 50};
        int repeats = 400;
        Random rng = new Random(1);
        for (int n : labelCounts) {
            float[] data = new float[n];
            for (int i = 0; i < n; i++) {
                data[i] = 0.0001f + rng.nextFloat();
            }
            Vector output = new DenseVector(data);
            for (int k : ks) {
                if (k > n) {
                    continue;
                }
                List<ScoreIdPair> heap = new ArrayList<>();
                for (int i = 0; i < 30; i++) {
                    findKBestSortEachInsert(k, 0, heap, output);
                    findKBestHeap(k, 0, heap, output);
                }
                long tSort = 0;
                long tHeap = 0;
                for (int r = 0; r < repeats; r++) {
                    long t0 = System.nanoTime();
                    findKBestSortEachInsert(k, 0, heap, output);
                    tSort += System.nanoTime() - t0;
                    t0 = System.nanoTime();
                    findKBestHeap(k, 0, heap, output);
                    tHeap += System.nanoTime() - t0;
                }
                System.out.printf("  labels=%-5d k=%-3d  sort-each %7.2f ms  heap %7.2f ms  speedup %.2fx%n",
                        n, k, tSort / 1e6, tHeap / 1e6, (double) tSort / tHeap);
            }
        }
    }

    private void benchTrainLoad() throws Exception {
        System.out.println();
        System.out.println("-- 小文件训练数据装载 (<50MB 路径) --");
        Path file = Files.createTempFile("ft-load", ".txt");
        try {
            StringBuilder line = new StringBuilder("__label__pos");
            for (int i = 0; i < 40; i++) {
                line.append(" w").append(i);
            }
            line.append('\n');
            String row = line.toString();
            StringBuilder body = new StringBuilder(8 * 1024 * 1024);
            while (body.length() < 6 * 1024 * 1024) {
                body.append(row);
            }
            Files.writeString(file, body, StandardCharsets.UTF_8);
            System.out.printf("  文件大小 %.2f MB, 约 %d 行, thread=4%n",
                    file.toFile().length() / (1024.0 * 1024), body.toString().split("\n", -1).length - 1);

            FileSampleLineIterable parent = new FileSampleLineIterable(file.toFile());
            int thread = 4;
            for (int i = 0; i < 2; i++) {
                loadViaTempSplit(parent, thread);
                loadViaMemPartition(parent, thread);
            }

            long tTemp = 0;
            long tMem = 0;
            int rounds = 4;
            for (int r = 0; r < rounds; r++) {
                long t0 = System.nanoTime();
                List<List<SampleLine>> a = loadViaTempSplit(parent, thread);
                tTemp += System.nanoTime() - t0;
                t0 = System.nanoTime();
                List<List<SampleLine>> b = loadViaMemPartition(parent, thread);
                tMem += System.nanoTime() - t0;
                int na = a.stream().mapToInt(List::size).sum();
                int nb = b.stream().mapToInt(List::size).sum();
                if (na != nb) {
                    throw new IllegalStateException("split size mismatch " + na + " vs " + nb);
                }
            }
            System.out.printf("  临时文件拆分+读回  %7.2f ms%n", tTemp / 1e6 / rounds);
            System.out.printf("  一次读入内存再切分  %7.2f ms  speedup %.2fx%n",
                    tMem / 1e6 / rounds, (double) tTemp / tMem);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static List<List<SampleLine>> loadViaTempSplit(FileSampleLineIterable parent, int thread) {
        List<FileSampleLineIterable> splits = parent.splitMutiFiles(thread);
        List<List<SampleLine>> out = new ArrayList<>();
        for (FileSampleLineIterable split : splits) {
            out.add(split.toMemList());
        }
        return out;
    }

    private static List<List<SampleLine>> loadViaMemPartition(FileSampleLineIterable parent, int thread) {
        return partitionRoundRobin(parent.toMemList(), thread);
    }

    static List<List<SampleLine>> partitionRoundRobin(List<SampleLine> all, int n) {
        List<List<SampleLine>> parts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            parts.add(new ArrayList<>());
        }
        for (int i = 0; i < all.size(); i++) {
            parts.get(i % n).add(all.get(i));
        }
        return parts;
    }

    private void benchPredictCopy() {
        System.out.println();
        System.out.println("-- predict(Iterable) 组 token 列表 --");
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            tokens.add("词" + i);
        }
        int repeats = 200_000;
        for (int i = 0; i < 20_000; i++) {
            copyThenEos(tokens);
            eosIterator(tokens);
        }
        long tCopy = 0;
        long tIter = 0;
        List<String> sink = new ArrayList<>();
        for (int r = 0; r < repeats; r++) {
            long t0 = System.nanoTime();
            for (String s : copyThenEos(tokens)) {
                sink.add(s);
            }
            tCopy += System.nanoTime() - t0;
            sink.clear();
            t0 = System.nanoTime();
            for (String s : eosIterator(tokens)) {
                sink.add(s);
            }
            tIter += System.nanoTime() - t0;
            sink.clear();
        }
        System.out.printf("  ArrayList 拷贝+EOS  %7.2f ms%n", tCopy / 1e6);
        System.out.printf("  拼接迭代器          %7.2f ms  speedup %.2fx%n",
                tIter / 1e6, (double) tCopy / tIter);
    }

    private static List<String> copyThenEos(Iterable<String> tokens) {
        List<String> tokensList = new ArrayList<>();
        for (String t : tokens) {
            tokensList.add(t);
        }
        tokensList.add("</s>");
        return tokensList;
    }

    private static Iterable<String> eosIterator(Iterable<String> tokens) {
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
                return "</s>";
            }
        };
    }

    private void benchEndToEndPredict() throws Exception {
        System.out.println();
        System.out.println("-- 端到端 predict（2000 标签 softmax / hs）--");
        Path train = Files.createTempFile("ft-e2e", ".txt");
        try {
            StringBuilder sb = new StringBuilder();
            int labels = 2000;
            for (int i = 0; i < labels; i++) {
                sb.append("__label__c").append(i)
                        .append(" token").append(i % 50)
                        .append(" word").append(i % 80)
                        .append('\n');
            }
            Files.writeString(train, sb, StandardCharsets.UTF_8);

            Args softmaxArgs = e2eArgs(LossName.softmax);
            FastText softmax = FastText.trainSupervised(train.toFile(), softmaxArgs);
            Args hsArgs = e2eArgs(LossName.hs);
            FastText hs = FastText.trainSupervised(train.toFile(), hsArgs);

            List<String> query = List.of("token1", "word2");
            int loops = 3000;
            for (int i = 0; i < 200; i++) {
                softmax.predict(query, 5, 0);
                hs.predict(query, 5, 0);
            }

            long tSoftK5 = timePredict(softmax, query, 5, loops);
            long tSoftAll = timePredict(softmax, query, Model.kUnlimitedPredictions, loops / 2);
            long tHsK5 = timePredict(hs, query, 5, loops);
            long tHsAll = timePredict(hs, query, Model.kUnlimitedPredictions, loops / 2);
            System.out.printf("  softmax k=5     %7.2f ms  (%.0f pred/s)%n", tSoftK5 / 1e6, loops * 1e9 / tSoftK5);
            System.out.printf("  softmax k=all   %7.2f ms  (%.0f pred/s)%n", tSoftAll / 1e6, (loops / 2.0) * 1e9 / tSoftAll);
            System.out.printf("  hs k=5          %7.2f ms  (%.0f pred/s)%n", tHsK5 / 1e6, loops * 1e9 / tHsK5);
            System.out.printf("  hs k=all        %7.2f ms  (%.0f pred/s)%n", tHsAll / 1e6, (loops / 2.0) * 1e9 / tHsAll);
        } finally {
            Files.deleteIfExists(train);
        }
    }

    private static Args e2eArgs(LossName loss) {
        Args args = new Args();
        args.dim = 16;
        args.epoch = 1;
        args.lr = 0.1;
        args.loss = loss;
        args.minCount = 1;
        args.minn = 0;
        args.maxn = 0;
        args.wordNgrams = 1;
        args.thread = 1;
        args.bucket = 100;
        args.seed = 1;
        return args;
    }

    private static long timePredict(FastText model, List<String> query, int k, int loops) {
        long t0 = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            model.predict(query, k, 0);
        }
        return System.nanoTime() - t0;
    }
}
