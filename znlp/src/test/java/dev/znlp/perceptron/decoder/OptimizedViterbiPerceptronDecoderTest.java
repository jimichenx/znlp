package dev.znlp.perceptron.decoder;

import dev.znlp.perceptron.PerceptronDecoder;
import org.junit.jupiter.api.Test;
import dev.znlp.base.common.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 对照参考实现 {@link ViterbiPerceptronDecoder}，验证优化版 Viterbi
 * 在标签数 ≠ 4、序列长度为偶数时的终点选择（B01）。
 */
public class OptimizedViterbiPerceptronDecoderTest {

    /**
     * 偶数长度 + 5 标签：最后一位强烈偏好标签 4，前一位偏好标签 0。
     * 若误用上一时刻得分选终点，会得到以 0 结尾的路径。
     */
    @Test
    public void evenLengthNon4Labels_matchesReference() {
        int labelCount = 5;
        float[] parameter = new float[12 * labelCount];

        // BOS 倾向标签 0
        parameter[labelCount * labelCount] = 1.0f;
        // 位置 0 发射特征：标签 0 得 100
        parameter[6 * labelCount] = 100.0f;
        // 位置 1 发射特征：标签 4 得 100
        parameter[7 * labelCount + 4] = 100.0f;

        List<IntArrayList> sequence = List.of(
                features(6),
                features(7)
        );

        int[] reference = decodeWith(new ViterbiPerceptronDecoder(labelCount, parameter), sequence);
        int[] optimized = decodeWith(new OptimizedViterbiPerceptronDecoder(labelCount, parameter), sequence);

        assertEquals(4, reference[1], "参考实现终点应为标签 4");
        assertArrayEquals(reference, optimized,
                "偶数长度且标签数≠4 时优化版应与参考 Viterbi 一致，实际=" + Arrays.toString(optimized));
    }

    @Test
    public void oddLengthNon4Labels_matchesReference() {
        int labelCount = 5;
        float[] parameter = new float[14 * labelCount];
        parameter[labelCount * labelCount] = 1.0f;
        parameter[6 * labelCount] = 100.0f;
        parameter[7 * labelCount + 4] = 100.0f;
        parameter[8 * labelCount + 2] = 100.0f;

        List<IntArrayList> sequence = List.of(features(6), features(7), features(8));

        int[] reference = decodeWith(new ViterbiPerceptronDecoder(labelCount, parameter), sequence);
        int[] optimized = decodeWith(new OptimizedViterbiPerceptronDecoder(labelCount, parameter), sequence);

        assertArrayEquals(reference, optimized);
    }

    @Test
    public void evenLength4Labels_matchesReference() {
        int labelCount = 4;
        float[] parameter = new float[12 * labelCount];
        parameter[labelCount * labelCount] = 1.0f;
        parameter[6 * labelCount] = 100.0f;
        parameter[7 * labelCount + 3] = 100.0f;

        List<IntArrayList> sequence = List.of(features(6), features(7));

        int[] reference = decodeWith(new ViterbiPerceptronDecoder(labelCount, parameter), sequence);
        int[] optimized = decodeWith(new OptimizedViterbiPerceptronDecoder(labelCount, parameter), sequence);

        assertEquals(3, reference[1]);
        assertArrayEquals(reference, optimized);
    }

    @Test
    public void matchesReferenceAcrossLengthsAndLabelCounts() {
        Random random = new Random(42);
        int[] labelCounts = {2, 3, 4, 5, 7, 8};
        int[] lengths = {1, 2, 3, 4, 5, 6, 8};

        for (int labelCount : labelCounts) {
            for (int length : lengths) {
                float[] parameter = randomParameters(labelCount, 20, random);
                List<IntArrayList> sequence = randomSequence(length, 6, 19, random);

                int[] reference = decodeWith(new ViterbiPerceptronDecoder(labelCount, parameter), sequence);
                int[] optimized = decodeWith(new OptimizedViterbiPerceptronDecoder(labelCount, parameter), sequence);

                assertArrayEquals(reference, optimized,
                        "labelCount=" + labelCount + " length=" + length
                                + " ref=" + Arrays.toString(reference)
                                + " opt=" + Arrays.toString(optimized));
            }
        }
    }

    private static int[] decodeWith(PerceptronDecoder decoder,
                                    List<IntArrayList> sequence) {
        return decoder.decode(sequence);
    }

    private static IntArrayList features(int... featureIds) {
        IntArrayList list = new IntArrayList(featureIds.length + 1);
        for (int id : featureIds) {
            list.add(id);
        }
        list.add(0); // 最后一位留给转移特征，解码时不参与发射得分
        return list;
    }

    private static float[] randomParameters(int labelCount, int featureCount, Random random) {
        float[] parameter = new float[featureCount * labelCount];
        for (int i = 0; i < parameter.length; i++) {
            parameter[i] = (random.nextFloat() - 0.5f) * 20.0f;
        }
        return parameter;
    }

    private static List<IntArrayList> randomSequence(int length, int minFeatureId, int maxFeatureId, Random random) {
        List<IntArrayList> sequence = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            int featureId = minFeatureId + random.nextInt(maxFeatureId - minFeatureId + 1);
            sequence.add(features(featureId));
        }
        return sequence;
    }
}
