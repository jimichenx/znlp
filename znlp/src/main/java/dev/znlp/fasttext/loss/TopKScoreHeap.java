package dev.znlp.fasttext.loss;

import dev.znlp.fasttext.ScoreIdPair;

import java.util.List;

/**
 * 容量为 k 的最小堆，保留分数最高的 k 个 (score, id)。
 * 插入 O(log k)；满堆后更差的分数直接丢弃。
 */
final class TopKScoreHeap {

    private final int k;
    private final float[] scores;
    private final int[] ids;
    private int size;

    TopKScoreHeap(int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k needs to be 1 or higher");
        }
        this.k = k;
        this.scores = new float[k];
        this.ids = new int[k];
    }

    boolean cannotBeat(float score) {
        return size == k && score < scores[0];
    }

    void offer(float score, int id) {
        if (cannotBeat(score)) {
            return;
        }
        if (size < k) {
            scores[size] = score;
            ids[size] = id;
            size++;
            if (size == k) {
                for (int i = k / 2 - 1; i >= 0; i--) {
                    siftDown(i);
                }
            }
        } else {
            scores[0] = score;
            ids[0] = id;
            siftDown(0);
        }
    }

    void drainDescending(List<ScoreIdPair> heap) {
        heap.clear();
        for (int i = 0; i < size; i++) {
            heap.add(new ScoreIdPair(scores[i], ids[i]));
        }
        heap.sort((a, b) -> Float.compare(b.score(), a.score()));
    }

    private void siftDown(int i) {
        int min = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        if (left < size && scores[left] < scores[min]) {
            min = left;
        }
        if (right < size && scores[right] < scores[min]) {
            min = right;
        }
        if (min != i) {
            float ts = scores[i];
            scores[i] = scores[min];
            scores[min] = ts;
            int ti = ids[i];
            ids[i] = ids[min];
            ids[min] = ti;
            siftDown(min);
        }
    }
}
