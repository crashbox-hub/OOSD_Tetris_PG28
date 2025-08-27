package org.oosd;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.oosd.core.AbstractScreen;
import org.oosd.ui.*;

public class Main extends Application {

    private Stage stage;
    private Scene scene;
    private AbstractScreen current;

    @Override
    public void start(Stage stage) {
        this.stage = stage;


        this.scene = new Scene(new StackPane(), 820, 680);

        // 🔗 Attach app-wide stylesheet
        String css = getClass().getResource("/styles.css").toExternalForm();
        this.scene.getStylesheets().add(css);

        stage.setScene(scene);
        stage.setResizable(true);
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
        GameView game = new GameView(this::showMainMenu); // if your ctor uses a Back callback
        setScreen(game);
        stage.setTitle("Tetris — Game");
    }

    private void showHighScores() {
        HighScoresView scores = new HighScoresView(this::showMainMenu);
        setScreen(scores);
        stage.setTitle("Tetris — High Scores");
    }

    private void showConfiguration() {
        ConfigurationView cfg = new ConfigurationView(this::showMainMenu);
        setScreen(cfg);
        stage.setTitle("Tetris — Configuration");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
