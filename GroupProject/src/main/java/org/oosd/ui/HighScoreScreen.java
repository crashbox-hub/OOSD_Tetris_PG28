


package org.oosd.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import org.oosd.game.HighScoreManager;

public class HighScoreScreen {

    private final HighScoreManager manager = new HighScoreManager();

    /** Create a Scene that shows Top-10 high scores and a Back button. */
    public Scene createScene(Stage stage, Runnable onBack) {
        Label title = new Label("High Scores (Top 10)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<HighScoreManager.HighScoreEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<HighScoreManager.HighScoreEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().name));
        nameCol.setMinWidth(150);

        TableColumn<HighScoreManager.HighScoreEntry, Number> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().score));
        scoreCol.setMinWidth(100);

        table.getColumns().addAll(nameCol, scoreCol);
        table.setItems(FXCollections.observableArrayList(manager.getHighScores()));

        Button back = new Button("Back");
        back.setOnAction(e -> onBack.run());

        VBox root = new VBox(12, title, table, back);
        root.setPadding(new Insets(16));
        return new Scene(root, 400, 420);
    }
}