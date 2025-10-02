package org.oosd.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Persists high scores in JSON to: {user.home}/.oosd-tetris/highscores.json
Format:
    {
       "scores": [ {"name": "AAA", "score": 12000}, ... ]
     }
 */

public final class HighScoreStore {
    private HighScoreStore() {}

    private static final Path DIR  = Paths.get(System.getProperty("user.home"), ".oosd-tetris");
    private static final Path FILE = DIR.resolve("highscores.json");
    private static final int MAX_SCORES = 10;


    /** Ensure directory and an empty highscores file exist. */
    public static void initIfMissing() {
        try {
            if (Files.notExists(DIR)) Files.createDirectories(DIR);
            if (Files.notExists(FILE)) {
                Files.writeString(FILE, "{\n  \"scores\": []\n}\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Represents a single entry */
        public record Entry(String name, int score) {
    }

    /** Load high scores (sorted descending). */
    public static List<Entry> load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                return fromJson(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(); // empty if no file
    }

    /** Save high scores (clamped to MAX_SCORES). */
    public static void save(List<Entry> scores) {
        try {
            if (Files.notExists(DIR)) Files.createDirectories(DIR);
            // Clamp to max entries and sort
            scores.sort((a, b) -> Integer.compare(b.score, a.score));
            if (scores.size() > MAX_SCORES) scores = scores.subList(0, MAX_SCORES);
            String json = toJson(scores);
            Files.writeString(FILE, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Add a new score (auto-loads, inserts, saves). */
    public static void addScore(String name, int score) {
        // Prevent adding 0 or negative scores
        if (score <= 0) return;

        List<Entry> scores = load();
        scores.add(new Entry(name, score));
        save(scores);
    }


    /* ---------- JSON helpers ---------- */

    private static String toJson(List<Entry> scores) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"scores\": [\n");
        for (int i = 0; i < scores.size(); i++) {
            Entry e = scores.get(i);
            sb.append("    {\"name\": \"").append(e.name).append("\", \"score\": ").append(e.score).append("}");
            if (i < scores.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static List<Entry> fromJson(String json) {
        List<Entry> list = new ArrayList<>();
        Pattern p = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"score\"\\s*:\\s*(\\d+)\\s*}");
        Matcher m = p.matcher(json);
        while (m.find()) {
            list.add(new Entry(m.group(1), Integer.parseInt(m.group(2))));
        }
        list.sort((a, b) -> Integer.compare(b.score, a.score));
        if (list.size() > MAX_SCORES) list = list.subList(0, MAX_SCORES);
        return list;
    }
}
