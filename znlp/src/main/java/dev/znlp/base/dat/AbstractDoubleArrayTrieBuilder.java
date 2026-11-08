package dev.znlp.base.dat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * 双数组Trie构建器的抽象基类，提取公共字段和方法，减少重复代码。
 */
public abstract class AbstractDoubleArrayTrieBuilder implements DoubleArrayTrieBuilder {

    /**
     * 未指定容量时的起点。真正的大小在 {@link #build(List)} 里按词表估算，建完再收到实际占用。
     */
    protected static final int DEFAULT_CAPACITY = 1024;

    /**
     * 数组的容量
     */
    protected int arrayCapacity;

    /**
     * check数组
     */
    protected int[] check;

    /**
     * base数组
     */
    protected int[] base;

    /**
     * base和check的实际大小
     */
    protected int size;

    /**
     * 已使用的位置集合
     */
    protected BitSet used;

    /**
     * 待处理的数据列表
     */
    protected List<String> data;

    /**
     * 数据大小
     */
    protected int dataSize;

    /**
     * 进度
     */
    protected int progress = 0;

    /**
     * 下一个检查位置
     */
    protected int nextCheckPos;

    /**
     * 错误码
     */
    protected int error;

    /**
     * 默认构造方法，使用默认容量。
     */
    public AbstractDoubleArrayTrieBuilder() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * 带初始容量的构造方法。
     *
     * @param initCapacity 初始容量
     */
    public AbstractDoubleArrayTrieBuilder(int initCapacity) {
        this.arrayCapacity = initCapacity;
        this.check = new int[initCapacity];
        this.base = new int[initCapacity];
        this.used = new BitSet(initCapacity);

        // 初始化根节点
        this.base[0] = 1;
        this.nextCheckPos = 0;
    }

    /**
     * 构建双数组Trie。
     *
     * @param keys 已排序的字符串列表，用于构建双数组
     * @return 构建好的DoubleArrayTrie对象
     * @throws IllegalArgumentException 词表为空、含 null、未按字典序，或有重复键
     */
    @Override
    public DoubleArrayTrie build(List<String> keys) {
        validateKeys(keys);

        this.data = keys;
        this.dataSize = keys.size();
        this.progress = 0;
        this.error = 0;
        ensureCapacity(keys);

        try {
            Node rootNode = new Node();
            rootNode.left = 0;
            rootNode.right = this.dataSize;
            rootNode.depth = 0;

            List<Node> siblings = new ArrayList<>(16); // 预分配大小
            fetch(rootNode, siblings);
            insert(siblings);
            if (this.error < 0 || this.size <= 0) {
                throw new IllegalStateException("failed to build double array trie");
            }

            // 收到实际占用的下标。失败转移由匹配器做下标检查，不再为每个 char 预留 65536 槽。
            int length = this.size;
            this.base = Arrays.copyOfRange(this.base, 0, length);
            this.check = Arrays.copyOfRange(this.check, 0, length);
        } finally {
            // 释放临时数据
            this.used = null;
            this.data = null;
        }

        return new DoubleArrayTrie(keys.size(), this.check, this.base);
    }

    private static void validateKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("keys cannot be null or empty");
        }
        String previous = keys.get(0);
        if (previous == null) {
            throw new IllegalArgumentException("key at 0 is null");
        }
        for (int i = 1; i < keys.size(); i++) {
            String key = keys.get(i);
            if (key == null) {
                throw new IllegalArgumentException("key at " + i + " is null");
            }
            int order = previous.compareTo(key);
            if (order > 0) {
                throw new IllegalArgumentException("keys must be in dictionary order, index " + i);
            }
            if (order == 0) {
                throw new IllegalArgumentException("duplicate key: " + key);
            }
            previous = key;
        }
    }

    /**
     * 按最大字符编码和总字数预留数组，避免小词表先占一块 4M 槽的空数组。
     */
    private void ensureCapacity(List<String> keys) {
        int maxCode = 1;
        long slots = 0;
        for (String key : keys) {
            int length = key.length();
            slots += length + 1L;
            for (int i = 0; i < length; i++) {
                int code = key.charAt(i) + 1;
                if (code > maxCode) {
                    maxCode = code;
                }
            }
        }
        long estimate = Math.max(maxCode + 1L, slots * 3);
        if (estimate > arrayCapacity && estimate <= Integer.MAX_VALUE - 8) {
            resize((int) estimate);
        }
    }

    /**
     * 插入节点。
     *
     * @param siblings 等待插入的兄弟节点列表
     * @return 插入位置
     */
    protected abstract int insert(List<Node> siblings);

    /**
     * 获取直接相连的子节点。
     *
     * @param parent   父节点
     * @param siblings 子节点列表，用于存放结果
     * @return 子节点个数
     */
    protected abstract int fetch(Node parent, List<Node> siblings);

    /**
     * 扩展数组容量。
     *
     * @param newCapacity 新的容量
     */
    protected void resize(int newCapacity) {
        if (newCapacity <= this.arrayCapacity) {
            return;
        }

        int grown = this.arrayCapacity <= 0 ? newCapacity : this.arrayCapacity + (this.arrayCapacity >> 1);
        if (grown < newCapacity || grown < 0) {
            grown = newCapacity;
        }
        if (grown < 0) {
            throw new IllegalStateException("double array trie capacity overflow");
        }

        this.base = Arrays.copyOf(this.base, grown);
        this.check = Arrays.copyOf(this.check, grown);
        this.used = resizeBitSet(this.used, grown);

        this.arrayCapacity = grown;
    }

    /**
     * 调整BitSet的大小。
     *
     * @param original 原始BitSet
     * @param newSize  新的大小
     * @return 调整大小后的BitSet
     */
    private BitSet resizeBitSet(BitSet original, int newSize) {
        BitSet newBitSet = new BitSet(newSize);
        newBitSet.or(original);
        return newBitSet;
    }

    /**
     * 内部节点类，用于构建双数组Trie。
     */
    protected static class Node {
        /**
         * 字符编码
         */
        int code;
        /**
         * 深度
         */
        int depth;
        /**
         * 左边界
         */
        int left;
        /**
         * 右边界
         */
        int right;

        @Override
        public String toString() {
            return "Node{" +
                    "code=" + code +
                    ", depth=" + depth +
                    ", left=" + left +
                    ", right=" + right +
                    '}';
        }
    }

}