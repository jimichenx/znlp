package dev.znlp.starspace;

import dev.znlp.base.blas.DenseArrayMatrix;
import dev.znlp.base.blas.DenseMatrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.MinHeapTopKInt;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * StarSpace 模型主类，包含嵌入矩阵和核心的前向/投影方法
 * <p>
 * 该类是 StarSpace 模型的核心实现，提供了向量表示计算、相似度计算、模型保存/加载等功能。
 * 它是模型训练完成后的主要接口，用于执行预测、评估等任务。
 * <p>
 * 主要功能：
 * 1. 向量投影（projectLHS/projectRHS）：将输入特征转换为向量表示
 * 2. 相似度搜索（findLHSLike/findRHSLike）：查找最相似的向量
 * 3. 文档向量计算（getDocVector）：将文本转换为向量表示
 * 4. 模型保存/加载（saveModel/loadModel）：持久化和加载模型
 * 5. 评估（evaluate）：在测试集上评估模型性能
 * 6. 预测（prediction）：创建预测器对象
 */
public final class StarSpace {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StarSpace.class);

    private final Dictionary dict;
    private final Args args;
    private final SparseLinear lhsEmbeddings;
    private final SparseLinear rhsEmbeddings;

    /**
     * 构造 StarSpace 模型（规范构造：使用 SparseLinear 嵌入）
     *
     * @param dict          词典
     * @param args          训练参数
     * @param lhsEmbeddings 左侧嵌入
     * @param rhsEmbeddings 右侧嵌入
     */
    public StarSpace(Dictionary dict, Args args, SparseLinear lhsEmbeddings, SparseLinear rhsEmbeddings) {
        this.dict = dict;
        this.args = args;
        this.lhsEmbeddings = lhsEmbeddings;
        this.rhsEmbeddings = rhsEmbeddings;
    }

    /**
     * 构造 StarSpace 模型（从 DenseMatrix 包装为 SparseLinear）
     *
     * @param dict          词典
     * @param args          训练参数
     * @param lhsEmbeddings 左侧嵌入矩阵
     * @param rhsEmbeddings 右侧嵌入矩阵
     */
    public StarSpace(Dictionary dict, Args args, DenseMatrix lhsEmbeddings, DenseMatrix rhsEmbeddings) {
        this(dict, args, new SparseLinear(lhsEmbeddings), new SparseLinear(rhsEmbeddings));
    }

    public Dictionary dict() {
        return dict;
    }

    public Args args() {
        return args;
    }

    public SparseLinear lhsEmbeddings() {
        return lhsEmbeddings;
    }

    public SparseLinear rhsEmbeddings() {
        return rhsEmbeddings;
    }

    /**
     * 投影左侧特征到向量空间
     *
     * @param ws 特征-权重对列表
     * @return 投影后的向量
     */
    public Vector projectLHS(List<Utils.XPair> ws) {
        Vector vec = lhsEmbeddings.forward(ws);
        if (!ws.isEmpty()) {
            float norm = args.norm2Computer(ws, vec);
            vec.divAssign(norm);
        }
        return vec;
    }

    /**
     * 投影右侧特征到向量空间
     *
     * @param ws 特征-权重对列表
     * @return 投影后的向量
     */
    public Vector projectRHS(List<Utils.XPair> ws) {
        Vector vec = rhsEmbeddings.forward(ws);
        if (!ws.isEmpty()) {
            float norm = args.norm2Computer(ws, vec);
            vec.divAssign(norm);
        }
        return vec;
    }

    /**
     * 查找与给定向量最相似的左侧向量
     *
     * @param point  目标向量
     * @param numSim 返回的相似向量数量
     * @return 相似向量列表，每个元素是 (索引, 相似度) 对
     */
    public List<Utils.XPair> findLHSLike(Vector point, int numSim) {
        if (numSim <= 0) numSim = 5;
        return kNN(lhsEmbeddings, point, numSim);
    }

    /**
     * 查找与给定向量最相似的右侧向量
     *
     * @param point  目标向量
     * @param numSim 返回的相似向量数量
     * @return 相似向量列表，每个元素是 (索引, 相似度) 对
     */
    public List<Utils.XPair> findRHSLike(Vector point, int numSim) {
        if (numSim <= 0) numSim = 5;
        return kNN(rhsEmbeddings, point, numSim);
    }

    /**
     * k近邻搜索
     *
     * @param lookup 嵌入矩阵
     * @param point  目标向量
     * @param numSim 返回的相似向量数量
     * @return 相似向量列表，每个元素是 (索引, 相似度) 对
     */
    public List<Utils.XPair> kNN(SparseLinear lookup, Vector point, int numSim) {
        int maxn = dict.nwords() + dict.nlabels();

        // 使用 MinHeapTopKInt 来高效维护 top K 相似的向量
        MinHeapTopKInt topK = new MinHeapTopKInt(Math.min(numSim, maxn));

        for (int i = 0; i < maxn; i++) {
            Vector contV = lookup.forward(i);
            float sim = args.similarity(point, contV);
            topK.push(i, sim);
        }

        List<MinHeapTopKInt.Pair<Integer, Float>> results = topK.resort();
        List<Utils.XPair> xPairs = new ArrayList<>();
        for (MinHeapTopKInt.Pair<Integer, Float> p : results) {
            if (p.first() != -1) {
                xPairs.add(new Utils.XPair(p.first(), p.second()));
            }
        }
        return xPairs;
    }

    /**
     * 获取文档的向量表示
     *
     * @param line 文档文本
     * @return 文档向量
     */
    public Vector getDocVector(String line) {
        List<Utils.XPair> pairs = dict.parseDoc(line);
        return projectLHS(pairs);
    }

    /**
     * 创建预测器
     *
     * @param docBaseFile 基础文档文件
     * @return StarSpacePrediction 对象
     */
    public StarSpacePrediction prediction(String docBaseFile) {
        return new StarSpacePrediction(this, docBaseFile);
    }

    /**
     * 创建预测器（使用默认参数）
     *
     * @return StarSpacePrediction 对象
     */
    public StarSpacePrediction prediction() {
        return prediction(null);
    }

    /**
     * 加载基础文档
     *
     * @param basedoc 基础文档文件路径
     * @return BaseDocs 对象
     */
    public BaseDocs loadBaseDocs(String basedoc) {
        BaseDocs result = new BaseDocs();
        if (basedoc == null || basedoc.trim().isEmpty()) {
            if (args.fileFormat == Args.FileFormat.LabelDoc) {
                throw new IllegalArgumentException("Must provide base labels when label is featured.");
            }

            for (int i = 0; i < dict.nlabels(); i++) {
                List<Utils.XPair> doc = new ArrayList<>();
                doc.add(new Utils.XPair(i + dict.nwords(), 1.0f));
                result.baseDocs.add(doc);
                result.baseDocVectors.add(this.projectRHS(doc));
            }

            logger.info("Prediction use {} known labels.", dict.nlabels());
        } else {
            logger.info("Loading base docs from file : {}", basedoc);
            try (Stream<String> lines = Utils.lines(new File(basedoc))) {
                lines.forEach(line -> {
                    List<Utils.XPair> ids = dict.parseDoc(line);
                    result.baseDocs.add(ids);
                    Vector docVec = this.projectRHS(ids);
                    result.baseDocVectors.add(docVec);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (result.baseDocVectors.size() == 0) {
                throw new RuntimeException("ERROR: basedocFile file '" + basedoc + "' is empty.");
            }
            logger.info("Finished loading {} base docs.", result.baseDocVectors.size());
        }
        return result;
    }

    /**
     * 计算两个向量的相似度
     *
     * @param a 向量a
     * @param b 向量b
     * @return 相似度值
     */
    public float similarity(Vector a, Vector b) {
        return args.similarity(a, b);
    }

    /**
     * 在测试集上评估模型性能
     *
     * @param testFile       测试文件路径
     * @param k              评估指标中的K值（如Hit@K）
     * @param basedocFile    基础文档文件路径
     * @param predictionFile 预测结果输出文件路径
     */
    public void evaluate(String testFile, int k, String basedocFile, String predictionFile) {
        new Evaluate(this, testFile, k, basedocFile, predictionFile).evaluate();
    }

    /**
     * 在测试集上评估模型性能（使用默认参数）
     *
     * @param testFile 测试文件路径
     */
    public void evaluate(String testFile) {
        evaluate(testFile, 5, null, null);
    }

    /**
     * 保存模型
     *
     * @param file 模型保存路径
     * @throws IOException IO异常
     */
    public void saveModel(String file) throws IOException {
        if (!file.endsWith("/")) {
            file += "/";
        }

        File f = new File(file);
        if (!f.exists()) {
            f.mkdirs();
        }

        // 保存参数
        try (OutputStream out = Files.newOutputStream(Paths.get(file, "arg.bin"))) {
            this.args.write(out);
        }

        // 保存词典
        try (OutputStream out = Files.newOutputStream(Paths.get(file, "dict.bin"))) {
            try (BufferedOutputStream bout = new BufferedOutputStream(out)) {
                dict.save(bout);
            }
        }

        // 保存左侧嵌入矩阵
        try (FileOutputStream fos = new FileOutputStream(new File(file, "lsh.matrix"))) {
            lhsEmbeddings.matrix().save(fos.getChannel());
        }

        // 保存右侧嵌入矩阵（如果不共享嵌入）
        if (!args.shareEmb) {
            try (FileOutputStream fos = new FileOutputStream(new File(file, "rsh.matrix"))) {
                rhsEmbeddings.matrix().save(fos.getChannel());
            }
        }
    }

    /**
     * 加载模型
     *
     * @param file 模型路径
     * @param mmap 是否使用内存映射
     * @return StarSpace 模型对象
     * @throws IOException IO异常
     */
    public static StarSpace loadModel(String file, boolean mmap) throws IOException {
        long t1 = System.currentTimeMillis();

        if (!file.endsWith("/")) {
            file += "/";
        }

        // 读取参数
        Args args = new Args();
        try (InputStream in = Files.newInputStream(Paths.get(file, "arg.bin"))) {
            args.read(in);
        }

        // 读取词典
        Dictionary dict = new Dictionary(args);
        try (InputStream in = Files.newInputStream(Paths.get(file, "dict.bin"))) {
            try (BufferedInputStream bin = new BufferedInputStream(in)) {
                dict.load(bin);
            }
        }

        int numLhs = dict.nwords() + dict.nlabels();
        if (args.ngrams > 1) {
            numLhs += args.bucket;
        }

        // 加载左侧嵌入矩阵
        DenseMatrix lhsMatrix = loadMatrix(new File(file, "lsh.matrix"), numLhs, args.dim);

        // 加载右侧嵌入矩阵（如果不共享嵌入）
        DenseMatrix rhsMatrix;
        if (args.shareEmb) {
            rhsMatrix = lhsMatrix;
        } else {
            rhsMatrix = loadMatrix(new File(file, "rsh.matrix"), numLhs, args.dim);
        }

        long t2 = System.currentTimeMillis();
        logger.info("Load model use time {} ms", t2 - t1);

        return new StarSpace(dict, args, lhsMatrix, rhsMatrix);
    }

    /**
     * 加载矩阵
     *
     * @param file 矩阵文件
     * @param rows 行数
     * @param cols 列数
     * @return DenseMatrix 对象
     * @throws IOException IO异常
     */
    private static DenseMatrix loadMatrix(File file, int rows, int cols) throws IOException {
        // 读取文件内容到 float 数组
        // 文件格式：int rows, int cols, 然后是 float 数据

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {

            ByteBuffer header = ByteBuffer.allocate(8);
            channel.read(header);
            header.flip();
            int fRows = header.getInt();
            int fCols = header.getInt();

            if (fRows != rows || fCols != cols) {
                // 维度不匹配时，使用文件中的维度
            }

            long size = (long) fRows * fCols * 4;
            float[] floats = new float[fRows * fCols];

            // 使用内存映射读取数据，提高速度
            ByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 8, size);
            mapped.asFloatBuffer().get(floats);

            return new DenseArrayMatrix(fRows, fCols, floats);
        }
    }

    /**
     * 基础文档类，用于存储基础文档和对应的向量
     */
    public static class BaseDocs {
        /**
         * 基础文档向量列表
         */
        public List<Vector> baseDocVectors = new ArrayList<>();
        /**
         * 基础文档列表
         */
        public List<List<Utils.XPair>> baseDocs = new ArrayList<>();
    }

    /**
     * 查询实体类，用于表示查询结果
     */
    public static class QueryEntity {
        /**
         * 索引
         */
        public int idx;
        /**
         * 词条
         */
        public String entry;
        /**
         * 分数
         */
        public float score;

        /**
         * 构造查询实体
         *
         * @param idx   索引
         * @param entry 词条
         * @param score 分数
         */
        public QueryEntity(int idx, String entry, float score) {
            this.idx = idx;
            this.entry = entry;
            this.score = score;
        }
    }
}
