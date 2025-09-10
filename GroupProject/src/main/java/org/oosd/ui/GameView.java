package org.oosd.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.oosd.core.AbstractScreen;
import org.oosd.game.*;

import java.util.Random;

public class GameView extends AbstractScreen {

    private static final int TILE = 30;
    private static final int BOARD_W = Board.COLS * TILE;
    private static final int BOARD_H = Board.ROWS * TILE;

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

    private final Runnable onExitToMenu;   // Back callback

    private final Canvas boardCanvas = new Canvas(BOARD_W, BOARD_H);
    private final GraphicsContext g = boardCanvas.getGraphicsContext2D();

    private final Canvas nextCanvas = new Canvas(4 * TILE, 4 * TILE);
    private final GraphicsContext gNext = nextCanvas.getGraphicsContext2D();

    private final Label scoreLabel = new Label("SCORE 0");
    private final Label linesLabel = new Label("LINES 0");
    private final Label timeLabel  = new Label("TIME 00:00");

    private final AnimationTimer loop = new AnimationTimer() {
        @Override public void handle(long now) {
            if (!paused) {
                if (active == null) spawn();
                if (now - lastDrop >= dropIntervalNanos) {
                    if (!tryMove(1, 0, 0)) lockPiece();
                    lastDrop = now;
                }
            }
            draw();
            updateHud(now);
        }
    };

