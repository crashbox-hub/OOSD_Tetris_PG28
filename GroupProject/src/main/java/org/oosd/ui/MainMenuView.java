package org.oosd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.oosd.core.AbstractScreen;
import org.oosd.core.GameConfig;
import org.oosd.core.SettingsStore;


public class MainMenuView extends AbstractScreen {

    public MainMenuView(Runnable onPlay,
                        Runnable onConfig,
                        Runnable onHighScores,
                        Runnable onExit) {

        /* ===== Panel (card) ===== */
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel");    // gradient + border + shadow (styles.css)
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));

        // Title
        Label title = new Label("TETRIS");
        title.getStyleClass().addAll("title", "title-xxl");

        // Buttons
        Button startButton      = new Button("Start Game");
        Button configButton     = new Button("Configuration");
        Button highScoresButton = new Button("High Scores");
        Button exitButton       = new Button("Exit");

        startButton.getStyleClass().addAll("btn", "btn-primary");
        configButton.getStyleClass().addAll("btn", "btn-secondary");
        highScoresButton.getStyleClass().addAll("btn", "btn-secondary");
        exitButton.getStyleClass().addAll("btn", "btn-ghost");

        startButton.setOnAction(e -> { if (onPlay != null) onPlay.run(); });
        configButton.setOnAction(e -> { if (onConfig != null) onConfig.run(); });
        highScoresButton.setOnAction(e -> { if (onHighScores != null) onHighScores.run(); });
        exitButton.setOnAction(e -> {
            var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit");
            alert.setHeaderText("Exit the game?");
            alert.setContentText("Are you sure you want to quit?");
            var yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            var no  = new ButtonType("No",  ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);
            var result = alert.showAndWait();
            if (result.isPresent() && result.get() == yes && onExit != null) onExit.run();
        });

        VBox buttons = new VBox(12, startButton, configButton, highScoresButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(title, buttons);

        /* ===== Centering root =====
           This StackPane fills the whole AbstractScreen and centers the panel. */
        StackPane root = new StackPane(panel);
        root.getStyleClass().add("app-bg"); // same gradient background as other screens
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);

        // Make the StackPane match the screen size
        root.setMinSize(0, 0);
        root.prefWidthProperty().bind(widthProperty());
        root.prefHeightProperty().bind(heightProperty());

        getChildren().add(root);

        /* ===== Keyboard shortcuts ===== */
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> startButton.fire();
                case C            -> configButton.fire();
                case H            -> highScoresButton.fire();
                case ESCAPE, Q    -> exitButton.fire();
                default -> {}
            }
        });
        setFocusTraversable(true);
    }

    @Override public void onShow() {
        requestFocus();

        // Make sure highscores.json exists so it shows up in the folder immediately.
        org.oosd.core.HighScoreStore.initIfMissing();

        if (org.oosd.core.GameConfig.get().isMusicEnabled()) {
            org.oosd.ui.Sound.startMenuBgm();
        } else {
            org.oosd.ui.Sound.stopBgm();
        }
    }


    @Override public void onHide() {
        org.oosd.ui.Sound.stopBgm();
    }

}
