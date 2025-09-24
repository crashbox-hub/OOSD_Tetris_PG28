package org.oosd.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.oosd.core.AbstractScreen;
import org.oosd.game.Board;
import org.oosd.game.PieceState;
import org.oosd.game.Tetromino;
import org.oosd.ui.sprites.BlockSprite;
import org.oosd.ui.sprites.Sprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gameplay screen with:
 *  - Grid + block sprites (no Canvas)
 *  - Falling piece control (left/right/rotate/soft drop)
 *  - Gravity via AnimationTimer
 *  - Scoring + lines + elapsed time HUD
 *  - Pause (P) overlay and exit-to-main (ESC while paused)
 *  - Light styling (gradient bg, framed board, glossy tiles)
 */
public class GameView extends AbstractScreen {

    /* ============ sizes ============ */
    private static final int TILE = 30;
    private static final int BOARD_W = Board.COLS * TILE;   // 300
    private static final int BOARD_H = Board.ROWS * TILE;   // 600

    /* ============ game state ============ */
    private final Board board = new Board();
    private final Random rng = new Random();
    private PieceState active = null;
    private Tetromino nextPiece = null;
    private boolean paused = false;
    private long lastDrop = 0;
    private long dropIntervalNanos = 500_000_000L;
    private long runStartNanos;
    private int score = 0;
    private int lines = 0;

    private final Runnable onExitToMenu;

    /* ============ view ============ */
    // layer that contains the grid and all block sprites
    private final Group gridLayer = new Group();
    private final Group boardLayer = new Group(); // holds gridLayer + blocks

    private final Label pauseOverlay = new Label();

    // HUD labels (score, lines, time)
    private final Label scoreLabel = new Label("SCORE 0");
    private final Label linesLabel = new Label("LINES 0");
    private final Label timeLabel  = new Label("TIME 00:00");

