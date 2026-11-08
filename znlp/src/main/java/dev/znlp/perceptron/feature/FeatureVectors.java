package dev.znlp.perceptron.feature;

import dev.znlp.base.common.IntArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前线程的特征序列缓冲。返回的列表只在下一次 {@link #borrow} 之前有效。
 */
public final class FeatureVectors {

    private static final ThreadLocal<ArrayList<IntArrayList>> LOCAL = ThreadLocal.withInitial(ArrayList::new);

    private FeatureVectors() {
    }

    public static List<IntArrayList> borrow(int size) {
        ArrayList<IntArrayList> all = LOCAL.get();
        while (all.size() < size) {
            all.add(new IntArrayList(8));
        }
        for (int i = 0; i < size; i++) {
            all.get(i).reset();
        }
        return all.subList(0, size);
    }

    /**
     * 把缓冲里的特征立刻换成编号。{@link #vector} 在每个位置开始前指定。
     */
    public static final class Consumer implements FeatureConsumer {
        private final FeatureIdSeek featureSet;
        public IntArrayList vector;

        public Consumer(FeatureIdSeek featureSet) {
            this.featureSet = featureSet;
        }

        @Override
        public void accept(String feature) {
            add(featureSet.featureId(feature));
        }

        @Override
        public void acceptChars(char[] chars, int length) {
            add(featureSet.featureId(chars, length));
        }

        private void add(int id) {
            if (id >= 0) {
                vector.add(id);
            }
        }
    }
}
