package org.oosd.game;

import org.oosd.core.GameConfig;

import java.util.*;

/** Next-piece-aware AI controller for Tetris (instance-based; no static helper calls). */
public final class AiController {

    // Cadence while travelling/rotating; once aligned we soft-drop each frame
    private static final long MOVE_NS   = 120_000_000L;
    private static final long ROTATE_NS = 120_000_000L;

    /** Per-side transient AI state */
    private static final class State {
        long lastMoveNs   = 0L;
        long lastRotateNs = 0L;

        int lastPieceIdentity = 0;

        // Planned move for current piece
        int targetCol = 0;
        int targetRot = 0;        // 0..3

        // Sweep bias
        int sweepCol = 0;
        int sweepDir = +1;        // ping-pong: +1 → right, -1 → left
    }

    private final Map<Integer, State> states = new HashMap<>();

    /** Back-compat overload: no next-piece lookahead. */
    public void update(int sideId,
                       Board board,
                       ActivePieceEntity piece,
                       long nowNanos,
                       int tileSize) {
        update(sideId, board, piece, /*nextVisible*/ null, nowNanos, tileSize);
    }

    /** Drive AI for one side (no-op if piece is null). */
    public void update(int sideId,
                       Board board,
                       ActivePieceEntity piece,
                       Tetromino nextVisible,     // pass from GameView (side.nextPiece)
                       long nowNanos,
                       int tileSize) {

        if (piece == null) return;

        State st = states.computeIfAbsent(sideId, k -> new State());

        // Detect new active piece & compute a plan once
        int identity = System.identityHashCode(piece);
        if (identity != st.lastPieceIdentity) {
            st.lastPieceIdentity = identity;

            Plan plan = planBestPlacement(board, piece, nextVisible, st);
            st.targetCol = plan.targetCol;
            st.targetRot = plan.targetRot & 3;
        }

        final int px   = pieceCol(piece, tileSize);
        final int prot = piece.piece().rot() & 3;

        // 1) Rotate toward target rotation on cadence (CW-only for max compatibility)
        if (prot != st.targetRot && (nowNanos - st.lastRotateNs) >= ROTATE_NS) {
            piece.tryRotateCW();
            st.lastRotateNs = nowNanos;
            return; // let rotation settle this frame
        }

        // 2) Move horizontally toward target column at a steady cadence
        if (px != st.targetCol && (nowNanos - st.lastMoveNs) >= MOVE_NS) {
            if (px < st.targetCol) piece.tryRight();
            else                   piece.tryLeft();
            st.lastMoveNs = nowNanos;
            return; // moved this frame
        }

        // 3) If aligned in col & rot, nudge down to lock quickly
        if (px == st.targetCol && prot == st.targetRot) {
            boolean locked = !piece.softDropOrLock(); // returns false if it locked
            if (locked) {
                // On lock, sweep ping-pongs between edges when healthy
                int[][] f = snapshot(board, GameConfig.get().rows(), GameConfig.get().cols());
                if (columnHealthy(f, st.sweepCol)) {
                    st.sweepCol += st.sweepDir;
                    if (st.sweepCol <= 0) { st.sweepCol = 0; st.sweepDir = +1; }
                    else if (st.sweepCol >= GameConfig.get().cols() - 1) {
                        st.sweepCol = GameConfig.get().cols() - 1;
                        st.sweepDir = -1;
                    }
                }
            }
        }
    }

    // ---------- Planning (try all useful reachable placements, pick the best) ----------

    private record Plan(int targetCol, int targetRot) { }

