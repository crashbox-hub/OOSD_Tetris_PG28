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
 * GameView with 1|2 player support.
 * - 1P: original single-board UI.
 * - 2P: two boards side-by-side sharing a 7-bag piece sequence (players advance independently).
 * - Uses dynamic board size from GameConfig.
 */
public class GameView extends AbstractScreen {

    /* =========================
       Config-derived sizing
       ========================= */
    private final int TILE = GameConfig.get().tileSize();

    /* =========================
       Mode
       ========================= */
    private final int players;           // 1 or 2
    private final Runnable onExitToMenu;

    /* =========================
       Shared piece generator (7-bag)
       ========================= */
    private static final class PieceBag {
        private final Random rng = new Random(System.nanoTime());
        private final ArrayDeque<Tetromino> bag = new ArrayDeque<>(7);
        synchronized Tetromino next() {
            if (bag.isEmpty()) refill();
            return bag.removeFirst();
        }
        private void refill() {
            List<Tetromino> list = new ArrayList<>(List.of(Tetromino.values()));
            Collections.shuffle(list, rng);
            bag.addAll(list);
        }
    }
    private final PieceBag pieceBag = new PieceBag();

    /* =========================
       Per-player container
       ========================= */
    private static final class Side {
        final int id; // 1 or 2
        final Board board = new Board();
        final List<GameEntity> entities = new ArrayList<>();
        final List<Sprite<?, ?>> sprites = new ArrayList<>();

        // layers
        final Group gridLayer  = new Group();
        final Group boardLayer = new Group();
        final Group fxLayer    = new Group();

        // HUD
        final Label scoreLabel = new Label("SCORE 0");
        final Label linesLabel = new Label("LINES 0");
        final Label timeLabel  = new Label("TIME 00:00");
        final Label pauseOverlay = new Label();

        // NEXT preview (per side)
        final StackPane nextBox = new StackPane();
        final Group     nextLayer = new Group();

        // state
        Tetromino nextPiece = null;
        boolean paused = false;
        boolean gameOver = false;
        int score = 0;
        int lines = 0;
        long runStartNanos = 0L;
        boolean spawnQueued = false;

        // rng only used if single-player mode (kept for compatibility)
        final Random rng = new Random();

        Side(int id) { this.id = id; }
    }

    /* Single-player fields kept for compatibility (mapped to p1) */
    private final Side p1 = new Side(1);
    private final Side p2 = new Side(2);

    /* Convenience: board pixel size from GameConfig */
    private int boardW() { return GameConfig.get().cols() * TILE; }
    private int boardH() { return GameConfig.get().rows() * TILE; }

