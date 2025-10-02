package org.oosd.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.oosd.core.AbstractScreen;
import org.oosd.core.GameConfig;
import org.oosd.core.HighScoreStore;
import org.oosd.game.*;
import org.oosd.ui.sprites.PieceSprite;
import org.oosd.ui.sprites.Sprite;
import org.oosd.ui.sprites.SpriteFactory;

import java.util.*;

public class GameView extends AbstractScreen {

    /* Config-derived sizing */
    private final int TILE = GameConfig.get().tileSize();

    /* Mode */
    private final int players;           // 1 or 2
    private final Runnable onExitToMenu;

    /* AI toggle (from config) */
    private final boolean aiEnabled = GameConfig.get().isAiEnabled();

    /* Shared piece generator (7-bag) */
    private static final class PieceBag {
        private final Random rng = new Random(System.nanoTime());
        private final ArrayDeque<Tetromino> bag = new ArrayDeque<>(7);
        synchronized Tetromino next() { if (bag.isEmpty()) refill(); return bag.removeFirst(); }
        private void refill() {
            List<Tetromino> list = new ArrayList<>(List.of(Tetromino.values()));
            Collections.shuffle(list, rng);
            bag.addAll(list);
        }
    }
    private final PieceBag pieceBag = new PieceBag();

    /* Per-player container */
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

        // board node for scaling
        StackPane boardSurface;

        // state
        Tetromino nextPiece = null;
        boolean paused = false;
        boolean gameOver = false;
        boolean scoreSaved = false;   // prevent multiple prompts
        int score = 0;
        int lines = 0;
        long runStartNanos = 0L;
        boolean spawnQueued = false;

        // AI timing (per-side)
        long aiLastMoveNs = 0L;
        long aiLastDropNs = 0L;
        long aiLastRotateNs = 0L;
        int  aiDir = 1; // simple left/right alternation

