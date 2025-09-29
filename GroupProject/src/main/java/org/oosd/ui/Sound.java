package org.oosd.ui;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.oosd.core.GameConfig;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class Sound {
    private Sound() {}

    /* ---------- SFX ---------- */
    private static final Map<String, AudioClip> SFX = new HashMap<>();

    private static AudioClip sfx(String path) {
        return SFX.computeIfAbsent(path, p -> {
            URL url = Sound.class.getResource(p);
            if (url == null) throw new IllegalArgumentException("Missing resource: " + p);
            AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(1.0); // ensure full volume
            return clip;
        });
    }

    public static void playRotate() {
        if (GameConfig.get().isSfxEnabled()) sfx("/audio/sfx_rotate.mp3").play();
    }

    public static void playLine() {
        if (GameConfig.get().isSfxEnabled()) sfx("/audio/sfx_line.mp3").play();
    }

    public static void playGameOver() {
        if (GameConfig.get().isSfxEnabled()) sfx("/audio/sfx_gameover.wav").play();
    }

    /* ---------- BGM ---------- */
    private static MediaPlayer bgm;

    public static void startMenuBgm() {
        if (!GameConfig.get().isMusicEnabled()) return;
        startBgm("/audio/bgm_menu.mp3", 0.35);
    }

    public static void startGameBgm() {
        if (!GameConfig.get().isMusicEnabled()) return;
        startBgm("/audio/bgm_game.mp3", 0.35);
    }

    public static void stopBgm() {
        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
            bgm = null;
        }
    }

    private static void startBgm(String path, double volume) {
        stopBgm();
        URL url = Sound.class.getResource(path);
        if (url == null) throw new IllegalArgumentException("Missing resource: " + path);
        bgm = new MediaPlayer(new Media(url.toExternalForm()));
        bgm.setCycleCount(MediaPlayer.INDEFINITE);
        bgm.setVolume(volume);
        bgm.play();
    }
}
