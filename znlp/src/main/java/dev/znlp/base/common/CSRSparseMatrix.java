package dev.znlp.base.common;

import java.io.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Compressed Sparse Row (CSR) Sparse Matrix implementation.
 * <p>
 * This class represents a sparse matrix using the CSR (Compressed Sparse Row) format,
 * which is an efficient way to store matrices that are primarily composed of zero values.
 * </p>
 * <p>
 * The CSR format uses three arrays:
 * <ul>
 *   <li>rowOffset: An array of size (rows + 1) containing the starting index in columnIndices/values for each row</li>
 *   <li>columnIndices: An array containing the column index for each non-zero value</li>
 *   <li>values: An array containing the actual non-zero values</li>
 * </ul>
 * </p>
 * <p>
 * Example:
 * For a matrix with 3 rows and the following non-zero values:
 * <pre>
 * Row 0: column 1 = 5, column 3 = 2
 * Row 1: (no non-zero values)
 * Row 2: column 0 = 6, column 2 = 4
 * </pre>
 * Would be represented as:
 * <pre>
 * rowOffset = [0, 2, 2, 4]
 * columnIndices = [1, 3, 0, 2]
 * values = [5, 2, 6, 4]
 * </pre>
 * </p>
 *
 * @author jimichan
 */
public class CSRSparseMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int[] rowOffset;
    private final int[] columnIndices;
    private final int[] values;

//    private BitSet collBitSet = new BitSet();


    /**
     * Constructs a CSRSparseMatrix with the given arrays.
     *
     * @param rowOffset     An array of size (rows + 1) containing the starting index
     *                      in columnIndices/values for each row
     * @param columnIndices An array containing the column index for each non-zero value
     * @param values        An array containing the actual non-zero values
     */
    public CSRSparseMatrix(int[] rowOffset, int[] columnIndices, int[] values) {
        this.rowOffset = rowOffset;
        this.columnIndices = columnIndices;
        this.values = values;
    }

    /**
     * Returns the row offset array.
     *
     * @return the row offset array
     */
    public int[] getRowOffset() {
        return rowOffset;
    }

    /**
     * Returns the column indices array.
     *
     * @return the column indices array
     */
    public int[] getColumnIndices() {
        return columnIndices;
    }

    /**
     * Returns the values array.
     *
     * @return the values array
     */
    public int[] getValues() {
        return values;
    }


//    public CSRSparseMatrix() {
//        
//    }

    /**
     * Constructs a CSRSparseMatrix from a TreeBasedTable.
     *
     * @param table    A TreeBasedTable containing the non-zero values with row/column keys
     * @param totalRow The total number of rows in the matrix
     */
    public CSRSparseMatrix(TreeBasedTable<Integer, Integer, Integer> table, int totalRow) {
        int size = table.size();
        values = new int[size];
        columnIndices = new int[size];

        this.rowOffset = new int[totalRow + 1];
//        collBitSet = new BitSet(totalRow + 1);
        //rowOffset[0]=0;

        int point = -1;
        int inSize = 0;

        for (Integer rowNum : table.rowKeySet()) {
            Map<Integer, Integer> row = table.row(rowNum);
            inSize += row.size();
            rowOffset[rowNum + 1] = inSize;

            for (Map.Entry<Integer, Integer> entry : row.entrySet()) {
                point++;

//                collBitSet.set(entry.getKey());

                columnIndices[point] = entry.getKey();
                values[point] = entry.getValue();
            }
        }

        int x = 0;
        for (int i = 0; i < this.rowOffset.length; i++) {
            int p = this.rowOffset[i];

            if (p > 0) {
                x = p;
            } else {
                this.rowOffset[i] = x;
            }
        }

    }


    /**
     * Writes the matrix data to an ObjectOutput stream.
     *
     * @param out the ObjectOutput stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        DataInOutputUtils.writeIntArray(this.getColumnIndices(), out);
        DataInOutputUtils.writeIntArray(this.getRowOffset(), out);
        DataInOutputUtils.writeIntArray(this.getValues(), out);
    }

    /**
     * Writes the matrix data to a DataOutputStream stream.
     *
     * @param out the DataOutputStream stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        DataInOutputUtils.writeIntArray(this.getColumnIndices(), out);
        DataInOutputUtils.writeIntArray(this.getRowOffset(), out);
        DataInOutputUtils.writeIntArray(this.getValues(), out);
    }

    /**
     * Reads a CSRSparseMatrix from an ObjectInput stream.
     *
     * @param in the ObjectInput stream to read from
     * @return a new CSRSparseMatrix instance
     * @throws IOException if an I/O error occurs
     */
    public static CSRSparseMatrix readExternal(ObjectInput in) throws IOException {
        int[] columnIndices = DataInOutputUtils.readIntArray(in);
        int[] rowOffset = DataInOutputUtils.readIntArray(in);
        int[] values = DataInOutputUtils.readIntArray(in);
        return new CSRSparseMatrix(rowOffset, columnIndices, values);
    }

    /**
     * Reads a CSRSparseMatrix from a DataInputStream stream.
     *
     * @param in the DataInputStream stream to read from
     * @return a new CSRSparseMatrix instance
     * @throws IOException if an I/O error occurs
     */
    public static CSRSparseMatrix readExternal(DataInputStream in) throws IOException {
        int[] columnIndices = DataInOutputUtils.readIntArray(in);
        int[] rowOffset = DataInOutputUtils.readIntArray(in);
        int[] values = DataInOutputUtils.readIntArray(in);
        return new CSRSparseMatrix(rowOffset, columnIndices, values);
    }

    /**
     * Gets the value at the specified row and column.
     *
     * @param row the row index
     * @param col the column index
     * @return the value at the specified position, or 0 if not found
     */
    public final int get(int row, int col) {

        if (row < 0 || col < 0) {
            return 0;
        }

        // Check if row index is within bounds
        if (row >= rowOffset.length - 1) {
            return 0;
        }

        int off = rowOffset[row];
        int end = rowOffset[row + 1];
        if (off == end) {
            return 0;
        }

//        if (!collBitSet.get(col)) {
//            return 0;
//        }
        //columnIndices

        final int index = Arrays.binarySearch(columnIndices, off, end, col);
        if (index >= 0) {
            return values[index];
        }

        return 0;
    }
}