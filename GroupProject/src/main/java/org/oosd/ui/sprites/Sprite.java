package org.oosd.ui.sprites;

import javafx.scene.Node;

public interface Sprite {
    Node getNode();

    void setXY(double x, double y);
}
