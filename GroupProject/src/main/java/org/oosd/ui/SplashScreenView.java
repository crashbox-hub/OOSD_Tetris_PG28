package org.oosd.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.oosd.core.AbstractScreen;

/** Simple splash: shows course + team, then moves to the Main Menu. */
public class SplashScreenView extends AbstractScreen {

    private final Runnable onDone;

    public SplashScreenView(Runnable onDone) {
        this.onDone = onDone;

        Label title  = new Label("Tetris");
        Label course = new Label("2006ICT / 2805ICT / 3815ICT");
        Label team   = new Label(
                "Team: Alexander Abbosh • Christopher Burrell • Vishva Pandya • Bailey Reeves"
        );

        title.setStyle("-fx-font-size:26px; -fx-font-weight:bold;");
        course.setStyle("-fx-font-size:14px;");
        team.setStyle("-fx-font-size:12px; -fx-opacity:0.85;");

        VBox box = new VBox(10, title, course, team);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));

        // AbstractScreen is a Pane/StackPane; add the content
        getChildren().add(box);
    }

    @Override
    public void onShow() {
        // Auto-advance after ~2.5 seconds (FR-01)
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> { if (onDone != null) onDone.run(); });
        delay.play();
    }

    @Override
    public void onHide() {
        // nothing to stop for now
    }

    @Override
    public void update(long nowNanos) {

    }
}
