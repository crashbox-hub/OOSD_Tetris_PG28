package org.oosd.ui;

import javafx.animation.PauseTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;   // centering container
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.oosd.core.AbstractScreen;

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
        } else {
            System.out.println("Splash image not found at /images/Tetris.png");
        }

        // Animated Title
        String titleText = "TETRIS";
        javafx.scene.layout.HBox titleBox = new javafx.scene.layout.HBox(5);
        titleBox.setAlignment(Pos.CENTER);
        ParallelTransition allDrops = new ParallelTransition();
        String[] colours = {"red","orange","purple","green","blue","purple"};

        for (int i = 0; i < titleText.length(); i++) {
            Label letter = new Label(String.valueOf(titleText.charAt(i)));
            letter.setStyle("-fx-font-size:50px; -fx-font-weight:bold; -fx-text-fill:" + colours[i] + ";");

            Group letterGroup = new Group(letter);
            letterGroup.setTranslateY(-200);

            TranslateTransition drop = new TranslateTransition(Duration.seconds(0.5), letterGroup);
            drop.setToY(0);
            drop.setInterpolator(Interpolator.EASE_OUT);
            drop.setDelay(Duration.seconds(i * 0.5));
            allDrops.getChildren().add(drop);

            drop.setOnFinished(e -> Platform.runLater(() -> {
                double pivotX = letter.getWidth() / 2.0;
                double pivotY = letter.getHeight();
                Rotate rotate = new Rotate(0, pivotX, pivotY); // rotate around bottom-center
                letterGroup.getTransforms().add(rotate);

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

        Label team = new Label("Team: Alexander Abbosh • Christopher Burrell • Vishva Pandya • Bailey Reeves");
        team.setMaxWidth(Double.MAX_VALUE);
        team.setAlignment(Pos.CENTER);

        course.setStyle("-fx-font-size:14px;");
        team.setStyle("-fx-font-size:12px; -fx-opacity:0.85;");

        // Content vertical box
        VBox content = new VBox(10);
        if (splashImage != null) content.getChildren().add(splashImage);
        content.getChildren().addAll(titleBox, course, team);
        content.setAlignment(Pos.CENTER);
        content.setFillWidth(false); // keep compact

        // Centering container (fills the whole AbstractScreen)
        StackPane root = new StackPane(content);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));

        // Make StackPane fill the AbstractScreen (AbstractScreen extends Pane)
        root.setMinSize(0, 0);
        root.prefWidthProperty().bind(widthProperty());
        root.prefHeightProperty().bind(heightProperty());

        getChildren().add(root);
    }

    @Override
    public void onShow() {
        // Auto-advance after ~5 seconds (FR-01)
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> { if (onDone != null) onDone.run(); });
        delay.play();
    }

    @Override
    public void onHide() {
        // nothing to stop for now
    }
}
