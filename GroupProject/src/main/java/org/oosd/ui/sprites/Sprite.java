package org.oosd.ui.sprites;

import javafx.scene.Node;
import org.oosd.game.GameEntity;

public interface Sprite {
    /** visual node to attach into the scene graph */
    Node getNode();

    /** entity this sprite is rendering */
    GameEntity getEntity();

    /** convenience positioning in pixels */
    void setXY(double x, double y);
}
