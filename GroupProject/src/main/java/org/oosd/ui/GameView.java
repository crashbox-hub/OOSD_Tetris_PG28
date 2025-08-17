package org.oosd.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.oosd.core.AbstractScreen;
import org.oosd.game.*;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Random;

/**
 * Gameplay screen with:
 *  - Board drawing (10x20 grid on a Canvas)
 *  - Falling piece control (left/right/rotate/soft drop)
 *  - Gravity via AnimationTimer
 *  - Scoring + lines + elapsed time HUD
 *  - Next-piece preview
 *  - Pause (P) overlay and exit-to-main (ESC while paused)
 *  - Light styling (gradient bg, framed board, glossy tiles)
 */
public class GameView extends AbstractScreen {

    /* =========================
       Layout + sizing constants
       ========================= */
    private static final int TILE = 30;                      // size of one cell in pixels
    private static final int BOARD_W = Board.COLS * TILE;    // 10 * 30 = 300 px wide
    private static final int BOARD_H = Board.ROWS * TILE;    // 20 * 30 = 600 px tall

    /* =========================
       Core game state
       ========================= */
    private final Board board = new Board();     // stores placed blocks (0 empty, >0 colorId)
    private final Random rng = new Random();     // randomizer for next pieces

    private PieceState active = null;            // current falling piece (type, rot, row, col)
    private Tetromino nextPiece = null;          // queued next piece (shown in preview)

    private boolean paused = false;              // pause flag (P toggles)
    private long lastDrop = 0;                   // last time (nanos) gravity applied
    private long dropIntervalNanos = 500_000_000L; // gravity interval ~0.5s
    private long runStartNanos;                  // run start time (for elapsed time HUD)

    private int score = 0;                       // total score
    private int lines = 0;                       // total cleared lines

    // callback to return to main menu (provided by Main when constructing this view)
    private final Runnable onExitToMenu;

    /* =========================
       View nodes (Canvas + HUD)
       ========================= */
    private final Canvas boardCanvas = new Canvas(BOARD_W, BOARD_H);
    private final GraphicsContext g = boardCanvas.getGraphicsContext2D();

    // 4x4 preview canvas for next piece
    private final Canvas nextCanvas = new Canvas(4 * TILE, 4 * TILE);
    private final GraphicsContext gNext = nextCanvas.getGraphicsContext2D();

    // HUD labels (score, lines, time)
    private final Label scoreLabel = new Label("SCORE 0");
    private final Label linesLabel = new Label("LINES 0");
    private final Label timeLabel  = new Label("TIME 00:00");

