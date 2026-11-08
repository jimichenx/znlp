package dev.znlp.perceptron.decoder;

import org.junit.jupiter.api.Test;
import dev.znlp.base.common.IntArrayList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuickPerceptronDecoderTest {

    @Test
    void picksHighestScoringLabelPerPosition() {
        int labelCount = 3;
        float[] parameter = new float[10 * labelCount];
        parameter[5 * labelCount + 2] = 100f;
        parameter[6 * labelCount + 1] = 50f;

        QuickPerceptronDecoder decoder = new QuickPerceptronDecoder(labelCount, parameter);
        int[] labels = new int[2];
        decoder.decode(List.of(features(5), features(6)), labels);

        assertEquals(2, labels[0]);
        assertEquals(1, labels[1]);
    }

    @Test
    void labelIndicesStayWithinRange() {
        int labelCount = 4;
        float[] parameter = new float[20 * labelCount];
        for (int f = 0; f < 20; f++) {
            parameter[f * labelCount + (f % labelCount)] = f;
        }
        QuickPerceptronDecoder decoder = new QuickPerceptronDecoder(labelCount, parameter);
        int[] labels = new int[5];
        decoder.decode(List.of(features(1), features(2), features(3), features(4), features(5)), labels);
        for (int label : labels) {
            assertTrue(label >= 0 && label < labelCount);
        }
    }

    private static IntArrayList features(int id) {
        IntArrayList list = new IntArrayList(2);
        list.add(id);
        list.add(0);
        return list;
    }
}
