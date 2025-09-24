package org.oosd.ui.sprites;

import javafx.scene.Node;
import org.oosd.game.GameEntity;

/**
 * Generic sprite contract:
 * E = entity type rendered by this sprite
 * N = Node type used to display it
 */
public interface Sprite<E extends GameEntity, N extends Node> {

    /** The entity this sprite renders. */
    E getEntity();

    /** The JavaFX node to add to the scene graph. */
    N getNode();

    /** Convenience: move the node in pixels. */
    default void setXY(double x, double y) {
        getNode().setTranslateX(x);
        getNode().setTranslateY(y);
    }
}
