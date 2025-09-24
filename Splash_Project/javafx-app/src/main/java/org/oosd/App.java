package org.oosd;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.concurrent.Task;

public class App extends Application {
    @Override
    public void start(Stage primarStage) {
        // Splash Stage
        Stage splashStage = new Stage(StageStyle.UNDECORATED);

        // Splash Content
        ImageView splashImage = new ImageView(new 
        Image(getClass().getResource("/splash-image.png").toExternalForm()));
        splashImage.setFitWidth(500);
        splashImage.setFitHeight(500);
        splashImage.setPreserveRatio(true);
        splashImage.setSmooth(true);
        
        Label loadingLabel = new Label("Loading, please wait...");

        StackPane splashLayout = new StackPane(splashImage, loadingLabel);
        Scene splashScene = new Scene(splashLayout, 500, 500);

        splashStage.setScene(splashScene);
        splashStage.show();

        // Simulating Loading Task
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(3000);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    splashStage.close();
                    showMainStage(primarStage);
                });
            }
        };

        new Thread(loadTask).start();
    }

    private void showMainStage(Stage primaryStage) {
        // Your main application window
        Label mainLabel = new Label("Welcome to the Main Application!");
        Scene mainScene = new Scene(new StackPane(mainLabel), 600, 400);
        primaryStage.setTitle("Main App");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}