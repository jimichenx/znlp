package dev.znlp.base.blas;

public abstract class BasicDenseMatrix implements DenseMatrix {

    private static final long serialVersionUID = 1L;

    protected final int row;
    protected final int col;

    public BasicDenseMatrix(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public float dotRow(Vector vec, int i) {
        return this.get(i).prod(vec);
    }

    @Override
    public void addVectorToRow(Vector vector, int i, float a) {
        this.get(i).plusAssign(a, vector);
    }

    @Override
    public void addRowToVector(Vector target, int i, double a) {
        Vector rowVec = this.get(i);
        if (a == 1.0) { // optimization
             for (int j = 0; j < col; j++) {
                 target.set(j, target.get(j) + rowVec.get(j));
             }
        } else {
             float af = (float) a;
             for (int j = 0; j < col; j++) {
                 target.set(j, target.get(j) + rowVec.get(j) * af);
             }
        }
    }

    @Override
    public void multiplyRow(Vector nums, int ib, int ie_) {
        int ie = (ie_ == -1) ? row : ie_;
        if (ie > nums.length()) throw new IllegalArgumentException("ie > nums.length()");

        for (int i = ib; i < ie; i++) {
            float n = nums.get(i - ib);
            if (n != 0f) {
                this.get(i).timesAssign(n);
            }
        }
    }

    @Override
    public void divideRow(Vector denoms, int ib, int ie_) {
        int ie = (ie_ == -1) ? row : ie_;
        if (ie > denoms.length()) throw new IllegalArgumentException("ie > denoms.length()");

        for (int i = ib; i < ie; i++) {
            float n = denoms.get(i - ib);
            if (n != 0f) {
                this.get(i).divAssign(n);
            }
        }
    }

    @Override
    public float l2NormRow(int i) {
        return this.get(i).norm2();
    }

    @Override
    public void l2NormRow(Vector norms) {
        if (norms.length() != row) throw new IllegalArgumentException("norms.length() != row");
        for (int i = 0; i < row; i++) {
            norms.set(i, l2NormRow(i));
        }
    }
}
