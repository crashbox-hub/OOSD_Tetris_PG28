package org.oosd.core;

public final class GameConfig {
    private static final GameConfig INSTANCE = new GameConfig();

    /* ---------------- Defaults & bounds ---------------- */

    // Single-player board bounds
    public static final int MIN_COLS_1P = 6;
    public static final int MAX_COLS_1P = 25;
    public static final int MIN_ROWS_1P = 15;
    public static final int MAX_ROWS_1P = 20;

    // Two-player recommended bounds (kept a bit tighter so both boards are visible)
    public static final int MIN_COLS_2P = 6;
    public static final int MAX_COLS_2P = 20;  // tighter than 1P to fit two boards
    public static final int MIN_ROWS_2P = 15;
    public static final int MAX_ROWS_2P = 20;

    // Defaults
    private static final int  DEF_ROWS     = 20;
    private static final int  DEF_COLS     = 10;
    private static final int  DEF_TILE     = 30;
    private static final double DEF_GRAV_CPS = 2.0;
    private static final int  DEF_SPAWN_COL = 3;
    private static final boolean DEF_MUSIC  = true;
    private static final boolean DEF_SFX    = true;
    private static final int  DEF_PLAYERS   = 1; // 1 or 2

    /* ---------------- Instance state ---------------- */

    // Board / tiles
    private int rows      = DEF_ROWS;
    private int cols      = DEF_COLS;
    private int tileSize  = DEF_TILE;

    // Gravity (cells per second)
    private double gravityCps = DEF_GRAV_CPS;

    // Piece spawn column (approx centre)
    private int spawnCol = DEF_SPAWN_COL;

    // Audio
    private boolean musicEnabled = DEF_MUSIC;
    private boolean sfxEnabled   = DEF_SFX;

    // Players (1 or 2)
    private int players = DEF_PLAYERS;

    private GameConfig() { }

    public static GameConfig get() { return INSTANCE; }

    /* ---------------- Getters ---------------- */
    public int rows()            { return rows; }
    public int cols()            { return cols; }
    public int tileSize()        { return tileSize; }
    public double gravityCps()   { return gravityCps; }
    public int spawnCol()        { return spawnCol; }
    public boolean isMusicEnabled() { return musicEnabled; }
    public boolean isSfxEnabled()   { return sfxEnabled; }
    public int players()         { return players; }

    /* ---------------- Setters (with clamping) ---------------- */

    public void setRows(int r) {
        rows = clamp(r, currentMinRows(), currentMaxRows());
    }

    public void setCols(int c) {
        cols = clamp(c, currentMinCols(), currentMaxCols());
        // keep spawn column roughly centered & valid
        spawnCol = clamp(spawnCol, 0, Math.max(0, cols - 1));
    }

    public void setTileSize(int s) { tileSize = Math.max(8, s); } // protect against tiny sizes
    public void setGravityCps(double g) { gravityCps = Math.max(0.1, g); }
    public void setSpawnCol(int c) { spawnCol = clamp(c, 0, Math.max(0, cols - 1)); }

    public void setMusicEnabled(boolean enabled) { musicEnabled = enabled; }
    public void setSfxEnabled(boolean enabled)   { sfxEnabled = enabled; }

    /** Only 1 or 2 allowed; also re-clamps board for the chosen mode. */
    public void setPlayers(int p) {
        players = (p == 2) ? 2 : 1;
        // re-clamp board immediately to the active mode's bounds
        rows = clamp(rows, currentMinRows(), currentMaxRows());
        cols = clamp(cols, currentMinCols(), currentMaxCols());
        spawnCol = clamp(spawnCol, 0, Math.max(0, cols - 1));
    }

    /* ---------------- Helpers ---------------- */

    private int currentMinCols() { return (players == 2) ? MIN_COLS_2P : MIN_COLS_1P; }
    private int currentMaxCols() { return (players == 2) ? MAX_COLS_2P : MAX_COLS_1P; }
    private int currentMinRows() { return (players == 2) ? MIN_ROWS_2P : MIN_ROWS_1P; }
    private int currentMaxRows() { return (players == 2) ? MAX_ROWS_2P : MAX_ROWS_1P; }

    private static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }
}
