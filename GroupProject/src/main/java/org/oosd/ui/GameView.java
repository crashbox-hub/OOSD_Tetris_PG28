package org.oosd.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import org.oosd.core.AbstractScreen;
import org.oosd.game.*;

import java.util.Random;

/**
 * Main gameplay view:
 * - Draws the board and the currently falling piece
 * - Handles player input (movement, rotation, pause)
 * - Runs the main game loop (gravity, collision detection, row clearing)
 */
public class GameView extends AbstractScreen {

    // Size of a single Tetris block (in pixels)
    private static final int TILE = 30;

    // Canvas for drawing game elements
    private final Canvas canvas = new Canvas(Board.COLS * TILE, Board.ROWS * TILE);
    private final GraphicsContext g = canvas.getGraphicsContext2D();

    // Game board stores placed blocks
    private final Board board = new Board();
    private final Random rng = new Random();

    // Current falling piece
    private PieceState active = null;

    // Pause state toggle
    private boolean paused = false;

    // Simple gravity timer values
    private long lastDrop = 0;                     // Time when the last drop happened
    private long dropIntervalNanos = 500_000_000L; // Drop speed = every 0.5 seconds

    // Main game loop: runs every frame
    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            if (paused) {
                draw();
                return;
            }

            // If no active piece, spawn a new one
            if (active == null) spawn();

            // Gravity step — move piece down at set intervals
            if (now - lastDrop >= dropIntervalNanos) {
                // Try to move down; if can't, lock piece in place
                if (!tryMove(1, 0, 0)) lockPiece();
                lastDrop = now;
            }

            // Render everything
            draw();
        }
    };

    public GameView() {
        // Add canvas to the scene and set up keyboard input
        getChildren().add(canvas);
        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);
    }

    @Override public void onShow() {
        // When this screen is shown, request focus and start loop
        requestFocus();
        lastDrop = 0;
        loop.start();
    }

    @Override public void onHide() {
        // Stop game loop when this view is hidden
        loop.stop();
    }

    @Override public void update(long nowNanos) {
        // We use AnimationTimer loop instead of this update
    }

    // ------------------------
    //  GAMEPLAY CORE FUNCTIONS
    // ------------------------

    /** Spawn a new piece at the top of the board. */
    private void spawn() {
        // Pick a random Tetromino shape
        Tetromino t = Tetromino.values()[rng.nextInt(Tetromino.values().length)];
        active = new PieceState(t, 0, 0, 3); // Start near top-center

        // If the new piece can't be placed, game over
        if (!canPlace(active)) {
            paused = true; // For now, just pause the game
        }
    }

    /** Check if a given piece position is valid on the board. */
    private boolean canPlace(PieceState p) {
        int[][] m = p.type().shape(p.rot());
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = p.row() + r; // Board row
                    int bc = p.col() + c; // Board column

                    // If out of bounds or collides with existing block
                    if (!board.inBounds(br, bc) || board.get(br, bc) != 0)
                        return false;
                }
            }
        }
        return true;
    }

    /** Attempt to move or rotate the active piece. */
    private boolean tryMove(int dr, int dc, int drot) {
        int newRot = active.rot();

        // Calculate new rotation index (wraps around)
        if (drot != 0) {
            newRot = (newRot + drot + active.type().rotationCount()) % active.type().rotationCount();
        }

        // Create a test piece in the new position
        PieceState next = new PieceState(active.type(), newRot, active.row() + dr, active.col() + dc);

        // If valid, commit movement
        if (canPlace(next)) {
            active = next;
            return true;
        }
        return false;
    }

    /** Lock the current piece into the board and spawn the next one. */
    private void lockPiece() {
        int[][] m = active.type().shape(active.rot());

        // Copy active piece cells into the board
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = active.row() + r;
                    int bc = active.col() + c;
                    if (board.inBounds(br, bc))
                        board.set(br, bc, active.type().colorId());
                }
            }
        }

        // Remove any full rows
        board.clearFullRows();

        // Clear active piece so new one will spawn
        active = null;
    }

    /** Handle keyboard input. */
    private void onKey(KeyEvent e) {
        switch (e.getCode()) {
            case LEFT  -> tryMove(0, -1, 0);    // Move left
            case RIGHT -> tryMove(0, +1, 0);    // Move right
            case UP    -> tryMove(0, 0, +1);    // Rotate clockwise
            case DOWN  -> {                     // Soft drop
                if (!tryMove(1, 0, 0)) lockPiece();
            }
            case P     -> paused = !paused;     // Pause toggle
            default    -> {}
        }
        draw();
    }

    // ------------------------
    //  RENDERING FUNCTIONS
    // ------------------------

    /** Draw the game board and active piece. */
    private void draw() {
        // Clear background
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw placed blocks from the board
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                int v = board.get(r, c);
                if (v != 0) drawCell(c, r, v);
            }
        }

        // Draw currently active falling piece
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

        // Draw faint grid lines for reference
        g.setStroke(Color.color(1,1,1,0.1));
        for (int x = 0; x <= Board.COLS; x++)
            g.strokeLine(x*TILE, 0, x*TILE, Board.ROWS*TILE);
        for (int y = 0; y <= Board.ROWS; y++)
            g.strokeLine(0, y*TILE, Board.COLS*TILE, y*TILE);

        // Overlay pause screen if paused
        if (paused) {
            g.setFill(Color.color(0,0,0,0.5));
            g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
            g.setFill(Color.WHITE);
            g.fillText("PAUSED (P)", 10, 20);
        }
    }

    /** Draw a single Tetris cell at given board position. */
    private void drawCell(int col, int row, int colorId) {
        // Pick color based on Tetromino type
        Color color = switch (colorId) {
            case 1 -> Color.CYAN;       // I
            case 2 -> Color.YELLOW;     // O
            case 3 -> Color.PURPLE;     // T
            case 4 -> Color.LIMEGREEN;  // S
            case 5 -> Color.RED;        // Z
            case 6 -> Color.BLUE;       // J
            case 7 -> Color.ORANGE;     // L
            default -> Color.GRAY;
        };

        // Fill square
        g.setFill(color);
        g.fillRect(col * TILE, row * TILE, TILE, TILE);

        // Border for visual separation
        g.setStroke(Color.color(0,0,0,0.3));
        g.strokeRect(col * TILE, row * TILE, TILE, TILE);
    }
}
