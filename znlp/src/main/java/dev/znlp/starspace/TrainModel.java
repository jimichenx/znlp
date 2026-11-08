package dev.znlp.starspace;

import dev.znlp.base.blas.DenseArrayMatrix;
import dev.znlp.base.blas.DenseMatrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;

import java.util.Random;

/**
 * 训练模型类，用于存储训练过程中的模型权重和梯度
 * <p>
 * 该类处理矩阵的初始化和训练阶段使用的投影逻辑。
 * 它是模型训练过程中的核心组件，负责管理模型的参数和相关操作。
 */
public class TrainModel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TrainModel.class);

    /** 左侧嵌入矩阵 */
    public final SparseLinear lhsEmbeddings;
    /** 左侧嵌入矩阵（密集表示） */
    public final DenseMatrix lhsMatrix;

    /** 右侧嵌入矩阵 */
    public final SparseLinear rhsEmbeddings;
    /** 右侧嵌入矩阵（密集表示） */
    public final DenseMatrix rhsMatrix;

    /** 左侧更新数组，用于 Adagrad 优化 */
    public final float[] lhsUpdates;
    /** 右侧更新数组，用于 Adagrad 优化 */
    public final float[] rhsUpdates;

    /** 训练参数 */
    public final Args args;
    
    /** 随机数生成器 */
    private final Random random = new Random();

    /**
     * 构造训练模型
     * @param dict 词典
     * @param args 训练参数
     */
    public TrainModel(Dictionary dict, Args args) {
        this.args = args;
        
        int numLhs = dict.nwords() + dict.nlabels();
        if (args.ngrams > 1) {
            numLhs += args.bucket;
        }

        lhsMatrix = new DenseArrayMatrix(numLhs, args.dim);
        if (args.initRandSd > 0) {
            // 使用高斯分布初始化矩阵
            gaussRandom(lhsMatrix, args.initRandSd);
        }
        
        lhsEmbeddings = new SparseLinear(lhsMatrix);

        if (args.shareEmb) {
            // 共享嵌入矩阵
            rhsEmbeddings = lhsEmbeddings;
            rhsMatrix = lhsMatrix;
        } else {
            // 不共享嵌入矩阵，创建新的右侧嵌入矩阵
            rhsMatrix = new DenseArrayMatrix(numLhs, args.dim);
            if (args.initRandSd > 0) {
                gaussRandom(rhsMatrix, args.initRandSd);
            }
            rhsEmbeddings = new SparseLinear(rhsMatrix);
        }

        logger.info("Initialized model weights. StarSpace Model length : matrix : {} {}",
            lhsEmbeddings.numRows(), lhsEmbeddings.numCols());

        if (args.adagrad) {
            // 使用 Adagrad 优化，初始化更新数组
            lhsUpdates = new float[lhsEmbeddings.numRows()];
            rhsUpdates = new float[rhsEmbeddings.numRows()];
        } else {
            // 不使用 Adagrad 优化，使用空数组
            lhsUpdates = new float[0];
            rhsUpdates = new float[0];
        }
    }

    /**
     * 使用高斯分布初始化矩阵
     * @param matrix 要初始化的矩阵
     * @param sd 高斯分布的标准差
     */
    private void gaussRandom(DenseMatrix matrix, double sd) {
        int rows = matrix.getRow();
        int cols = matrix.getCol();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix.set(i, j, (float)(random.nextGaussian() * sd));
            }
        }
    }

    /**
     * 投影左侧特征到向量空间
     * @param ws 特征-权重对列表
     * @return 投影后的向量
     */
    public Vector projectLHS(java.util.List<Utils.XPair> ws) {
        Vector vec = lhsEmbeddings.forward(ws);

        if (!ws.isEmpty()) {
            float norm = args.norm2Computer(ws, vec);
            vec.divAssign(norm);
        }

        return vec;
    }

    /**
     * 投影右侧特征到向量空间
     * @param ws 特征-权重对列表
     * @return 投影后的向量
     */
    public Vector projectRHS(java.util.List<Utils.XPair> ws) {
        Vector vec = rhsEmbeddings.forward(ws);
        if (!ws.isEmpty()) {
            float norm = args.norm2Computer(ws, vec);
            vec.divAssign(norm);
        }
        return vec;
    }
}
