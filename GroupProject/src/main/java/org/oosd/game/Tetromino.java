package org.oosd.game;

/**
 * Enum representing all 7 standard Tetris tetromino shapes.
 * Each tetromino:
 *  - Has a set of rotation states (matrices of 0s and 1s).
 *  - Has a colorId (1–7) used for rendering in the UI.
 *
 * The shape is represented as a 2D grid for each rotation.
 *   0 = empty cell
 *   1 = filled cell (part of the tetromino)
 */
public enum Tetromino {
    // I-piece: straight line (1x4 or 4x1)
    I(new int[][][]{
            { {0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0} },    // Rotation 0
            { {0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0} },    // Rotation 1
            { {0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0} },    // Rotation 2
            { {0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0} }     // Rotation 3
    }, 1),
    // O-piece: square (2x2 block)
    O(new int[][][]{
            { {0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0} },
            { {0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0} },
            { {0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0} },
            { {0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0} }
    }, 2),
    // T-piece: T-shaped block
    T(new int[][][]{
            { {0,1,0},{1,1,1},{0,0,0} },
            { {0,1,0},{0,1,1},{0,1,0} },
            { {0,0,0},{1,1,1},{0,1,0} },
            { {0,1,0},{1,1,0},{0,1,0} }
    }, 3),
    // S-piece: zigzag (S shape)
    S(new int[][][]{
            { {0,1,1},{1,1,0},{0,0,0} },
            { {0,1,0},{0,1,1},{0,0,1} },
            { {0,0,0},{0,1,1},{1,1,0} },
            { {1,0,0},{1,1,0},{0,1,0} }
    }, 4),
    // Z-piece: reverse zigzag
    Z(new int[][][]{
            { {1,1,0},{0,1,1},{0,0,0} },
            { {0,0,1},{0,1,1},{0,1,0} },
            { {0,0,0},{1,1,0},{0,1,1} },
            { {0,1,0},{1,1,0},{1,0,0} }
    }, 5),
    // J-piece: L-shape with short side on the left
    J(new int[][][]{
            { {1,0,0},{1,1,1},{0,0,0} },
            { {0,1,1},{0,1,0},{0,1,0} },
            { {0,0,0},{1,1,1},{0,0,1} },
            { {0,1,0},{0,1,0},{1,1,0} }
    }, 6),
    // L-piece: L-shape with short side on the right
    L(new int[][][]{
            { {0,0,1},{1,1,1},{0,0,0} },
            { {0,1,0},{0,1,0},{0,1,1} },
            { {0,0,0},{1,1,1},{1,0,0} },
            { {1,1,0},{0,1,0},{0,1,0} }
    }, 7);

    // Array of all rotations for the tetromino
    private final int[][][] rotations;

    // Color identifier (1–7) for rendering in the UI
    private final int colorId;

    /**
     * Constructor for Tetromino enum constants.
     * @param rotations All rotation states for this piece.
     * @param colorId   The color code for this piece.
     */
    Tetromino(int[][][] rotations, int colorId) {
        this.rotations = rotations;
        this.colorId = colorId;
    }

    /**
     * Get the shape matrix for a given rotation index.
     * Rotation wraps around if the index exceeds the rotation count.
     */
    public int[][] shape(int rot) {
        return rotations[rot % rotations.length];
    }

    /**
     * Get the number of rotation states this piece has.
     */
    public int rotationCount() {
        return rotations.length;
    }

    /**
     * Get the color ID used for rendering this piece in the UI.
     */
    public int colorId() {
        return colorId;
    }
}
