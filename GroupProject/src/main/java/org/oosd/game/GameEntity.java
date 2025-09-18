package org.oosd.game;

public abstract class GameEntity {
    // world-space position in *cells* unless otherwise noted
    protected double x;     // column (can be fractional while falling)
    protected double y;     // row    (fractional for smooth gravity)
    protected final EntityType type;

    // lifecycle (seconds). life<0 => eternal
    protected double lifeSeconds = -1;

    private long createdNanos = System.nanoTime();
    private long lastNanos = createdNanos;
    private boolean dead = false;

    protected GameEntity(EntityType type) {
        this.type = type;
    }

    /** Called each frame. Time-based movement, independent of FPS. */
    public final void tick(long nowNanos) {
        double dt = (nowNanos - lastNanos) / 1_000_000_000.0;
        lastNanos = nowNanos;

        if (lifeSeconds >= 0) {
            double age = (nowNanos - createdNanos) / 1_000_000_000.0;
            if (age >= lifeSeconds) { dead = true; return; }
        }
        if (!dead) process(dt);
    }

    /** Subclasses implement their behavior using delta time (seconds). */
    protected abstract void process(double dt);

    public boolean isDead() { return dead; }
    public void kill() { dead = true; }

    public double x() { return x; }
    public double y() { return y; }
    public EntityType entityType() { return type; }
}