    public GameView(Runnable onBack) {
        this.onExitToMenu = onBack;

        /* --------- HUD (right) --------- */
        VBox hud = new VBox(16);
        hud.getStyleClass().add("hud");
        hud.setAlignment(Pos.TOP_LEFT);
        Label nextTitle = new Label("NEXT");
        nextTitle.getStyleClass().add("hud-title");
        for (Label lbl : new Label[]{scoreLabel, linesLabel, timeLabel}) {
            lbl.getStyleClass().add("hud-label");
        }
        hud.getChildren().addAll(nextTitle, nextCanvas, new Label(), scoreLabel, linesLabel, timeLabel);

        /* --------- Board (left) inside framed rectangle --------- */
        Group boardGroup = new Group(boardCanvas);
        StackPane boardPane = new StackPane(boardGroup);
        boardPane.getStyleClass().add("board-frame");

        /* --------- Top “game area” row (board + HUD) --------- */
        HBox gameRow = new HBox(16, boardPane, hud);
        gameRow.getStyleClass().add("game-row");
        gameRow.setAlignment(Pos.TOP_CENTER);

        /* --------- Back button at the bottom (same style as Config screen) --------- */
        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onExitToMenu != null) onExitToMenu.run(); });
        HBox backBar = new HBox(back);
        backBar.getStyleClass().add("center-bar");

        /* --------- Panel that holds top game area and bottom back button --------- */
        VBox panel = new VBox(16, gameRow, backBar);
        panel.getStyleClass().add("panel");
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));

        /* --------- App background wrapper --------- */
        StackPane bg = new StackPane(panel);
        bg.getStyleClass().add("app-bg");
        getChildren().add(bg);

        // Input
        setFocusTraversable(true);
        setOnKeyPressed(this::onKey);
    }

    @Override public void onShow() {
        requestFocus();
        lastDrop = 0;
        runStartNanos = System.nanoTime();
        loop.start();
    }
    @Override public void onHide() { loop.stop(); }

    /* ---------------- Core gameplay (unchanged) ---------------- */
    private void spawn() {
        if (nextPiece == null) nextPiece = randomPiece();
        Tetromino t = nextPiece;
        nextPiece = randomPiece();
        active = new PieceState(t, 0, 0, 3);
        if (!canPlace(active)) paused = true;
        drawNextPreview();
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
        if (drot != 0) newRot = (newRot + drot + active.type().rotationCount()) % active.type().rotationCount();
        PieceState next = new PieceState(active.type(), newRot, active.row() + dr, active.col() + dc);
        if (canPlace(next)) { active = next; return true; }
        return false;
    }
    private void lockPiece() {
        int[][] m = active.type().shape(active.rot());
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    int br = active.row() + r, bc = active.col() + c;
                    if (board.inBounds(br, bc)) board.set(br, bc, active.type().colorId());
                }
            }
        }
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
        active = null;
    }

    /* ---------------- Input ---------------- */
    private void onKey(KeyEvent e) {
        KeyCode k = e.getCode();
        switch (k) {
            case LEFT  -> tryMove(0, -1, 0);
            case RIGHT -> tryMove(0, +1, 0);
            case UP    -> tryMove(0, 0, +1);
            case DOWN  -> { if (!tryMove(1, 0, 0)) lockPiece(); }
            case P     -> paused = !paused;
            case ESCAPE -> { if (onExitToMenu != null) onExitToMenu.run(); } // also exits from gameplay
            case R     -> { if (paused) restartGame(); }
            default    -> {}
        }
        draw();
    }

    /* ---------------- Rendering & HUD (unchanged) ---------------- */
    private void draw() {
        g.setFill(Color.rgb(8,12,20));
        g.fillRect(0, 0, BOARD_W, BOARD_H);
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                int v = board.get(r, c);
                if (v != 0) drawCell(c, r, v);
            }
        }
        if (active != null) {
            int[][] m = active.type().shape(active.rot());
            for (int r = 0; r < m.length; r++) {
                for (int c = 0; c < m[r].length; c++) {
                    if (m[r][c] != 0) drawCell(active.col() + c, active.row() + r, active.type().colorId());
                }
            }
        }
        g.setStroke(Color.color(1,1,1,0.10));
        for (int x = 0; x <= Board.COLS; x++) g.strokeLine(x*TILE, 0, x*TILE, BOARD_H);
        for (int y = 0; y <= Board.ROWS; y++) g.strokeLine(0, y*TILE, BOARD_W, y*TILE);

        if (paused) {
            g.setFill(Color.color(0,0,0,0.55));
            g.fillRect(0, 0, BOARD_W, BOARD_H);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            String[] lines = {"Game Paused (P)", "ESC to Main Menu", "R to Restart Game"};
            double lh = 28, total = lines.length * lh, y0 = (BOARD_H - total)/2 + 6;
            for (int i = 0; i < lines.length; i++) {
                Text t = new Text(lines[i]); t.setFont(g.getFont());
                double x = (BOARD_W - t.getLayoutBounds().getWidth())/2;
                g.fillText(lines[i], x, y0 + i*lh);
            }
        }
    }
    private void drawCell(int col, int row, int colorId) { /* same as your version */
        Color fill = switch (colorId) {
            case 1 -> Color.CYAN; case 2 -> Color.YELLOW; case 3 -> Color.PURPLE;
            case 4 -> Color.LIMEGREEN; case 5 -> Color.RED; case 6 -> Color.BLUE;
            case 7 -> Color.ORANGE; default -> Color.GRAY;
        };
        double x = col * TILE, y = row * TILE;
        g.setFill(fill); g.fillRect(x, y, TILE, TILE);
        g.setFill(Color.color(1,1,1,0.10)); g.fillRect(x, y, TILE, TILE*0.25);
        g.setStroke(Color.color(0,0,0,0.35)); g.strokeRect(x+0.5, y+0.5, TILE-1, TILE-1);
    }
    private void drawNextPreview() { /* unchanged */
        gNext.clearRect(0,0,nextCanvas.getWidth(),nextCanvas.getHeight());
        gNext.setFill(Color.rgb(12,18,28)); gNext.fillRect(0,0,nextCanvas.getWidth(),nextCanvas.getHeight());
        if (nextPiece == null) return;
        int[][] m = nextPiece.shape(0); int w=m[0].length, h=m.length;
        int xOff = (4 - w) * TILE / 2, yOff = (4 - h) * TILE / 2;
        for (int r=0;r<h;r++) for (int c=0;c<w;c++) if (m[r][c]!=0) {
            double x=c*TILE+xOff, y=r*TILE+yOff;
            gNext.setFill(Color.GRAY); gNext.fillRect(x,y,TILE,TILE);
            gNext.setFill(Color.color(1,1,1,0.10)); gNext.fillRect(x,y,TILE,TILE*0.25);
            gNext.setStroke(Color.color(0,0,0,0.35)); gNext.strokeRect(x+0.5,y+0.5,TILE-1,TILE-1);
        }
    }
    private void updateHud(long now) {
        long elapsedSec = Math.max(0, (now - runStartNanos) / 1_000_000_000L);
        long mm = elapsedSec / 60, ss = elapsedSec % 60;
        timeLabel.setText(String.format("TIME %02d:%02d", mm, ss));
        scoreLabel.setText("SCORE " + score);
        linesLabel.setText("LINES " + lines);
    }
    private void restartGame() {
        for (int r=0;r<Board.ROWS;r++) for (int c=0;c<Board.COLS;c++) board.set(r,c,0);
        score=0; lines=0; active=null; nextPiece=null; lastDrop=0; runStartNanos=System.nanoTime();
        paused=false; drawNextPreview(); draw(); requestFocus();
    }
}
