package org.oosd.game;

public class BlockEntity extends GameEntity {
    private final int colorId;

    public BlockEntity(int col, int row, int colorId) {
        super(EntityType.BLOCK);
        this.x = col;
        this.y = row;
        this.colorId = colorId;
    }

    @Override protected void process(double dt) {
        // static – nothing to do
    }

    public int colorId() { return colorId; }
}
