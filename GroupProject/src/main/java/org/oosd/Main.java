package org.oosd;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.oosd.ui.GameView;
// import org.oosd.ui.HighScoresView; // <- use this if you want to test that screen

/**
 * App entry point. Creates a JavaFX Stage (window) and shows our GameView.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // --- Choose which screen to show first ---
        GameView root = new GameView();               // main gameplay screen
        // HighScoresView root = new HighScoresView(() -> System.out.println("Back pressed"));

        // Create a Scene from the chosen root node
        Scene scene = new Scene(root);

        // Window setup
        stage.setTitle("Tetris - PG28");
        stage.setResizable(false);
        stage.setScene(scene);

        // Show the window, then notify the screen it became visible
        stage.show();
        root.onShow();

        // When window is closing, notify the screen (stop timers, etc.)
        stage.setOnCloseRequest(e -> root.onHide());
    }

    public static void main(String[] args) {
        launch(args); // boots JavaFX and calls start()
    }
}
