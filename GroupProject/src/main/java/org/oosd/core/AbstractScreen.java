package org.oosd.core;

import javafx.scene.layout.Pane;

/**
 * This is a base class for all screens in the game (e.g., main menu, game, high scores).
 *
 * We extend JavaFX's 'Pane' so we can easily add visual elements (buttons, canvas, etc.).
 *
 * We also implement Updatable so each screen can respond to the game loop.
 */
public abstract class AbstractScreen extends Pane implements Updatable {

    /**
     * Called when this screen becomes visible.
     * Example: When we switch to the game view, we can set focus or start timers.
     */
    public abstract void onShow();

    /**
     * Called when this screen is hidden or replaced.
     * Example: When leaving the game view, stop timers or music.
     */
    public abstract void onHide();
}
