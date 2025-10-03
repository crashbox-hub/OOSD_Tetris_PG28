package org.oosd.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    private GameConfig config;
    private int rows;
    private int cols;
    private int spawnCol;
    private int players;
    private boolean extendMode;

    @BeforeEach
    void snapshotConfig() {
        config = GameConfig.get();
        rows = config.rows();
        cols = config.cols();
        spawnCol = config.spawnCol();
        players = config.players();
        extendMode = config.isExtendModeEnabled();
    }

    @AfterEach
    void restoreConfig() {
        config.setPlayers(players);
        config.setRows(rows);
        config.setCols(cols);
        config.setSpawnCol(spawnCol);
        config.setExtendModeEnabled(extendMode);
    }

    @Test
    void switchingPlayerCountReclampsBoardAndSpawn() {
        config.setPlayers(1);
        config.setCols(GameConfig.MAX_COLS_1P);
        config.setRows(GameConfig.MAX_ROWS_1P);
        config.setSpawnCol(config.cols() - 1);

        assertAll(
                () -> assertEquals(1, config.players()),
                () -> assertEquals(GameConfig.MAX_COLS_1P, config.cols()),
                () -> assertEquals(GameConfig.MAX_COLS_1P - 1, config.spawnCol())
        );

        config.setPlayers(2);

        assertAll(
                () -> assertEquals(2, config.players()),
                () -> assertEquals(GameConfig.MAX_COLS_2P, config.cols()),
                () -> assertEquals(GameConfig.MAX_COLS_2P - 1, config.spawnCol())
        );

        config.setPlayers(1);

        assertAll(
                () -> assertEquals(1, config.players()),
                () -> assertEquals(GameConfig.MAX_COLS_2P, config.cols()),
                () -> assertEquals(GameConfig.MAX_COLS_2P - 1, config.spawnCol())
        );

        config.setCols(GameConfig.MAX_COLS_1P);

        assertTrue(config.spawnCol() >= 0);
        assertTrue(config.spawnCol() <= config.cols() - 1);
    }
}