    private final List<Sprite> tempSprites = new ArrayList<>();

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
            drawSprites();
            updateHud(now);
        }
    };

    public GameView(Runnable onExitToMenu) {
        this.onExitToMenu = onExitToMenu;

        /* ---------- HUD (right) ---------- */
        var hud = new VBox(16);
        hud.setAlignment(Pos.TOP_LEFT);
        hud.getStyleClass().add("hud");

        Label nextTitle = new Label("NEXT (M1: placeholder)");
        nextTitle.getStyleClass().add("hud-title");

        for (Label lbl : new Label[]{scoreLabel, linesLabel, timeLabel}) {
            lbl.setTextFill(Color.WHITE);
            lbl.setFont(Font.font(14));
        }
        hud.getChildren().addAll(nextTitle, new Label(""), scoreLabel, linesLabel, timeLabel);

        /* ---------- Grid + board layer ---------- */
        buildGrid(gridLayer);
        boardLayer.getChildren().add(gridLayer); // grid first; blocks drawn over it

        // surface that holds boardLayer (fixed size)
        StackPane boardSurface = new StackPane(boardLayer);
        boardSurface.getStyleClass().add("board-surface");
        boardSurface.setMinSize(BOARD_W, BOARD_H);
        boardSurface.setPrefSize(BOARD_W, BOARD_H);
        boardSurface.setMaxSize(BOARD_W, BOARD_H);

        // centered pause overlay on top of board
        pauseOverlay.setText("Game Paused (P)\nESC to Main Menu\nR to Restart Game");
        pauseOverlay.setTextFill(Color.WHITE);
        pauseOverlay.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        pauseOverlay.setVisible(false);
        boardSurface.getChildren().add(pauseOverlay);
        StackPane.setAlignment(pauseOverlay, Pos.CENTER);

        /* ---------- Framed game rectangle (board + HUD) ---------- */
        HBox gameRow = new HBox(16, boardSurface, hud);
        gameRow.getStyleClass().add("game-row");

        // frame around the whole game area
        StackPane gameFrame = new StackPane(gameRow);
        gameFrame.getStyleClass().add("board-frame"); // cyan border + shadow via CSS
        gameFrame.setPadding(new Insets(8));

        /* ---------- Back button (bottom, separate) ---------- */
        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onExitToMenu != null) onExitToMenu.run(); });

        HBox backBar = new HBox(back);
        backBar.getStyleClass().add("center-bar");

        /* ---------- Root ---------- */
        VBox root = new VBox(12, gameFrame, backBar);
        root.getStyleClass().add("app-bg");
        root.setFillWidth(false);
        getChildren().add(root);

        // input
        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);
    }

    /* ============ lifecycle ============ */
    @Override public void onShow() {
        // when screen becomes visible: focus, reset timer, start loop
        requestFocus();
        lastDrop = 0;
        runStartNanos = System.nanoTime();
        loop.start();
    }

    /* ============ gameplay ============ */
    private void spawn() {
        if (nextPiece == null) nextPiece = randomPiece(); // ensure queue has a piece
        Tetromino t = nextPiece;
        nextPiece = randomPiece();
        active = new PieceState(t, 0, 0, 3);
        if (!canPlace(active)) paused = true; // simple game-over
    }
    private Tetromino randomPiece() {
        return Tetromino.values()[rng.nextInt(Tetromino.values().length)];
    }

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

    private boolean tryMove(int dr, int dc, int drot) {
        int newRot = active.rot();
        if (drot != 0) {
            newRot = (newRot + drot + active.type().rotationCount()) % active.type().rotationCount();
        }
        PieceState next = new PieceState(active.type(), newRot, active.row() + dr, active.col() + dc);
        if (canPlace(next)) { active = next; return true; }
        return false;
    }

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

    /* ============ input ============ */
    private void onKey(KeyEvent e) {
        KeyCode k = e.getCode();
        switch (k) {
            case LEFT  -> tryMove(0, -1, 0);
            case RIGHT -> tryMove(0, +1, 0);
            case UP    -> tryMove(0, 0, +1);
            case DOWN  -> { if (!tryMove(1, 0, 0)) lockPiece(); }
            case P     -> { paused = !paused; pauseOverlay.setVisible(paused); }
            case ESCAPE -> { if (paused && onExitToMenu != null) onExitToMenu.run(); }
            case R     -> { if (paused) restartGame(); }
            default -> {}
        }
    }

    /* ============ rendering (sprites) ============ */
    private void drawSprites() {
        // keep only the grid; re-add block nodes
        boardLayer.getChildren().setAll(gridLayer);

        tempSprites.clear();

        // placed cells
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                int v = board.get(r, c);
                if (v != 0) addBlock(c, r, v);
            }
        }

        // active piece
        if (active != null) {
            int[][] m = active.type().shape(active.rot());
            for (int r = 0; r < m.length; r++) {
                for (int c = 0; c < m[r].length; c++) {
                    if (m[r][c] != 0) addBlock(active.col() + c, active.row() + r, active.type().colorId());
                }
            }
        }

        // commit blocks
        for (Sprite s : tempSprites) boardLayer.getChildren().add(s.getNode());
    }

    private void addBlock(int col, int row, int colorId) {
        BlockSprite sprite = new BlockSprite(TILE, colorFor(colorId));
        sprite.setXY(col * TILE, row * TILE);
        tempSprites.add(sprite);
    }

    private Color colorFor(int id) {
        return switch (id) {
            case 1 -> Color.CYAN;       // I
            case 2 -> Color.YELLOW;     // O
            case 3 -> Color.PURPLE;     // T
            case 4 -> Color.LIMEGREEN;  // S
            case 5 -> Color.RED;        // Z
            case 6 -> Color.BLUE;       // J
            case 7 -> Color.ORANGE;     // L
            default -> Color.GRAY;
        };
    }

    /* ============ HUD & restart ============ */
    private void updateHud(long now) {
        long elapsedSec = Math.max(0, (now - runStartNanos) / 1_000_000_000L);
        long mm = elapsedSec / 60, ss = elapsedSec % 60;
        timeLabel.setText(String.format("TIME %02d:%02d", mm, ss));
        scoreLabel.setText("SCORE " + score);
        linesLabel.setText("LINES " + lines);
    }

    private void restartGame() {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) board.set(r, c, 0);
        }
        score = 0; lines = 0; active = null; nextPiece = null;
        lastDrop = 0; runStartNanos = System.nanoTime();
        paused = false; pauseOverlay.setVisible(false);
        drawSprites();
        requestFocus();
    }

    /* ============ helpers ============ */
    private void buildGrid(Group into) {
        into.getChildren().clear();
        Color gridColor = Color.color(1,1,1,0.10);
        for (int x = 0; x <= Board.COLS; x++) {
            var line = new javafx.scene.shape.Line(x * TILE, 0, x * TILE, BOARD_H);
            line.setStroke(gridColor);
            into.getChildren().add(line);
        }
        for (int y = 0; y <= Board.ROWS; y++) {
            var line = new javafx.scene.shape.Line(0, y * TILE, BOARD_W, y * TILE);
            line.setStroke(gridColor);
            into.getChildren().add(line);
        }
    }
}
