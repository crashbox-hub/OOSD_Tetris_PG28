package org.oosd.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.oosd.core.AbstractScreen;
import org.oosd.core.GameConfig;

import java.util.prefs.Preferences;

public class ConfigurationView extends AbstractScreen {

    // Simple persistence store for user settings
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(ConfigurationView.class);

    // Preference keys
    private static final String KEY_WIDTH   = "cfg.cols";
    private static final String KEY_HEIGHT  = "cfg.rows";
    private static final String KEY_LEVEL   = "cfg.level";
    private static final String KEY_MUSIC   = "cfg.music";
    private static final String KEY_SFX     = "cfg.sfx";
    private static final String KEY_PLAYERS = "cfg.players";   // NEW

    public ConfigurationView(Runnable onBack) {
        // Load persisted values into GameConfig on entry
        GameConfig cfg = GameConfig.get();
        cfg.setCols(PREFS.getInt(KEY_WIDTH,  cfg.cols()));
        cfg.setRows(PREFS.getInt(KEY_HEIGHT, cfg.rows()));
        cfg.setGravityCps(PREFS.getInt(KEY_LEVEL, 1)); // map however later
        cfg.setMusicEnabled(PREFS.getBoolean(KEY_MUSIC, cfg.isMusicEnabled()));
        cfg.setSfxEnabled  (PREFS.getBoolean(KEY_SFX,   cfg.isSfxEnabled()));
        int savedPlayers = PREFS.getInt(KEY_PLAYERS, Math.max(1, Math.min(2, cfg.players())));
        cfg.setPlayers(savedPlayers);

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

        // --- Width 6..30 ---
        Label widthLbl = label("Field Width (No of cells):");
        Slider width = slider(6, 25, cfg.cols());
        Label widthVal = valueLabel(width);
        width.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            cfg.setCols(v);
            PREFS.putInt(KEY_WIDTH, v);
        });
        grid.add(widthLbl, 0, row); grid.add(width, 1, row);
        grid.add(widthVal, 2, row++); GridPane.setHalignment(widthVal, HPos.RIGHT);

        // --- Height 15..20 ---
        Label heightLbl = label("Field Height (No of cells):");
        Slider height = slider(15, 20, cfg.rows());
        Label heightVal = valueLabel(height);
        height.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            cfg.setRows(v);
            PREFS.putInt(KEY_HEIGHT, v);
        });
        grid.add(heightLbl, 0, row); grid.add(height, 1, row);
        grid.add(heightVal, 2, row++); GridPane.setHalignment(heightVal, HPos.RIGHT);

        // --- Game Level (placeholder mapping) ---
        Label levelLbl = label("Game Level:");
        int levelInitial = PREFS.getInt(KEY_LEVEL, 1);
        Slider level = slider(1, 10, levelInitial);
        Label levelVal = valueLabel(level);
        level.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            PREFS.putInt(KEY_LEVEL, v);
            // Optionally map to gravity:
            // cfg.setGravityCps(1.0 + 0.3 * (v-1));
        });
        grid.add(levelLbl, 0, row); grid.add(level, 1, row);
        grid.add(levelVal, 2, row++); GridPane.setHalignment(levelVal, HPos.RIGHT);

        // --- Players: 1 or 2 (persistent) ---
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
            PREFS.putInt(KEY_PLAYERS, p);
            playersState.setText(p == 2 ? "2P" : "1P");
        });

        grid.add(playersLbl, 0, row);
        grid.add(playersBox, 1, row);
        grid.add(playersState, 2, row++);
        GridPane.setHalignment(playersState, HPos.RIGHT);

        // --- Music toggle ---
        row = addToggleRow(grid, row, "Music (On/Off):",
                cfg.isMusicEnabled(),
                isSel -> {
                    cfg.setMusicEnabled(isSel);
                    PREFS.putBoolean(KEY_MUSIC, isSel);
                    if (isSel) {
                        Sound.startMenuBgm();
                    } else {
                        Sound.stopBgm();
                    }
                });

        // --- SFX toggle ---
        row = addToggleRow(grid, row, "Sound Effects (On/Off):",
                cfg.isSfxEnabled(),
                isSel -> {
                    cfg.setSfxEnabled(isSel);
                    PREFS.putBoolean(KEY_SFX, isSel);
                });

        // (Placeholders – wire up later if needed)
        row = addToggleRow(grid, row, "AI Play (On/Off):", false, isSel -> {});
        row = addToggleRow(grid, row, "Extend Mode (On/Off):", false, isSel -> {});

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

    /**
     * Adds a toggle row (label + checkbox + "On/Off" indicator) and runs
     * the callback whenever the checkbox changes.
     */
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

    @Override public void onShow() {
        requestFocus();
        if (GameConfig.get().isMusicEnabled()) {
            Sound.startMenuBgm();
        } else {
            Sound.stopBgm();
        }
    }

    @Override public void onHide() { }
}
