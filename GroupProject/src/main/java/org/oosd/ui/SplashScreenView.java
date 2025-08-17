package org.oosd.ui;

import javafx.animation.PauseTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.oosd.core.AbstractScreen;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.net.URL;

/** Simple splash: shows course + team, then moves to the Main Menu. */
public class SplashScreenView extends AbstractScreen {

    private final Runnable onDone;

    public SplashScreenView(Runnable onDone) {
        this.onDone = onDone;

        // Image
        URL imageUrl = getClass().getResource("/images/Tetris.png");
        ImageView splashImage = null;

        if (imageUrl != null) {
            Image img = new Image(imageUrl.toExternalForm());
            splashImage = new ImageView(img);
            splashImage.setFitWidth(200);
            splashImage.setPreserveRatio(true);
            splashImage.setSmooth(true);
        }
        else {
            System.out.println("Splash image not found at /images/Tetris.png");
        }

        // Animated Title
        String titleText = "TETRIS";
        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER);
        ParallelTransition allDrops = new ParallelTransition();
        String[] colours = {"red", "orange", "purple", "green", "blue", "purple"}; // Colours for each letter
        
        for (int i = 0; i < titleText.length(); i++) {
            Label letter = new Label(String.valueOf(titleText.charAt(i)));
            String colour = colours[i];
            letter.setStyle("-fx-font-size:50px; -fx-font-weight:bold; -fx-text-fill:" + colour + ";");

            Group letterGroup = new Group(letter);
            letterGroup.setTranslateY(-200);

            // Drop Animation
            TranslateTransition drop = new TranslateTransition(Duration.seconds(0.5), letterGroup);
            drop.setToY(0);
            drop.setInterpolator(Interpolator.EASE_OUT);
            drop.setDelay(Duration.seconds(i * 0.5));
            allDrops.getChildren().add(drop);

            // Sway Animation after Drop
            drop.setOnFinished(e -> Platform.runLater(() -> {
                double pivotX = letter.getWidth() / 2.0;
                double pivotY = letter.getHeight();

                // Rotating around bottom centre
                Rotate rotate = new Rotate(0, pivotX, pivotY);
                letterGroup.getTransforms().add(rotate);

                // Sway animation
                RotateTransition sway = new RotateTransition(Duration.seconds(0.4), letterGroup);
                sway.setByAngle(10);
                sway.setCycleCount(RotateTransition.INDEFINITE);
                sway.setAutoReverse(true);
                sway.setAxis(Rotate.Z_AXIS);
                sway.play();
            }));
            titleBox.getChildren().add(letterGroup);
        }
        allDrops.play();

        // Text 
        Label course = new Label("2006ICT / 2805ICT / 3815ICT");
        course.setMaxWidth(Double.MAX_VALUE);
        course.setAlignment(Pos.CENTER);

        Label team   = new Label(
                "Team: Alexander Abbosh • Christopher Burrell • Vishva Pandya • Bailey Reeves"
        );
        team.setMaxWidth(Double.MAX_VALUE);
        team.setAlignment(Pos.CENTER);

        course.setStyle("-fx-font-size:14px;");
        team.setStyle("-fx-font-size:12px; -fx-opacity:0.85;");

        VBox box = new VBox(10);
        if (splashImage != null) box.getChildren().add(splashImage);
        box.getChildren().addAll(titleBox, course, team);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));

        // AbstractScreen is a Pane/StackPane; add the content
        getChildren().add(box);
    }

    @Override
    public void onShow() {
        // Auto-advance after ~2.5 seconds (FR-01)
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
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