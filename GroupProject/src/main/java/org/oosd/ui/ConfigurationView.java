package org.oosd.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.oosd.core.AbstractScreen;

public class ConfigurationView extends AbstractScreen {

    public ConfigurationView(Runnable onBack) {

        /* ---------- Root background to match MainMenuView ---------- */
        StackPane root = new StackPane();
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf("linear-gradient(#0b1220, #0f1830)"),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        /* ---------- Center panel (same look as MainMenuView) ---------- */
        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_CENTER);
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

        /* ---------- Title ---------- */
        Label title = new Label("Configuration");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(null, FontWeight.EXTRA_BOLD, 36));
        title.setEffect(new DropShadow(24, Color.color(0,0,0,0.6)));

        /* ---------- Grid with controls ---------- */
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(6, 16, 6, 16));

        // Columns: label | control | value/right
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(38);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(12);
        grid.getColumnConstraints().addAll(c0, c1, c2);

        int row = 0;

        // Field Width (No of cells): 6..13
        Label widthLbl = label("Field Width (No of cells):");
        Slider width = slider(6, 13, 10);
        Label widthVal = valueLabel(width);
        grid.add(widthLbl, 0, row); grid.add(width, 1, row);
        grid.add(widthVal, 2, row++); GridPane.setHalignment(widthVal, HPos.RIGHT);

        // Field Height (No of cells): 15..20
        Label heightLbl = label("Field Height (No of cells):");
        Slider height = slider(15, 20, 20);
        Label heightVal = valueLabel(height);
        grid.add(heightLbl, 0, row); grid.add(height, 1, row);
        grid.add(heightVal, 2, row++); GridPane.setHalignment(heightVal, HPos.RIGHT);

        // Game Level: 1..10
        Label levelLbl = label("Game Level:");
        Slider level = slider(1, 10, 1);
        Label levelVal = valueLabel(level);
        grid.add(levelLbl, 0, row); grid.add(level, 1, row);
        grid.add(levelVal, 2, row++); GridPane.setHalignment(levelVal, HPos.RIGHT);

        // Toggles
        row = addToggleRow(grid, row, "Music (On/Off):",   true);
        row = addToggleRow(grid, row, "Sound Effect (On/Off):", true);
        row = addToggleRow(grid, row, "AI Play (On/Off):", false);
        row = addToggleRow(grid, row, "Extend Mode (On/Off):", false);

        /* ---------- Footer & Back ---------- */
        Button back = ghostButton("Back");
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });
        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(12, 0, 0, 0));

        Label footer = new Label("Author: AA, CB, VP, BR");
        footer.setTextFill(Color.color(1,1,1,0.75));
        HBox footerBox = new HBox(footer);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setPadding(new Insets(0, 6, 0, 0));

        panel.getChildren().addAll(title, grid, backBox, footerBox);
        root.getChildren().add(panel);
        getChildren().add(root);
    }

    /* ---------------------- Styling helpers (mirrors MainMenuView) ---------------------- */

    private Label label(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.color(1,1,1,0.9));
        l.setFont(Font.font(14));
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

        // Light CSS tint so ticks/labels are readable on the dark panel
        s.lookupAll(".axis").forEach(n -> n.setStyle("-fx-tick-label-fill: white;"));
        s.setStyle("""
                -fx-control-inner-background: rgba(255,255,255,0.08);
                -fx-base: #26c2d4;
                -fx-focus-color: #2fd0ff;
                -fx-faint-focus-color: rgba(47,208,255,0.25);
                """);
        return s;
    }

    private Label valueLabel(Slider s) {
        Label l = new Label(Integer.toString((int) s.getValue()));
        l.setTextFill(Color.web("#2fd0ff"));
        l.setFont(Font.font(null, FontWeight.BOLD, 14));
        s.valueProperty().addListener((obs, o, n) -> l.setText(Integer.toString(n.intValue())));
        return l;
    }

    private int addToggleRow(GridPane grid, int row, String labelText, boolean initial) {
        Label lbl = label(labelText);
        CheckBox cb = new CheckBox();
        cb.setSelected(initial);

        // Cyan "On" / grey "Off"
        Label state = new Label(initial ? "On" : "Off");
        state.setTextFill(initial ? Color.web("#2fd0ff") : Color.color(1,1,1,0.75));
        state.setFont(Font.font(null, FontWeight.BOLD, 13));
        cb.selectedProperty().addListener((obs, was, isSel) -> {
            state.setText(isSel ? "On" : "Off");
            state.setTextFill(isSel ? Color.web("#2fd0ff") : Color.color(1,1,1,0.75));
        });

        // Subtle checkbox tint
        cb.setStyle("""
                -fx-mark-color: #0b0f12;
                -fx-focus-color: #2fd0ff;
                -fx-faint-focus-color: rgba(47,208,255,0.25);
                """);

        grid.add(lbl, 0, row);
        grid.add(cb, 1, row);
        grid.add(state, 2, row);
        GridPane.setHalignment(state, HPos.RIGHT);
        return row + 1;
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
        b.setMinWidth(220);
        b.setPadding(new Insets(10, 18, 10, 18));
        b.setFont(Font.font(16));
        DropShadow focusGlow = new DropShadow(18, Color.color(1,1,1,0.25));
        b.focusedProperty().addListener((obs, was, is) -> b.setEffect(is ? focusGlow : null));
        return b;
    }

    /* ---------- Lifecycle ---------- */
    @Override public void onShow() { requestFocus(); }
    @Override public void onHide() { }
}