    private Plan planBestPlacement(Board board,
                                   ActivePieceEntity pieceEntity,
                                   Tetromino nextVisible,
                                   State st) {

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
        final int spawnCol = clamp(GameConfig.get().spawnCol(), cols - 1);

        // current column
        final int currentCol = pieceCol(pieceEntity, GameConfig.get().tileSize());

        // Precompute bottom-row data
        final int[] bottomRow = Arrays.copyOf(field[rows - 1], cols);
        final int bottomEmpty = countZeros(bottomRow);

        // Precompute row empties for imminent-completion bonuses
        RowStat[] rs = rowStats(field);

        // exploration cap for perf
        int explored = 0;
        final int EXPLORE_CAP = 600;

        for (int rot : rots) {
            int[][] shape = t.shape(rot);
            int w = (shape.length == 0) ? 0 : shape[0].length;
            if (w == 0 || w > cols) continue;

            for (int col = 0; col <= cols - w; col++) {
                if (++explored > EXPLORE_CAP) break;

                // find landing row by "dropping" until collision
                int row = 0;
                if (!canPlace(field, shape, row, col)) continue; // blocked immediately
                while (row + 1 < rows && canPlace(field, shape, row + 1, col)) row++;

                // reachable from spawn? (prevents magical side-slips)
                if (!pathExistsFromSpawn(field, t, spawnCol, rows, cols, rot, col)) continue;

                // simulate placement & line clears
                int[][] after = deepCopy(field);
                place(after, shape, row, col);
                int linesCleared = clearFullLines(after);

                // quick bottom-fill awareness
                int bottomFilledByThisPiece = cellsPlacedOnRow(shape, row, rows - 1);

                // imminent-completion helpers (on the pre-placement board)
                int help1 = cellsHelpingCriticalRows(shape, row, rs, /*criticalMaxEmpty=*/1);
                int help2 = (help1 == 0) ? cellsHelpingCriticalRows(shape, row, rs, 2) : 0;
                int help3 = (help1 == 0 && help2 == 0) ? cellsHelpingCriticalRows(shape, row, rs, 3) : 0;

                // one-ply score
                double s1 = evaluate(after, linesCleared, bottomEmpty, bottomFilledByThisPiece,
                        row, rows, help1, help2, help3);

                // ------------- next-piece lookahead (2-ply) -------------
                double s2 = 0.0;
                if (nextVisible != null) {
                    s2 = bestReplyScore(after, nextVisible, st);
                }

                double score = s1 + 0.65 * s2; // weight lookahead less than immediate result

                // reduce side-to-side motion (weaker so edges are viable)
                int horizDist = Math.abs(col - currentCol);
                final double DIST_PENALTY = 0.03; // was 0.08
                score -= DIST_PENALTY * horizDist;

                // mild sweep pull toward the current sweep column
                int sweepDist = Math.abs(col - st.sweepCol);
                final double SWEEP_PULL = 0.05;
                score -= SWEEP_PULL * sweepDist;

                // NEW: tiny edge preference when edges are shallow
                int distanceToEdge = Math.min(col, cols - 1 - col);
                int localHeight = columnHeight(after, col);
                int edgeShallowBonus = Math.max(0, 6 - localHeight); // more bonus if shallow
                final double EDGE_PULL = 0.02;
                score += EDGE_PULL * Math.max(0, 3 - distanceToEdge) * edgeShallowBonus;

                // prefer deeper landings as tie-break
                if (score > bestScore ||
                        (Math.abs(score - bestScore) < 1e-6 && row > bestLandingRow)) {
                    bestScore = score;
                    bestCol = col;
                    bestRot = rot;
                    bestLandingRow = row;
                }
            }
        }

        return new Plan(bestCol, bestRot & 3);
    }

    /** Evaluate the best placement score for the given next piece on a hypothetical board. */
    private double bestReplyScore(int[][] baseField, Tetromino next, State st) {
        int rows = baseField.length, cols = baseField[0].length;
        int[] rots = rotationsToTry(next);

        // Precompute stats on the post-first-move field
        int[] bottomRow = Arrays.copyOf(baseField[rows - 1], cols);
        int bottomEmpty = countZeros(bottomRow);
        RowStat[] rs = rowStats(baseField);

        double best = -Double.MAX_VALUE;
        int explored = 0, CAP = 400;

        for (int rot : rots) {
            int[][] shape = next.shape(rot);
            int w = (shape.length == 0) ? 0 : shape[0].length;
            if (w == 0 || w > cols) continue;

            for (int col = 0; col <= cols - w; col++) {
                if (++explored > CAP) break;

                int row = 0;
                if (!canPlace(baseField, shape, row, col)) continue;
                while (row + 1 < rows && canPlace(baseField, shape, row + 1, col)) row++;

                int[][] after = deepCopy(baseField);
                place(after, shape, row, col);
                int cleared = clearFullLines(after);

                int bottomFilled = cellsPlacedOnRow(shape, row, rows - 1);
                int help1 = cellsHelpingCriticalRows(shape, row, rs, 1);
                int help2 = (help1 == 0) ? cellsHelpingCriticalRows(shape, row, rs, 2) : 0;
                int help3 = (help1 == 0 && help2 == 0) ? cellsHelpingCriticalRows(shape, row, rs, 3) : 0;

                double s = evaluate(after, cleared, bottomEmpty, bottomFilled,
                        row, rows, help1, help2, help3);

                // small bias toward continuing the sweep
                final double SWEEP_PULL = 0.03;
                s -= SWEEP_PULL * Math.abs(col - st.sweepCol);

                if (s > best) best = s;
            }
        }
        return (best == -Double.MAX_VALUE) ? -5.0 : best; // if no move, punish
    }

