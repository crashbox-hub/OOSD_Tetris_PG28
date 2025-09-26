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
import javafx.scene.shape.Rectangle;
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
 * GameView (entity/sprite version) with "Next" preview + flying +N message on line clears.
 * - Piece-aware spawn check (only game-over if the actual next piece cannot fit).
 * - Queue of next piece displayed in a 4x4 preview on the HUD.
 * - Flying message layer shows +N for 3s when lines are cleared.
 */
public class GameView extends AbstractScreen {

    /* =========================
       Config-derived sizing
       ========================= */
    private final int TILE   = GameConfig.get().tileSize();
    private final int BOARD_W = Board.COLS * TILE;
    private final int BOARD_H = Board.ROWS * TILE;

    /* =========================
       Core model
       ========================= */
    private final Board board = new Board();
    private final Runnable onExitToMenu;

    // Entities/Sprites
    private final List<GameEntity> entities = new ArrayList<>();
    private final List<Sprite<?, ?>> sprites = new ArrayList<>();

    // Layers
    private final Group boardLayer = new Group(); // grid + placed + piece sprite(s)
    private final Group gridLayer  = new Group(); // faint grid (bottom-most)
    private final Group fxLayer    = new Group(); // transient flying messages (top of board)

    // HUD
    private final Label scoreLabel = new Label("SCORE 0");
    private final Label linesLabel = new Label("LINES 0");
    private final Label timeLabel  = new Label("TIME 00:00");
    private final Label pauseOverlay = new Label();

    // "Next" piece preview
    private final StackPane nextBox = new StackPane(); // fixed 4x4 area
    private final Group     nextLayer = new Group();   // we redraw shapes here

    private Tetromino nextPiece = null;   // queued piece (displayed)
    private final Random rng = new Random();

    private boolean paused = false;
    private boolean gameOver = false;
    private int score = 0;
    private int lines = 0;
    private long runStartNanos = 0L;

    // queue a spawn to avoid modifying entities during iteration
    private boolean spawnQueued = false;

