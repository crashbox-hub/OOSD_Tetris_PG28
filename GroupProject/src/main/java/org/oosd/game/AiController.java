package org.oosd.game;

import org.oosd.core.GameConfig;
import org.oosd.ui.Sound;

import java.util.*;

/** Owns all AI state & logic; GameView just calls update(...) per side. */
public final class AiController {

    // Cadence while travelling; once aligned we fast-drop aggressively
    private static final long MOVE_NS   = 120_000_000L;
    private static final long ROTATE_NS = 240_000_000L;

    /** Per-side transient AI state */
    private static final class State {
        long lastMoveNs   = 0L;
        long lastRotateNs = 0L;

        int lastPieceIdentity = 0;

        // Planned move for current piece
        int targetCol = 0;
        int targetRotationsCW = 0; // from spawn rotation (assumed 0)
        int rotationsDone = 0;
    }

    private final Map<Integer, State> states = new HashMap<>();

    /* Drive AI for one side (no-op if piece is null). */
    public void update(int sideId,
                       Board board,
                       ActivePieceEntity piece,
                       long nowNanos,
                       int tileSize,
                       boolean sfxEnabled) {

        if (piece == null) return;

        State st = states.computeIfAbsent(sideId, k -> new State());

        // Detect new active piece & compute a plan once
        int identity = System.identityHashCode(piece);
        if (identity != st.lastPieceIdentity) {
            st.lastPieceIdentity = identity;
            st.rotationsDone = 0;

            Plan plan = planBestPlacement(board, piece);
            st.targetCol = plan.targetCol;
            st.targetRotationsCW = plan.rotationsCW;
        }

        // 1) Perform planned rotations first (kept deliberate)
        if (st.rotationsDone < st.targetRotationsCW &&
                nowNanos - st.lastRotateNs >= ROTATE_NS) {
            piece.tryRotateCW();
            st.rotationsDone++;
            st.lastRotateNs = nowNanos;
            if (sfxEnabled) Sound.playRotate();
            return; // rotate only this frame to stay stable
        }

        final int px = pieceCol(piece, tileSize);

        // 2) If aligned, aggressively soft-drop to lock quickly
        if (px == st.targetCol && st.rotationsDone >= st.targetRotationsCW) {
            // Push down multiple cells this frame until we either move or lock.
            // Hard cap to avoid very rare infinite loops.
            for (int i = 0; i < 64; i++) {
                boolean moved = piece.softDropOrLock();
                if (!moved) break; // locked
            }
            return;
        }

        // 3) Move horizontally toward target column at a steady cadence
        if (nowNanos - st.lastMoveNs >= MOVE_NS) {
            if (px < st.targetCol)      piece.tryRight();
            else if (px > st.targetCol) piece.tryLeft();
            st.lastMoveNs = nowNanos;
        }
    }

    // ---------- Planning (try all useful reachable placements, pick the best) ----------

    private record Plan(int targetCol, int rotationsCW) {}

    private Plan planBestPlacement(Board board, ActivePieceEntity pieceEntity) {
        Tetromino t = pieceEntity.piece().type();
        int cols = GameConfig.get().cols();
        int rows = GameConfig.get().rows();

        int[] rots = rotationsToTry(t);

        double bestScore = -Double.MAX_VALUE;
        int bestCol = 0;
        int bestRot = 0;
        int bestLandingRow = -1;

        // snapshot field
        int[][] field = snapshot(board, rows, cols);

        // spawn location (rotation 0 assumed)
        final int spawnCol = clamp(GameConfig.get().spawnCol(), 0, cols - 1);

        // current column (usually equals spawnCol at time of planning, but we read it anyway)
        final int currentCol = pieceCol(pieceEntity, GameConfig.get().tileSize());

        // Precompute bottom-row data for quick “bottom awareness”
        final int[] bottomRow = Arrays.copyOf(field[rows - 1], cols);
        final int bottomEmpty = countZeros(bottomRow);

        for (int rot : rots) {
            int[][] shape = t.shape(rot);
            int w = (shape.length == 0) ? 0 : shape[0].length;
            if (w == 0 || w > cols) continue;

            for (int col = 0; col <= cols - w; col++) {
                // find landing row by "dropping" until collision
                int row = 0;
                if (!canPlace(field, shape, row, col)) continue; // blocked immediately
                while (row + 1 < rows && canPlace(field, shape, row + 1, col)) row++;

                // reachable from spawn? (prevents magical side-slips)
                if (!pathExistsFromSpawn(field, t, spawnCol, rows, cols, rot, col)) continue;

                // simulate placement & line clears
                int[][] after = deepCopy(field);
                place(after, shape, row, col, 1);
                int linesCleared = clearFullLines(after);

                // quick bottom-fill awareness: how many cells of this piece were placed on bottom row?
                int bottomFilledByThisPiece = cellsPlacedOnRow(shape, row, col, rows - 1);

                // measure how “low” the placement is (deeper is better to avoid dithering)
                int landingDepth = row;

                // evaluate board
                double score = evaluate(after, linesCleared, bottomEmpty, bottomFilledByThisPiece);

                // reduce side-to-side motion: prefer closer targets when scores are close
                int horizDist = Math.abs(col - currentCol);
                final double DIST_PENALTY = 0.08; // small but effective
                score -= DIST_PENALTY * horizDist;

                // prefer deeper landings as tie-break
                if (score > bestScore ||
                        (Math.abs(score - bestScore) < 1e-6 && landingDepth > bestLandingRow)) {
                    bestScore = score;
                    bestCol = col;
                    bestRot = rot;
                    bestLandingRow = landingDepth;
                }
            }
        }

        // #CW rotations from spawn (0) to chosen rotation
        return new Plan(bestCol, bestRot & 3);
    }

