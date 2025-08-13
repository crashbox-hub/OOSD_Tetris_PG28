package org.oosd.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.oosd.core.AbstractScreen;

/**
 * This is the main gameplay screen.
 * It draws the grid, handles user input, and updates the game state.
 */
public class GameView extends AbstractScreen {

    // Size of each tile in pixels
    private static final int TILE = 30;

    // Canvas is where we draw everything for the game
    private final Canvas canvas = new Canvas(10 * TILE, 20 * TILE);
    private final GraphicsContext g = canvas.getGraphicsContext2D();

    public GameView() {
        // Add the canvas to the screen
        getChildren().add(canvas);

        // Make sure the screen can receive keyboard input
        setFocusTraversable(true);
    }

    @Override
    public void onShow() {
        // Called when the game view becomes visible
        requestFocus();
        drawGrid();
    }

    @Override
    public void onHide() {
        // Called when leaving the game view
    }

    @Override
    public void update(long nowNanos) {
        // Will be called each frame in the game loop
    }

    /**
     * Draws the empty Tetris grid lines.
     */
    private void drawGrid() {
        // Fill background
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw faint grid lines
        g.setStroke(Color.color(1, 1, 1, 0.1));
        for (int x = 0; x <= 10; x++)
            g.strokeLine(x * TILE, 0, x * TILE, 20 * TILE);
        for (int y = 0; y <= 20; y++)
            g.strokeLine(0, y * TILE, 10 * TILE, y * TILE);
    }
}
