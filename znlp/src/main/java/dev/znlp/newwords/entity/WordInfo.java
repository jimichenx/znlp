package dev.znlp.newwords.entity;

import java.util.HashMap;
import java.util.HashSet;

/**
 * 词信息统计
 * <p>
 * 优化点：
 * <ul>
 *   <li>使用紧凑开放寻址哈希表存储左右邻字频次：
 *     <ul>
 *       <li>int[] 数组存储 (char, count) 对，每对仅 8 字节</li>
 *       <li>初始容量小，按需动态扩展</li>
 *       <li>100 个邻字仅需 ~1KB（远优于 HashMap 的 ~5KB）</li>
 *     </ul>
 *   </li>
 *   <li>使用开放寻址哈希表替代 BitSet 存储 docSet：
 *     <ul>
 *       <li>避免 BitSet 在大 docId 时的内存爆炸问题</li>
 *       <li>达到最大容量限制后自动转为计数模式</li>
 *     </ul>
 *   </li>
 *   <li>极致内存优化，适合大量 WordInfo 实例场景</li>
 * </ul>
 */
public class WordInfo {
    // 开放寻址哈希表初始容量（必须是 2 的幂）
    private static final int INITIAL_CAPACITY = 8;
    // 负载因子阈值（超过则扩容）
    private static final float LOAD_FACTOR = 0.7f;
    // 空槽标记
    private static final int EMPTY_KEY = 0;

    private static final HashMap<Character, IntCount> EMPTY_MAP = new HashMap<>();
    private static final HashSet<Integer> EMPTY_SET = new HashSet<>();

    private final String word;
    private int count = 0;
    private float mi = 0f;
    private float mi_avg = 0f;

    private float entropy = 0f;

    /**
     * 左邻字统计：紧凑开放寻址哈希表
     * 数组布局: [key0, value0, key1, value1, ...]
     * - key: 字符的 int 值（0 表示空槽）
     * - value: 出现频次
     */
    private int[] leftData;
    private int leftSize = 0;  // 已存储的不同字符数

    /**
     * 右邻字统计
     */
    private int[] rightData;
    private int rightSize = 0;

    /**
     * 文档ID集合：使用开放寻址哈希表
     * - 初始为null，延迟初始化
     * - 使用int[]存储docId，0表示空槽（docId+1存储以避免冲突）
     * - 当达到最大容量限制时，转为计数模式（只记录数量）
     */
    private int[] docIdTable;
    private int docIdSize = 0;  // 已存储的不同文档数
    private boolean docCountMode = false;  // 是否已转为计数模式
    private static final int DOC_TABLE_INITIAL_CAPACITY = 8;  // 初始容量
    private static final int DOC_TABLE_MAX_CAPACITY = 512;  // 最大容量限制，防止内存爆炸

    private float score = 0f;
    private float idf = 0f;
    private int doc = 0;
    private float le = 0f;
    private float re = 0f;

    /**
     * 左右邻字种类数（Accessor Variety）
     * 在计算熵之前保存，用于高级打分算法
     */
    private int leftVariety = 0;
    private int rightVariety = 0;

    public WordInfo(String word) {
        this.word = word;
        // 延迟初始化，只在需要时才创建数组
    }

    /**
     * 添加字符到左侧统计（紧凑哈希表）
     */
    public void addToLeft(char key, int count) {
        if (leftData == null) {
            leftData = new int[INITIAL_CAPACITY * 2];  // 初始仅 64 字节
        }
        leftData = addToTable(leftData, key, count, true);
    }

    public void addToRight(char key, int count) {
        if (rightData == null) {
            rightData = new int[INITIAL_CAPACITY * 2];
        }
        rightData = addToTable(rightData, key, count, false);
    }

    /**
     * 向开放寻址哈希表添加数据
     *
     * @return 可能扩容后的新数组
     */
    private int[] addToTable(int[] data, char key, int count, boolean isLeft) {
        int intKey = key + 1;  // +1 避免与 EMPTY_KEY(0) 冲突
        int capacity = data.length / 2;
        int mask = capacity - 1;
        int index = (key * 31) & mask;  // 简单哈希

        // 线性探测查找
        while (true) {
            int pos = index * 2;
            int storedKey = data[pos];

            if (storedKey == EMPTY_KEY) {
                // 空槽：插入新键
                data[pos] = intKey;
                data[pos + 1] = count;
                if (isLeft) {
                    leftSize++;
                } else {
                    rightSize++;
                }
                // 检查是否需要扩容
                int size = isLeft ? leftSize : rightSize;
                if (size > capacity * LOAD_FACTOR) {
                    return resize(data, capacity * 2, isLeft);
                }
                return data;
            } else if (storedKey == intKey) {
                // 找到相同键：累加计数
                data[pos + 1] += count;
                return data;
            }

            // 冲突：线性探测下一个位置
            index = (index + 1) & mask;
        }
    }

    /**
     * 扩容哈希表
     */
    private int[] resize(int[] oldData, int newCapacity, boolean isLeft) {
        int[] newData = new int[newCapacity * 2];
        int oldCapacity = oldData.length / 2;

        // 重新哈希所有元素
        for (int i = 0; i < oldCapacity; i++) {
            int pos = i * 2;
            int key = oldData[pos];
            if (key != EMPTY_KEY) {
                int value = oldData[pos + 1];
                rehashInsert(newData, key, value, newCapacity);
            }
        }
        return newData;
    }

