package dev.znlp.fasttext.loss;

import dev.znlp.base.blas.Matrix;
import dev.znlp.base.blas.Vector;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.fasttext.Model;
import dev.znlp.fasttext.ScoreIdPair;

import java.util.ArrayList;
import java.util.List;

/**
 * 分层Softmax损失函数实现，适用于大规模分类任务。
 * 通过构建霍夫曼树将分类问题转化为一系列二分类问题，显著降低计算复杂度，
 * 从O(n)降低到O(log n)，其中n是类别数量。
 */
public class HierarchicalSoftmaxLoss extends BinaryLogisticLoss {

    /**
     * 输出大小，即类别数量
     */
    private final int osz;
    
    /**
     * 每个叶子节点到根节点的路径
     */
    private final List<int[]> paths;
    
    /**
     * 每个叶子节点到根节点路径上的二进制编码
     */
    private final List<boolean[]> codes;
    
    /**
     * 霍夫曼树的节点列表
     */
    private final List<Node> tree;

    /**
     * 构造HierarchicalSoftmaxLoss实例
     *
     * @param wo 输出矩阵
     * @param targetCounts 目标类别或词的频率数组
     */
    public HierarchicalSoftmaxLoss(Matrix wo, long[] targetCounts) {
        super(wo);
        this.osz = targetCounts.length;
        if (osz < 1) {
            throw new IllegalArgumentException("Hierarchical softmax requires at least 1 target");
        }

        List<int[]> pathsLocal = new ArrayList<>(osz);
        List<boolean[]> codesLocal = new ArrayList<>(osz);
        List<Node> treeLocal = new ArrayList<>(2 * osz - 1);

        for (int i = 0; i < 2 * osz - 1; i++) {
            Node node = new Node();
            node.parent = -1;
            node.left = -1;
            node.right = -1;
            node.count = 1000000000000000L;
            node.binary = false;
            treeLocal.add(node);
        }

        for (int i = 0; i < osz; i++) {
            treeLocal.get(i).count = targetCounts[i];
        }

        int leaf = osz - 1;
        int node = osz;
        for (int i = osz; i < 2 * osz - 1; i++) {
            int[] mini = new int[2];
            for (int j = 0; j <= 1; j++) {
                if (leaf >= 0 && treeLocal.get(leaf).count < treeLocal.get(node).count) {
                    mini[j] = leaf--;
                } else {
                    mini[j] = node++;
                }
            }
            treeLocal.get(i).left = mini[0];
            treeLocal.get(i).right = mini[1];
            treeLocal.get(i).count = treeLocal.get(mini[0]).count + treeLocal.get(mini[1]).count;

            treeLocal.get(mini[0]).parent = i;
            treeLocal.get(mini[1]).parent = i;
            treeLocal.get(mini[1]).binary = true;
        }

        for (int i = 0; i < osz; i++) {
            IntArrayList path = new IntArrayList();
            List<Boolean> code = new ArrayList<>();

            int j = i;
            while (treeLocal.get(j).parent != -1) {
                path.add(treeLocal.get(j).parent - osz);
                code.add(treeLocal.get(j).binary);
                j = treeLocal.get(j).parent;
            }
            pathsLocal.add(path.toArray());
            
            boolean[] codeArray = new boolean[code.size()];
            for(int k=0;k<code.size();k++) codeArray[k] = code.get(k);
            codesLocal.add(codeArray);
        }

        this.paths = pathsLocal;
        this.codes = codesLocal;
        this.tree = treeLocal;
    }

    /**
     * 深度优先搜索霍夫曼树，查找前k个最佳结果
     *
     * @param k 返回结果的数量
     * @param threshold 预测分数阈值
     * @param node 当前节点索引
     * @param score 当前分数
     * @param heap 存储结果的列表
     * @param hidden 隐藏层向量
     */
    private void dfs(float threshold, int node, float score, TopKScoreHeap heap, Vector hidden) {
        if (score < stdLog(threshold)) {
            return;
        }
        if (heap.cannotBeat(score)) {
            return;
        }

        if (tree.get(node).left == -1 && tree.get(node).right == -1) {
            heap.offer(score, node);
            return;
        }

        float f = wo.dotRow(hidden, node - osz);
        f = 1.0f / (1 + (float) Math.exp(-f));

        dfs(threshold, tree.get(node).left, score + stdLog(1.0f - f), heap, hidden);
        dfs(threshold, tree.get(node).right, score + stdLog(f), heap, hidden);
    }

    /**
     * 前向传播，沿着目标节点到根节点的路径计算损失并更新梯度
     *
     * @param targets 目标标签ID列表
     * @param targetIndex 目标索引
     * @param state 模型状态
     * @param lr 学习率
     * @param backprop 是否进行反向传播
     * @return 损失值
     */
    @Override
    public float forward(IntArrayList targets, int targetIndex, Model.State state, float lr, boolean backprop) {
        float loss = 0f;
        int target = targets.get(targetIndex);
        boolean[] binaryCode = codes.get(target);
        int[] pathToRoot = paths.get(target);
        for (int i = 0; i < pathToRoot.length; i++) {
            loss += binaryLogistic(pathToRoot[i], state, binaryCode[i], lr, backprop);
        }
        return loss;
    }

    /**
     * 预测前k个最佳结果，通过深度优先搜索霍夫曼树实现
     *
     * @param k 返回结果的数量
     * @param threshold 预测分数阈值
     * @param heap 存储结果的列表
     * @param state 模型状态
     */
    @Override
    public void predict(int k, float threshold, List<ScoreIdPair> heap, Model.State state) {
        TopKScoreHeap top = new TopKScoreHeap(k);
        dfs(threshold, 2 * osz - 2, 0f, top, state.hidden);
        top.drainDescending(heap);
    }

    /**
     * 霍夫曼树节点类，用于构建分层Softmax的树结构
     */
    public static class Node {
        /**
         * 父节点索引
         */
        public int parent;
        
        /**
         * 左子节点索引
         */
        public int left;
        
        /**
         * 右子节点索引
         */
        public int right;
        
        /**
         * 节点对应的词频或类别频率
         */
        public long count;
        
        /**
         * 节点的二进制编码（true表示右子节点，false表示左子节点）
         */
        public boolean binary;
    }
}
