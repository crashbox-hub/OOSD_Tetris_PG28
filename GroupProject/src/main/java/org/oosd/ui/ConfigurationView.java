package org.oosd.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ConfigurationView {

    public Scene createScene(Stage stage, Runnable onBack) {
        // ----- Title -----
        Label title = new Label("Configuration");
        title.setFont(Font.font(20));
        HBox titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(6, 0, 10, 0));

        // ----- Grid with controls -----
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(6, 16, 6, 16));
        int row = 0;

        // Field Width (No of cells): 6..13
        Label widthLbl = new Label("Field Width (No of cells):");
        Slider width = slider(6, 13, 10);
        Label widthVal = bindValueLabel(width);
        grid.add(widthLbl, 0, row); grid.add(width, 1, row); grid.add(widthVal, 2, row++); GridPane.setHalignment(widthVal, HPos.RIGHT);

        // Field Height (No of cells): 15..20
        Label heightLbl = new Label("Field Height (No of cells):");
        Slider height = slider(15, 20, 20);
        Label heightVal = bindValueLabel(height);
        grid.add(heightLbl, 0, row); grid.add(height, 1, row); grid.add(heightVal, 2, row++); GridPane.setHalignment(heightVal, HPos.RIGHT);

        // Game Level: 1..10
        Label levelLbl = new Label("Game Level:");
        Slider level = slider(1, 10, 1);
        Label levelVal = bindValueLabel(level);
        grid.add(levelLbl, 0, row); grid.add(level, 1, row); grid.add(levelVal, 2, row++); GridPane.setHalignment(levelVal, HPos.RIGHT);

        // Toggles with On/Off labels
        row = addToggleRow(grid, row, "Music (On/Off):", true);
        row = addToggleRow(grid, row, "Sound Effect (On/Off):", true);
        row = addToggleRow(grid, row, "AI Play (On/Off):", false);
        row = addToggleRow(grid, row, "Extend Mode (On/Off):", false);

        // ----- Back button -----
        var back = new Button("Back");
        back.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });
        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(10, 0, 6, 0));

        // ----- Footer -----
        Label footer = new Label("Author: AA, CB, VP, BR");
        HBox footerBox = new HBox(footer);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setPadding(new Insets(0, 10, 8, 0));

        // ----- Root layout -----
        BorderPane root = new BorderPane();
        root.setTop(titleBox);
        root.setCenter(grid);
        root.setBottom(new VBox(backBox, footerBox));

        return new Scene(root, 640, 480);
    }

    // Helpers
    private Slider slider(int min, int max, int val) {
        Slider s = new Slider(min, max, val);
        s.setMajorTickUnit(1);
        s.setMinorTickCount(0);
        s.setBlockIncrement(1);
        s.setSnapToTicks(true);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        return s;
    }

    private Label bindValueLabel(Slider s) {
        Label l = new Label(Integer.toString((int) s.getValue()));
        s.valueProperty().addListener((obs, o, n) -> l.setText(Integer.toString(n.intValue())));
        return l;
    }

    private int addToggleRow(GridPane grid, int row, String labelText, boolean initial) {
        Label lbl = new Label(labelText);
        CheckBox cb = new CheckBox();
        cb.setSelected(initial);
        Label state = new Label(initial ? "On" : "Off");
        cb.selectedProperty().addListener((obs, was, isSel) -> state.setText(isSel ? "On" : "Off"));

        grid.add(lbl,   0, row);
        grid.add(cb,    1, row);
        grid.add(state, 2, row);
        GridPane.setHalignment(state, HPos.RIGHT);
        return row + 1;
    }
}
