package dev.znlp.base.common;

/**
 * Zero-copy CharSequence implementation for efficient string manipulation in NLP tasks.
 * <p>
 * This class wraps a char array and provides a view-based approach to substring operations,
 * avoiding array copying for better performance in text segmentation scenarios.
 * <p>
 * The underlying char array is mutable, allowing in-place character modifications.
 * <p>
 * Example usage:
 * <pre>
 * String input = "你好世界";
 * ZeroCopyString zcs = new ZeroCopyString(input);
 * CharSequence sub = zcs.subSequence(0, 2); // No array copy, just view
 * </pre>
 *
 * @author zaprt
 */
public final class ZeroCopyString implements CharSequence {

    /**
     * The underlying character array (mutable)
     */
    private final char[] chars;

    /**
     * Start offset (inclusive) in the char array
     */
    private final int offset;

    /**
     * Length of this character sequence
     */
    private final int length;

    /**
     * Constructs a ZeroCopyString from a String.
     *
     * @param str the input string
     */
    public ZeroCopyString(String str) {
        this(str.toCharArray());
    }

    /**
     * Constructs a ZeroCopyString from a char array.
     * The array is directly used (not copied), so modifications will be visible.
     *
     * @param chars the character array
     */
    public ZeroCopyString(char[] chars) {
        this(chars, 0, chars.length);
    }

    /**
     * Constructs a ZeroCopyString from a char array with specified offset and length.
     *
     * @param chars  the character array
     * @param offset the start offset (inclusive)
     * @param length the length of the sequence
     * @throws IndexOutOfBoundsException if offset or length is invalid
     */
    public ZeroCopyString(char[] chars, int offset, int length) {
        if (chars == null) {
            throw new NullPointerException("char array cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > chars.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid offset(" + offset + ") or length(" + length + ") for array size " + chars.length);
        }
        this.chars = chars;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }
        return chars[offset + index];
    }

    /**
     * Returns a zero-copy view of the specified subsequence.
     * No array copying is performed.
     *
     * @param start the start index (inclusive)
     * @param end   the end index (exclusive)
     * @return a new ZeroCopyString view
     */
    @Override
    public ZeroCopyString subSequence(int start, int end) {
        if (start < 0 || end > length || start > end) {
            throw new IndexOutOfBoundsException(
                    "start: " + start + ", end: " + end + ", length: " + length);
        }
        return new ZeroCopyString(chars, offset + start, end - start);
    }

    /**
     * Returns a zero-copy substring view.
     * Equivalent to subSequence(start, end).
     *
     * @param start the start index (inclusive)
     * @param end   the end index (exclusive)
     * @return a new ZeroCopyString view
     */
    public ZeroCopyString substring(int start, int end) {
        return subSequence(start, end);
    }

    /**
     * Returns a zero-copy substring view from start to the end.
     *
     * @param start the start index (inclusive)
     * @return a new ZeroCopyString view
     */
    public ZeroCopyString substring(int start) {
        return substring(start, length);
    }

    /**
     * Sets the character at the specified index.
     * This modifies the underlying char array.
     *
     * @param index the index
     * @param ch    the new character
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public void setCharAt(int index, char ch) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }
        chars[offset + index] = ch;
    }

    /**
     * Returns the underlying char array.
     * Note: Modifications to this array will affect all views.
     *
     * @return the char array
     */
    public char[] getChars() {
        return chars;
    }

    /**
     * Returns the start offset in the underlying array.
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Copies the characters to a new char array.
     *
     * @return a new char array containing only the characters in this sequence
     */
    public char[] toCharArray() {
        char[] result = new char[length];
        System.arraycopy(chars, offset, result, 0, length);
        return result;
    }

    /**
     * Converts this ZeroCopyString to a standard String.
     * This creates a new String object.
     *
     * @return a new String
     */
    @Override
    public String toString() {
        return new String(chars, offset, length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CharSequence other)) {
            return false;
        }
        if (length != other.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (charAt(i) != other.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < length; i++) {
            h = 31 * h + charAt(i);
        }
        return h;
    }
}
