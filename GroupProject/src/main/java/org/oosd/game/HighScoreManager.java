package org.oosd.game;

/**
 * Creates logic for the dumy data of top 10 High Scores to display
 * Milestone 1 Dummy Data only
 * Keeps list of high scores in memory
 */

import java.util.ArrayList;
import java.util.List;

public class HighScoreManager {

    public static class HighScoreEntry {
        public final String name;
        public final int score;

        public HighScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    // Dummy Scores
    private final List<HighScoreEntry> highScores = new ArrayList<>();

    public HighScoreManager() {
        scores.add(new HighScoreEntry("Tom", 1200));
        scores.add(new HighScoreEntry("Lara", 1100));
        scores.add(new HighScoreEntry("Sarah", 1000));
        scores.add(new HighScoreEntry("Lisa", 950));
        scores.add(new HighScoreEntry("James", 900));
        scores.add(new HighScoreEntry("Grant", 850));
        scores.add(new HighScoreEntry("Clare", 800));
        scores.add(new HighScoreEntry("Alex", 750));
        scores.add(new HighScoreEntry("Harry", 700));
        scores.add(new HighScoreEntry("Jill", 650));
    }

    // Return the list (read-only)
    public List<HighScoreEntry> getHighScores() {
        return List.copyOf(scores); // immutable copy
    }
}

