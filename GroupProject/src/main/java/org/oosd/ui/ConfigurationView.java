package org.oosd.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.oosd.core.AbstractScreen;

public class ConfigurationView extends AbstractScreen {

    public ConfigurationView(Runnable onBack) {
        // Root background container
        StackPane bg = new StackPane();
        bg.getStyleClass().add("app-bg"); // gradient + padding

        // Inner panel (card)
        VBox panel = new VBox(16);
        panel.getStyleClass().add("panel");     // gradient, radius, border, shadow
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));

        // Title
        Label title = new Label("Configuration");
        title.getStyleClass().add("title");     // big, bold, glow

        // Controls grid
        GridPane grid = new GridPane();
        grid.getStyleClass().add("config-grid");
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(6, 16, 6, 16));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(38);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(12);
        grid.getColumnConstraints().addAll(c0, c1, c2);

        int row = 0;

        // Width 6..13
        Label widthLbl = label("Field Width (No of cells):");
        Slider width = slider(6, 13, 10);
        Label widthVal = valueLabel(width);
        grid.add(widthLbl, 0, row); grid.add(width, 1, row);
        grid.add(widthVal, 2, row++); GridPane.setHalignment(widthVal, HPos.RIGHT);

        // Height 15..20
        Label heightLbl = label("Field Height (No of cells):");
        Slider height = slider(15, 20, 20);
        Label heightVal = valueLabel(height);
        grid.add(heightLbl, 0, row); grid.add(height, 1, row);
        grid.add(heightVal, 2, row++); GridPane.setHalignment(heightVal, HPos.RIGHT);

        // Level 1..10
        Label levelLbl = label("Game Level:");
        Slider level = slider(1, 10, 1);
        Label levelVal = valueLabel(level);
        grid.add(levelLbl, 0, row); grid.add(level, 1, row);
        grid.add(levelVal, 2, row++); GridPane.setHalignment(levelVal, HPos.RIGHT);

        // Toggles
        row = addToggleRow(grid, row, "Music (On/Off):", true);
        row = addToggleRow(grid, row, "Sound Effect (On/Off):", true);
        {
            Label lbl = label("AI Play (On/Off):");
            CheckBox cb = new CheckBox();
            cb.getStyleClass().add("config-checkbox");
            cb.setSelected(false);

            Label state = new Label("Off");
            state.getStyleClass().add("state-off");
            cb.selectedProperty().addListener((obs, was, isSel) -> {
                state.setText(isSel ? "On" : "Off");
                state.getStyleClass().removeAll("state-on", "state-off");
                state.getStyleClass().add(isSel ? "state-on" : "state-off");
                org.oosd.core.GameConfig.get().setAiEnabled(isSel);
            });

            grid.add(lbl, 0, row);
            grid.add(cb, 1, row);
            grid.add(state, 2, row);
            GridPane.setHalignment(state, HPos.RIGHT);
            row++;
        }

        row = addToggleRow(grid, row, "Extend Mode (On/Off):", false);

        // Back button + footer
        Button back = new Button("Back");
        back.getStyleClass().addAll("btn", "btn-ghost");
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox backBox = new HBox(back);
        backBox.getStyleClass().add("center-bar");

        Label footer = new Label("Author: AA, CB, VP, BR");
        footer.getStyleClass().add("footer");
        HBox footerBox = new HBox(footer);
        footerBox.getStyleClass().add("footer-bar");

        panel.getChildren().addAll(title, grid, backBox, footerBox);
        bg.getChildren().add(panel);
        getChildren().add(bg);
    }

    /* ---------- helpers (logic only; no inline CSS) ---------- */

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("label-dim");
        return l;
    }

    private Slider slider(int min, int max, int val) {
        Slider s = new Slider(min, max, val);
        s.setMajorTickUnit(1);
        s.setMinorTickCount(0);
        s.setBlockIncrement(1);
        s.setSnapToTicks(true);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        s.getStyleClass().add("config-slider");
        return s;
    }

    private Label valueLabel(Slider s) {
        Label l = new Label(Integer.toString((int) s.getValue()));
        l.getStyleClass().add("value-label");
        s.valueProperty().addListener((obs, o, n) -> l.setText(Integer.toString(n.intValue())));
        return l;
    }

    private int addToggleRow(GridPane grid, int row, String labelText, boolean initial) {
        Label lbl = label(labelText);

        CheckBox cb = new CheckBox();
        cb.getStyleClass().add("config-checkbox");
        cb.setSelected(initial);

        Label state = new Label(initial ? "On" : "Off");
        state.getStyleClass().add(initial ? "state-on" : "state-off");
        cb.selectedProperty().addListener((obs, was, isSel) -> {
            state.setText(isSel ? "On" : "Off");
            state.getStyleClass().removeAll("state-on", "state-off");
            state.getStyleClass().add(isSel ? "state-on" : "state-off");
        });

        grid.add(lbl, 0, row);
        grid.add(cb, 1, row);
        grid.add(state, 2, row);
        GridPane.setHalignment(state, HPos.RIGHT);
        return row + 1;
    }

    @Override public void onShow() { requestFocus(); }
    @Override public void onHide() { }
}
