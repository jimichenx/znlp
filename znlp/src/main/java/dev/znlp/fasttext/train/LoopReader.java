package dev.znlp.fasttext.train;

import dev.znlp.base.common.AbstractIterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * 循环读取器：训练多 epoch 时在数据源末尾自动从头再读。
 */
public class LoopReader extends AbstractIterator<SampleLine> implements Closeable {

    private final Iterable<SampleLine> list;
    private Iterator<SampleLine> iterator;
    private boolean first = true;

    public LoopReader(Iterable<SampleLine> list) {
        this.list = list;
        this.iterator = list.iterator();
    }

    @Override
    protected void computeNext() {
        if (iterator.hasNext()) {
            setNext(iterator.next());
        } else {
            closeQuietly(iterator);
            if (first) {
                done();
            } else {
                iterator = list.iterator();
                if (iterator.hasNext()) {
                    setNext(iterator.next());
                } else {
                    done();
                }
            }
        }
        if (first) {
            first = false;
        }
    }

    @Override
    public void close() {
        closeQuietly(iterator);
    }

    private static void closeQuietly(Iterator<?> it) {
        if (it instanceof Closeable c) {
            try {
                c.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
