package org.oosd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.oosd.core.AbstractScreen;

/** Main Menu with Play, Configuration, High Scores, Exit (FR-02..FR-09). */
public class MainMenuView extends AbstractScreen {

    public MainMenuView(Runnable onPlay,
                        Runnable onConfig,
                        Runnable onHighScores,
                        Runnable onExit) {

        Button playBtn  = new Button("Play");
        Button cfgBtn   = new Button("Configuration");
        Button scoreBtn = new Button("High Scores");
        Button exitBtn  = new Button("Exit");

        playBtn.setMaxWidth(Double.MAX_VALUE);
        cfgBtn.setMaxWidth(Double.MAX_VALUE);
        scoreBtn.setMaxWidth(Double.MAX_VALUE);
        exitBtn.setMaxWidth(Double.MAX_VALUE);

        playBtn.setOnAction(e -> { if (onPlay != null) onPlay.run(); });
        cfgBtn.setOnAction(e -> { if (onConfig != null) onConfig.run(); });
        scoreBtn.setOnAction(e -> { if (onHighScores != null) onHighScores.run(); });
        exitBtn.setOnAction(e -> { if (onExit != null) onExit.run(); });

        VBox root = new VBox(12, playBtn, cfgBtn, scoreBtn, exitBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));

        getChildren().add(root);
    }

    @Override public void onShow() { /* no-op */ }
    @Override public void onHide() { /* no-op */ }

    @Override
    public void update(long nowNanos) {

    }
}
