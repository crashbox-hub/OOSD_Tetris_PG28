package org.oosd.ui.sprites;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.transform.Translate;

/** Concrete Sprite wrapping a Star node. */
public class StarSprite implements Sprite {
    private final Star node;
    private final Translate xy = new Translate();

    public StarSprite() {
        this(10, 5);
    }

    public StarSprite(double outer, double inner) {
        this.node = new Star(outer, inner, Color.web("#FFD54F"), Color.web("#FFC107"));
        this.node.getTransforms().add(xy);
        // a tiny glow-ish opacity so it sits nicely on your dark UI
        this.node.setOpacity(0.9);
    }

    @Override public Node getNode() { return node; }

    @Override public void setXY(double x, double y) {
        xy.setX(x);
        xy.setY(y);
    }
}
