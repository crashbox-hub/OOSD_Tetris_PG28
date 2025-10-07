package org.oosd.game;

import java.util.Random;

/*
Falling piece entity.
Gravity is driven externally by GameView.enforceGravity(...), NOT by dt here.
We keep process(dt) side-effect free (just syncs x/y to state) to avoid
"catch-up" jumps after pausing/unpausing.
 */
public class ActivePieceEntity extends GameEntity {
    private final Board board;
    private PieceState state;              // Tetromino, rotation, row, col
    private final Random rng = new Random();

    public ActivePieceEntity(Board board, Tetromino next, int spawnCol) {
        super(EntityType.ACTIVE_PIECE);
        this.board = board;
        Tetromino t = (next != null) ? next : randomPiece();
        this.state = new PieceState(t, 0, 0, spawnCol);
        this.x = state.col();
        this.y = state.row();
    }

    private Tetromino randomPiece() {
        Tetromino[] all = Tetromino.values();
        return all[rng.nextInt(all.length)];
    }

    @Override
    protected void process(double dt) {
        // IMPORTANT: no dt-based gravity here. Gravity is enforced by GameView.enforceGravity().
        // Just keep entity's x/y in sync with the logical state.
        this.x = state.col();
        this.y = state.row();
    }

    /* --------- input helpers ---------- */
    public void tryRotateCW()  { tryMove(0, 0, +1); }
    public void tryLeft()      { tryMove(0, -1, 0); }
    public void tryRight()     { tryMove(0, +1, 0); }

    /* One-cell soft drop. If blocked, locks to board and marks dead. */
    public boolean softDropOrLock() {
        if (tryMove(1, 0, 0)) return true;
        lockToBoard();
        kill();
        return false;
    }

    /** Manual soft drop for player input; does NOT lock when blocked. */
    public boolean softDrop() {
        return tryMove(1, 0, 0);
    }

    /* --------- core movement ---------- */
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

    /** Write current piece cells into the board grid (bounds guarded). */
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

    /* Accessor for sprite */
    public PieceState piece() { return state; }
}