    /* =========================
       Loop
       ========================= */
    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            if (players == 1) {
                tickSide(p1, now);
            } else {
                tickSide(p1, now);
                tickSide(p2, now);
            }
        }
    };

    private void tickSide(Side S, long now) {
        if (!S.paused) {
            // 1) advance entities
            for (GameEntity e : S.entities) e.tick(now);

            // 2) cleanup (defer spawn to after loop)
            removeDeadAndRespawn(S);

            // 3) perform deferred spawn (safe)
            if (S.spawnQueued && !S.gameOver) {
                S.spawnQueued = false;
                spawnActivePiece(S);
            }

            // 4) sync piece sprites
            for (Sprite<?, ?> s : S.sprites)
                if (s instanceof PieceSprite ps) ps.syncToEntity();

            // 5) clear rows + score + sfx/fx
            int cleared = S.board.clearFullRows();
            if (cleared > 0) {
                S.lines += cleared;
                switch (cleared) {
                    case 1 -> S.score += 100;
                    case 2 -> S.score += 300;
                    case 3 -> S.score += 500;
                    case 4 -> S.score += 800;
                    default -> S.score += cleared * 100;
                }
                if (GameConfig.get().isSfxEnabled()) Sound.playLine();
                showFlyingMessage(S, "+" + cleared, boardW() / 2.0 - TILE, boardH() / 2.0);
            }
        }

        // 6) redraw placed + HUD
        drawPlacedBlocks(S);
        updateHud(S, now);
    }

    /* =========================
       ctor / layout
       ========================= */
    public GameView(Runnable onExitToMenu) {
        this(onExitToMenu, org.oosd.core.GameConfig.get().players());
    }
    public GameView(Runnable onExitToMenu, int players) {
        this.onExitToMenu = onExitToMenu;
        this.players = (players == 2 ? 2 : 1);

        // build UI
        VBox root = new VBox(12);
        root.getStyleClass().add("app-bg");
        root.setFillWidth(false);

        HBox row = (this.players == 1)
                ? new HBox(16, buildSide(p1))
                : new HBox(24, buildSide(p1), buildSide(p2));
        row.getStyleClass().add("game-row");
        row.setAlignment(Pos.CENTER);


        // back button
        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onExitToMenu != null) onExitToMenu.run(); });
        HBox backBar = new HBox(back);
        backBar.getStyleClass().add("center-bar");

        StackPane frame = new StackPane(row);
        frame.getStyleClass().add("board-frame");
        frame.setPadding(new Insets(8));
        StackPane.setAlignment(row, Pos.CENTER);


        root.getChildren().addAll(frame, backBar);
        getChildren().add(root);

        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);

        // Prime each side
        if (this.players == 1) {
            p1.nextPiece = pieceBag.next();
            spawnActivePiece(p1);
            drawNextPreview(p1);
        } else {
            p1.nextPiece = pieceBag.next();
            spawnActivePiece(p1);
            drawNextPreview(p1);

            p2.nextPiece = pieceBag.next();
            spawnActivePiece(p2);
            drawNextPreview(p2);
        }
    }

    private VBox buildSide(Side S) {
        // HUD
        Label nextTitle = new Label((players == 2 ? ("PLAYER " + S.id + " — ") : "") + "NEXT");
        nextTitle.getStyleClass().add("hud-title");

        S.nextBox.setMinSize(4 * TILE, 4 * TILE);
        S.nextBox.setPrefSize(4 * TILE, 4 * TILE);
        S.nextBox.setMaxSize(4 * TILE, 4 * TILE);
        S.nextBox.getChildren().add(S.nextLayer);
        S.nextBox.setStyle(
                "-fx-background-color: rgba(12,18,28,1.0);" +
                        "-fx-border-color: rgba(255,255,255,0.18);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;"
        );

        for (Label lbl : new Label[]{S.scoreLabel, S.linesLabel, S.timeLabel}) {
            lbl.getStyleClass().add("hud-label");
        }
        VBox hud = new VBox(16, nextTitle, S.nextBox, S.scoreLabel, S.linesLabel, S.timeLabel);
        hud.setAlignment(Pos.TOP_LEFT);

        // grid bottom-most
        buildGrid(S.gridLayer);
        S.boardLayer.getChildren().add(S.gridLayer);

        // board + fx + overlay
        StackPane boardSurface = new StackPane(S.boardLayer, S.fxLayer);
        boardSurface.getStyleClass().add("board-surface");
        boardSurface.setMinSize(boardW(), boardH());
        boardSurface.setPrefSize(boardW(), boardH());
        boardSurface.setMaxSize(boardW(), boardH());

        S.pauseOverlay.setText("Game Paused (" + (S.id == 1 ? "P" : "L") + ")\nESC to Main Menu\nR to Restart");
        S.pauseOverlay.setTextFill(Color.WHITE);
        S.pauseOverlay.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        S.pauseOverlay.setVisible(false);
        boardSurface.getChildren().add(S.pauseOverlay);
        StackPane.setAlignment(S.pauseOverlay, Pos.CENTER);

        return new VBox(10, boardSurface, hud);
    }

    /* =========================
       Lifecycle
       ========================= */
    @Override public void onShow() {
        requestFocus();
        long now = System.nanoTime();
        p1.runStartNanos = now;
        if (players == 2) p2.runStartNanos = now;

        if (GameConfig.get().isMusicEnabled()) Sound.startGameBgm();
        loop.start();
    }

    @Override public void onHide() {
        loop.stop();
        Sound.stopBgm();
    }

    /* =========================
       Spawning / removal
       ========================= */
    private void spawnActivePiece(Side S) {
        if (S.gameOver) return;
        if (S.nextPiece == null) S.nextPiece = pieceBag.next();

        Tetromino t = S.nextPiece;
        int desiredCol = GameConfig.get().spawnCol();
        int width = pieceWidth(t, 0);
        int col = Math.max(0, Math.min(desiredCol, GameConfig.get().cols() - width));

        if (!canPlaceAt(S.board, t, 0, 0, col)) {
            triggerGameOver(S);
            return;
        }

        ActivePieceEntity piece = new ActivePieceEntity(S.board, t, col);
        addEntityWithSprite(S, piece);

        // roll next from shared bag
        S.nextPiece = pieceBag.next();
        drawNextPreview(S);
    }

    private void addEntityWithSprite(Side S, GameEntity e) {
        S.entities.add(e);
        Sprite<?, ?> s = SpriteFactory.create(e);
        S.sprites.add(s);
        S.boardLayer.getChildren().add(s.getNode());
    }

    private void removeDeadAndRespawn(Side S) {
        for (Iterator<GameEntity> it = S.entities.iterator(); it.hasNext();) {
            GameEntity e = it.next();
            if (e.isDead()) {
                it.remove();
                Sprite<?, ?> s = findSprite(S, e);
                if (s != null) {
                    S.sprites.remove(s);
                    S.boardLayer.getChildren().remove(s.getNode());
                }
                if (e.entityType() == EntityType.ACTIVE_PIECE && !S.gameOver) {
                    S.spawnQueued = true;
                }
            }
        }
    }

    private Sprite<?, ?> findSprite(Side S, GameEntity e) {
        for (Sprite<?, ?> s : S.sprites) if (s.getEntity() == e) return s;
        return null;
    }

    private void triggerGameOver(Side S) {
        S.gameOver = true;
        S.paused = true;
        S.pauseOverlay.setText("Game Over\nESC to Main Menu\nR to Restart");
        S.pauseOverlay.setVisible(true);
        if (GameConfig.get().isSfxEnabled()) Sound.playGameOver();
    }

    /* =========================
       Input handling
       ========================= */
    private void onKey(KeyEvent e) {
        if (players == 1) {
            handleControls(p1, e.getCode(), KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN);
            if (e.getCode() == KeyCode.P && !p1.gameOver) { p1.paused = !p1.paused; p1.pauseOverlay.setVisible(p1.paused); }
            if (e.getCode() == KeyCode.ESCAPE && p1.paused && onExitToMenu != null) onExitToMenu.run();
            if (e.getCode() == KeyCode.R && p1.paused) restartSide(p1);
            return;
        }

        // 2P
        handleControls(p1, e.getCode(), KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN);
        handleControls(p2, e.getCode(), KeyCode.A,    KeyCode.D,     KeyCode.W,  KeyCode.S);

        if (e.getCode() == KeyCode.P && !p1.gameOver) { p1.paused = !p1.paused; p1.pauseOverlay.setVisible(p1.paused); }
        if (e.getCode() == KeyCode.L && !p2.gameOver) { p2.paused = !p2.paused; p2.pauseOverlay.setVisible(p2.paused); }

        boolean anyPaused = p1.paused || p2.paused;
        if (e.getCode() == KeyCode.ESCAPE && anyPaused && onExitToMenu != null) onExitToMenu.run();
        if (e.getCode() == KeyCode.R && anyPaused) { restartSide(p1); restartSide(p2); }
    }

    private void handleControls(Side S, KeyCode code,
                                KeyCode left, KeyCode right, KeyCode rot, KeyCode down) {
        ActivePieceEntity piece = S.entities.stream()
                .filter(ge -> ge.entityType() == EntityType.ACTIVE_PIECE)
                .map(ge -> (ActivePieceEntity) ge)
                .findFirst().orElse(null);

        if (piece == null || S.paused) return;

        if (code == left) piece.tryLeft();
        else if (code == right) piece.tryRight();
        else if (code == rot)  { piece.tryRotateCW(); if (GameConfig.get().isSfxEnabled()) Sound.playRotate(); }
        else if (code == down) piece.softDropOrLock();
    }

    /* =========================
       Rendering / HUD
       ========================= */
    private void drawPlacedBlocks(Side S) {
        S.boardLayer.getChildren().removeIf(n -> "placed".equals(n.getUserData()));

        Group placed = new Group();
        placed.setUserData("placed");

        for (int r = 0; r < GameConfig.get().rows(); r++) {
            for (int c = 0; c < GameConfig.get().cols(); c++) {
                int v = S.board.get(r, c);
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
        if (S.boardLayer.getChildren().isEmpty() || S.boardLayer.getChildren().getFirst() != S.gridLayer)
            S.boardLayer.getChildren().add(0, S.gridLayer);
        S.boardLayer.getChildren().add(1, placed);
    }

    private void drawNextPreview(Side S) {
        S.nextLayer.getChildren().clear();
        if (S.nextPiece == null) return;

        int[][] m = S.nextPiece.shape(0);
        int w = (m.length == 0) ? 0 : m[0].length;
        int h = m.length;

        int xOff = (4 - w) * TILE / 2;
        int yOff = (4 - h) * TILE / 2;

        Color fill = colorFor(S.nextPiece.colorId());

        for (int r = 0; r < h; r++) for (int c = 0; c < w; c++) if (m[r][c] != 0) {
            double x = c * TILE + xOff, y = r * TILE + yOff;

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

            S.nextLayer.getChildren().addAll(rect, sheen);
        }
    }

    private void updateHud(Side S, long now) {
        long elapsedSec = Math.max(0, (now - S.runStartNanos) / 1_000_000_000L);
        long mm = elapsedSec / 60, ss = elapsedSec % 60;
        S.timeLabel.setText(String.format("TIME %02d:%02d", mm, ss));
        S.scoreLabel.setText("SCORE " + S.score);
        S.linesLabel.setText("LINES " + S.lines);
    }

    private void restartSide(Side S) {
        for (int r = 0; r < GameConfig.get().rows(); r++)
            for (int c = 0; c < GameConfig.get().cols(); c++)
                S.board.set(r, c, 0);

        S.entities.clear();
        S.sprites.clear();
        S.boardLayer.getChildren().setAll(S.gridLayer);

        S.score = 0; S.lines = 0;
        S.paused = false; S.gameOver = false;
        S.pauseOverlay.setText("Game Paused (" + (S.id == 1 ? "P" : "L") + ")\nESC to Main Menu\nR to Restart");
        S.pauseOverlay.setVisible(false);

        S.runStartNanos = System.nanoTime();

        // keep bag shared, just new "next" then spawn
        S.nextPiece = pieceBag.next();
        spawnActivePiece(S);
        drawNextPreview(S);

        requestFocus();
    }

    /* =========================
       Helpers
       ========================= */
    private int pieceWidth(Tetromino t, int rot) {
        int[][] m = t.shape(rot);
        return (m.length == 0) ? 0 : m[0].length;
    }

    private boolean canPlaceAt(Board board, Tetromino t, int rot, int row, int col) {
        int[][] m = t.shape(rot);
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) if (m[r][c] != 0) {
                int br = row + r, bc = col + c;
                if (br < 0 || br >= GameConfig.get().rows() || bc < 0 || bc >= GameConfig.get().cols()) return false;
                if (board.get(br, bc) != 0) return false;
            }
        }
        return true;
    }

    private void buildGrid(Group into) {
        into.getChildren().clear();
        Color gridColor = Color.color(1,1,1,0.10);
        for (int x = 0; x <= GameConfig.get().cols(); x++) {
            var line = new javafx.scene.shape.Line(x * TILE, 0, x * TILE, boardH());
            line.setStroke(gridColor);
            into.getChildren().add(line);
        }
        for (int y = 0; y <= GameConfig.get().rows(); y++) {
            var line = new javafx.scene.shape.Line(0, y * TILE, boardW(), y * TILE);
            line.setStroke(gridColor);
            into.getChildren().add(line);
        }
    }

    private void showFlyingMessage(Side S, String text, double startX, double startY) {
        Label msg = new Label(text);
        Color fill = switch (text) {
            case "+2" -> Color.AQUA;
            case "+3" -> Color.ORCHID;
            case "+4" -> Color.GOLD;
            default -> Color.LIMEGREEN;
        };
        msg.setTextFill(fill);
        msg.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        msg.setTranslateX(startX);
        msg.setTranslateY(startY);

        S.fxLayer.getChildren().add(msg);

        var move = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(3), msg);
        move.setByY(-50);

        var fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(3), msg);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        var anim = new javafx.animation.ParallelTransition(move, fade);
        anim.setOnFinished(e -> S.fxLayer.getChildren().remove(msg));
        anim.play();
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
}