    // Heuristic evaluation with bottom-row / low-fill preference
    private double evaluate(int[][] field,
                            int linesCleared,
                            int bottomEmptyBefore,
                            int bottomFilledByThisPiece) {

        int cols = field[0].length;
        int rows = field.length;

        int[] heights = new int[cols];
        int holes = 0;

        // heights & holes
        for (int c = 0; c < cols; c++) {
            int h = 0;
            boolean blockSeen = false;
            for (int r = 0; r < rows; r++) {
                if (field[r][c] != 0) {
                    if (!blockSeen) { h = rows - r; blockSeen = true; }
                } else {
                    if (blockSeen) holes++; // empty below a block
                }
            }
            heights[c] = h;
        }

        int aggregateHeight = 0;
        for (int h : heights) aggregateHeight += h;

        int bumpiness = 0;
        for (int c = 0; c < cols - 1; c++) bumpiness += Math.abs(heights[c] - heights[c + 1]);

        // additional nudge: encourage filling low/near-bottom empties
        int lowGapsScore = 0;
        for (int c = 0; c < cols; c++) {
            // count empty cells from the bottom up until first block
            int empties = 0;
            for (int r = rows - 1; r >= 0; r--) {
                if (field[r][c] == 0) empties++;
                else break;
            }
            // higher reward for columns that were shallow before
            if (empties > 0) lowGapsScore += Math.max(1, 4 - empties);
        }

        // Weights (tuned for “clear lines fast” and “bottom fill first”)
        final double W_LINES       = 2.40;     // reward clearing lines strongly
        final double W_TETRIS      = 1.70;     // extra on 4-line clears
        final double W_BOTTOM_FILL = 0.45;     // reward cells placed on bottom row
        final double W_LOW_GAPS    = 0.10;     // reward reducing low gaps
        final double W_HEIGHT      = -0.38;
        final double W_HOLES       = -0.70;
        final double W_BUMPINESS   = -0.18;

        double tetrisBonus = (linesCleared == 4) ? 1.0 : 0.0;

        // If the bottom row had empties, giving a little more credit to filling them speeds play
        double bottomHelp = (bottomEmptyBefore > 0) ? (bottomFilledByThisPiece * W_BOTTOM_FILL) : 0.0;

        return W_LINES * linesCleared
                + W_TETRIS * tetrisBonus
                + bottomHelp
                + W_LOW_GAPS * lowGapsScore
                + W_HEIGHT * aggregateHeight
                + W_HOLES  * holes
                + W_BUMPINESS * bumpiness;
    }

    // ---------- Reachability check (BFS over (row,col,rot) while falling) ----------

