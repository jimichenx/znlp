package dev.znlp.perceptron.feature;

import org.junit.jupiter.api.Test;
import dev.znlp.base.common.IntArrayList;
import dev.znlp.base.dat.DoubleArrayTrie;
import dev.znlp.base.dat.DoubleArrayTrieBuilder2;

import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatureBufferLookupTest {

    @Test
    void charBufferMatchesStringLookup() {
        TreeSet<String> keys = new TreeSet<>(List.of("语言", "语言☺", "处★", "a/b5"));
        DoubleArrayTrie dat = new DoubleArrayTrieBuilder2().build(keys);
        ImmutableFeatureSet features = new ImmutableFeatureSet(dat);

        for (String key : keys) {
            char[] chars = key.toCharArray();
            assertEquals(features.featureId(key), features.featureId(chars, chars.length));
        }
        char[] missing = "没有".toCharArray();
        assertEquals(-1, features.featureId(missing, missing.length));
        assertEquals(-1, features.featureId(new char[4], 0));
    }

    @Test
    void borrowedVectorsDoNotKeepPreviousIds() {
        List<IntArrayList> first = FeatureVectors.borrow(2);
        first.get(0).add(3);
        first.get(1).add(9);

        List<IntArrayList> second = FeatureVectors.borrow(2);
        assertEquals(0, second.get(0).size());
        assertEquals(0, second.get(1).size());
        second.get(0).add(1);
        assertEquals(1, second.get(0).size());
        assertEquals(1, second.get(0).get(0));
    }
}
