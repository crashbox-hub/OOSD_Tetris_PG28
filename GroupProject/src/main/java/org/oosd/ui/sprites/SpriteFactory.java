package org.oosd.ui.sprites;

import java.util.concurrent.ThreadLocalRandom;


public final class SpriteFactory {
    private static final SpriteFactory INSTANCE = new SpriteFactory();
    private SpriteFactory() {}
    public static SpriteFactory getInstance() { return INSTANCE; }

    public Sprite createSprite() {
        // For now: always STAR at random location (caller may reposition)
        return createSprite(SpriteType.STAR);
    }

    public Sprite createSprite(SpriteType type) {
        switch (type) {
            case STAR -> { return new StarSprite(); }
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public Sprite createSprite(SpriteType type, double x, double y) {
        Sprite s = createSprite(type);
        s.setXY(x, y);
        return s;
    }

    public Sprite createRandomIn(SpriteType type, double width, double height) {
        double x = ThreadLocalRandom.current().nextDouble(12, Math.max(12, width - 12));
        double y = ThreadLocalRandom.current().nextDouble(12, Math.max(12, height - 12));
        return createSprite(type, x, y);
    }
}
