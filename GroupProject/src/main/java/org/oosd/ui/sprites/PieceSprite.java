package org.oosd.ui.sprites;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.oosd.core.GameConfig;
import org.oosd.game.ActivePieceEntity;

import java.util.ArrayList;
import java.util.List;

public class PieceSprite implements Sprite<ActivePieceEntity, Group> {
    private final ActivePieceEntity entity;
    private final Group group = new Group();
    private final List<BlockSprite> blocks = new ArrayList<>();
    private final double tile = GameConfig.get().tileSize();

    public PieceSprite(ActivePieceEntity entity) {
        this.entity = entity;
        rebuild();
    }

    private void rebuild() {
        group.getChildren().clear();
        blocks.clear();

        int[][] m = entity.piece().type().shape(entity.piece().rot());
        int colorId = entity.piece().type().colorId();
        Color fill = colorFor(colorId);

        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    BlockSprite b = new BlockSprite(entity, tile, fill);
                    group.getChildren().add(b.getNode());
                    blocks.add(b);
                }
            }
        }
        syncToEntity(); // initial placement
    }

    /** Update the per-block positions from the entity's row/col. */
    public void syncToEntity() {
        int[][] m = entity.piece().type().shape(entity.piece().rot());
        int i = 0;
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] != 0) {
                    double px = (entity.piece().col() + c) * tile;
                    double py = (entity.piece().row() + r) * tile;
                    blocks.get(i++).setXY(px, py);
                }
            }
        }
    }

    @Override public Group getNode() { return group; }
    @Override public ActivePieceEntity getEntity() { return entity; }

    // setXY implemented by the default method in Sprite

    private static Color colorFor(int id) {
        return switch (id) {
            case 1 -> Color.CYAN; case 2 -> Color.YELLOW; case 3 -> Color.PURPLE;
            case 4 -> Color.LIMEGREEN; case 5 -> Color.RED; case 6 -> Color.BLUE;
            case 7 -> Color.ORANGE; default -> Color.GRAY;
        };
    }
}