    /**
     * 扩容时重新插入（不检查扩容，不更新 size）
     */
    private void rehashInsert(int[] data, int intKey, int value, int capacity) {
        int mask = capacity - 1;
        int index = ((intKey - 1) * 31) & mask;  // intKey-1 还原为原始 char 计算哈希

        while (true) {
            int pos = index * 2;
            if (data[pos] == EMPTY_KEY) {
                data[pos] = intKey;
                data[pos + 1] = value;
                return;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * 计算 TF-IDF
     */
    public void tfIdf(double docCount, double ziCount) {
        int docSize = docIdSize + 1;
        idf = (float) Math.log10(docCount / docSize);
        // 释放内存
        docIdTable = null;
        docCountMode = false;
        this.doc = docSize - 1;
    }

    /**
     * 计算熵（紧凑哈希表版）
     */
    public void entropy() {
        float leftEntropy = calculateEntropy(leftData);
        float rightEntropy = calculateEntropy(rightData);

        le = leftEntropy;
        re = rightEntropy;
        entropy = Math.min(leftEntropy, rightEntropy);

        // 保存左右邻字种类数（用于高级打分算法）
        leftVariety = leftSize;
        rightVariety = rightSize;

        // 释放内存
        leftData = null;
        rightData = null;
    }

    /**
     * 计算单侧熵值
     */
    private float calculateEntropy(int[] data) {
        if (data == null) {
            return 0f;
        }

        float entropy = 0f;
        int capacity = data.length / 2;

        for (int i = 0; i < capacity; i++) {
            int pos = i * 2;
            if (data[pos] != EMPTY_KEY) {
                int freq = data[pos + 1];
                if (freq > 0) {
                    float p = freq / (float) count;
                    entropy -= (float) (p * Math.log(p));
                }
            }
        }
        return entropy;
    }

    // Getters and Setters
    public String getWord() {
        return word;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        this.count++;
    }

    public float getMi() {
        return mi;
    }

    public void setMi(float mi) {
        this.mi = mi;
    }

    public float getMi_avg() {
        return mi_avg;
    }

    public void setMi_avg(float mi_avg) {
        this.mi_avg = mi_avg;
    }

    public float getEntropy() {
        return entropy;
    }

    public void setEntropy(float entropy) {
        this.entropy = entropy;
    }

    /**
     * 兼容旧接口：返回空 Map
     *
     * @deprecated 使用 addToLeft 和 entropy 方法代替
     */
    @Deprecated
    public HashMap<Character, IntCount> getLeft() {
        return EMPTY_MAP;
    }

    /**
     * 兼容旧接口：返回空 Map
     *
     * @deprecated 使用 addToRight 和 entropy 方法代替
     */
    @Deprecated
    public HashMap<Character, IntCount> getRight() {
        return EMPTY_MAP;
    }

    /**
     * 获取文档集合（兼容旧接口）
     * 返回一个包装的 HashSet 视图
     */
    public HashSet<Integer> getDocSet() {
        // 返回一个包装类，支持 add 操作
        return new DocSetWrapper();
    }

    /**
     * DocSet 包装类，内部使用开放寻址哈希表实现
     * <p>
     * 优化策略：
     * - 小集合：使用int[]哈希表精确去重
     * - 大集合：达到最大容量后转为计数模式，不再精确去重但内存可控
     */
    private class DocSetWrapper extends HashSet<Integer> {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean add(Integer docId) {
            // 已转为计数模式，直接计数（放弃精确去重，换取内存可控）
            if (docCountMode) {
                docIdSize++;
                return true;
            }

            // 延迟初始化
            if (docIdTable == null) {
                docIdTable = new int[DOC_TABLE_INITIAL_CAPACITY];
            }

            int storedKey = docId + 1;  // +1 避免与空槽标记0冲突
            int capacity = docIdTable.length;
            int mask = capacity - 1;
            int index = (docId * 0x9E3779B9) & mask;  // 黄金比例哈希

            // 线性探测查找
            while (true) {
                int existing = docIdTable[index];
                if (existing == 0) {
                    // 空槽：插入新docId
                    docIdTable[index] = storedKey;
                    docIdSize++;

                    // 检查是否需要扩容或转为计数模式
                    if (docIdSize > capacity * LOAD_FACTOR) {
                        if (capacity >= DOC_TABLE_MAX_CAPACITY) {
                            // 达到最大容量，转为计数模式
                            docIdTable = null;
                            docCountMode = true;
                        } else {
                            // 扩容
                            resizeDocTable(capacity * 2);
                        }
                    }
                    return true;
                } else if (existing == storedKey) {
                    // 已存在
                    return false;
                }
                // 冲突：线性探测下一个位置
                index = (index + 1) & mask;
            }
        }

        @Override
        public int size() {
            return docIdSize;
        }
    }

    /**
     * 扩容docId哈希表
     */
    private void resizeDocTable(int newCapacity) {
        int[] oldTable = docIdTable;
        docIdTable = new int[newCapacity];
        int mask = newCapacity - 1;

        for (int storedKey : oldTable) {
            if (storedKey != 0) {
                int docId = storedKey - 1;
                int index = (docId * 0x9E3779B9) & mask;
                while (docIdTable[index] != 0) {
                    index = (index + 1) & mask;
                }
                docIdTable[index] = storedKey;
            }
        }
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getIdf() {
        return idf;
    }

    public void setIdf(float idf) {
        this.idf = idf;
    }

    public int getDoc() {
        return doc;
    }

    public void setDoc(int doc) {
        this.doc = doc;
    }

    public float getLe() {
        return le;
    }

    public void setLe(float le) {
        this.le = le;
    }

    public float getRe() {
        return re;
    }

    public void setRe(float re) {
        this.re = re;
    }

    public int getLeftVariety() {
        return leftVariety;
    }

    public void setLeftVariety(int leftVariety) {
        this.leftVariety = leftVariety;
    }

    public int getRightVariety() {
        return rightVariety;
    }

    public void setRightVariety(int rightVariety) {
        this.rightVariety = rightVariety;
    }
}
