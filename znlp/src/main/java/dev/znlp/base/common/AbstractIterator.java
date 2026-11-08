package dev.znlp.base.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 抽象迭代器基类，提供基于计算下一个元素的迭代器实现。
 * <p>
 * 该抽象类简化了迭代器的实现，子类只需实现{@link #computeNext()}方法
 * 来计算下一个元素，而不需要手动管理迭代状态。
 * <p>
 * 该实现参考了Kotlin的AbstractIterator设计。
 *
 * @param <T> 迭代元素的类型
 */
public abstract class AbstractIterator<T> implements Iterator<T> {

    /**
     * 迭代器状态枚举
     */
    private enum State {
        /**
         * 尚未计算下一个元素
         */
        NotReady,
        /**
         * 已准备好下一个元素
         */
        Ready,
        /**
         * 迭代已完成
         */
        Done,
        /**
         * 迭代失败
         */
        Failed
    }

    private State state = State.NotReady;
    private T next;

    /**
     * 计算下一个元素。
     * <p>
     * 子类应该实现此方法来计算下一个元素。如果还有下一个元素，
     * 应该调用{@link #setNext(Object)}来设置它；如果没有更多元素，
     * 应该调用{@link #done()}来标记迭代完成。
     */
    protected abstract void computeNext();

    /**
     * 设置下一个元素。
     *
     * @param value 下一个元素的值
     */
    protected void setNext(T value) {
        next = value;
        state = State.Ready;
    }

    /**
     * 标记迭代完成，没有更多元素。
     */
    protected void done() {
        state = State.Done;
    }

    @Override
    public boolean hasNext() {
        if (state == State.Failed) {
            throw new IllegalStateException("Iterator has failed");
        }
        switch (state) {
            case Done:
                return false;
            case Ready:
                return true;
            default:
                return tryToComputeNext();
        }
    }

    private boolean tryToComputeNext() {
        state = State.Failed;
        computeNext();
        return state == State.Ready;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        state = State.NotReady;
        T result = next;
        next = null;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
