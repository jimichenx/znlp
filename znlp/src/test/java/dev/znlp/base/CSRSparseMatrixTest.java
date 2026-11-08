package dev.znlp.base;

import org.junit.jupiter.api.Test;
import dev.znlp.base.common.CSRSparseMatrix;
import dev.znlp.base.common.TreeBasedTable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for CSRSparseMatrix class.
 */
public class CSRSparseMatrixTest {

    @Test
    public void testConstructionAndGet() {
        // Create a TreeBasedTable with test data
        TreeBasedTable<Integer, Integer, Integer> table = new TreeBasedTable<>();
        table.put(2, 0, 6);
        table.put(3, 2, 4);
        table.put(0, 0, 5);
        table.put(0, 3, 2);
        table.put(4, 1, 2);
        table.put(4, 4, 9);

        // Create CSRSparseMatrix from the table
        CSRSparseMatrix csr = new CSRSparseMatrix(table, 5);

        // Test get method for existing values
        assertEquals(6, csr.get(2, 0));
        assertEquals(4, csr.get(3, 2));
        assertEquals(5, csr.get(0, 0));
        assertEquals(2, csr.get(0, 3));
        assertEquals(2, csr.get(4, 1));
        assertEquals(9, csr.get(4, 4));

        // Test get method for non-existing values (should return 0)
        assertEquals(0, csr.get(1, 0));
        assertEquals(0, csr.get(0, 1));
        assertEquals(0, csr.get(4, 3)); // Row within bounds but no value
        assertEquals(0, csr.get(-1, 0)); // Negative index
        assertEquals(0, csr.get(0, -1)); // Negative index
    }

    @Test
    public void testConstructorWithArrays() {
        // Test construction with arrays
        int[] rowOffset = {0, 2, 2, 3, 4, 6};
        int[] columnIndices = {0, 3, 0, 2, 1, 4};
        int[] values = {5, 2, 6, 4, 2, 9};

        CSRSparseMatrix csr = new CSRSparseMatrix(rowOffset, columnIndices, values);

        // Verify getters
        assertArrayEquals(rowOffset, csr.getRowOffset());
        assertArrayEquals(columnIndices, csr.getColumnIndices());
        assertArrayEquals(values, csr.getValues());
    }

    @Test
    public void testEmptyTable() {
        // Test with empty table
        TreeBasedTable<Integer, Integer, Integer> table = new TreeBasedTable<>();
        CSRSparseMatrix csr = new CSRSparseMatrix(table, 5);

        // All gets should return 0
        assertEquals(0, csr.get(0, 0));
        assertEquals(0, csr.get(4, 4));
    }

    @Test
    public void testSingleElement() {
        // Test with single element
        TreeBasedTable<Integer, Integer, Integer> table = new TreeBasedTable<>();
        table.put(2, 3, 42);
        CSRSparseMatrix csr = new CSRSparseMatrix(table, 5);

        assertEquals(42, csr.get(2, 3));
        assertEquals(0, csr.get(0, 0));
        assertEquals(0, csr.get(4, 4));
    }

    @Test
    public void testOutOfBoundsRow() {
        // Test accessing row indices that are out of bounds
        TreeBasedTable<Integer, Integer, Integer> table = new TreeBasedTable<>();
        table.put(2, 0, 6);
        table.put(3, 2, 4);
        CSRSparseMatrix csr = new CSRSparseMatrix(table, 5);

        // Row 5 is out of bounds (valid rows are 0-4)
        assertEquals(0, csr.get(5, 0));
        assertEquals(0, csr.get(10, 0));
        assertEquals(0, csr.get(100, 100));
    }
}