package dev.znlp.base.dat;

import dev.znlp.base.common.DataInputWrapper;
import dev.znlp.base.common.DataOutputWrapper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;


/**
 * A memory-efficient map implementation based on Double Array Trie that maps String keys to int values.
 *
 * <p>This implementation uses primitive int arrays instead of Integer objects to reduce memory overhead,
 * making it suitable for large-scale dictionaries where memory efficiency is crucial.
 *
 * <p>Features:
 * <ul>
 *   <li>Fast exact matching using Double Array Trie algorithm</li>
 *   <li>Memory efficient with primitive int storage</li>
 *   <li>Supports prefix matching with matchers</li>
 *   <li>Thread-safe for read operations after construction</li>
 * </ul>
 *
 * @author jimichan
 */
public class StringIntDATMap {

    private final int[] values;

    private final DoubleArrayTrie dat;

    /**
     * Constructs a StringIntDATMap with the given DoubleArrayTrie and int values array.
     *
     * @param dat    the DoubleArrayTrie instance
     * @param values the int values array corresponding to the keys in the trie
     */
    public StringIntDATMap(DoubleArrayTrie dat, int[] values) {
        this.values = values;
        this.dat = dat;
    }

    /**
     * Constructs a StringIntDATMap from sorted keys and corresponding int values.
     *
     * @param keys   sorted list of String keys (must be in dictionary order)
     * @param values int values corresponding to the keys
     */
    public StringIntDATMap(ArrayList<String> keys, int[] values) {
        this(DoubleArrayTrie.builder().build(keys), values);
    }

    /**
     * Constructs a StringIntDATMap from a TreeMap.
     *
     * @param map the TreeMap containing String keys and Integer values
     */
    public StringIntDATMap(TreeMap<String, Integer> map) {
        ArrayList<String> keys = new ArrayList<>(map.keySet());
        int[] values = new int[map.size()];
        Collection<Integer> values1 = map.values();
        int c = 0;
        for (Integer integer : values1) {
            values[c++] = integer.intValue();
        }

        this.dat = DoubleArrayTrie.builder().build(keys);
        this.values = values;
    }

    /**
     * Creates a matcher for finding string patterns in the given text starting from the specified offset.
     *
     * @param text   the text to search in
     * @param offset the starting offset in the text
     * @return a DATMapMatcherInt for finding matches
     */
    public DATMapMatcherInt match(String text, int offset) {
        return new DATMapMatcherInt(dat.matcher(text, offset));
    }

    /**
     * Creates a long matcher for finding string patterns in the given char array starting from the specified offset.
     * This matcher is optimized for long texts.
     *
     * @param text   the char array to search in
     * @param offset the starting offset in the text
     * @return a DATMapLongMatcherInt for finding matches
     */
    public DATMapLongMatcherInt matchLong(char[] text, int offset) {
        return new DATMapLongMatcherInt(dat.matcherLong(text, offset));
    }

    public DATMapLongMatcherInt matchLong(char[] text, int offset, int end) {
        return new DATMapLongMatcherInt(dat.matcherLong(text, offset, end));
    }


    /**
     * Creates a long matcher for finding string patterns in the given text starting from the specified offset.
     * This matcher is optimized for long texts.
     *
     * @param text   the text to search in
     * @param offset the starting offset in the text
     * @return a DATMapLongMatcherInt for finding matches
     */
    public DATMapLongMatcherInt matchLong(String text, int offset) {
        return new DATMapLongMatcherInt(dat.matcherLong(text, offset));
    }

    /**
     * Creates a matcher for finding string patterns in the given char array starting from the specified offset.
     *
     * @param text   the char array to search in
     * @param offset the starting offset in the text
     * @return a DATMapMatcherInt for finding matches
     */
    public DATMapMatcherInt match(char[] text, int offset) {
        return new DATMapMatcherInt(dat.matcher(text, offset));
    }

    public DATMapMatcherInt match(char[] text, int offset, int end) {
        return new DATMapMatcherInt(dat.matcher(text, offset, end));
    }

    /**
     * Matcher for finding string patterns in text and retrieving their associated int values.
     * This matcher works with String or char[] inputs.
     */
    public class DATMapMatcherInt {

        private final DATMatcher datMater;

        /**
         * Constructs a DATMapMatcherInt with the given DATMatcher.
         *
         * @param datMater the underlying DATMatcher
         */
        public DATMapMatcherInt(DATMatcher datMater) {
            this.datMater = datMater;
        }

        /**
         * Advances to the next match.
         *
         * @return true if there is a next match, false otherwise
         */
        public boolean next() {
            return datMater.next();
        }

        /**
         * Gets the beginning position of the current match.
         *
         * @return the beginning position
         */
        public int getBegin() {
            return datMater.getBegin();
        }

        /**
         * Gets the length of the current match.
         *
         * @return the length of the match
         */
        public int getLength() {
            return datMater.getLength();
        }

        /**
         * Gets the int value associated with the current match.
         *
         * @return the int value, or -1 if no match
         */
        public int getValue() {
            int index = datMater.getIndex();
            if (index == -1) {
                return -1;
            } else {
                return values[index];
            }
        }