    /**
     * Main game loop — JavaFX calls handle(now) ~60fps.
     * We:
     *  - spawn an active piece when needed
     *  - apply gravity at fixed intervals
     *  - draw the board & piece
     *  - update the HUD text
     */
    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            if (!paused) {
                // Ensure a piece exists
                if (active == null) spawn();

                // Apply gravity step if interval elapsed
                if (now - lastDrop >= dropIntervalNanos) {
                    // Move down one; if blocked, lock into board
                    if (!tryMove(1, 0, 0)) lockPiece();
                    lastDrop = now;
                }
            }
            // Render everything and refresh HUD each frame
            draw();
            updateHud(now);
        }
    };

    /**
     * Construct the gameplay screen.
     * @param onExitToMenu callback used to return to the main menu (triggered on ESC while paused)
     */
    public GameView(Runnable onExitToMenu) {
        this.onExitToMenu = onExitToMenu;

        /* ----- HUD (right column) ----- */
        var hud = new VBox(16);
        hud.setPadding(new Insets(12));
        hud.setAlignment(Pos.TOP_LEFT);

        Label nextTitle = new Label("NEXT");
        nextTitle.setFont(Font.font(16));
        nextTitle.setTextFill(Color.WHITE);

        // Style HUD labels
        for (Label lbl : new Label[]{scoreLabel, linesLabel, timeLabel}) {
            lbl.setTextFill(Color.WHITE);
            lbl.setFont(Font.font(14));
        }

        // Stack "NEXT" title, preview canvas, spacer, then HUD stats
        hud.getChildren().addAll(nextTitle, nextCanvas, new Label(""), scoreLabel, linesLabel, timeLabel);

        /* ----- Board (left) with a framed background ----- */
        Group boardGroup = new Group(boardCanvas); // Canvas sits in a Group (no automatic resizing)
        StackPane boardPane = new StackPane(boardGroup);
        boardPane.setPadding(new Insets(8)); // visual frame padding
        boardPane.setStyle(
                // A simple teal-ish framed panel behind the board
                "-fx-background-color: linear-gradient(#072032,#0d2b45);" + // frame bg
                        "-fx-border-color: #2fd0ff;" +
                        "-fx-border-width: 3px;" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;"
        );

        /* ----- Root layout: Board on left, HUD on right ----- */
        HBox root = new HBox(16, boardPane, hud);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: linear-gradient(#0b1220, #0f1830);");
        getChildren().add(root);


        // Make this Pane focusable so it can receive key events
        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);
    }

    /* =========================
       Lifecycle hooks
       ========================= */
    @Override public void onShow() {
        // when screen becomes visible: focus, reset timer, start loop
        requestFocus();
        lastDrop = 0;
        runStartNanos = System.nanoTime();
        loop.start();
    }

    @Override public void onHide() {
        // stop the loop when we navigate away from this screen
        loop.stop();
    }

    /* =========================
       Core gameplay
       ========================= */

    /**
     * Spawn a new piece from the NEXT queue into play area.
     * If NEXT is empty, roll one first. Then roll the subsequent NEXT.
     * If the freshly spawned piece cannot be placed → pause (simple game over for M1).
     * Also redraw the next-piece preview.
     */
    private void spawn() {
        if (nextPiece == null) nextPiece = randomPiece(); // ensure queue has a piece
        Tetromino t = nextPiece;
        nextPiece = randomPiece();                        // roll the next NEXT

        active = new PieceState(t, 0, 0, 3);              // start near top-center
        if (!canPlace(active)) paused = true;             // blocked at spawn = game over (simplified)

        drawNextPreview();                                // refresh the preview panel
    }

    /** Pick a random Tetromino (I,O,T,S,Z,J,L). */
    private Tetromino randomPiece() {
        return Tetromino.values()[rng.nextInt(Tetromino.values().length)];
    }

    /**
     * Check whether a given piece state can be placed on the board:
     *  - for each '1' in the shape matrix, the target board cell must be in bounds AND empty.
     */
    private boolean canPlace(PieceState p) {
        int[][] m = p.type().shape(p.rot());
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = p.row() + r, bc = p.col() + c;
                    if (!board.inBounds(br, bc) || board.get(br, bc) != 0) return false;
                }
            }
        }
        return true;
    }

    /**
     * Attempt to move/rotate the active piece.
     * @param dr   row delta (1 = down)
     * @param dc   col delta (-1 left, +1 right)
     * @param drot rotation delta (+1 next rotation)
     * @return true if movement valid & applied, false if blocked
     */
    private boolean tryMove(int dr, int dc, int drot) {
        int newRot = active.rot();
        if (drot != 0) {
            // wrap rotation index within available rotation states
            newRot = (newRot + drot + active.type().rotationCount()) % active.type().rotationCount();
        }

        // build a tentative new state and validate it
        PieceState next = new PieceState(active.type(), newRot, active.row() + dr, active.col() + dc);
        if (canPlace(next)) { active = next; return true; }
        return false;
    }

    /**
     * Lock the active piece onto the board, score any cleared lines,
     * and clear 'active' so the next loop tick spawns a new piece.
     */
    private void lockPiece() {
        // write active piece blocks into the board grid
        int[][] m = active.type().shape(active.rot());
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = active.row() + r, bc = active.col() + c;
                    if (board.inBounds(br, bc)) board.set(br, bc, active.type().colorId());
                }
            }
        }

        // clear full rows and award points (simple classic-ish values)
        int cleared = board.clearFullRows();
        if (cleared > 0) {
            lines += cleared;
            switch (cleared) {
                case 1 -> score += 100;
                case 2 -> score += 300;
                case 3 -> score += 500;
                case 4 -> score += 800;
                default -> score += cleared * 100;
            }
        }

        // force a respawn on the next frame
        active = null;
    }

    /* =========================
       Input handling
       ========================= */
    private void onKey(KeyEvent e) {
        KeyCode k = e.getCode();
        switch (k) {
            case LEFT  -> tryMove(0, -1, 0);                 // move left
            case RIGHT -> tryMove(0, +1, 0);                 // move right
            case UP    -> tryMove(0, 0, +1);                 // rotate clockwise
            case DOWN  -> { if (!tryMove(1, 0, 0)) lockPiece(); } // soft drop
            case P     -> paused = !paused;                  // toggle pause overlay
            case ESCAPE -> { if (paused && onExitToMenu != null) onExitToMenu.run();}// exit to menu when paused
            case R -> { if (paused) restartGame(); }// restart game
            default -> {}
        }
        draw(); // immediate visual feedback after input
    }

    /* =========================
       Rendering (board + HUD)
       ========================= */

    /**
     * Draw board frame each frame:
     *  - background
     *  - placed cells
     *  - active falling piece
     *  - grid overlay
     *  - optional pause overlay
     */
    private void draw() {
        // board background
        g.setFill(Color.rgb(8,12,20));
        g.fillRect(0, 0, BOARD_W, BOARD_H);

        // placed cells (from board grid)
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                int v = board.get(r, c);
                if (v != 0) drawCell(c, r, v);
            }
        }

        // active falling piece (draw over the board)
        if (active != null) {
            int[][] m = active.type().shape(active.rot());
            for (int r = 0; r < m.length; r++) {
                for (int c = 0; c < m[r].length; c++) {
                    if (m[r][c] != 0) {
                        drawCell(active.col() + c, active.row() + r, active.type().colorId());
                    }
                }
            }
        }

        // faint grid overlay for readability
        g.setStroke(Color.color(1,1,1,0.10));
        for (int x = 0; x <= Board.COLS; x++) g.strokeLine(x*TILE, 0, x*TILE, BOARD_H);
        for (int y = 0; y <= Board.ROWS; y++) g.strokeLine(0, y*TILE, BOARD_W, y*TILE);

        // pause overlay text & dimmer
        if (paused) {
            // dark transparent overlay
            g.setFill(Color.color(0, 0, 0, 0.55));
            g.fillRect(0, 0, BOARD_W, BOARD_H);

            // text settings
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial", FontWeight.BOLD, 20)); // requires FontWeight import

            // pause menu lines
            String[] pauseLines = {
                    "Game Paused (P)",
                    "ESC to Main Menu",
                    "R to Restart Game"
            };

            double lineHeight = 28;
            double totalHeight = pauseLines.length * lineHeight;
            double startY = (BOARD_H - totalHeight) / 2 + 6; // slight vertical tweak

            for (int i = 0; i < pauseLines.length; i++) {
                String text = pauseLines[i];

                // measure width using a Text node
                Text temp = new Text(text);
                temp.setFont(g.getFont());
                double textWidth = temp.getLayoutBounds().getWidth();

                double x = (BOARD_W - textWidth) / 2; // center horizontally
                double y = startY + i * lineHeight;
                g.fillText(text, x, y);
            }
        }


    }

    /**
     * Draw a single board cell with a subtle glossy style.
     * @param col board column index
     * @param row board row index
     * @param colorId tetromino color id (1..7)
     */
    private void drawCell(int col, int row, int colorId) {
        // Map colorId to JavaFX Color (kept same as Tetromino enum ids)
        Color fill = switch (colorId) {
            case 1 -> Color.CYAN;       // I
            case 2 -> Color.YELLOW;     // O
            case 3 -> Color.PURPLE;     // T
            case 4 -> Color.LIMEGREEN;  // S
            case 5 -> Color.RED;        // Z
            case 6 -> Color.BLUE;       // J
            case 7 -> Color.ORANGE;     // L
            default -> Color.GRAY;
        };

        double x = col * TILE, y = row * TILE;

        // fill
        g.setFill(fill);
        g.fillRect(x, y, TILE, TILE);

        // top sheen highlight
        g.setFill(Color.color(1,1,1,0.10));
        g.fillRect(x, y, TILE, TILE * 0.25);

        // border
        g.setStroke(Color.color(0,0,0,0.35));
        g.strokeRect(x+0.5, y+0.5, TILE-1, TILE-1);
    }

    /**
     * Redraws the next-piece preview panel.
     * Clears previous drawing fully to avoid overdraw.
     */
    private void drawNextPreview() {
        // Clear previous content
        gNext.clearRect(0, 0, nextCanvas.getWidth(), nextCanvas.getHeight());

        // Paint a solid background that matches HUD styling
        gNext.setFill(Color.rgb(12, 18, 28));
        gNext.fillRect(0, 0, nextCanvas.getWidth(), nextCanvas.getHeight());

        if (nextPiece == null) return;

        // Use rotation 0 for preview
        int[][] m = nextPiece.shape(0);
        int w = m[0].length, h = m.length;

        // Center the piece within the 4x4 preview canvas
        int xOff = (4 - w) * TILE / 2;
        int yOff = (4 - h) * TILE / 2;

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (m[r][c] != 0) {
                    double x = c * TILE + xOff, y = r * TILE + yOff;

                    // Neutral preview color
                    gNext.setFill(Color.GRAY);
                    gNext.fillRect(x, y, TILE, TILE);

                    // Sheen and border for consistency
                    gNext.setFill(Color.color(1,1,1,0.10));
                    gNext.fillRect(x, y, TILE, TILE*0.25);
                    gNext.setStroke(Color.color(0,0,0,0.35));
                    gNext.strokeRect(x+0.5, y+0.5, TILE-1, TILE-1);
                }
            }
        }
    }

    /**
     * Update HUD labels (score / lines / elapsed time).
     * Called every frame from the AnimationTimer.
     */
    private void updateHud(long now) {
        long elapsedSec = Math.max(0, (now - runStartNanos) / 1_000_000_000L);
        long mm = elapsedSec / 60, ss = elapsedSec % 60;
        timeLabel.setText(String.format("TIME %02d:%02d", mm, ss));
        scoreLabel.setText("SCORE " + score);
        linesLabel.setText("LINES " + lines);
    }

    // Reset the entire game state and resume play
    private void restartGame() {
        // clear the board
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                board.set(r, c, 0);
            }
        }

        // reset stats & timers
        score = 0;
        lines = 0;
        active = null;
        nextPiece = null;
        lastDrop = 0;
        runStartNanos = System.nanoTime();

        // unpause and refresh HUD/preview
        paused = false;
        drawNextPreview();
        draw();
        requestFocus(); // ensure we still receive key events
    }
}