    /* =========================
       Main loop (FPS independent)
       ========================= */
    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            if (!paused) {
                // 1) advance entities
                for (GameEntity e : entities) e.tick(now);

                // 2) cleanup (defer spawn to after loop)
                removeDeadAndRespawn();

                // ---- perform deferred spawns safely (prevents CME) ----
                if (spawnQueued && !gameOver) {
                    spawnQueued = false;
                    spawnActivePiece();    // will also refresh Next preview
                }

                // 3) sync piece sprites
                for (Sprite<?, ?> s : sprites) {
                    if (s instanceof PieceSprite ps) ps.syncToEntity();
                }

                // 4) clear rows + score (+ flying message)
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
                    // play SFX and show flying “+N”
                    org.oosd.ui.Sound.playLine();
                    showFlyingMessage("+" + cleared, BOARD_W / 2.0 - TILE, BOARD_H / 2.0);
                }
            }

            // 5) redraw placed blocks
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

        // ----- HUD -----
        var hud = new VBox(16);
        hud.setAlignment(Pos.TOP_LEFT);
        hud.getStyleClass().add("hud");

        Label nextTitle = new Label("NEXT");
        nextTitle.getStyleClass().add("hud-title");

        // Next box: 4x4 tiles area
        double nbSize = 4 * TILE;
        nextBox.setMinSize(nbSize, nbSize);
        nextBox.setPrefSize(nbSize, nbSize);
        nextBox.setMaxSize(nbSize, nbSize);
        nextBox.getChildren().add(nextLayer);
        nextBox.setStyle(
                "-fx-background-color: rgba(12,18,28,1.0);" +
                        "-fx-border-color: rgba(255,255,255,0.18);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );

        for (Label lbl : new Label[]{scoreLabel, linesLabel, timeLabel}) {
            lbl.getStyleClass().add("hud-label");
        }
        hud.getChildren().addAll(nextTitle, nextBox, scoreLabel, linesLabel, timeLabel);

        // ----- Board surface + grid -----
        buildGrid(gridLayer);
        boardLayer.getChildren().add(gridLayer);

        // Stack the board, fx messages, then add pause overlay as another child
        StackPane boardSurface = new StackPane(boardLayer, fxLayer);
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

        // ----- Frame + Back button -----
        HBox gameRow = new HBox(16, boardSurface, hud);
        gameRow.getStyleClass().add("game-row");

        StackPane gameFrame = new StackPane(gameRow);
        gameFrame.getStyleClass().add("board-frame");
        gameFrame.setPadding(new Insets(8));

        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onExitToMenu != null) onExitToMenu.run(); });

        HBox backBar = new HBox(back);
        backBar.getStyleClass().add("center-bar");

        VBox root = new VBox(12, gameFrame, backBar);
        root.getStyleClass().add("app-bg");
        root.setFillWidth(false);
        getChildren().add(root);

        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);

        // Prime queue + spawn first active piece
        nextPiece = randomPiece();
        spawnActivePiece();          // uses queued piece, then rolls the next
        drawNextPreview();           // reflect current "next"
    }

    /* =========================
       Lifecycle
       ========================= */
    @Override public void onShow() {
        requestFocus();
        runStartNanos = System.nanoTime();
        org.oosd.ui.Sound.startGameBgm();
        loop.start();
    }
    @Override public void onHide() {
        loop.stop();
        org.oosd.ui.Sound.stopBgm();
    }

    /* =========================
       Spawning / removal
       ========================= */
    private Tetromino randomPiece() {
        Tetromino[] all = Tetromino.values();
        return all[rng.nextInt(all.length)];
    }

    /**
     * Spawn the queued piece (nextPiece) if it fits at row 0; then queue a new next.
     * Uses a piece-aware game-over check and column clamping by piece width.
     */
    private void spawnActivePiece() {
        if (gameOver) return;

        // safety: if queue somehow empty
        if (nextPiece == null) nextPiece = randomPiece();

        Tetromino t = nextPiece;

        // Clamp column for the piece width (rotation 0 used for spawn)
        int desiredCol = GameConfig.get().spawnCol();
        int width = pieceWidth(t, 0);
        int col = Math.max(0, Math.min(desiredCol, Board.COLS - width));

        if (!canPlaceAt(t, 0, 0, col)) {
            triggerGameOver();
            return;
        }

        // Create entity for the queued piece
        ActivePieceEntity piece = new ActivePieceEntity(board, t, col);
        addEntityWithSprite(piece);

        // Roll the next and refresh preview
        nextPiece = randomPiece();
        drawNextPreview();
    }

    private void addEntityWithSprite(GameEntity e) {
        entities.add(e);
        Sprite<?, ?> s = SpriteFactory.create(e);
        sprites.add(s);
        boardLayer.getChildren().add(s.getNode());
    }

    private void removeDeadAndRespawn() {
        for (Iterator<GameEntity> it = entities.iterator(); it.hasNext();) {
            GameEntity e = it.next();
            if (e.isDead()) {
                // remove entity
                it.remove();

                // remove matching sprite (if any)
                Sprite<?, ?> s = findSprite(e);
                if (s != null) {
                    sprites.remove(s);
                    boardLayer.getChildren().remove(s.getNode());
                }

                // don't spawn here; just queue it (spawn happens after loop)
                if (e.entityType() == EntityType.ACTIVE_PIECE && !gameOver) {
                    spawnQueued = true;
                }
            }
        }
    }

    private Sprite<?, ?> findSprite(GameEntity e) {
        for (Sprite<?, ?> s : sprites) if (s.getEntity() == e) return s;
        return null;
    }

    private void triggerGameOver() {
        gameOver = true;
        paused = true;
        pauseOverlay.setText("Game Over\nESC to Main Menu\nR to Restart");
        pauseOverlay.setVisible(true);
        org.oosd.ui.Sound.stopBgm(); org.oosd.ui.Sound.playGameOver(); // <-- add this
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
            case LEFT -> {
                if (!paused && piece != null) {
                    piece.tryLeft();
                }
            }
            case RIGHT -> {
                if (!paused && piece != null) {
                    piece.tryRight();
                }
            }
            case UP -> {
                if (!paused && piece != null) {
                    piece.tryRotateCW();
                    org.oosd.ui.Sound.playRotate();
                }
            }
            case DOWN -> {
                if (!paused && piece != null) {
                    boolean moved = piece.softDropOrLock();
                }
            }
            case P -> {
                if (!gameOver) {
                    paused = !paused;
                    pauseOverlay.setVisible(paused);
                }
            }
            case ESCAPE -> { if (paused && onExitToMenu != null) onExitToMenu.run(); }
            case R -> { if (paused) restartGame(); }
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
                    Rectangle rect = new Rectangle(TILE, TILE);
                    rect.setTranslateX(c * TILE);
                    rect.setTranslateY(r * TILE);
                    rect.setFill(colorFor(v));
                    rect.setArcWidth(6); rect.setArcHeight(6);
                    rect.setStroke(Color.color(0,0,0,0.35));

                    Rectangle sheen = new Rectangle(TILE, TILE * 0.25);
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

    private Color colorFor(Tetromino t) {
        return colorFor(t.colorId());
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
        for (int r = 0; r < Board.ROWS; r++)
            for (int c = 0; c < Board.COLS; c++)
                board.set(r, c, 0);

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

        // reset queue and spawn
        nextPiece = randomPiece();
        spawnActivePiece();
        drawNextPreview();

        requestFocus();
    }

    /* =========================
       Helpers (spawn + next)
       ========================= */

    /** width in cells of the given rotation matrix (assumes rectangular) */
    private int pieceWidth(Tetromino t, int rot) {
        int[][] m = t.shape(rot);
        return (m.length == 0) ? 0 : m[0].length;
    }

    /** Can the given piece/rotation be placed at board row/col? */
    private boolean canPlaceAt(Tetromino t, int rot, int row, int col) {
        int[][] m = t.shape(rot);
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = row + r, bc = col + c;
                    if (br < 0 || br >= Board.ROWS || bc < 0 || bc >= Board.COLS) return false;
                    if (board.get(br, bc) != 0) return false;
                }
            }
        }
        return true;
    }

    /** Draw the queued next piece centered in the 4x4 preview box. */
    private void drawNextPreview() {
        nextLayer.getChildren().clear();
        if (nextPiece == null) return;

        int[][] m = nextPiece.shape(0);
        int w = (m.length == 0) ? 0 : m[0].length;
        int h = m.length;

        // center within 4x4
        int xOff = (4 - w) * TILE / 2;
        int yOff = (4 - h) * TILE / 2;

        Color fill = colorFor(nextPiece);

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (m[r][c] != 0) {
                    double x = c * TILE + xOff;
                    double y = r * TILE + yOff;

                    Rectangle rect = new Rectangle(TILE, TILE);
                    rect.setTranslateX(x);
                    rect.setTranslateY(y);
                    rect.setFill(fill);
                    rect.setArcWidth(6); rect.setArcHeight(6);
                    rect.setStroke(Color.color(0,0,0,0.35));

                    Rectangle sheen = new Rectangle(TILE, TILE * 0.25);
                    sheen.setTranslateX(x);
                    sheen.setTranslateY(y);
                    sheen.setFill(Color.color(1,1,1,0.10));

                    nextLayer.getChildren().addAll(rect, sheen);
                }
            }
        }
    }

    /** Build faint grid lines once. */
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

    /* =========================
       Flying message helper
       ========================= */
    private void showFlyingMessage(String text, double startX, double startY) {
        Label msg = new Label(text);
        // color by number of lines for a tiny bit of flair
        Color fill = switch (text) {
            case "+1" -> Color.LIMEGREEN;
            case "+2" -> Color.AQUA;
            case "+3" -> Color.ORCHID;
            case "+4" -> Color.GOLD;
            default -> Color.LIMEGREEN;
        };
        msg.setTextFill(fill);
        msg.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        msg.setTranslateX(startX);
        msg.setTranslateY(startY);

        fxLayer.getChildren().add(msg);

        // 3s upward drift + fade
        var move = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(3), msg);
        move.setByY(-50);

        var fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(3), msg);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        var anim = new javafx.animation.ParallelTransition(move, fade);
        anim.setOnFinished(e -> fxLayer.getChildren().remove(msg));
        anim.play();
    }
}