    // ---------- Heuristic ----------

    private double evaluate(int[][] field,
                            int linesCleared,
                            int bottomEmptyBefore,
                            int bottomFilledByThisPiece,
                            int landingRow,
                            int totalRows,
                            int help1, int help2, int help3) {

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

        // encourage filling low/near-bottom empties
        int lowGapsScore = 0;
        for (int c = 0; c < cols; c++) {
            int empties = 0;
            for (int r = rows - 1; r >= 0; r--) {
                if (field[r][c] == 0) empties++;
                else break;
            }
            if (empties > 0) lowGapsScore += Math.max(1, 4 - empties);
        }

        // well preference: reward deep, clean wells anywhere (no center bias)
        int well = 0;
        for (int c = 1; c < cols - 1; c++) {
            int left = heights[c - 1], h = heights[c], right = heights[c + 1];
            if (h < left && h < right) {
                well += Math.max(0, Math.min(6, Math.min(left, right) - h));
            }
        }

        // edge cliff penalty: tall edges next to low neighbors are awkward
        int edgeCliff = 0;
        edgeCliff += Math.max(0, heights[0]      - heights[1]);
        edgeCliff += Math.max(0, heights[cols-1] - heights[cols-2]);

        // row-depth weight (lower is better)
        double depthWeight = Math.max(0.0, (double) landingRow / Math.max(1, totalRows - 1));

        // Weights (close to your originals)
        final double W_LINES        = 3.40;
        final double W_TETRIS       = 2.00;
        final double W_BOTTOM_FILL  = 0.45;
        final double W_LOW_GAPS     = 0.12;
        final double W_HEIGHT       = -0.36;
        final double W_HOLES        = -0.82;
        final double W_BUMPINESS    = -0.18;
        final double W_WELL         = 0.05;
        final double W_EDGE_CLIFF   = -0.10;

        // Imminent-completion helpers (tiered)
        final double W_HELP1        = 0.90;
        final double W_HELP2        = 0.45;
        final double W_HELP3        = 0.20;
        final double W_DEPTH_BONUS  = 0.40;

        double tetrisBonus = (linesCleared == 4) ? 1.0 : 0.0;
        double bottomHelp = (bottomEmptyBefore > 0) ? (bottomFilledByThisPiece * W_BOTTOM_FILL) : 0.0;

        return  W_LINES * linesCleared
                + W_TETRIS * tetrisBonus
                + bottomHelp
                + W_LOW_GAPS * lowGapsScore
                + W_HEIGHT * aggregateHeight
                + W_HOLES  * holes
                + W_BUMPINESS * bumpiness
                + W_WELL * well
                + W_EDGE_CLIFF * edgeCliff
                + W_HELP1 * help1 + W_HELP2 * help2 + W_HELP3 * help3
                + W_DEPTH_BONUS * depthWeight;
    }

    private int findDeepest(int[] h) {
        int best = 0, bi = 0;
        for (int i = 0; i < h.length; i++) if (h[i] > best) { best = h[i]; bi = i; }
        return bi;
    }

    // ---------- Reachability (BFS over (row,col,rot) while falling; no kicks) ----------

