package org.oosd.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Random;

/** Shared 7-bag generator so both players see the same piece order. */
public final class PieceBag {
    private final Random rng;
    private final Deque<Tetromino> bag = new ArrayDeque<>(7);

    public PieceBag() { this(System.nanoTime()); }
    public PieceBag(long seed) { this.rng = new Random(seed); }

    /** Next tetromino from the global sequence. */
    public synchronized Tetromino next() {
        if (bag.isEmpty()) refill();
        return bag.removeFirst();
    }

    private void refill() {
        ArrayList<Tetromino> list = new ArrayList<>();
        Collections.addAll(list, Tetromino.values());
        Collections.shuffle(list, rng);
        bag.addAll(list);
    }
}