        /**
         * Gets the index of the current match.
         *
         * @return the index, or -1 if no match
         */
        public int getIndex() {
            return datMater.getIndex();
        }
    }


    /**
     * Matcher for finding string patterns in long text and retrieving their associated int values.
     * This matcher is optimized for long texts.
     */
    public class DATMapLongMatcherInt {

        private final DATLongMatcher datMater;

        /**
         * Constructs a DATMapLongMatcherInt with the given DATLongMatcher.
         *
         * @param datMater the underlying DATLongMatcher
         */
        public DATMapLongMatcherInt(DATLongMatcher datMater) {
            this.datMater = datMater;
        }

        /**
         * Advances to the next match.
         *
         * @return true if there is a next match, false otherwise
         */
        public boolean next() {
            return datMater.next();
        }

        /**
         * Gets the beginning position of the current match.
         *
         * @return the beginning position
         */
        public int getBegin() {
            return datMater.getBegin();
        }

        /**
         * Gets the length of the current match.
         *
         * @return the length of the match
         */
        public int getLength() {
            return datMater.getLength();
        }

        /**
         * Gets the int value associated with the current match.
         *
         * @return the int value, or -1 if no match
         */
        public int getValue() {
            int index = datMater.getIndex();
            if (index == -1) {
                return -1;
            } else {
                return values[index];
            }
        }

        /**
         * Gets the index of the current match.
         *
         * @return the index, or -1 if no match
         */
        public int getIndex() {
            return datMater.getIndex();
        }
    }


    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings
     */
    public int size() {
        return values.length;
    }

    /**
     * Returns the index of the specified key in the trie, or -1 if not found.
     *
     * @param key the key to search for
     * @return the index of the key, or -1 if not found
     */
    public int indexOf(CharSequence key) {
        return dat.indexOf(key, 0, 0, 0);
    }

    /**
     * Returns the index of the specified key in the trie, or -1 if not found.
     *
     * @param key     the key to search for
     * @param pos     the starting position in the key
     * @param len     the length of the key segment to match
     * @param nodePos the starting node position in the trie
     * @return the index of the key, or -1 if not found
     */
    public int indexOf(CharSequence key, int pos, int len, int nodePos) {
        return dat.indexOf(key, pos, len, nodePos);
    }

    /**
     * Returns the index of the specified key in the trie, or -1 if not found.
     *
     * @param chars the char array containing the key
     * @param pos   the starting position in the char array
     * @param len   the length of the key
     * @return the index of the key, or -1 if not found
     */
    public int indexOf(char[] chars, int pos, int len) {
        return dat.indexOf(chars, pos, len, 0);
    }

    /**
     * Returns the index of the specified key in the trie, or -1 if not found.
     *
     * @param keyChars the char array containing the key
     * @param pos      the starting position in the char array
     * @param len      the length of the key
     * @param nodePos  the starting node position in the trie
     * @return the index of the key, or -1 if not found
     */
    public int indexOf(char[] keyChars, int pos, int len, int nodePos) {
        return dat.indexOf(keyChars, pos, len, nodePos);
    }

    /**
     * Returns the index of the specified character key in the trie, or -1 if not found.
     *
     * @param ch the character key to search for
     * @return the index of the character key, or -1 if not found
     */
    public int indexOf(char ch) {
        return dat.indexOf(ch);
    }

    // ======================= 虚拟拼接查询（零拷贝优化） =======================

    /**
     * Returns the index of the concatenated key (part1 + separator + part2) in the trie, or -1 if not found.
     * <p>
     * This method avoids creating a new String object by performing virtual concatenation query,
     * which is optimized for BiGram and similar scenarios that require frequent combined key lookups.
     * </p>
     *
     * @param part1     the first part of the key
     * @param separator the separator character
     * @param part2     the second part of the key
     * @return the index of the concatenated key, or -1 if not found
     */
    public int indexOfConcat(CharSequence part1, char separator, CharSequence part2) {
        return dat.indexOfConcat(part1, separator, part2);
    }

    /**
     * Returns the index of the concatenated key in the trie, or -1 if not found.
     * <p>
     * This method supports char array inputs with position and length parameters,
     * suitable for zero-copy scenarios like word segmentation.
     * </p>
     *
     * @param chars1    the first part char array
     * @param pos1      the starting position of the first part
     * @param len1      the length of the first part
     * @param separator the separator character
     * @param chars2    the second part char array
     * @param pos2      the starting position of the second part
     * @param len2      the length of the second part
     * @return the index of the concatenated key, or -1 if not found
     */
    public int indexOfConcat(char[] chars1, int pos1, int len1, char separator, char[] chars2, int pos2, int len2) {
        return dat.indexOfConcat(chars1, pos1, len1, separator, chars2, pos2, len2);
    }

