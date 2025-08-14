package org.oosd.game;

/**
 * Represents the 10x20 Tetris board/grid.
 *
 * Board cells:
 *  - 0  = empty cell
 *  - >0 = filled cell, value corresponds to the tetromino's color/type ID
 */
public class Board {
    // Dimensions of the standard Tetris board
    public static final int ROWS = 20;
    public static final int COLS = 10;

    // 2D grid storing the state of the board
    // cells[r][c] = 0 (empty) or >0 (filled with a tetromino colorId)
    private final int[][] cells = new int[ROWS][COLS];

    /**
     * Returns the value of the cell at row r, column c.
     */
    public int get(int r, int c) {
        return cells[r][c];
    }

    /**
     * Sets the cell at (r, c) to value v.
     * @param r Row index
     * @param c Column index
     * @param v New cell value (0 = empty, >0 = filled)
     */
    public void set(int r, int c, int v) {
        cells[r][c] = v;
    }

    /**
     * Checks whether the given coordinates are inside the board.
     * @param r Row index
     * @param c Column index
     * @return true if inside the board boundaries, false otherwise
     */
    public boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    /**
     * Checks whether the cell at (r, c) is empty.
     * @return true if in bounds and cell value is 0, false otherwise
     */
    public boolean empty(int r, int c) {
        return inBounds(r, c) && cells[r][c] == 0;
    }

    /**
     * Detects and clears all full rows.
     *
     * - A row is "full" if every cell is non-zero.
     * - When a row is cleared, all rows above it shift down by one.
     * - The topmost row becomes empty after a shift.
     *
     * @return the number of rows cleared
     */
    public int clearFullRows() {
        int cleared = 0;

        // Start checking from the bottom row upwards
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;

            // Check if this row is full
            for (int c = 0; c < COLS; c++) {
                if (cells[r][c] == 0) {
                    full = false;
                    break;
                }
            }

            if (full) {
                cleared++;

                // Shift all rows above down by one
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(cells[rr - 1], 0, cells[rr], 0, COLS);
                }

                // Set the top row to empty
                for (int c = 0; c < COLS; c++)
                    cells[0][c] = 0;

                // After shifting, check the same row index again
                // (since it now contains the row that was above)
                r++;
            }
        }
        return cleared;
    }
}
