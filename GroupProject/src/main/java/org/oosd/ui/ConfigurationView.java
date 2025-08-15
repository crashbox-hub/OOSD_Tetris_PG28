package org.oosd.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ConfigurationView {
    public Scene createScene(Stage stage, Runnable onBack) {
        Label title = new Label("Configuration (Milestone 1 placeholder)");
        Button back = new Button("Back");
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });

        VBox root = new VBox(12, title, back);
        root.setPadding(new Insets(16));
        return new Scene(root, 640, 480);
    }
}