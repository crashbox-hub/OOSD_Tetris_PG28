package org.oosd.game;

import java.util.Random;

public class ActivePieceEntity extends GameEntity {
    private final Board board;
    private PieceState state;       // holds Tetromino, rotation, top-left row/col (ints)
    private final Random rng = new Random();

    public ActivePieceEntity(Board board, Tetromino next, int spawnCol) {
        super(EntityType.ACTIVE_PIECE);
        this.board = board;
        Tetromino t = (next != null) ? next : randomPiece();
        this.state = new PieceState(t, 0, 0, spawnCol);
        this.x = state.col(); // for rendering interpolation (cells)
        this.y = state.row();
    }

    private Tetromino randomPiece() {
        Tetromino[] all = Tetromino.values();
        return all[rng.nextInt(all.length)];
    }

    @Override
    protected void process(double dt) {
        // Gravity in cells/second, from GameConfig
        double gravity = org.oosd.core.GameConfig.get().gravityCps();
        double newY = y + gravity * dt;

        // When passing next integral row, try to move piece down one row.
        while (Math.floor(newY) > Math.floor(y)) {
            if (!tryMove(1, 0, 0)) { // blocked => lock piece
                lockToBoard();
                kill();
                return;
            }
        }
        y = newY;
    }

    public boolean tryRotateCW()  { return tryMove(0, 0, +1); }
    public boolean tryLeft()      { return tryMove(0, -1, 0); }
    public boolean tryRight()     { return tryMove(0, +1, 0); }
    public boolean trySoftDrop()  { return tryMove(1, 0, 0);  }

    private boolean tryMove(int dr, int dc, int drot) {
        int newRot = state.rot();
        if (drot != 0) {
            newRot = (newRot + drot + state.type().rotationCount()) % state.type().rotationCount();
        }
        PieceState next = new PieceState(state.type(), newRot, state.row() + dr, state.col() + dc);
        if (canPlace(next)) {
            state = next;
            x = state.col();
            y = state.row();
            return true;
        }
        return false;
    }

    private boolean canPlace(PieceState s) {
        int[][] m = s.type().shape(s.rot());
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = s.row() + r, bc = s.col() + c;
                    if (!board.inBounds(br, bc) || board.get(br, bc) != 0) return false;
                }
            }
        }
        return true;
    }

    private void lockToBoard() {
        int[][] m = state.type().shape(state.rot());
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = state.row() + r, bc = state.col() + c;
                    if (board.inBounds(br, bc)) board.set(br, bc, state.type().colorId());
                }
            }
        }
    }

    /* Accessors so the sprite can render the active piece */
    public PieceState piece() { return state; }
}
