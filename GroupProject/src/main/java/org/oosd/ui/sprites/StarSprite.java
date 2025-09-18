package org.oosd.ui.sprites;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.transform.Translate;
import org.oosd.game.GameEntity;
import org.oosd.ui.sprites.Sprite;

public class StarSprite implements Sprite {
    private final Star node;
    private final Translate xy = new Translate();
    private final GameEntity entity;

    public StarSprite(double outer, double inner) {
        this(outer, inner, null);
    }

    public StarSprite(double outer, double inner, GameEntity entity) {
        this.node = new Star(outer, inner, Color.web("#FFD54F"), Color.web("#FFC107"));
        this.node.getTransforms().add(xy);
        this.node.setOpacity(0.9);
        this.entity = entity;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public void setXY(double x, double y) {
        xy.setX(x);
        xy.setY(y);
    }

    @Override
    public GameEntity getEntity() {
        return entity; // return linked entity or null
    }
}
