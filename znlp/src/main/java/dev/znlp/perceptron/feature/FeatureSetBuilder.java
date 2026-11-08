package dev.znlp.perceptron.feature;

import dev.znlp.base.dat.DoubleArrayTrie;
import dev.znlp.base.dat.DoubleArrayTrieBuilder;

import java.util.HashSet;
import java.util.List;


/**
 * DAT的特征集合构建器
 */
public class FeatureSetBuilder {
    private final HashSet<String> keys = new HashSet<>(10_0000);

    public FeatureSetBuilder(int labelCount) {
        // Hanlp需要从 0=< <= labelCount 上站位 占用labelCount+1个位置
        // \u0000\u0001BL= 要保证这个排在前面
        for (int i = 0; i <= labelCount; i++) {
            keys.add("\u0000\u0001BL=" + i);
        }
    }

    public void put(String feature) {
        keys.add(feature);
    }

    public ImmutableFeatureSetWithKeys build() {
        List<String> list = keys.stream().sorted().toList();
        DoubleArrayTrieBuilder builder = DoubleArrayTrie.builder();
        DoubleArrayTrie dat = builder.build(list);
        keys.clear();
        return new ImmutableFeatureSetWithKeys(dat, list);
    }
}