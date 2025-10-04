package org.oosd.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import org.oosd.core.AbstractScreen;
import org.oosd.core.GameConfig;
import org.oosd.core.SettingsStore;

public class ConfigurationView extends AbstractScreen {

    public ConfigurationView(Runnable onBack) {
        // Load JSON so UI reflects persisted settings
        GameConfig cfg = GameConfig.get();
        SettingsStore.loadInto(cfg);

        // Root background container
        StackPane bg = new StackPane();
        bg.getStyleClass().add("app-bg");

        // Inner panel (card)
        VBox panel = new VBox(16);
        panel.getStyleClass().add("panel");
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));

        // Title
        Label title = new Label("Configuration");
        title.getStyleClass().add("title");

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

        // --- Width / Height with dynamic bounds tied to Extend Mode ---
        Label widthLbl  = label("Field Width (No of cells):");
// seed; real min/max will be set immediately after
        Slider width    = slider(GameConfig.MIN_COLS_1P, GameConfig.EXT_MAX_COLS_1P, cfg.cols());
        Label widthVal  = valueLabel(width);

        Label heightLbl = label("Field Height (No of cells):");
// seed; real min/max will be set immediately after
        Slider height   = slider(GameConfig.MIN_ROWS_1P, GameConfig.EXT_MAX_ROWS_1P, cfg.rows());
        Label heightVal = valueLabel(height);

// keep config in sync with slider motion
        width.valueProperty().addListener((obs, o, n) -> {
            cfg.setCols(n.intValue());
            SettingsStore.save(cfg);
        });
        height.valueProperty().addListener((obs, o, n) -> {
            cfg.setRows(n.intValue());
            SettingsStore.save(cfg);
        });

// helper: recompute min/max from current cfg + extend mode (no other changes)
        Runnable refreshSliderBounds = () -> {
            int minCols = (cfg.players() == 2) ? GameConfig.MIN_COLS_2P : GameConfig.MIN_COLS_1P;
            int minRows = (cfg.players() == 2) ? GameConfig.MIN_ROWS_2P : GameConfig.MIN_ROWS_1P;

            int maxCols = (cfg.players() == 2)
                    ? (cfg.isExtendModeEnabled() ? GameConfig.EXT_MAX_COLS_2P : GameConfig.MAX_COLS_2P)
                    : (cfg.isExtendModeEnabled() ? GameConfig.EXT_MAX_COLS_1P : GameConfig.MAX_COLS_1P);

            int maxRows = (cfg.players() == 2)
                    ? (cfg.isExtendModeEnabled() ? GameConfig.EXT_MAX_ROWS_2P : GameConfig.MAX_ROWS_2P)
                    : (cfg.isExtendModeEnabled() ? GameConfig.EXT_MAX_ROWS_1P : GameConfig.MAX_ROWS_1P);

            width.setMin(minCols);  width.setMax(maxCols);
            height.setMin(minRows); height.setMax(maxRows);

            // reflect any clamping done inside cfg after limits change
            width.setValue(cfg.cols());
            height.setValue(cfg.rows());
        };

// add to grid (unchanged)
        grid.add(widthLbl, 0, row); grid.add(width, 1, row);
        grid.add(widthVal, 2, row++); GridPane.setHalignment(widthVal, HPos.RIGHT);

        grid.add(heightLbl, 0, row); grid.add(height, 1, row);
        grid.add(heightVal, 2, row++); GridPane.setHalignment(heightVal, HPos.RIGHT);

