package dev.znlp.segment.graph;

import dev.znlp.base.common.AbstractIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 词图中一个字符位置上的全部候选。按词长升序放在数组里，同长度只有一个。
 */
public final class VertexRow implements Iterable<Vertex> {

    private static final Vertex[] EMPTY = new Vertex[0];

    public final int rowNum;

    Wordnet wordnet;

    private Vertex[] nodes = EMPTY;
    private int size;

    VertexRow(int rowNum, Wordnet wordnet) {
        this.rowNum = rowNum;
        this.wordnet = wordnet;
    }

    public String subString(int length) {
        if (rowNum == -1 || length == 0) {
            return "";
        }
        return new String(wordnet.charArray, wordnet.origin + rowNum, length);
    }

    public Vertex getOrCrete(int length) {
        Vertex existing = get(length);
        if (existing != null) {
            return existing;
        }
        Vertex created = wordnet.acquire(length, -1, 0);
        put(created);
        return created;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            nodes[i] = null;
        }
        size = 0;
    }

    public Vertex get(int length) {
        int index = indexOf(length);
        return index >= 0 ? nodes[index] : null;
    }

    public Vertex remove(short length) {
        int index = indexOf(length);
        if (index < 0) {
            return null;
        }
        Vertex removed = nodes[index];
        System.arraycopy(nodes, index + 1, nodes, index, size - index - 1);
        nodes[--size] = null;
        return removed;
    }

    /**
     * 按词长插入或替换。返回被替换的旧节点；没有旧节点时返回 null。
     */
    public Vertex put(Vertex vertex) {
        vertex.vertexRow = this;
        int index = indexOf(vertex.length);
        if (index >= 0) {
            Vertex old = nodes[index];
            nodes[index] = vertex;
            return old;
        }
        int insertAt = -index - 1;
        if (size == nodes.length) {
            nodes = Arrays.copyOf(nodes, size == 0 ? 4 : size * 2);
        }
        System.arraycopy(nodes, insertAt, nodes, insertAt + 1, size - insertAt);
        nodes[insertAt] = vertex;
        size++;
        return null;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(int len) {
        return indexOf(len) >= 0;
    }

    public int size() {
        return size;
    }

    public Vertex vertexAt(int index) {
        return nodes[index];
    }

    public Set<Integer> keys() {
        Set<Integer> set = new TreeSet<>();
        for (int i = 0; i < size; i++) {
            set.add(nodes[i].length);
        }
        return set;
    }

    public int lastLen() {
        return size == 0 ? 0 : nodes[size - 1].length;
    }

    @Override
    public String toString() {
        return "Slot[" + rowNum + "]=>" + keys();
    }

    @Override
    public Iterator<Vertex> iterator() {
        return new AbstractIterator<>() {
            private int cursor;

            @Override
            protected void computeNext() {
                if (cursor >= size) {
                    done();
                } else {
                    setNext(nodes[cursor++]);
                }
            }
        };
    }

    public List<Vertex> values() {
        List<Vertex> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(nodes[i]);
        }
        return list;
    }

    public Vertex getFirst() {
        return size == 0 ? null : nodes[0];
    }

    public Vertex first() {
        return getFirst();
    }

    public char theChar() {
        return wordnet.charAt(rowNum);
    }

    public int getRowNum() {
        return rowNum;
    }

    public boolean isNotEmpty() {
        return size != 0;
    }

    void releaseInto(Wordnet owner) {
        for (int i = 0; i < size; i++) {
            owner.recycle(nodes[i]);
            nodes[i] = null;
        }
        size = 0;
    }

    private int indexOf(int length) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midLength = nodes[mid].length;
            if (midLength == length) {
                return mid;
            }
            if (midLength < length) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -low - 1;
    }
}