    private boolean pathExistsFromSpawn(int[][] field,
                                        Tetromino tet,
                                        int spawnCol,
                                        int rows,
                                        int cols,
                                        int targetRot,
                                        int targetCol) {

        spawnCol = clamp(spawnCol, cols - 1);

        boolean[][][] vis = new boolean[rows][cols][4];
        Deque<StateNode> q = new ArrayDeque<>();

        if (!canPlace(field, tet.shape(0), 0, spawnCol)) return false;

        q.add(new StateNode(0, spawnCol, 0));
        vis[0][spawnCol][0] = true;

        while (!q.isEmpty()) {
            StateNode s = q.removeFirst();

            // gravity
            if (s.row + 1 < rows && canPlace(field, tet.shape(s.rot), s.row + 1, s.col)) {
                if (!vis[s.row + 1][s.col][s.rot]) {
                    vis[s.row + 1][s.col][s.rot] = true;
                    q.addLast(new StateNode(s.row + 1, s.col, s.rot));
                }
            } else {
                // landing
                if (s.col == targetCol && s.rot == (targetRot & 3)) return true;
            }

            // rotations (no kicks)
            tryVisit(field, tet, s.row, s.col, (s.rot + 1) & 3, vis, q);
            tryVisit(field, tet, s.row, s.col, (s.rot + 3) & 3, vis, q);
            tryVisit(field, tet, s.row, s.col, (s.rot + 2) & 3, vis, q);

            // left/right
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

    private void tryVisit(int[][] field, Tetromino tet, int row, int col, int rot,
                          boolean[][][] vis, Deque<StateNode> q) {
        if (canPlace(field, tet.shape(rot), row, col) && !vis[row][col][rot]) {
            vis[row][col][rot] = true;
            q.addLast(new StateNode(row, col, rot));
        }
    }

    private record StateNode(int row, int col, int rot) { }

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

    private void place(int[][] field, int[][] shape, int row, int col) {
        for (int r = 0; r < shape.length; r++)
            for (int c = 0; c < shape[r].length; c++)
                if (shape[r][c] != 0) field[row + r][col + c] = 1;
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

    private int cellsPlacedOnRow(int[][] shape, int baseRow, int targetRow) {
        int count = 0;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0 && (baseRow + r) == targetRow) count++;
            }
        }
        return count;
    }

    private static final class RowStat { int empties; RowStat(int e){ empties = e; } }

    private RowStat[] rowStats(int[][] field) {
        int rows = field.length, cols = field[0].length;
        RowStat[] rs = new RowStat[rows];
        for (int r = 0; r < rows; r++) {
            int empties = 0;
            for (int c = 0; c < cols; c++) if (field[r][c] == 0) empties++;
            rs[r] = new RowStat(empties);
        }
        return rs;
    }

    private int cellsHelpingCriticalRows(int[][] shape, int baseRow,
                                         RowStat[] rs, int criticalMaxEmpty) {
        int count = 0;
        for (int r = 0; r < shape.length; r++) {
            int absR = baseRow + r;
            if (absR < 0 || absR >= rs.length) continue;
            if (rs[absR].empties > 0 && rs[absR].empties <= criticalMaxEmpty) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) count++;
                }
            }
        }
        return count;
    }

    private boolean columnHealthy(int[][] field, int col) {
        int rows = field.length;
        int empties = 0;
        for (int r = rows - 1; r >= Math.max(0, rows - 6); r--) {
            if (field[r][col] == 0) empties++;
        }
        return empties <= 2; // tweakable threshold
    }

    private int[] rotationsToTry(Tetromino t) {
        if (t == null) return new int[]{0};
        return switch (t) {
            case O -> new int[]{0};
            case I, S, Z -> new int[]{0, 1};
            default -> new int[]{0, 1, 2, 3}; // T, J, L
        };
    }

    private int pieceCol(ActivePieceEntity piece, int tileSize) {
        double x = piece.x(); // may be pixel or cell units depending on your entity
        if (x > GameConfig.get().cols() + 0.5) x /= Math.max(1, tileSize); // pixel->cell if needed
        int col = (int)Math.round(x);
        return clamp(col, GameConfig.get().cols() - 1);
    }

    private int columnHeight(int[][] f, int c) {
        int rows = f.length;
        for (int r = 0; r < rows; r++) {
            if (f[r][c] != 0) return rows - r;
        }
        return 0;
    }

    private static int clamp(int v, int hi) { return (v < 0) ? 0 : Math.min(v, hi); }
    private static int countZeros(int[] arr) { int z = 0; for (int v : arr) if (v == 0) z++; return z; }
}