// initial sync with current extend-mode state
        refreshSliderBounds.run();


        // --- Game Level (maps to gravityCps) ---
        Label levelLbl = label("Game Level:");
        int levelInitial = levelFromGravity(cfg.gravityCps()); // derive from saved gravity
        Slider level = slider(1, 10, levelInitial);
        Label levelVal = valueLabel(level);
        level.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            cfg.setGravityCps(gravityFromLevel(v));
            SettingsStore.save(cfg);
        });
        HBox levelBox = new HBox(10, level);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(levelLbl, 0, row); grid.add(levelBox, 1, row);
        grid.add(levelVal, 2, row++); GridPane.setHalignment(levelVal, HPos.RIGHT);

        // --- Players: 1 or 2 ---
        Label playersLbl = label("Players:");
        ToggleGroup playersGroup = new ToggleGroup();
        RadioButton oneP = new RadioButton("1 Player");
        RadioButton twoP = new RadioButton("2 Players");
        oneP.setToggleGroup(playersGroup);
        twoP.setToggleGroup(playersGroup);
        if (cfg.players() == 2) twoP.setSelected(true); else oneP.setSelected(true);

        HBox playersBox = new HBox(12, oneP, twoP);
        playersBox.setAlignment(Pos.CENTER_LEFT);

        Label playersState = new Label(cfg.players() == 2 ? "2P" : "1P");
        playersState.getStyleClass().add("value-label");

        playersGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            int p = (newT == twoP) ? 2 : 1;
            cfg.setPlayers(p);
            playersState.setText(p == 2 ? "2P" : "1P");
            SettingsStore.save(cfg);
        });
        grid.add(playersLbl, 0, row);
        grid.add(playersBox, 1, row);
        grid.add(playersState, 2, row++); GridPane.setHalignment(playersState, HPos.RIGHT);

        // --- Music toggle ---
        row = addToggleRow(grid, row, "Music (On/Off):", cfg.isMusicEnabled(), isSel -> {
            cfg.setMusicEnabled(isSel);
            SettingsStore.save(cfg);
            if (isSel) Sound.startMenuBgm(); else Sound.stopBgm();
        });

        // --- SFX toggle ---
        row = addToggleRow(grid, row, "Sound Effects (On/Off):", cfg.isSfxEnabled(), isSel -> {
            cfg.setSfxEnabled(isSel);
            SettingsStore.save(cfg);
        });

        // --- AI Play toggle (merged from branch) ---
        row = addToggleRow(grid, row, "Player 1 AI Play (On/Off):", cfg.isAiP1Enabled(), isSel -> {
            cfg.setAiP1Enabled(isSel);
            SettingsStore.save(cfg);
        });

        row = addToggleRow(grid, row, "Player 2 AI Play (On/Off):", cfg.isAiP2Enabled(), isSel -> {
            cfg.setAiP2Enabled(isSel);
            SettingsStore.save(cfg);
        });


        // Extend Mode toggle (persisted)
        addToggleRow(grid, row, "Extend Mode (On/Off):", cfg.isExtendModeEnabled(), isSel -> {
            cfg.setExtendModeEnabled(isSel);
            SettingsStore.save(cfg);
            refreshSliderBounds.run();
        });



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

    /* ---------- helpers ---------- */

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

    private int addToggleRow(GridPane grid, int row, String labelText, boolean initial,
                             java.util.function.Consumer<Boolean> onToggle) {
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
            if (onToggle != null) onToggle.accept(isSel);
        });

        grid.add(lbl, 0, row);
        grid.add(cb, 1, row);
        grid.add(state, 2, row);
        GridPane.setHalignment(state, HPos.RIGHT);
        return row + 1;
    }

    // ---- Level/Gravity mapping ----
    private static double gravityFromLevel(int level) {
        int lvl = Math.max(1, Math.min(10, level));
        return 1.8 + 0.25 * (lvl - 1);
    }

    private static int levelFromGravity(double cps) {
        // invert mapping and clamp to 1..10
        int lvl = (int)Math.round(((cps - 1.8) / 0.25) + 1.0);
        return Math.max(1, Math.min(10, lvl));
    }

    @Override public void onShow() {
        requestFocus();
        if (GameConfig.get().isMusicEnabled()) Sound.startMenuBgm();
        else Sound.stopBgm();
    }

    @Override public void onHide() { }
}
