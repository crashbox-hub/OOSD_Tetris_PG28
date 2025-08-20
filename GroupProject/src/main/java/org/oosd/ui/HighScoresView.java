package org.oosd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.oosd.core.AbstractScreen;

public class HighScoresView extends AbstractScreen {

    public HighScoresView(Runnable onBack) {

        /* ---------- Root background (same as MainMenuView) ---------- */
        StackPane root = new StackPane();
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Paint.valueOf("linear-gradient(#0b1220, #0f1830)"),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        /* ---------- Center panel ---------- */
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
        Label title = new Label("High Scores");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(null, FontWeight.EXTRA_BOLD, 36));
        title.setEffect(new DropShadow(24, Color.color(0,0,0,0.6)));

        /* ---------- Scores List ---------- */
        ListView<String> list = new ListView<>();
        list.getItems().addAll(
                "1) Tom - 12000","2) Sar - 9500","3) Alx - 9000",
                "4) Dom - 8500","5) Eve - 8000","6) Cal - 7500",
                "7) Grg - 7000","8) Hap - 6500","9) Ian - 6000","10) Jax - 5500"
        );

        // Visuals for list: dark glass look + cyan border
        list.setBackground(new Background(new BackgroundFill(
                Color.color(1,1,1,0.06), new CornerRadii(10), Insets.EMPTY
        )));
        list.setBorder(new Border(new BorderStroke(
                Color.color(1,1,1,0.18),
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5)
        )));
        list.setPrefWidth(420);
        list.setPrefHeight(360);

        // Cell styling: white text; top 3 colored gold/silver/bronze
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setBackground(Background.EMPTY);
                    return;
                }
                setText(item);
                setTextFill(Color.WHITE);
                setFont(Font.font("Consolas", 16));

                // zebra rows (very subtle)
                int idx = getIndex();
                setBackground(new Background(new BackgroundFill(
                        idx % 2 == 0 ? Color.rgb(255,255,255,0.04)
                                : Color.rgb(255,255,255,0.02),
                        CornerRadii.EMPTY, Insets.EMPTY
                )));

                // top-3 medal tint
                if (item.startsWith("1)")) setTextFill(Color.web("#FFD54F")); // gold
                else if (item.startsWith("2)")) setTextFill(Color.web("#CFD8DC")); // silver
                else if (item.startsWith("3)")) setTextFill(Color.web("#FFAB91")); // bronze
            }
        });

        /* ---------- Back button (ghost style like Exit) ---------- */
        Button back = ghostButton("Back");
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });
        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(8, 0, 0, 0));

        /* ---------- Assemble ---------- */
        panel.getChildren().addAll(title, list, backBox);
        root.getChildren().add(panel);
        getChildren().add(root);

        // Keyboard shortcuts
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE, BACK_SPACE -> back.fire();
                default -> {}
            }
        });
    }

    @Override public void onShow() { requestFocus(); }
    @Override public void onHide() { }

    /* ---------- Shared button styling (matches MainMenuView ghost) ---------- */
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
}
