package dev.znlp.starspace;

import dev.znlp.base.blas.Vector;

import java.io.*;

/**
 * 训练参数配置类，用于设置 StarSpace 模型的各种训练参数
 * <p>
 * 该类包含了 StarSpace 模型训练所需的所有配置参数，包括字典设置、训练模式、
 * 损失函数、相似度计算方式、学习率等。通过调整这些参数，可以控制模型的训练行为
 * 和性能表现。
 * <p>
 * 主要功能：
 * 1. 定义训练模式枚举（Mode0-Mode5），支持不同的训练策略
 * 2. 定义文件格式枚举（FastText, LabelDoc）
 * 3. 定义损失函数枚举（Hinge, SoftMax）
 * 4. 定义相似度计算枚举（Cosine, Dot）
 * 5. 提供参数的读写方法，支持模型持久化
 * 6. 提供相似度计算和归一化方法
 */
public class Args implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TrainMode {
        /**
         * one Label from RHS is picked as true Label; LHS is the same from input.
         */
        Mode0,

        /**
         * one Label from RHS is picked as true Label; LHS is the bag of the rest RHS labels.
         */
        Mode1,

        /**
         * one Label from RHS is picked as LHS; the bag of the rest RHS labels becomes the true Label.
         */
        Mode2,

        /**
         * one Label from RHS is picked as true Label and another Label from RHS is picked as LHS.
         */
        Mode3,

        /**
         * the first Label from RHS is picked as LHS and the second one picked as true Label.
         */
        Mode4,

        /**
         * continuous bag of words training.
         */
        Mode5
    }

    public enum FileFormat {
        FastText, LabelDoc
    }

    public enum LossFunction {
        Hinge, SoftMax
    }

    public enum Similarity {
        Cosine, Dot
    }

    // The following arguments for the dictionary are optional:

    /**
     * minimal number of Word occurences
     */
    public int minCount = 1;

    /**
     * minimal number of Label occurences
     */
    public int minCountLabel = 1;


    public String label = "__label__";

    /**
     * max length of Word ngram
     */
    public int ngrams = 1;

    /**
     * number of buckets
     */
    public int bucket = 2000000;


    /**
     * takes value in [0, 1, 2, 3, 4, 5], see Training Mode Section. [0]
     */
    public TrainMode trainMode = TrainMode.Mode0;

    /**
     * currently support 'fastText' and 'labelDoc', see File Format Section.
     */
    public FileFormat fileFormat = FileFormat.FastText;

    /**
     * learning rate
     */
    public double lr = 0.01;


    /**
     * length of embedding vectors
     */
    public int dim = 100;

    /**
     * number of epochs
     */
    public int epoch = 5;

    /**
     * max train time (secs)
     */
    public int maxTrainTime = 60 * 60 * 24 * 100;

    /**
     * number of negatives sampled
     */
    public int negSearchLimit = 50;

    /**
     * max number of negatives in a batch update
     */
    public int maxNegSamples = 10;

    /**
     * loss function {hinge, softmax} [hinge]
     */
    public LossFunction loss = LossFunction.Hinge;


    /**
     * margin parameter in hinge loss. It's only effective if hinge loss is used.
     */
    public double margin = 0.05;

    /**
     * takes value in [cosine, dot]. Whether to use cosine or dot product as similarity function in  hinge loss.
     * It's only effective if hinge loss is used.
     */
    public Similarity similarity = Similarity.Cosine;

    /**
     * normalization parameter: we normalize sum of embeddings by deviding Size^p, when p=1, it's equivalent to taking average of embeddings; when p=0, it's equivalent to taking sum of embeddings. [0.5]
     */
    public double p = 0.5;


    /**
     * whether to use adagrad in training
     */
    public boolean adagrad = true;


    /**
     * whether to use the same embedding matrix for LHS and RHS.
     */
    public boolean shareEmb = true;

    /**
     * only used in trainMode 5, the length of the context window for Word level training.
     */
    public int ws = 5;


    /**
     * dropout probability for LHS features. [0]
     */
    public double dropoutLHS = 0.0;

    /**
     * dropout probability for RHS features. [0]
     */
    public double dropoutRHS = 0.0;

    /**
     * initial values of embeddings are randomly generated from normal distribution with mean=0, standard deviation=initRandSd.
     */
    public double initRandSd = 0.001;

    /**
     * whether to train Word level together with other tasks (for multi-tasking).
     */
    public boolean trainWord = false;

    /**
     * if trainWord is true, wordWeight specifies example weight for Word level training examples.
     */
    public float wordWeight = 0.5f;


    public double norm = 1.0;


    /**
     * number of threads
     */
    public int thread = Math.max(2, Math.min(10, Runtime.getRuntime().availableProcessors() - 1));

    public boolean debug = false;

    /**
     * whether input file contains weights
     */
    public boolean useWeight = false;

    /**
     * whether to run basic text preprocess for input files
     */
    public boolean normalizeText = false;

    public void setThread(int value) {
        if (value > Runtime.getRuntime().availableProcessors()) {
            this.thread = Runtime.getRuntime().availableProcessors();
        } else {
            this.thread = value;
        }
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(dim);
        data.writeInt(epoch);
        data.writeInt(minCount);
        data.writeInt(minCountLabel);
        data.writeInt(maxNegSamples);
        data.writeInt(negSearchLimit);
        data.writeInt(ngrams);
        data.writeInt(bucket);
        data.writeInt(trainMode.ordinal());
        data.writeBoolean(shareEmb);
        data.writeBoolean(useWeight);
        data.writeInt(fileFormat.ordinal());
        data.writeInt(similarity.ordinal());
        data.flush();
    }

    public void read(InputStream in) throws IOException {
        DataInputStream input = new DataInputStream(in);
        dim = input.readInt();
        epoch = input.readInt();
        minCount = input.readInt();
        minCountLabel = input.readInt();
        maxNegSamples = input.readInt();
        negSearchLimit = input.readInt();
        ngrams = input.readInt();
        bucket = input.readInt();
        trainMode = TrainMode.values()[input.readInt()];
        shareEmb = input.readBoolean();
        useWeight = input.readBoolean();
        fileFormat = FileFormat.values()[input.readInt()];
        similarity = Similarity.values()[input.readInt()];
    }


    public float norm2Computer(java.util.List<?> ws, Vector vec) {
        if (similarity == Similarity.Dot) {
            return (float) Math.pow(ws.size(), p);
        } else {
            return norm2(vec);
        }
    }

    public float similarity(Vector a, Vector b) {
        if (similarity == Similarity.Dot) {
            float value = a.prod(b);
            checkArgument(!Float.isNaN(value) && !Float.isInfinite(value));
            return value;
        } else {
            // Cosine similarity
            float value = cosine(a, b);
            checkArgument(!Float.isNaN(value) && !Float.isInfinite(value));
            return value;
        }
    }

    public float norm2(Vector vec) {
        return Math.max(1.19209e-007f, vec.norm2());
    }

    private void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    private float cosine(Vector a, Vector b) {
        float dot = a.prod(b);
        float normA = a.norm2();
        float normB = b.norm2();
        return dot / (normA * normB);
    }
}
