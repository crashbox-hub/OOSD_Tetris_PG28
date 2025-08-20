package org.oosd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.oosd.core.AbstractScreen;

public class MainMenuView extends AbstractScreen {

    public MainMenuView(Runnable onPlay,
                        Runnable onConfig,
                        Runnable onHighScores,
                        Runnable onExit) {

        StackPane root = new StackPane();

        root.setPrefSize(510, 640);
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf("linear-gradient(#0b1220, #0f1830)"),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));
        panel.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf("linear-gradient(#072032, #0d2b45)"),
                new CornerRadii(12), Insets.EMPTY
        )));
        panel.setBorder(new Border(new BorderStroke(
                Color.web("#2fd0ff"),
                BorderStrokeStyle.SOLID, new CornerRadii(12),
                new BorderWidths(3)
        )));
        panel.setEffect(new DropShadow(20, Color.color(0,0,0,0.45)));

        Label title = new Label("TETRIS");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(null, FontWeight.EXTRA_BOLD, 56));
        title.setEffect(new DropShadow(24, Color.color(0,0,0,0.6)));

        Button startButton = primaryButton("Start Game");
        Button configButton = secondaryButton("Configuration");
        Button highScoresButton = secondaryButton("High Scores");
        Button exitButton = ghostButton("Exit");

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
        root.getChildren().add(panel);
        getChildren().add(root);

        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> startButton.fire();
                case C -> configButton.fire();
                case H -> highScoresButton.fire();
                case ESCAPE, Q -> exitButton.fire();
                default -> {}
            }
        });
        setFocusTraversable(true);
    }
    @Override public void onShow() { requestFocus(); }
    @Override public void onHide() { }

    private Button primaryButton(String text) {
        Button b = baseButton(text);
        b.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf("linear-gradient(#5ce1e6, #26c2d4)"),
                new CornerRadii(12), Insets.EMPTY
        )));
        b.setTextFill(Color.web("#0b0f12"));
        addHoverPressEffects(b,
                "linear-gradient(#6cf0ef, #30d0df)",
                "linear-gradient(#42cdd1, #1bb6c9)");
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = baseButton(text);
        b.setBackground(new Background(new BackgroundFill(
                Color.color(1,1,1,0.10),
                new CornerRadii(12), Insets.EMPTY
        )));
        b.setTextFill(Color.WHITE);
        addHoverPressEffects(b,
                "rgba(255,255,255,0.16)",
                "rgba(255,255,255,0.22)");
        return b;
    }

    private Button ghostButton(String text) {
        Button b = baseButton(text);
        b.setBackground(new Background(new BackgroundFill(
                Color.TRANSPARENT, new CornerRadii(12), Insets.EMPTY
        )));
        b.setBorder(new Border(new BorderStroke(
                Color.color(1,1,1,0.25),
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.5)
        )));
        b.setTextFill(Color.color(1,1,1,0.85));
        b.setOnMouseEntered(e -> {
            b.setBorder(new Border(new BorderStroke(
                    Color.color(1,1,1,0.45),
                    BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.5)
            )));
            b.setTextFill(Color.WHITE);
        });
        b.setOnMouseExited(e -> {
            b.setBorder(new Border(new BorderStroke(
                    Color.color(1,1,1,0.25),
                    BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.5)
            )));
            b.setTextFill(Color.color(1,1,1,0.85));
        });
        return b;
    }

    private Button baseButton(String text) {
        Button b = new Button(text);
        b.setMinWidth(260);
        b.setPadding(new Insets(12, 20, 12, 20));
        b.setFont(Font.font(16));
        DropShadow focusGlow = new DropShadow(18, Color.color(1,1,1,0.25));
        b.focusedProperty().addListener((obs, was, is) -> b.setEffect(is ? focusGlow : null));
        return b;
    }

    private void addHoverPressEffects(Button b, String hoverPaint, String pressPaint) {
        Background normal = b.getBackground();
        Background hover = new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf(hoverPaint),
                new CornerRadii(12), Insets.EMPTY
        ));
        Background pressed = new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf(pressPaint),
                new CornerRadii(12), Insets.EMPTY
        ));
        b.setOnMouseEntered(e -> b.setBackground(hover));
        b.setOnMouseExited(e -> b.setBackground(normal));
        b.setOnMousePressed(e -> b.setBackground(pressed));
        b.setOnMouseReleased(e -> b.setBackground(hover));
    }
}
