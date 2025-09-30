package org.oosd.core;

public final class GameConfig {
    private static final GameConfig INSTANCE = new GameConfig();

    // Board / tiles
    private int rows = 20;
    private int cols = 10;
    private int tileSize = 30;

    // Gravity (cells per second) – used by the active piece entity
    private double gravityCps = 2.0;   // fall 2 rows per second (tune as you like)

    // Piece spawn column (approx centre)
    private int spawnCol = 3;

    // === Person 4: AI toggle ===
    private boolean aiEnabled = false;

    private GameConfig() {}

    public static GameConfig get() { return INSTANCE; }

    // getters/setters (expose only what the Config screen may change)
    public int rows() { return rows; }
    public int cols() { return cols; }
    public int tileSize() { return tileSize; }
    public double gravityCps() { return gravityCps; }
    public int spawnCol() { return spawnCol; }

    public void setRows(int r){ rows = r; }
    public void setCols(int c){ cols = c; }
    public void setTileSize(int s){ tileSize = s; }
    public void setGravityCps(double g){ gravityCps = g; }
    public void setSpawnCol(int c){ spawnCol = c; }

    // === Person 4: AI toggle ===
    public boolean aiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean v) { aiEnabled = v; }
}
