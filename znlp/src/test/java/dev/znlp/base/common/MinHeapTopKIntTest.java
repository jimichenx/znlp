package dev.znlp.base.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinHeapTopKIntTest {

    @Test
    void keepsTopKByScore() {
        MinHeapTopKInt heap = new MinHeapTopKInt(2);
        heap.push(1, 0.1f);
        heap.push(2, 0.9f);
        heap.push(3, 0.5f);
        heap.push(4, 0.8f);

        List<MinHeapTopKInt.Pair<Integer, Float>> result = heap.resort();
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).first());
        assertEquals(4, result.get(1).first());
        assertTrue(result.get(0).second() >= result.get(1).second());
    }

    @Test
    void emptyResultBeforePush() {
        MinHeapTopKInt heap = new MinHeapTopKInt(3);
        assertTrue(heap.resort().isEmpty());
    }
}
