package org.oosd;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.oosd.core.AbstractScreen;
import org.oosd.ui.*;

public class Main extends Application {

    private Stage stage;
    private Scene scene;            // the one shared scene whose root we swap
    private AbstractScreen current; // the current AbstractScreen in 'scene'

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

    private void ensureSharedSceneOnStage() {
        // If we previously replaced the Stage's Scene (e.g., ConfigurationView),
        // switch back to the shared 'scene' before swapping roots.
        if (stage.getScene() != scene) {
            stage.setScene(scene);
        }
    }

    private void showSplash() {
        ensureSharedSceneOnStage();
        SplashScreenView splash = new SplashScreenView(this::showMainMenu);
        setScreen(splash);
        stage.setTitle("Tetris — Splash");
    }

    private void showMainMenu() {
        ensureSharedSceneOnStage();

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
        ensureSharedSceneOnStage();
        GameView game = new GameView();
        setScreen(game);
        stage.setTitle("Tetris — Game");
    }

    private void showHighScores() {
        ensureSharedSceneOnStage();
        HighScoresView scores = new HighScoresView(this::showMainMenu);
        setScreen(scores);
        stage.setTitle("Tetris — High Scores");
    }

    private void showConfiguration() {
        ConfigurationView cfg = new ConfigurationView();
        Scene cfgScene = cfg.createScene(stage, this::showMainMenu);
        stage.setScene(cfgScene);
        stage.setTitle("Tetris — Configuration");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
