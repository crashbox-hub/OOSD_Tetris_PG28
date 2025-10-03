package org.oosd.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oosd.core.GameConfig;
import org.oosd.core.SettingsStore;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ConfigurationViewPersistenceTest {

    private static Path tempHome;
    private static Path configDir;
    private static Path configFile;
    private static String previousUserHome;
    private static GameConfigSnapshot originalConfig;

    @BeforeAll
    static void setupAll() throws Exception {
        previousUserHome = System.getProperty("user.home");
        tempHome = Files.createTempDirectory("config-view-test");
        System.setProperty("user.home", tempHome.toString());
        configFile = resolveSettingsStoreFile();
        configDir = configFile.getParent();
        Files.createDirectories(configDir);
        startFxToolkit();
        originalConfig = new GameConfigSnapshot();
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        if (originalConfig != null) {
            originalConfig.restore();
        }
        if (previousUserHome != null) {
            System.setProperty("user.home", previousUserHome);
        }
        deleteRecursively(tempHome);
    }

    @BeforeEach
    void resetState() throws IOException {
        Files.deleteIfExists(configFile);
        Files.createDirectories(configDir);
        resetGameConfigToDefaults();
    }

    @AfterEach
    void restoreAfterTest() {
        resetGameConfigToDefaults();
    }

    @Test
    void loadsConfigurationFromExistingSettingsFile() throws Exception {
        String json = """
                {
                  "rows": 19,
                  "cols": 12,
                  "tileSize": 32,
                  "gravityCps": 3.3,
                  "spawnCol": 4,
                  "musicEnabled": false,
                  "sfxEnabled": true,
                  "players": 2,
                  "aiP1Enabled": true,
                  "aiP2Enabled": false,
                  "extendModeEnabled": true
                }
                """;
        Files.writeString(configFile, json, StandardCharsets.UTF_8);

        ConfigurationView view = createView();

        GameConfig cfg = GameConfig.get();
        Assertions.assertEquals(19, cfg.rows(), "Rows should load from persisted settings");
        Assertions.assertEquals(12, cfg.cols(), "Cols should load from persisted settings");
        Assertions.assertEquals(3.3, cfg.gravityCps(), 1e-6, "Gravity should load from persisted settings");
        Assertions.assertEquals(2, cfg.players(), "Player count should load from persisted settings");
        Assertions.assertTrue(cfg.isAiP1Enabled(), "Player 1 AI flag should load from persisted settings");
        Assertions.assertFalse(cfg.isAiP2Enabled(), "Player 2 AI flag should load from persisted settings");
        Assertions.assertTrue(cfg.isExtendModeEnabled(), "Extend mode should load from persisted settings");

        double widthValue = sliderValue(view, "Field Width (No of cells):");
        double heightValue = sliderValue(view, "Field Height (No of cells):");
        double levelValue = sliderValue(view, "Game Level:");
        boolean extendSelected = checkBoxSelected(view);
        boolean twoPlayersSelected = radioSelected(view, "2 Players");
        boolean onePlayerSelected = radioSelected(view, "1 Player");

        Assertions.assertEquals(12.0, widthValue, 1e-6, "Width slider should reflect persisted cols");
        Assertions.assertEquals(19.0, heightValue, 1e-6, "Height slider should reflect persisted rows");
        Assertions.assertEquals(7.0, levelValue, 1e-6, "Level slider should reflect persisted gravity");
        Assertions.assertTrue(extendSelected, "Extend mode checkbox should match persisted state");
        Assertions.assertTrue(twoPlayersSelected, "Two-player toggle should match persisted state");
        Assertions.assertFalse(onePlayerSelected, "One-player toggle should be deselected when persisted state is two players");
    }

    @Test
    void changingWidthSliderPersistsNewValue() throws Exception {
        ConfigurationView view = createView();

        setSliderValue(view);

        Assertions.assertTrue(Files.exists(configFile), "Saving slider change should create config file");
        Assertions.assertEquals(17, GameConfig.get().cols(), "GameConfig should reflect saved width");
        Assertions.assertEquals(17, readInt(configFile, "cols"), "Persisted JSON should store new width");
    }

    @Test
    void togglingExtendModePersistsState() throws Exception {
        ConfigurationView view = createView();

        setCheckBox(view);

        Assertions.assertTrue(GameConfig.get().isExtendModeEnabled(), "GameConfig should update extend mode");
        Assertions.assertTrue(readBoolean(configFile), "Persisted JSON should store extend mode flag");
    }

    @Test
    void selectingTwoPlayersPersistsPlayerCount() throws Exception {
        ConfigurationView view = createView();

        selectRadio(view);

        Assertions.assertEquals(2, GameConfig.get().players(), "GameConfig should update player count");
        Assertions.assertEquals(2, readInt(configFile, "players"), "Persisted JSON should store player count");
        Assertions.assertTrue(radioSelected(view, "2 Players"), "Two-player radio should remain selected");
    }

    private static ConfigurationView createView() throws Exception {
        return runOnFxThread(() -> new ConfigurationView(() -> {}));
    }

    private static double sliderValue(ConfigurationView view, String labelText) throws Exception {
        return runOnFxThread(() -> findSlider(view, labelText).getValue());
    }

    private static void setSliderValue(ConfigurationView view) throws Exception {
        runOnFxThread(() -> {
            Slider slider = findSlider(view, "Field Width (No of cells):");
            slider.setValue(17);
            return null;
        });
    }

    private static boolean checkBoxSelected(ConfigurationView view) throws Exception {
        return runOnFxThread(() -> findCheckBox(view).isSelected());
    }

    private static void setCheckBox(ConfigurationView view) throws Exception {
        runOnFxThread(() -> {
            CheckBox box = findCheckBox(view);
            box.setSelected(true);
            return null;
        });
    }

    private static boolean radioSelected(ConfigurationView view, String buttonText) throws Exception {
        return runOnFxThread(() -> findRadioButton(view, buttonText).isSelected());
    }

    private static void selectRadio(ConfigurationView view) throws Exception {
        runOnFxThread(() -> {
            RadioButton button = findRadioButton(view, "2 Players");
            button.setSelected(true);
            return null;
        });
    }

    private static Slider findSlider(ConfigurationView view, String labelText) {
        GridPane grid = extractGrid(view);
        int row = rowIndexForLabel(grid, labelText);
        Node cell = nodeInCell(grid, row, Node.class);
        if (cell instanceof Slider slider) {
            return slider;
        }
        if (cell instanceof HBox box) {
            for (Node child : box.getChildren()) {
                if (child instanceof Slider sliderChild) {
                    return sliderChild;
                }
            }
        }
        throw new IllegalStateException("Missing slider for label: " + labelText);
    }
    private static CheckBox findCheckBox(ConfigurationView view) {
        GridPane grid = extractGrid(view);
        int row = rowIndexForLabel(grid, "Extend Mode (On/Off):");
        return nodeInCell(grid, row, CheckBox.class);
    }

    private static RadioButton findRadioButton(ConfigurationView view, String buttonText) {
        GridPane grid = extractGrid(view);
        int row = rowIndexForLabel(grid, "Players:");
        HBox playersBox = nodeInCell(grid, row, HBox.class);
        for (Node child : playersBox.getChildren()) {
            if (child instanceof RadioButton radio && buttonText.equals(radio.getText())) {
                return radio;
            }
        }
        throw new IllegalStateException("Missing radio button with text: " + buttonText);
    }

    private static GridPane extractGrid(ConfigurationView view) {
        if (view.getChildren().isEmpty()) {
            throw new IllegalStateException("ConfigurationView has no content");
        }
        StackPane background = (StackPane) view.getChildren().getFirst();
        if (background.getChildren().isEmpty()) {
            throw new IllegalStateException("Background stack pane is empty");
        }
        VBox panel = (VBox) background.getChildren().getFirst();
        for (Node child : panel.getChildren()) {
            if (child instanceof GridPane grid) {
                return grid;
            }
        }
        throw new IllegalStateException("Configuration grid not found");
    }

    private static int rowIndexForLabel(GridPane grid, String labelText) {
        for (Node child : grid.getChildren()) {
            if (child instanceof Label label && labelText.equals(label.getText())) {
                return rowIndex(child);
            }
        }
        throw new IllegalStateException("Missing label with text: " + labelText);
    }

    private static int rowIndex(Node node) {
        Integer row = GridPane.getRowIndex(node);
        return row != null ? row : 0;
    }

    private static <T> T nodeInCell(GridPane grid, int row, Class<T> type) {
        for (Node child : grid.getChildren()) {
            if (rowIndex(child) == row) {
                Integer col = GridPane.getColumnIndex(child);
                int colIndex = col != null ? col : 0;
                if (colIndex == 1 && type.isInstance(child)) {
                    return type.cast(child);
                }
            }
        }
        throw new IllegalStateException("Missing node in grid cell [" + row + "," + 1 + "] of type " + type.getSimpleName());
    }

    private static int readInt(Path file, String key) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Key '" + key + "' not found in settings JSON");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static boolean readBoolean(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"" + Pattern.quote("extendModeEnabled") + "\"\\s*:\\s*(true|false)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Key '" + "extendModeEnabled" + "' not found in settings JSON");
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private static void resetGameConfigToDefaults() {
        GameConfig cfg = GameConfig.get();
        cfg.setPlayers(1);
        cfg.setRows(20);
        cfg.setCols(10);
        cfg.setTileSize(30);
        cfg.setGravityCps(2.0);
        cfg.setSpawnCol(3);
        cfg.setMusicEnabled(false);
        cfg.setSfxEnabled(false);
        cfg.setAiP1Enabled(false);
        cfg.setAiP2Enabled(false);
        cfg.setExtendModeEnabled(false);
    }

    private static Path resolveSettingsStoreFile() throws Exception {
        Field field = SettingsStore.class.getDeclaredField("FILE");
        field.setAccessible(true);
        Path file = (Path) field.get(null);
        Assumptions.assumeTrue(file.startsWith(tempHome), "SettingsStore FILE path must reside in test temp home");
        return file;
    }

    private static void startFxToolkit() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                Assertions.fail("Timed out starting JavaFX platform");
            }
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit already running; ignore.
        }
    }

    private static <T> T runOnFxThread(Callable<T> task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                resultRef.set(task.call());
            } catch (Throwable throwable) {
                errorRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            Assertions.fail("Timed out waiting for task on FX thread");
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

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best-effort cleanup for temp directory.
                    }
                });
    }

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


