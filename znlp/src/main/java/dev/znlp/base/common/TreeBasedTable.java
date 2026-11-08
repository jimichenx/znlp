package dev.znlp.base.common;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A table implementation based on TreeMap for efficient storage and retrieval of
 * row-column-value mappings. This implementation is particularly useful for constructing
 * sparse tables where memory efficiency is important.
 *
 * <p>The table uses a nested TreeMap structure:
 * <ul>
 *   <li>Outer map: row keys mapped to TreeMap of column keys and values</li>
 *   <li>Inner map: column keys mapped to values for a specific row</li>
 * </ul>
 *
 * @param <R> the type of row keys
 * @param <C> the type of column keys
 * @param <V> the type of values
 * @author jimichan
 */
public class TreeBasedTable<R extends Comparable<R>, C extends Comparable<C>, V> {

    private final TreeMap<R, TreeMap<C, V>> map = new TreeMap<>();

    /**
     * Associates the specified value with the specified row and column keys.
     *
     * @param row   the row key
     * @param col   the column key
     * @param value the value to be stored
     */
    public void put(R row, C col, V value) {
        TreeMap<C, V> rowMap = map.computeIfAbsent(row, k -> new TreeMap<>());
        rowMap.put(col, value);
    }

    /**
     * Returns the number of row key / column key / value mappings in the table.
     *
     * @return the number of mappings
     */
    public int size() {
        int count = 0;
        for (TreeMap<C, V> rowMap : map.values()) {
            count += rowMap.size();
        }
        return count;
    }

    /**
     * Checks if the table is empty.
     *
     * @return true if the table contains no mappings, false otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns a set of row keys that have at least one column-value mapping.
     *
     * @return a set of row keys
     */
    public Set<R> rowKeySet() {
        return map.keySet();
    }

    /**
     * Returns the column map for the specified row key.
     *
     * @param row the row key
     * @return the column map for the specified row, or null if the row doesn't exist
     */
    public Map<C, V> row(R row) {
        return map.get(row);
    }
}