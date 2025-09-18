package org.oosd.ui.sprites;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.oosd.game.GameEntity;

public class BlockSprite implements Sprite {
    private final Rectangle rect;
    private final GameEntity entity;

    public BlockSprite(GameEntity entity, double size, Color color) {
        this.entity = entity;
        this.rect = new Rectangle(size, size);
        rect.setArcWidth(6);
        rect.setArcHeight(6);
        rect.setFill(color);
        rect.setStroke(Color.color(0,0,0,0.35));
    }

    @Override public Node getNode() { return rect; }
    @Override public GameEntity getEntity() { return entity; }

    @Override public void setXY(double x, double y) {
        rect.setTranslateX(x);
        rect.setTranslateY(y);
    }

    /** optional recolor */
    public void setFill(Color c) { rect.setFill(c); }
}
