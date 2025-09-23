package org.oosd.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.oosd.core.AbstractScreen;
import org.oosd.core.GameConfig;
import org.oosd.game.*;

import org.oosd.ui.sprites.PieceSprite;
import org.oosd.ui.sprites.Sprite;
import org.oosd.ui.sprites.SpriteFactory;

import java.util.*;

/**
 * GameView (entity/sprite version).
 * - Uses GameConfig for sizing & timing.
 * - Active piece is a GameEntity updated with frame-rate independent logic.
 * - Sprites mirror entities; SpriteFactory builds the visuals.
 * - Board + HUD are enclosed in a framed rectangle; Back button is separate.
 */
public class GameView extends AbstractScreen {

    /* =========================
       Config-derived sizing
       ========================= */
    private final int TILE = GameConfig.get().tileSize();
    private final int BOARD_W = Board.COLS * TILE;
    private final int BOARD_H = Board.ROWS * TILE;

    /* =========================
       Core model
       ========================= */
    private final Board board = new Board();
    private final Runnable onExitToMenu;

    // World lists
    private final List<GameEntity> entities = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();
    private final List<GameEntity> toRemove = new ArrayList<>(); // safe removal queue

    // UI layers
    private final Group boardLayer = new Group(); // holds grid + placed + sprites
    private final Group gridLayer  = new Group(); // static faint grid

    // HUD
    private final Label scoreLabel = new Label("SCORE 0");
    private final Label linesLabel = new Label("LINES 0");
    private final Label timeLabel  = new Label("TIME 00:00");
    private final Label pauseOverlay = new Label();

    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;
    private int lines = 0;
    private long runStartNanos = 0L;

    /* =========================
       Main loop (FPS independent)
       ========================= */
    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            if (!paused) {
                // 1) advance all entities with delta time
                for (GameEntity e : entities) e.tick(now);

                // 2) safe removal + respawn
                removeDeadAndRespawn();

                // 3) sync piece sprites to entities
                for (Sprite s : sprites) {
                    if (s instanceof PieceSprite ps) ps.syncToEntity();
                }

                // 4) scoring – clear rows that were filled by a lock
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
            }

            // 5) redraw placed cells
            drawPlacedBlocks();