    /**
     * Returns true if the piece can reach a landing at (targetRot, targetCol)
     * starting from (row=0, col=spawnCol, rot=0) by using left/right/rotate while falling.
     */
    private boolean pathExistsFromSpawn(int[][] field,
                                        Tetromino tet,
                                        int spawnCol,
                                        int rows,
                                        int cols,
                                        int targetRot,
                                        int targetCol) {

        // bounds guard for spawn
        spawnCol = clamp(spawnCol, 0, cols - 1);

        // visited[row][col][rot]
        boolean[][][] vis = new boolean[rows][cols][4];
        Deque<StateNode> q = new ArrayDeque<>();

        // initial states: at row 0, rotation 0; if blocked immediately, no path
        if (!canPlace(field, tet.shape(0), 0, spawnCol)) return false;

        q.add(new StateNode(0, spawnCol, 0));
        vis[0][spawnCol][0] = true;

        while (!q.isEmpty()) {
            StateNode s = q.removeFirst();

            // if we can go down, that's allowed (gravity)
            if (s.row + 1 < rows && canPlace(field, tet.shape(s.rot), s.row + 1, s.col)) {
                if (!vis[s.row + 1][s.col][s.rot]) {
                    vis[s.row + 1][s.col][s.rot] = true;
                    q.addLast(new StateNode(s.row + 1, s.col, s.rot));
                }
            } else {
                // cannot go further down → landing state
                if (s.col == targetCol && s.rot == (targetRot & 3)) return true;
            }

            // rotate CW in-place
            int nrot = (s.rot + 1) & 3;
            if (canPlace(field, tet.shape(nrot), s.row, s.col) && !vis[s.row][s.col][nrot]) {
                vis[s.row][s.col][nrot] = true;
                q.addLast(new StateNode(s.row, s.col, nrot));
            }

            // move left/right at same row
            if (s.col - 1 >= 0 && canPlace(field, tet.shape(s.rot), s.row, s.col - 1) && !vis[s.row][s.col - 1][s.rot]) {
                vis[s.row][s.col - 1][s.rot] = true;
                q.addLast(new StateNode(s.row, s.col - 1, s.rot));
            }
            if (s.col + 1 < cols && canPlace(field, tet.shape(s.rot), s.row, s.col + 1) && !vis[s.row][s.col + 1][s.rot]) {
                vis[s.row][s.col + 1][s.rot] = true;
                q.addLast(new StateNode(s.row, s.col + 1, s.rot));
            }
        }
        return false;
    }

    private record StateNode(int row, int col, int rot) {
    }

    // ---------- Field simulation & quick helpers ----------

    private int[][] snapshot(Board b, int rows, int cols) {
        int[][] f = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                f[r][c] = b.get(r, c);
        return f;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] out = new int[src.length][];
        for (int i = 0; i < src.length; i++) out[i] = Arrays.copyOf(src[i], src[i].length);
        return out;
    }

    private boolean canPlace(int[][] field, int[][] shape, int row, int col) {
        int rows = field.length, cols = field[0].length;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) if (shape[r][c] != 0) {
                int rr = row + r, cc = col + c;
                if (rr < 0 || rr >= rows || cc < 0 || cc >= cols) return false;
                if (field[rr][cc] != 0) return false;
            }
        }
        return true;
    }

    private void place(int[][] field, int[][] shape, int row, int col, int val) {
        for (int r = 0; r < shape.length; r++)
            for (int c = 0; c < shape[r].length; c++)
                if (shape[r][c] != 0) field[row + r][col + c] = val;
    }

    private int clearFullLines(int[][] field) {
        int rows = field.length, cols = field[0].length;
        int write = rows - 1;
        int cleared = 0;
        for (int r = rows - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < cols; c++) if (field[r][c] == 0) { full = false; break; }
            if (!full) {
                if (write != r) field[write] = Arrays.copyOf(field[r], cols);
                write--;
            } else {
                cleared++;
            }
        }
        for (int r = write; r >= 0; r--) Arrays.fill(field[r], 0);
        return cleared;
    }

    private int cellsPlacedOnRow(int[][] shape, int baseRow, int baseCol, int targetRow) {
        int count = 0;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0 && (baseRow + r) == targetRow) count++;
            }
        }
        return count;
    }

    private int[] rotationsToTry(Tetromino t) {
        if (t == null) return new int[]{0};
        // Explicit sets; L explicitly listed as requested.
        return switch (t) {
            case O -> new int[]{0};              // same in all rotations
            case I, S, Z -> new int[]{0, 1};     // 2 unique orientations
            case L -> new int[]{0, 1, 2, 3};     // explicit
            default -> new int[]{0, 1, 2, 3};    // T, J
        };
    }

    private int pieceCol(ActivePieceEntity piece, int tileSize) {
        double x = piece.x(); // from GameEntity
        if (x > GameConfig.get().cols() + 2) x /= tileSize; // convert pixels->cells if needed
        return (int) Math.round(x);
    }

    private static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : Math.min(v, hi);
    }

    private static int countZeros(int[] arr) {
        int z = 0; for (int v : arr) if (v == 0) z++; return z;
    }
}
