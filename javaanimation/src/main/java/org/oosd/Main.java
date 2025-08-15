package org.oosd;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Main extends Application {

    private StackPane root;
    private double dx = 3;
    private double dy = 3;

    private boolean hasShadow = false;
    private String colorString = "RED";
    private int size = 10;

    private final double fieldWidth = 400;
    private final double fieldHeight = 300;

    private Color getColor() {
        return switch (colorString) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "BLUE" -> Color.BLUE;
            default -> Color.BLACK;
        };
    }

    @Override
    public void start(Stage primaryStage) {
        root = new StackPane();
        Scene scene = new Scene(root, fieldWidth, fieldHeight);
        showMainScreen(scene);
        primaryStage.setTitle("JavaFX Multi-Screen Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showMainScreen(Scene scene) {
        VBox mainScreen = new VBox(10);
        mainScreen.setPadding(new Insets(20));
        Label label = new Label("Main Screen");

        Button startButton = new Button("Start Game");
        startButton.setOnAction(e -> showGameScreen(scene));

        Button configButton = new Button("Configuration");
        configButton.setOnAction(e -> showConfigScreen(scene));

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> System.exit(0));

        mainScreen.getChildren().addAll(label, startButton, configButton, exitButton);
        root.getChildren().setAll(mainScreen);
    }

    private void showConfigScreen(Scene scene) {
        VBox configScreen = new VBox(10);
        configScreen.setPadding(new Insets(20));
        Label label = new Label("Configuration");

        CheckBox cb = new CheckBox("Has Shadow");
        cb.setSelected(hasShadow);
        cb.setOnAction(e -> hasShadow = cb.isSelected());

        Label colorLabel = new Label("Color:");
        RadioButton rbRed = new RadioButton("Red");
        RadioButton rbGreen = new RadioButton("Green");
        RadioButton rbBlue = new RadioButton("Blue");

        ToggleGroup group = new ToggleGroup();
        rbRed.setToggleGroup(group);
        rbGreen.setToggleGroup(group);
        rbBlue.setToggleGroup(group);

        rbRed.setOnAction(e -> colorString = "RED");
        rbGreen.setOnAction(e -> colorString = "GREEN");
        rbBlue.setOnAction(e -> colorString = "BLUE");

        switch (colorString) {
            case "RED" -> rbRed.setSelected(true);
            case "GREEN" -> rbGreen.setSelected(true);
            default -> rbBlue.setSelected(true);
        }

        Label sizeLabel = new Label("Size: "+size);
        Slider sizeSlider = new Slider(5,20,size);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(5);
        sizeSlider.valueProperty().addListener(
                (obs,oldVal,newVal) -> {
                    size = newVal.intValue();
                    sizeLabel.setText("Size: "+size);
                }
        );

        Button back = new Button("Back");
        back.setOnAction(e -> showMainScreen(scene));

        configScreen.getChildren().addAll(label, cb, colorLabel, rbRed, rbGreen, rbBlue, sizeLabel, sizeSlider, back);
        root.getChildren().setAll(configScreen);
    }

    private void showGameScreen(Scene scene) {
        Pane gamePane = new Pane();

        Rectangle field = new Rectangle(0, 0, fieldWidth, fieldHeight);
        field.setFill(Color.TRANSPARENT);
        field.setStroke(Color.BLACK);

        Circle ball = new Circle(size, getColor());
        ball.setCenterX(fieldWidth / 2);
        ball.setCenterY(fieldHeight / 2);

        if (hasShadow) {
            DropShadow shadow = new DropShadow();
            shadow.setOffsetX(5);
            shadow.setOffsetY(5);
            ball.setEffect(shadow);
        }

        Button backButton = new Button("Back");
        backButton.setLayoutX(10);
        backButton.setLayoutY(10);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double nextX = ball.getCenterX() + dx;
                double nextY = ball.getCenterY() + dy;
                if (nextX - ball.getRadius() < 0 || nextX + ball.getRadius() > fieldWidth) {
                    dx = -dx;
                }
                if (nextY - ball.getRadius() < 0 || nextY + ball.getRadius() > fieldHeight) {
                    dy = -dy;
                }
                ball.setCenterX(ball.getCenterX() + dx);
                ball.setCenterY(ball.getCenterY() + dy);
            }
        };

        backButton.setOnAction(e -> {
            timer.stop();
            showMainScreen(scene);
        });

        gamePane.getChildren().addAll(field, ball, backButton);

        gamePane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP) {
                dx = 0;
                dy = -3;
            } else if (e.getCode() == KeyCode.DOWN) {
                dx = 0;
                dy = 3;
            } else if (e.getCode() == KeyCode.LEFT) {
                dx = -3;
                dy = 0;
            } else if (e.getCode() == KeyCode.RIGHT) {
                dx = 3;
                dy = 0;
            }
        });

        timer.start();
        root.getChildren().setAll(gamePane);
        gamePane.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
