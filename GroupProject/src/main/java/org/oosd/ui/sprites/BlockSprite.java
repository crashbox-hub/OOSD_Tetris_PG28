package org.oosd.ui.sprites;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class BlockSprite implements Sprite {
    private final Rectangle rect;

    public BlockSprite(double size, Color color) {
        rect = new Rectangle(size, size, color);
        rect.setArcWidth(6);
        rect.setArcHeight(6);
        // subtle “sheen”
        rect.setStroke(Color.color(0,0,0,0.35));
    }

    @Override public Node getNode() { return rect; }

    @Override public void setXY(double x, double y) {
        rect.setTranslateX(x);
        rect.setTranslateY(y);
    }

    /** Convenience if you need to recolor (e.g., reuse instances). */
    public void setFill(Color c) { rect.setFill(c); }
}
