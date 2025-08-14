package org.oosd.core;

/**
 * This is a simple interface (like a contract) that any game screen or object
 * can implement if it needs to update over time.
 *
 * The 'update' method will be called every frame (or tick) by the game loop,
 * so we can handle animations, movement, timers, etc.
 */
public interface Updatable {
    /**
     * Called by the game loop with the current time in nanoseconds.
     * @param nowNanos The current time in nanoseconds.
     */
    void update(long nowNanos);
}