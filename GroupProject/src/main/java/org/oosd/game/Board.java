package org.oosd.game;

import org.oosd.core.GameConfig;

/**
 * Configurable Tetris board/grid.
 * Cell values:
 *  - 0  = empty
 *  - >0 = filled, value corresponds to tetromino color/type ID
 */
public class Board {

    /** Sensible defaults; real size comes from GameConfig unless ctor says otherwise. */
    public static final int DEFAULT_ROWS = 20;
    public static final int DEFAULT_COLS = 10;

    private final int rows;
    private final int cols;
    private final int[][] cells;

    /** Use current GameConfig rows/cols. */
    public Board() {
        this(GameConfig.get().rows(), GameConfig.get().cols());
    }

    /** Explicit size (useful for tests or future multiplayer boards). */
    public Board(int rows, int cols) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.cells = new int[this.rows][this.cols];
    }

    // --- dimensions ---
    public int rows() { return rows; }
    public int cols() { return cols; }

    // --- cell access ---
    public int get(int r, int c) { return cells[r][c]; }
    public void set(int r, int c, int v) { cells[r][c] = v; }

    public boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public boolean empty(int r, int c) {
        return inBounds(r, c) && cells[r][c] == 0;
    }

    /** Detects & clears all full rows (bottom-up). @return number of cleared rows. */
    public int clearFullRows() {
        int cleared = 0;
        for (int r = rows - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < cols; c++) {
                if (cells[r][c] == 0) { full = false; break; }
            }
            if (full) {
                cleared++;
                // shift rows down
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(cells[rr - 1], 0, cells[rr], 0, cols);
                }
                // top row -> empty
                for (int c = 0; c < cols; c++) cells[0][c] = 0;
                r++; // re-check same index after shift
            }
        }
        return cleared;
    }
}
