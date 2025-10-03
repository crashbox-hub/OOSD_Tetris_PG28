package org.oosd.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oosd.core.GameConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class GameViewTest {

    private static GameConfigSnapshot originalConfig;

    @BeforeAll
    static void setupFxToolkit() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                Assertions.fail("Timed out waiting for JavaFX Platform to start");
            }
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit already running; ignore.
        }
        originalConfig = new GameConfigSnapshot();
    }

    @BeforeEach
    void resetConfig() {
        originalConfig.restore();
        GameConfig cfg = GameConfig.get();
        cfg.setMusicEnabled(false);
        cfg.setSfxEnabled(false);
    }

    @AfterAll
    static void restoreOriginalConfig() {
        originalConfig.restore();
    }

    @Test
    void constructsSidesUsingAiFlagsFromGameConfig() throws Exception {
        GameConfig cfg = GameConfig.get();
        cfg.setPlayers(2);
        cfg.setRows(20);
        cfg.setCols(10);
        cfg.setAiP1Enabled(true);
        cfg.setAiP2Enabled(false);

        List<SideAiSnapshot> sides = runOnFxThread(() -> {
            GameView view = new GameView(() -> {}, 2);
            return captureAiSnapshots(view);
        });

        Assertions.assertEquals(2, sides.size(), "Expected exactly two sides for a two-player game");

        SideAiSnapshot playerOne = findSide(sides, 1);
        SideAiSnapshot playerTwo = findSide(sides, 2);

        Assertions.assertTrue(playerOne.ai(), "Player 1 should inherit AI flag from config");
        Assertions.assertFalse(playerTwo.ai(), "Player 2 should inherit AI flag from config");
    }

    @Test
    void pressingPauseKeyTogglesPauseStateForHumanPlayer() throws Exception {
        GameConfig cfg = GameConfig.get();
        cfg.setPlayers(1);
        cfg.setRows(20);
        cfg.setCols(10);
        cfg.setAiP1Enabled(false);

        PauseToggleResult result = runOnFxThread(() -> {
            GameView view = new GameView(() -> {}, 1);

            Field sidesField = GameView.class.getDeclaredField("sides");
            sidesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> sides = (List<Object>) sidesField.get(view);
            Object side = sides.getFirst();

            Class<?> sideClass = side.getClass();
            Field pausedField = sideClass.getDeclaredField("paused");
            pausedField.setAccessible(true);
            Field overlayField = sideClass.getDeclaredField("pauseOverlay");
            overlayField.setAccessible(true);

            Method onKeyMethod = GameView.class.getDeclaredMethod("onKey", KeyEvent.class);
            onKeyMethod.setAccessible(true);

            KeyEvent pauseKey = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.P, false, false, false, false);

            boolean initialPaused = pausedField.getBoolean(side);
            onKeyMethod.invoke(view, pauseKey);
            boolean afterFirstToggle = pausedField.getBoolean(side);
            boolean overlayAfterFirstToggle = ((Label) overlayField.get(side)).isVisible();

            onKeyMethod.invoke(view, pauseKey);
            boolean afterSecondToggle = pausedField.getBoolean(side);
            boolean overlayAfterSecondToggle = ((Label) overlayField.get(side)).isVisible();

            return new PauseToggleResult(initialPaused, afterFirstToggle, overlayAfterFirstToggle,
                    afterSecondToggle, overlayAfterSecondToggle);
        });

        Assertions.assertFalse(result.initialPaused(), "Side should start unpaused");
        Assertions.assertTrue(result.afterFirstToggle(), "Pressing P should pause the side");
        Assertions.assertTrue(result.overlayVisibleAfterFirstToggle(), "Pause overlay should be visible while paused");
        Assertions.assertFalse(result.afterSecondToggle(), "Pressing P again should resume the side");
        Assertions.assertFalse(result.overlayVisibleAfterSecondToggle(), "Pause overlay should hide when resumed");
    }

    private static List<SideAiSnapshot> captureAiSnapshots(GameView view) throws Exception {
        Field sidesField = GameView.class.getDeclaredField("sides");
        sidesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Object> sides = (List<Object>) sidesField.get(view);
        Class<?> sideClass = Class.forName("org.oosd.ui.GameView$Side");
        Field idField = sideClass.getDeclaredField("id");
        Field aiField = sideClass.getDeclaredField("ai");
        idField.setAccessible(true);
        aiField.setAccessible(true);

        List<SideAiSnapshot> snapshots = new ArrayList<>();
        for (Object side : sides) {
            int id = idField.getInt(side);
            boolean ai = aiField.getBoolean(side);
            snapshots.add(new SideAiSnapshot(id, ai));
        }
        return snapshots;
    }

    private static SideAiSnapshot findSide(List<SideAiSnapshot> sides, int id) {
        return sides.stream()
                .filter(s -> s.id() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing side with id=" + id));
    }

    private static <T> T runOnFxThread(Callable<T> task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                resultRef.set(task.call());
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            Assertions.fail("Timed out waiting for task on JavaFX Application Thread");
        }

        if (errorRef.get() != null) {
            Throwable throwable = errorRef.get();
            if (throwable instanceof Exception exception) {
                throw exception;
            }
            if (throwable instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(throwable);
        }

        return resultRef.get();
    }

    private record SideAiSnapshot(int id, boolean ai) {}

    private record PauseToggleResult(boolean initialPaused,
                                     boolean afterFirstToggle,
                                     boolean overlayVisibleAfterFirstToggle,
                                     boolean afterSecondToggle,
                                     boolean overlayVisibleAfterSecondToggle) {}

    private static final class GameConfigSnapshot {
        private final int rows;
        private final int cols;
        private final int tileSize;
        private final double gravity;
        private final int spawnCol;
        private final int players;
        private final boolean music;
        private final boolean sfx;
        private final boolean aiP1;
        private final boolean aiP2;
        private final boolean extendMode;

        private GameConfigSnapshot() {
            GameConfig cfg = GameConfig.get();
            this.rows = cfg.rows();
            this.cols = cfg.cols();
            this.tileSize = cfg.tileSize();
            this.gravity = cfg.gravityCps();
            this.spawnCol = cfg.spawnCol();
            this.players = cfg.players();
            this.music = cfg.isMusicEnabled();
            this.sfx = cfg.isSfxEnabled();
            this.aiP1 = cfg.isAiP1Enabled();
            this.aiP2 = cfg.isAiP2Enabled();
            this.extendMode = cfg.isExtendModeEnabled();
        }

        private void restore() {
            GameConfig cfg = GameConfig.get();
            cfg.setPlayers(players);
            cfg.setRows(rows);
            cfg.setCols(cols);
            cfg.setTileSize(tileSize);
            cfg.setGravityCps(gravity);
            cfg.setSpawnCol(spawnCol);
            cfg.setMusicEnabled(music);
            cfg.setSfxEnabled(sfx);
            cfg.setAiP1Enabled(aiP1);
            cfg.setAiP2Enabled(aiP2);
            cfg.setExtendModeEnabled(extendMode);
        }
    }
}
