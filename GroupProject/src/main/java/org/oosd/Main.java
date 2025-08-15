package org.oosd;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.oosd.core.AbstractScreen;
import org.oosd.ui.GameView;
import org.oosd.ui.HighScoresView;
import org.oosd.ui.MainMenuView;
import org.oosd.ui.SplashScreenView;

public class Main extends Application {

    private Stage stage;
    private Scene scene;
    private AbstractScreen current;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // One Scene; swap its root with different AbstractScreen instances
        this.scene = new Scene(new StackPane(), 640, 480);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Tetris - PG28");
        stage.show();

        showSplash();

        stage.setOnCloseRequest(e -> {
            if (current != null) current.onHide();
        });
    }

    /* ---------------------- Navigation helpers ---------------------- */

    private void setScreen(AbstractScreen next) {
        if (current != null) current.onHide();
        scene.setRoot(next);
        current = next;
        current.onShow();
    }

    private void showSplash() {
        SplashScreenView splash = new SplashScreenView(this::showMainMenu);
        setScreen(splash);
        stage.setTitle("Tetris — Splash");
    }

    private void showMainMenu() {
        MainMenuView menu = new MainMenuView(
                this::showGame,          // Play
                this::showConfiguration, // Configuration
                this::showHighScores,    // High Scores
                stage::close             // Exit
        );
        setScreen(menu);
        stage.setTitle("Tetris — Main Menu");
    }

    private void showGame() {
        GameView game = new GameView();     // your existing gameplay screen (extends AbstractScreen)
        setScreen(game);
        stage.setTitle("Tetris — Game");
    }

    private void showHighScores() {
        HighScoresView scores = new HighScoresView(this::showMainMenu); // assumes ctor takes Back callback
        setScreen(scores);
        stage.setTitle("Tetris — High Scores");
    }

    private void showConfiguration() {
        // TODO: swap in your real ConfigurationView when ready
        // For now, just return to the menu or use a placeholder screen.
        showMainMenu();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
