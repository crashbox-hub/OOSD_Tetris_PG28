package org.oosd.ui.sprites;

import javafx.scene.paint.Color;
import org.oosd.core.GameConfig;
import org.oosd.game.*;

public final class SpriteFactory {
    private SpriteFactory() {}

    /** Create a sprite for a given entity. Safe to call from the game thread. */
    public static synchronized Sprite create(GameEntity e) {
        int tile = GameConfig.get().tileSize();

        return switch (e.entityType()) {
            case ACTIVE_PIECE -> new PieceSprite((ActivePieceEntity) e);
            case BLOCK -> {
                BlockEntity b = (BlockEntity)e;
                BlockSprite s = new BlockSprite(e, tile, colorFor(b.colorId()));
                s.setXY(b.x()*tile, b.y()*tile);
                yield s;
            }
        };
    }

    private static Color colorFor(int id) {
        return switch (id) {
            case 1 -> Color.CYAN; case 2 -> Color.YELLOW; case 3 -> Color.PURPLE;
            case 4 -> Color.LIMEGREEN; case 5 -> Color.RED; case 6 -> Color.BLUE;
            case 7 -> Color.ORANGE; default -> Color.GRAY;
        };
    }
}
