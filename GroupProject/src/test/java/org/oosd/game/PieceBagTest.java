package org.oosd.game;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PieceBagTest {

    @Test
    void nextCyclesThroughSevenUniquePiecesBeforeRepeat() {
        PieceBag bag = new PieceBag(12345L);

        Set<Tetromino> firstCycle = EnumSet.noneOf(Tetromino.class);
        Tetromino lastOfFirstCycle = null;

        for (int i = 0; i < 7; i++) {
            Tetromino next = bag.next();
            firstCycle.add(next);
            lastOfFirstCycle = next;
        }

        assertEquals(EnumSet.allOf(Tetromino.class), firstCycle);

        Tetromino firstOfSecondCycle = bag.next();
        assertNotEquals(lastOfFirstCycle, firstOfSecondCycle, "refill should not repeat the previous piece");

        Set<Tetromino> secondCycle = EnumSet.of(firstOfSecondCycle);
        for (int i = 0; i < 6; i++) {
            secondCycle.add(bag.next());
        }

        assertEquals(EnumSet.allOf(Tetromino.class), secondCycle);
    }
}
