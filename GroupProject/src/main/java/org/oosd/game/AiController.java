package org.oosd.game;

import org.oosd.core.GameConfig;
import org.oosd.game.ActivePieceEntity;
import org.oosd.game.Board;
import org.oosd.ui.Sound;

import java.util.*;

/** Owns all AI state & logic; GameView just calls update(...) per side. */
public final class AiController {

    // simple tempo values for the bot
    private static final long MOVE_NS   = 180_000_000L;
    private static final long DROP_NS   = 350_000_000L;
    private static final long ROTATE_NS = 2_000_000_000L;

    /** Per-side transient AI state */
    private static final class State {
        long lastMoveNs   = 0L;
        long lastDropNs   = 0L;
        long lastRotateNs = 0L;
        int  lastTargetCol = 5;
    }

    private final Map<Integer, State> states = new HashMap<>();

    /** Drive AI for one side (no-op if piece is null). */
    public void update(int sideId,
                       Board board,
                       ActivePieceEntity piece,
                       long nowNanos,
                       int tileSize,
                       boolean sfxEnabled) {

        if (piece == null) return;

        State st = states.computeIfAbsent(sideId, k -> new State());

        int px = pieceCol(piece, tileSize);
        int targetCol = chooseTargetColumn(board, st.lastTargetCol);
        st.lastTargetCol = targetCol;

        // horizontal move
        if (nowNanos - st.lastMoveNs >= MOVE_NS) {
            if (px < targetCol)      piece.tryRight();
            else if (px > targetCol) piece.tryLeft();
            st.lastMoveNs = nowNanos;
        }

        // drop when aligned
        if (px == targetCol && (nowNanos - st.lastDropNs >= DROP_NS)) {
            piece.softDropOrLock();
            st.lastDropNs = nowNanos;
        }

        // occasional rotate when aligned
        if (px == targetCol && (nowNanos - st.lastRotateNs >= ROTATE_NS)) {
            piece.tryRotateCW();
            if (sfxEnabled) Sound.playRotate();
            st.lastRotateNs = nowNanos;
        }
    }

    private int pieceCol(ActivePieceEntity piece, int tileSize) {
        double x = piece.x();
        // if x looks like pixels, convert to cells
        if (x > GameConfig.get().cols() + 2) x /= tileSize;
        return (int) Math.round(x);
    }

    private int chooseTargetColumn(Board board, int lastTargetCol) {
        int cols = GameConfig.get().cols();
        int rows = GameConfig.get().rows();

        int[] heights = new int[cols];
        for (int c = 0; c < cols; c++) {
            int h = 0;
            for (int r = 0; r < rows; r++) {
                if (board.get(r, c) != 0) { h = rows - r; break; }
            }
            heights[c] = h;
        }

        int minH = Integer.MAX_VALUE;
        for (int h : heights) minH = Math.min(minH, h);

        List<Integer> candidates = new ArrayList<>();
        for (int c = 0; c < cols; c++) if (heights[c] == minH) candidates.add(c);

        int center = cols / 2;
        candidates.sort(Comparator.comparingInt(c -> Math.abs(c - center)));

        for (int c : candidates) if (c != lastTargetCol) return c;
        return candidates.get(0);
    }
}
