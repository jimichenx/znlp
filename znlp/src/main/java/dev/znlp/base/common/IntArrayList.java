package dev.znlp.base.common;

import java.util.Arrays;

/**
 * A dynamic array implementation specifically for storing integers.
 *
 * <p>This class provides a more memory-efficient and faster alternative to {@link java.util.ArrayList} when dealing
 * exclusively with integer values, as it eliminates the need for autoboxing/unboxing operations.
 *
 * <p>Key features:
 * <ul>
 *   <li>Dynamic resizing with amortized constant time complexity for add operations</li>
 *   <li>Direct access to underlying array (via {@link #toArray()}) for efficient processing</li>
 *   <li>Optimized for memory usage by avoiding object overhead</li>
 *   <li>Simplified API tailored for integer operations</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * IntArrayList list = new IntArrayList(10); // Initial capacity of 10
 * list.add(1);
 * list.add(2);
 * list.add(3);
 * int value = list.get(1); // Returns 2
 * int size = list.size(); // Returns 3
 * int[] array = list.toArray(); // Returns [1, 2, 3]
 * </pre>
 *
 * <p>This implementation is thread-unsafe and designed for use in single-threaded environments, particularly
 * optimized for the perceptron model in the NLP base package.
 *
 * @author zaprt
 * @version 1.0
 */
public class IntArrayList {

    /**
     * Default initial capacity for the list when no expected size is specified.
     */
    public static final int DEFAULT_EXPECTED_ELEMENTS = 4;

    /**
     * An empty array singleton used for initialization.
     */
    public static final int[] EMPTY_ARRAY = new int[0];

    /**
     * The underlying array buffer that stores the elements.
     */
    private int[] buffer = EMPTY_ARRAY;

    /**
     * The number of elements currently stored in the list.
     */
    private int elementsCount;

    /**
     * Constructs an empty IntArrayList with the default initial capacity.
     */
    public IntArrayList() {
        this(DEFAULT_EXPECTED_ELEMENTS);
    }

    /**
     * Constructs an empty IntArrayList with the specified initial capacity.
     *
     * @param expectedElements the initial capacity of the list
     * @throws IllegalArgumentException if expectedElements is negative
     */
    public IntArrayList(int expectedElements) {
        if (expectedElements < 0) {
            throw new IllegalArgumentException("Expected elements cannot be negative: " + expectedElements);
        }
        ensureCapacity(expectedElements);
    }

    /**
     * Adds the specified integer to the end of the list.
     *
     * @param element the integer to be added to the list
     */
    public void add(int element) {
        ensureBufferSpace(1);
        buffer[elementsCount++] = element;
    }

    /**
     * Adds all elements from the specified array to the end of the list.
     *
     * @param elements the array containing elements to be added to the list
     * @throws NullPointerException if the specified array is null
     */
    public void addAll(int[] elements) {
        if (elements == null) {
            throw new NullPointerException("Elements array cannot be null");
        }
        ensureBufferSpace(elements.length);
        System.arraycopy(elements, 0, buffer, elementsCount, elements.length);
        elementsCount += elements.length;
    }

    /**
     * Adds all elements from another IntArrayList to the end of this list.
     *
     * @param other the IntArrayList containing elements to be added to this list
     * @throws NullPointerException if the specified IntArrayList is null
     */
    public void addAll(IntArrayList other) {
        if (other == null) {
            throw new NullPointerException("Other IntArrayList cannot be null");
        }
        ensureBufferSpace(other.size());
        System.arraycopy(other.buffer, 0, buffer, elementsCount, other.size());
        elementsCount += other.size();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public int get(int index) {
        checkIndex(index);
        return buffer[index];
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public int set(int index, int element) {
        checkIndex(index);
        int oldValue = buffer[index];
        buffer[index] = element;
        return oldValue;
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list, or -1 if this list does not contain the element.
     *
     * @param element element to search for
     * @return the index of the first occurrence of the specified element in this list, or -1 if this list does not contain the element
     */
    public int indexOf(int element) {
        for (int i = 0; i < elementsCount; i++) {
            if (buffer[i] == element) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes the element at the specified position in this list. Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size())
     */
    public int remove(int index) {
        checkIndex(index);
        int oldValue = buffer[index];
        int numMoved = elementsCount - index - 1;
        if (numMoved > 0) {
            System.arraycopy(buffer, index + 1, buffer, index, numMoved);
        }
        elementsCount--;
        // Optional: Clear the last element to help with garbage collection
        buffer[elementsCount] = 0;
        return oldValue;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return elementsCount;
    }

    /**
     * Returns true if this list contains no elements.
     *
     * @return true if this list contains no elements
     */
    public boolean isEmpty() {
        return elementsCount == 0;
    }

    /**
     * Removes all of the elements from this list. The list will be empty after this call returns.
     */
    public void clear() {
        elementsCount = 0;
        // Optional: Reduce capacity to default if buffer is too large
        if (buffer.length > DEFAULT_EXPECTED_ELEMENTS) {
            buffer = EMPTY_ARRAY;
        }
    }

    /**
     * 只把元素个数归零，保留底层数组，供特征序列复用。
     */
    public void reset() {
        elementsCount = 0;
    }

    /**
     * Returns an array containing all of the elements in this list in proper sequence (from first to last element).
     * 
     * @return an array containing all of the elements in this list in proper sequence
     */
    public int[] toArray() {
        return Arrays.copyOf(buffer, elementsCount);
    }
    
    /**
     * Returns the underlying buffer array. This method is provided for performance-critical operations
     * where direct access to the buffer is needed. 
     * 
     * <p><strong>Warning:</strong> This method returns the actual buffer array, not a copy. 
     * Modifying the array directly may cause unexpected behavior if not used carefully.
     * 
     * @return the underlying buffer array
     */
    public int[] getBuffer() {
        return buffer;
    }

    /**
     * Ensures that the underlying buffer has at least the specified capacity.
     *
     * @param expectedElements the minimum capacity to ensure
     */
    private void ensureCapacity(int expectedElements) {
        if (expectedElements > buffer.length) {
            int newSize = Math.max(buffer.length * 2, expectedElements);
            buffer = Arrays.copyOf(buffer, newSize);
        }
    }

    /**
     * Ensures there is enough space in the buffer to accommodate the specified number of additional elements.
     *
     * @param expectedAdditions the number of additional elements to accommodate
     */
    protected void ensureBufferSpace(int expectedAdditions) {
        int requiredCapacity = elementsCount + expectedAdditions;
        if (requiredCapacity > buffer.length) {
            int newSize = Math.max(buffer.length * 2, requiredCapacity);
            buffer = Arrays.copyOf(buffer, newSize);
        }
    }

    /**
     * Checks if the specified index is within the valid range for this list.
     *
     * @param index the index to check
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    private void checkIndex(int index) {
        if (index < 0 || index >= elementsCount) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds [0, " + elementsCount + ")");
        }
    }
}