        Side(int id) { this.id = id; }
    }

    /* AI cadence (nanoseconds) */
    private static final long AI_MOVE_INTERVAL_NS   = 180_000_000L;   // ~0.18s
    private static final long AI_DROP_INTERVAL_NS   = 350_000_000L;   // ~0.35s
    private static final long AI_ROTATE_INTERVAL_NS = 2_000_000_000L; // ~2.0s
    private static final long AI_DIR_FLIP_INTERVAL_NS = 1_200_000_000L; // ~1.2s

    /* Sides (1 or 2) */
    private final List<Side> sides = new ArrayList<>(2);

    /* Convenience: board pixel size from GameConfig */
    private int boardW() { return GameConfig.get().cols() * TILE; }
    private int boardH() { return GameConfig.get().rows() * TILE; }

    /* Loop */
    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            for (Side s : sides) tickSide(s, now);
        }
    };

    private void tickSide(Side S, long now) {
        if (!S.paused) {
            // Lightweight AI: only when enabled and game not over
            if (aiEnabled && !S.gameOver) runAI(S, now);

            for (GameEntity e : S.entities) e.tick(now);
            removeDeadAndRespawn(S);
            if (S.spawnQueued && !S.gameOver) { S.spawnQueued = false; spawnActivePiece(S); }
            for (Sprite<?, ?> spr : S.sprites) if (spr instanceof PieceSprite ps) ps.syncToEntity();

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
        drawPlacedBlocks(S);
        updateHud(S, now);
    }

    /* ctor / layout */
    public GameView(Runnable onExitToMenu) {
        this(onExitToMenu, GameConfig.get().players());
    }

    public GameView(Runnable onExitToMenu, int players) {
        this.onExitToMenu = onExitToMenu;
        this.players = (players == 2 ? 2 : 1);

        getStyleClass().add("app-bg");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        sides.add(new Side(1));
        if (this.players == 2) sides.add(new Side(2));

        HBox row = new HBox(this.players == 1 ? 16 : 24);
        row.getStyleClass().add("game-row");
        row.setAlignment(Pos.CENTER);
        for (Side s : sides) row.getChildren().add(buildSide(s));

        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onExitToMenu != null) onExitToMenu.run(); });
        HBox backBar = new HBox(back);
        backBar.getStyleClass().add("center-bar");
        backBar.setAlignment(Pos.CENTER);

        StackPane frame = new StackPane(row);
        frame.getStyleClass().add("board-frame");
        frame.setPadding(new Insets(8));
        StackPane.setAlignment(row, Pos.CENTER);
        frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        VBox content = new VBox(12, frame, backBar);
        content.setAlignment(Pos.CENTER);
        content.setFillWidth(false);
        content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().setAll(content);
        StackPane.setAlignment(content, Pos.CENTER);

        // Focus + controls
        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);

        // Prime pieces
        for (Side s : sides) {
            s.nextPiece = pieceBag.next();
            spawnActivePiece(s);
            drawNextPreview(s);
        }

        widthProperty().addListener((o, ov, nv) -> applyScaling());
        heightProperty().addListener((o, ov, nv) -> applyScaling());
        applyScaling();
    }

    private VBox buildSide(Side S) {
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

        for (Label lbl : new Label[]{S.scoreLabel, S.linesLabel, S.timeLabel})
            lbl.getStyleClass().add("hud-label");

        VBox hud = new VBox(12, nextTitle, S.nextBox, S.scoreLabel, S.linesLabel, S.timeLabel);
        hud.setAlignment(Pos.CENTER);

        buildGrid(S.gridLayer);
        S.boardLayer.getChildren().add(S.gridLayer);

        S.boardSurface = new StackPane(S.boardLayer, S.fxLayer);
        S.boardSurface.getStyleClass().add("board-surface");
        S.boardSurface.setMinSize(boardW(), boardH());
        S.boardSurface.setPrefSize(boardW(), boardH());
        S.boardSurface.setMaxSize(boardW(), boardH());

        S.pauseOverlay.setText("Game Paused (" + (S.id == 1 ? "P" : "L") + ")\nESC to Main Menu\nR to Restart");
        S.pauseOverlay.setTextFill(Color.WHITE);
        S.pauseOverlay.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        S.pauseOverlay.setVisible(false);
        S.boardSurface.getChildren().add(S.pauseOverlay);
        StackPane.setAlignment(S.pauseOverlay, Pos.CENTER);

        VBox sideBox = new VBox(10, S.boardSurface, hud);
        sideBox.setAlignment(Pos.CENTER);
        return sideBox;
    }

    /* Scaling / Centering */
    private void applyScaling() {
        double containerW = Math.max(1, getWidth());
        double containerH = Math.max(1, getHeight());

        double gap = (players == 1 ? 16 : 24);
        double totalBoardsW = players == 1 ? boardW() : (boardW() * 2 + gap);
        double totalBoardsH = boardH() + 160; // board + rough HUD

        double scaleX = (containerW * 0.92) / totalBoardsW; // margin
        double scaleY = (containerH * 0.90) / totalBoardsH; // margin

        double s = Math.min(1.0, Math.min(scaleX, scaleY));
        if (s <= 0) s = 1.0;

        for (Side side : sides) {
            if (side.boardSurface != null) {
                side.boardSurface.setScaleX(s);
                side.boardSurface.setScaleY(s);
            }
        }
    }

    /* Lifecycle */
    @Override public void onShow() {
        requestFocus();
        long now = System.nanoTime();
        for (Side s : sides) s.runStartNanos = now;
        if (GameConfig.get().isMusicEnabled()) Sound.startGameBgm();
        loop.start();
    }

    @Override public void onHide() {
        loop.stop();
        Sound.stopBgm();
    }

    /* Spawning / removal */
    private void spawnActivePiece(Side S) {
        if (S.gameOver) return;
        if (S.nextPiece == null) S.nextPiece = pieceBag.next();

        Tetromino t = S.nextPiece;
        int desiredCol = GameConfig.get().spawnCol();
        int width = pieceWidth(t, 0);
        int col = Math.max(0, Math.min(desiredCol, GameConfig.get().cols() - width));

        if (!canPlaceAt(S.board, t, 0, 0, col)) {
            // game over (no prompt here; we defer to exit)
            S.gameOver = true;
            S.paused = true;
            S.pauseOverlay.setText("Game Over\nESC to Main Menu\nR to Restart");
            S.pauseOverlay.setVisible(true);
            if (GameConfig.get().isSfxEnabled()) Sound.playGameOver();
            return;
        }

        ActivePieceEntity piece = new ActivePieceEntity(S.board, t, col);
        addEntityWithSprite(S, piece);

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

    /* ---------- High scores (deferred until exit) ---------- */

    /** Returns true if the given score would appear in the top-10. */
    private boolean qualifiesForHighScore(int score) {
        // Never allow 0 to qualify
        if (score <= 0) return false;

        var list = HighScoreStore.load(); // sorted desc as per our store
        if (list.size() < 10) return true;
        int lastScore = list.getLast().score;
        return score > lastScore;
    }

    /** Run prompts sequentially for all qualifying sides, then exit to menu. */
    private void handleHighScoresAndExit() {
        // stop animation while dialogs are shown
        loop.stop();

        for (Side s : sides) {
            if (!s.scoreSaved && qualifiesForHighScore(s.score)) {
                promptHighScore(s);
            }
        }
        if (onExitToMenu != null) onExitToMenu.run();
    }

    /** Blocking prompt used only on exit (safe: loop stopped). */
    private void promptHighScore(Side S) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("New High Score!");
        dlg.setHeaderText("Player " + S.id + " scored " + S.score + " points.\n" +
                "Enter a name (max 5 letters/numbers):");
        dlg.setContentText("Name:");

        var tf = dlg.getEditor();
        tf.setPromptText("AAA");
        tf.textProperty().addListener((obs, oldV, newV) -> {
            String cleaned = (newV == null ? "" : newV.replaceAll("[^A-Za-z0-9]", ""))
                    .toUpperCase(Locale.ROOT);
            if (cleaned.length() > 5) cleaned = cleaned.substring(0, 5);
            if (!cleaned.equals(newV)) tf.setText(cleaned);
        });

        var result = dlg.showAndWait();
        if (result.isPresent()) {
            String name = result.get().trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (name.isEmpty()) name = "PLAYER";
            if (name.length() > 5) name = name.substring(0, 5);
            HighScoreStore.addScore(name, S.score);
            S.scoreSaved = true;
        }
    }

    /* Input handling */
    private void onKey(KeyEvent e) {
        if (players == 1) {
            Side s = sides.getFirst();
            handleControls(s, e.getCode(), KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN);
            if (e.getCode() == KeyCode.P && !s.gameOver) togglePause(s);
            if (e.getCode() == KeyCode.ESCAPE && s.paused) {
                handleHighScoresAndExit();
            }
            if (e.getCode() == KeyCode.R && s.paused) restartSide(s);
            return;
        }

        Side s1 = sides.get(0);
        Side s2 = sides.get(1);

        handleControls(s1, e.getCode(), KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN);
        handleControls(s2, e.getCode(), KeyCode.A,    KeyCode.D,     KeyCode.W,  KeyCode.S);

        if (e.getCode() == KeyCode.P && !s1.gameOver) togglePause(s1);
        if (e.getCode() == KeyCode.L && !s2.gameOver) togglePause(s2);

        boolean anyPaused = s1.paused || s2.paused;
        if (e.getCode() == KeyCode.ESCAPE && anyPaused) {
            handleHighScoresAndExit();
        }
        if (e.getCode() == KeyCode.R && anyPaused) { restartSide(s1); restartSide(s2); }
    }

    private void togglePause(Side s) {
        s.paused = !s.paused;
        s.pauseOverlay.setVisible(s.paused);
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

    /* --------- Simple per-side AI driver --------- */
    private void runAI(Side S, long now) {
        ActivePieceEntity piece = S.entities.stream()
                .filter(ge -> ge.entityType() == EntityType.ACTIVE_PIECE)
                .map(ge -> (ActivePieceEntity) ge)
                .findFirst().orElse(null);
        if (piece == null) return;

        // Alternate horizontal nudges
        if (now - S.aiLastMoveNs >= AI_MOVE_INTERVAL_NS) {
            if (S.aiDir >= 0) piece.tryRight(); else piece.tryLeft();
            S.aiLastMoveNs = now;
        }
        // Periodically drop
        if (now - S.aiLastDropNs >= AI_DROP_INTERVAL_NS) {
            piece.softDropOrLock();
            S.aiLastDropNs = now;
        }
        // Occasionally rotate
        if (now - S.aiLastRotateNs >= AI_ROTATE_INTERVAL_NS) {
            piece.tryRotateCW();
            if (GameConfig.get().isSfxEnabled()) Sound.playRotate();
            S.aiLastRotateNs = now;
        }
        // Flip direction every so often
        if ((now / AI_DIR_FLIP_INTERVAL_NS) % 2 == 0) S.aiDir = 1; else S.aiDir = -1;
    }

    /* Rendering / HUD */
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
        S.paused = false; S.gameOver = false; S.scoreSaved = false;
        S.pauseOverlay.setText("Game Paused (" + (S.id == 1 ? "P" : "L") + ")\nESC to Main Menu\nR to Restart");
        S.pauseOverlay.setVisible(false);

        S.runStartNanos = System.nanoTime();

        S.nextPiece = pieceBag.next();
        spawnActivePiece(S);
        drawNextPreview(S);

        requestFocus();
        applyScaling();
    }

    /* Helpers */
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
}
