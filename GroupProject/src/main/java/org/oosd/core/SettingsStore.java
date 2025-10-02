package org.oosd.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 - Writes to:  {user.home}/.oosd-tetris/config.json
 - Reads values back with small regex helpers.
*/
public final class SettingsStore {
    private SettingsStore() {}

    private static final Path DIR  = Paths.get(System.getProperty("user.home"), ".oosd-tetris");
    private static final Path FILE = DIR.resolve("config.json");

    /* Load settings from disk and apply into cfg (leaves existing values as defaults). */
    public static void loadInto(GameConfig cfg) {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                fromJsonInto(json, cfg);
            }
        } catch (IOException e) {
            // Keep defaults if there's any problem.
            e.printStackTrace();
        }
    }

    /* Save current cfg settings to disk as JSON. */
    public static void save(GameConfig cfg) {
        try {
            if (Files.notExists(DIR)) Files.createDirectories(DIR);
            String json = toJson(cfg);
            Files.writeString(FILE, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ---------- JSON (very small, schema-fixed) ---------- */

    private static String toJson(GameConfig c) {
        // Pretty but simple JSON that our regex reader understands.
        return "{\n" +
                "  \"rows\": "          + c.rows()           + ",\n" +
                "  \"cols\": "          + c.cols()           + ",\n" +
                "  \"tileSize\": "      + c.tileSize()       + ",\n" +
                "  \"gravityCps\": "    + c.gravityCps()     + ",\n" +
                "  \"spawnCol\": "      + c.spawnCol()       + ",\n" +
                "  \"musicEnabled\": "  + c.isMusicEnabled() + ",\n" +
                "  \"sfxEnabled\": "    + c.isSfxEnabled()   + ",\n" +
                "  \"players\": "       + c.players()        + "\n" +
                "}\n";
    }

    private static void fromJsonInto(String json, GameConfig c) {
        c.setRows         (readInt    (json, "rows",          c.rows()));
        c.setCols         (readInt    (json, "cols",          c.cols()));
        c.setTileSize     (readInt    (json, "tileSize",      c.tileSize()));
        c.setGravityCps   (readDouble (json, "gravityCps",    c.gravityCps()));
        c.setSpawnCol     (readInt    (json, "spawnCol",      c.spawnCol()));
        c.setMusicEnabled (readBoolean(json, "musicEnabled",  c.isMusicEnabled()));
        c.setSfxEnabled   (readBoolean(json, "sfxEnabled",    c.isSfxEnabled()));
        c.setPlayers      (readInt    (json, "players",       c.players()));
    }

    /* ---------- tiny helpers ---------- */

    private static int readInt(String s, String key, int def) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    private static double readDouble(String s, String key, double def) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(s);
        return m.find() ? Double.parseDouble(m.group(1)) : def;
    }

    private static boolean readBoolean(String s, String key, boolean def) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)").matcher(s);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : def;
    }
}