            // 6) HUD
            updateHud(now);
        }
    };

    /* =========================
       ctor / layout
       ========================= */
    public GameView(Runnable onExitToMenu) {
        this.onExitToMenu = onExitToMenu;

        // ----- HUD (right column) -----
        var hud = new VBox(16);
        hud.setAlignment(Pos.TOP_LEFT);
        hud.getStyleClass().add("hud");

        Label nextTitle = new Label("NEXT");
        nextTitle.getStyleClass().add("hud-title");

        for (Label lbl : new Label[]{scoreLabel, linesLabel, timeLabel}) {
            lbl.getStyleClass().add("hud-label");
        }
        hud.getChildren().addAll(nextTitle, new Label(""), scoreLabel, linesLabel, timeLabel);

        // ----- Board surface + grid -----
        buildGrid(gridLayer);
        boardLayer.getChildren().add(gridLayer);

        StackPane boardSurface = new StackPane(boardLayer);
        boardSurface.getStyleClass().add("board-surface");
        boardSurface.setMinSize(BOARD_W, BOARD_H);
        boardSurface.setPrefSize(BOARD_W, BOARD_H);
        boardSurface.setMaxSize(BOARD_W, BOARD_H);

        // Pause / Game Over overlay
        pauseOverlay.setText("Game Paused (P)\nESC to Main Menu\nR to Restart Game");
        pauseOverlay.setTextFill(Color.WHITE);
        pauseOverlay.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        pauseOverlay.setVisible(false);
        boardSurface.getChildren().add(pauseOverlay);
        StackPane.setAlignment(pauseOverlay, Pos.CENTER);

        // ----- Framed game rectangle -----
        HBox gameRow = new HBox(16, boardSurface, hud);
        gameRow.getStyleClass().add("game-row");

        StackPane gameFrame = new StackPane(gameRow);
        gameFrame.getStyleClass().add("board-frame");
        gameFrame.setPadding(new Insets(8));

        // ----- Back button -----
        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onExitToMenu != null) onExitToMenu.run(); });

        HBox backBar = new HBox(back);
        backBar.getStyleClass().add("center-bar");

        // ----- Root -----
        VBox root = new VBox(12, gameFrame, backBar);
        root.getStyleClass().add("app-bg");
        root.setFillWidth(false);
        getChildren().add(root);

        // Input focus + keys
        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);

        // Start with one active piece
        spawnActivePiece();
    }

    /* =========================
       Lifecycle
       ========================= */
    @Override public void onShow() {
        requestFocus();
        runStartNanos = System.nanoTime();
        loop.start();
    }
    @Override public void onHide() { loop.stop(); }

    /* =========================
       Spawning / removal
       ========================= */
    private void spawnActivePiece() {
        if (gameOver) return;
        int spawnCol = GameConfig.get().spawnCol();

        if (spawnAreaBlocked(spawnCol)) {
            triggerGameOver();
            return;
        }

        ActivePieceEntity piece = new ActivePieceEntity(board, null, spawnCol);
        addEntityWithSprite(piece);
    }

    private void addEntityWithSprite(GameEntity e) {
        entities.add(e);
        Sprite s = SpriteFactory.create(e);
        sprites.add(s);
        boardLayer.getChildren().add(s.getNode());
    }

    /** Safe removal using a staging list */
    private void removeDeadAndRespawn() {
        for (GameEntity e : entities) {
            if (e.isDead()) toRemove.add(e);
        }

        for (GameEntity e : toRemove) {
            entities.remove(e);

            Sprite s = findSprite(e);
            if (s != null) {
                sprites.remove(s);
                boardLayer.getChildren().remove(s.getNode());
            }

            if (e.entityType() == EntityType.ACTIVE_PIECE && !gameOver) {
                int spawnCol = GameConfig.get().spawnCol();
                if (spawnAreaBlocked(spawnCol)) {
                    triggerGameOver();
                } else {
                    spawnActivePiece();
                }
            }
        }

        toRemove.clear();
    }

    private boolean spawnAreaBlocked(int spawnCol) {
        int startCol = Math.max(0, Math.min(spawnCol - 1, Board.COLS - 4));
        int endCol   = Math.min(Board.COLS - 1, startCol + 3);
        int maxRows  = Math.min(4, Board.ROWS);

        for (int r = 0; r < maxRows; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (board.get(r, c) != 0) return true;
            }
        }
        return false;
    }

    private void triggerGameOver() {
        gameOver = true;
        paused = true;
        pauseOverlay.setText("Game Over\nESC to Main Menu\nR to Restart");
        pauseOverlay.setVisible(true);
    }

    private Sprite findSprite(GameEntity e) {
        for (Sprite s : sprites) if (s.getEntity() == e) return s;
        return null;
    }

    /* =========================
       Input handling
       ========================= */
    private void onKey(KeyEvent e) {
        ActivePieceEntity piece = entities.stream()
                .filter(ge -> ge.entityType() == EntityType.ACTIVE_PIECE)
                .map(ge -> (ActivePieceEntity) ge)
                .findFirst().orElse(null);

        switch (e.getCode()) {
            case LEFT  -> { if (!paused && piece != null) piece.tryLeft(); }
            case RIGHT -> { if (!paused && piece != null) piece.tryRight(); }
            case UP    -> { if (!paused && piece != null) piece.tryRotateCW(); }
            case DOWN  -> { if (!paused && piece != null) piece.softDropOrLock(); }
            case P     -> { if (!gameOver) { paused = !paused; pauseOverlay.setVisible(paused); } }
            case ESCAPE -> { if (paused && onExitToMenu != null) onExitToMenu.run(); }
            case R     -> { if (paused) restartGame(); }
            default -> {}
        }
    }

    /* =========================
       Rendering of placed blocks
       ========================= */
    private void drawPlacedBlocks() {
        boardLayer.getChildren().removeIf(n -> "placed".equals(n.getUserData()));

        Group placed = new Group();
        placed.setUserData("placed");

        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                int v = board.get(r, c);
                if (v != 0) {
                    var rect = new javafx.scene.shape.Rectangle(TILE, TILE);
                    rect.setTranslateX(c * TILE);
                    rect.setTranslateY(r * TILE);
                    rect.setFill(colorFor(v));
                    rect.setArcWidth(6); rect.setArcHeight(6);
                    rect.setStroke(Color.color(0,0,0,0.35));

                    var sheen = new javafx.scene.shape.Rectangle(TILE, TILE * 0.25);
                    sheen.setTranslateX(c * TILE);
                    sheen.setTranslateY(r * TILE);
                    sheen.setFill(Color.color(1,1,1,0.10));

                    placed.getChildren().addAll(rect, sheen);
                }
            }
        }

        if (boardLayer.getChildren().isEmpty() || boardLayer.getChildren().get(0) != gridLayer) {
            boardLayer.getChildren().add(0, gridLayer);
        }
        boardLayer.getChildren().add(1, placed);
    }

    private Color colorFor(int id) {
        return switch (id) {
            case 1 -> Color.CYAN;
            case 2 -> Color.YELLOW;
            case 3 -> Color.PURPLE;
            case 4 -> Color.LIMEGREEN;
            case 5 -> Color.RED;
            case 6 -> Color.BLUE;
            case 7 -> Color.ORANGE;
            default -> Color.GRAY;
        };
    }

    /* =========================
       HUD + restart
       ========================= */
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
        entities.clear();
        sprites.clear();
        boardLayer.getChildren().setAll(gridLayer);

        score = 0;
        lines = 0;
        paused = false;
        gameOver = false;
        pauseOverlay.setText("Game Paused (P)\nESC to Main Menu\nR to Restart Game");
        pauseOverlay.setVisible(false);

        runStartNanos = System.nanoTime();
        spawnActivePiece();
        requestFocus();
    }

    /* =========================
       Helpers
       ========================= */
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
