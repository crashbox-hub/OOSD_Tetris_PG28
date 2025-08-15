package org.oosd.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.oosd.core.AbstractScreen;

/**
 * Displays the top 10 high scores.
 */
public class HighScoresView extends AbstractScreen {

    public HighScoresView(Runnable onBack) {
        // Create a list showing dummy scores for now
        var list = new ListView<String>();
        list.getItems().addAll(
                "1) Tom - 12000","2) Sar - 9500","3) Alx - 9000",
                "4) Dom - 8500","5) Eve - 8000","6) Cal - 7500",
                "7) Grg - 7000","8) Hap - 6500","9) Ian - 6000","10) Jax - 5500"
        );

        // Back button to return to previous screen
        var back = new Button("Back");
        back.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        // Layout: vertical box with spacing of 10px
        getChildren().add(new VBox(10, list, back));
    }

    @Override
    public void onShow() { requestFocus(); }
    @Override
    public void onHide() { }
}