    /**
     * Returns the int value associated with the concatenated key (part1 + separator + part2), or -1 if not found.
     * <p>
     * This method avoids creating a new String object, optimized for BiGram lookups.
     * </p>
     *
     * @param part1     the first part of the key
     * @param separator the separator character
     * @param part2     the second part of the key
     * @return the value to which the concatenated key is mapped, or -1 if not found
     */
    public int getConcat(CharSequence part1, char separator, CharSequence part2) {
        int index = indexOfConcat(part1, separator, part2);
        if (index >= 0) {
            return getValueAt(index);
        }
        return -1;
    }

    /**
     * Returns the int value associated with the concatenated key, or -1 if not found.
     * <p>
     * This method supports char array inputs with position and length parameters.
     * </p>
     *
     * @param chars1    the first part char array
     * @param pos1      the starting position of the first part
     * @param len1      the length of the first part
     * @param separator the separator character
     * @param chars2    the second part char array
     * @param pos2      the starting position of the second part
     * @param len2      the length of the second part
     * @return the value to which the concatenated key is mapped, or -1 if not found
     */
    public int getConcat(char[] chars1, int pos1, int len1, char separator, char[] chars2, int pos2, int len2) {
        int index = indexOfConcat(chars1, pos1, len1, separator, chars2, pos2, len2);
        if (index >= 0) {
            return getValueAt(index);
        }
        return -1;
    }


    /**
     * Returns the int value associated with the specified key, or -1 if not found.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or -1 if not found
     */
    public int get(CharSequence key) {
        int index = indexOf(key);
        if (index >= 0) {
            return getValueAt(index);
        }
        return -1;
    }

    /**
     * Returns the int value associated with the specified key segment, or -1 if not found.
     *
     * @param key    the key containing the segment
     * @param offset the starting offset of the segment
     * @param length the length of the segment
     * @return the value to which the specified key segment is mapped, or -1 if not found
     */
    public int get(CharSequence key, int offset, int length) {
        int index = indexOf(key, offset, length, 0);
        if (index >= 0) {
            return getValueAt(index);
        }

        return -1;
    }

    /**
     * Returns the int value associated with the specified key, or -1 if not found.
     *
     * @param key the char array key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or -1 if not found
     */
    public int get(char[] key) {
        int index = indexOf(key, 0, key.length, 0);
        if (index >= 0) {
            return getValueAt(index);
        }

        return -1;
    }

    /**
     * Returns the int value associated with the specified key segment, or -1 if not found.
     *
     * @param key    the char array key containing the segment
     * @param offset the starting offset of the segment
     * @param len    the length of the segment
     * @return the value to which the specified key segment is mapped, or -1 if not found
     */
    public int get(char[] key, int offset, int len) {

        int index = indexOf(key, offset, len, 0);
        if (index >= 0) {
            return getValueAt(index);
        }
        return -1;
    }

    /**
     * Returns the int value at the specified index.
     *
     * @param index the index of the value to return
     * @return the int value at the specified index
     */
    public int getValueAt(int index) {
        return values[index];
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    public boolean containsKey(String key) {
        return indexOf(key) >= 0;
    }

    /**
     * Returns true if this map contains a mapping for the specified character key.
     *
     * @param key the character key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified character key
     */
    public boolean containsKey(char key) {
        return indexOf(key) >= 0;
    }

    /**
     * Updates the value associated with the specified key.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return true if the update was successful, false if the key was not found
     */
    public boolean set(String key, int value) {
        int index = indexOf(key);
        if (index >= 0) {
            values[index] = value;
            return true;
        }
        return false;
    }

    /**
     * Returns the int value at the specified index.
     * <p>Note: No parameter validation is performed for efficiency.</p>
     *
     * @param index the index of the value to return
     * @return the int value at the specified index
     */
    public int get(int index) {
        return values[index];
    }

    /**
     * Writes this StringIntDATMap to a DataOutput stream.
     * <p>
     * The serialization format is:
     * <ol>
     *   <li>DoubleArrayTrie (via dat.write())</li>
     *   <li>int[] values array (via DataOutputWrapper.writeIntArray())</li>
     * </ol>
     *
     * @param out the DataOutput stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void write(DataOutput out) throws IOException {
        DataOutputWrapper wrapper = new DataOutputWrapper(out);
        // 写入 DoubleArrayTrie
        dat.write(out);
        // 写入 values 数组
        wrapper.writeIntArray(values);
    }

    /**
     * Reads a StringIntDATMap from a DataInput stream.
     * <p>
     * The deserialization format matches the write format:
     * <ol>
     *   <li>DoubleArrayTrie (via DoubleArrayTrie.read())</li>
     *   <li>int[] values array (via DataInputWrapper.readIntArray())</li>
     * </ol>
     *
     * @param in the DataInput stream to read from
     * @return a new StringIntDATMap instance
     * @throws IOException if an I/O error occurs
     */
    public static StringIntDATMap read(DataInput in) throws IOException {
        DataInputWrapper wrapper = new DataInputWrapper(in);
        // 读取 DoubleArrayTrie
        DoubleArrayTrie dat = DoubleArrayTrie.read(in);
        // 读取 values 数组
        int[] values = wrapper.readIntArray();
        return new StringIntDATMap(dat, values);
    }

}